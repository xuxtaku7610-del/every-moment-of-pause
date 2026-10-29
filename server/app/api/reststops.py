"""POST /reststops/along-route, GET /reststops/{id} — api_spec §2.3·2.4.

경로 좌표는 계산에만 쓰고 저장하지 않는다 (architecture §6).
"""
from collections import defaultdict
from datetime import timedelta, timezone
from typing import Literal

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_session, require_device
from app.errors import ApiError
from app.geo import heading_toward, length, project
from app.models import RestStop, RestStopFacility

router = APIRouter(dependencies=[Depends(require_device)])
KST = timezone(timedelta(hours=9))
StopType = Literal["service_area", "drowsy_shelter"]


class Point(BaseModel):
    lat: float = Field(ge=-90, le=90)
    lng: float = Field(ge=-180, le=180)


class AlongRouteIn(BaseModel):
    points: list[Point]
    corridorMeters: int = Field(default=1500, ge=100, le=10_000)
    types: list[StopType] = Field(default_factory=lambda: ["service_area", "drowsy_shelter"])
    # 내비 총거리(m). 주면 직선 좌표열 거리를 이 척도로 늘려 distanceAlongRouteKm을 실제 도로 거리에 가깝게 한다.
    routeLengthMeters: int | None = Field(default=None, gt=0)


def _facilities(session: Session, ids: list[str]) -> dict[str, dict[str, int]]:
    out: dict[str, dict[str, int]] = defaultdict(dict)
    if ids:
        for f in session.scalars(select(RestStopFacility).where(RestStopFacility.rest_stop_id.in_(ids))):
            out[f.rest_stop_id][f.facility_code] = f.quantity
    return out


def _item(r: RestStop, fac: dict[str, int]) -> dict:
    return {
        "restStopId": r.id,
        "name": r.name,
        "type": r.stop_type,
        "routeName": r.route_name,
        "direction": r.direction,
        "lat": r.lat,
        "lng": r.lng,
        "facilities": sorted(c for c, q in fac.items() if q > 0),
        "evChargers": {"fast": fac.get("ev_fast", 0), "slow": fac.get("ev_slow", 0)},
        "updatedAt": r.updated_at.astimezone(KST).isoformat(timespec="seconds") if r.updated_at else None,
    }


@router.post("/reststops/along-route")
def along_route(body: AlongRouteIn, session: Session = Depends(get_session)) -> dict:
    if len(body.points) < 2:
        raise ApiError(400, "INVALID_REQUEST", "경로 좌표가 2개 이상 필요합니다.", {"points": len(body.points)})
    pts = [(p.lat, p.lng) for p in body.points]
    # 경로를 감싸는 사각형(여유 0.1도 ≈ 11km)으로 먼저 거른 뒤 정밀 계산한다.
    pad = 0.1
    lats, lngs = [p[0] for p in pts], [p[1] for p in pts]
    rows = session.scalars(
        select(RestStop).where(
            RestStop.stop_type.in_(body.types),
            RestStop.lat.between(min(lats) - pad, max(lats) + pad),
            RestStop.lng.between(min(lngs) - pad, max(lngs) + pad),
        )
    ).all()
    geo = length(pts)
    scale = body.routeLengthMeters / geo if body.routeLengthMeters and geo > 0 else 1.0
    hits = []
    for r in rows:
        proj = project(pts, r.lat, r.lng)
        # 반대 방향 휴게소 제외 (방향 종점을 모르면 남긴다)
        if proj.offset_m <= body.corridorMeters and not proj.behind and heading_toward(pts, r.direction) is not False:
            hits.append((proj.along_m * scale, r))
    hits.sort(key=lambda h: h[0])
    fac = _facilities(session, [r.id for _, r in hits])
    items = [{**_item(r, fac.get(r.id, {})), "distanceAlongRouteKm": round(along / 1000, 1)} for along, r in hits]
    return {"items": items, "count": len(items)}


@router.get("/reststops/{rest_stop_id}")
def detail(rest_stop_id: str, session: Session = Depends(get_session)) -> dict:
    r = session.get(RestStop, rest_stop_id)
    if r is None:
        raise ApiError(404, "REST_STOP_NOT_FOUND", "해당 휴게소를 찾을 수 없습니다.", {"restStopId": rest_stop_id})
    fac = _facilities(session, [r.id]).get(r.id, {})
    # themes: 테마휴게소 정보는 도로공사 API 연동 후 채운다.
    return {**_item(r, fac), "phone": r.phone, "address": r.address, "themes": []}
