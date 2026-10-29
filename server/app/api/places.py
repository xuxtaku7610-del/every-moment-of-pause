"""POST /places/along-route, GET /places/near — api_spec §2.5·2.6 (CONCEPT F4).

경로·현재 위치 좌표는 계산에만 쓰고 저장하지 않는다 (architecture §6).
"""
from typing import Literal

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_session, require_device
from app.api.reststops import Point
from app.errors import ApiError
from app.geo import distance_m, length, project
from app.models import PLACE_CATEGORIES, PetPlace

router = APIRouter(dependencies=[Depends(require_device)])
Category = Literal["restaurant", "cafe", "lodging", "tour", "walk"]
DEG_PER_M = 1 / 111_000


class AlongRouteIn(BaseModel):
    points: list[Point]
    bufferMeters: int = Field(default=5000, ge=500, le=20_000)
    categories: list[Category] = Field(default_factory=lambda: list(PLACE_CATEGORIES))
    routeLengthMeters: int | None = Field(default=None, gt=0)
    limit: int = Field(default=30, ge=1, le=100)


def item(p: PetPlace) -> dict:
    return {
        "placeId": p.id,
        "name": p.name,
        "category": p.category,
        "lat": p.lat,
        "lng": p.lng,
        "address": p.address,
        "petPolicy": p.pet_policy,
        "imageUrl": p.image_url,
        "spaceNote": p.space_note,
        "sizeNote": p.size_note,
        "companion": p.companion,
        "sample": p.source == "sample",
        "source": p.source,
    }


@router.post("/places/along-route")
def along_route(body: AlongRouteIn, session: Session = Depends(get_session)) -> dict:
    if len(body.points) < 2:
        raise ApiError(400, "INVALID_REQUEST", "경로 좌표가 2개 이상 필요합니다.", {"points": len(body.points)})
    pts = [(p.lat, p.lng) for p in body.points]
    pad = body.bufferMeters * DEG_PER_M * 1.5
    lats, lngs = [p[0] for p in pts], [p[1] for p in pts]
    rows = session.scalars(
        select(PetPlace).where(
            PetPlace.category.in_(body.categories),
            PetPlace.lat.between(min(lats) - pad, max(lats) + pad),
            PetPlace.lng.between(min(lngs) - pad, max(lngs) + pad),
        )
    ).all()
    geo = length(pts)
    scale = body.routeLengthMeters / geo if body.routeLengthMeters and geo > 0 else 1.0
    hits = []
    for r in rows:
        proj = project(pts, r.lat, r.lng)
        if proj.offset_m <= body.bufferMeters and not proj.behind:
            hits.append((proj.along_m * scale, proj.offset_m, r))
    hits.sort(key=lambda h: h[0])
    items = [{**item(r), "distanceAlongRouteKm": round(a / 1000, 1), "offRouteKm": round(o / 1000, 1)} for a, o, r in hits[: body.limit]]
    return {"items": items, "count": len(items)}


@router.get("/places/near")
def near(
    lat: float = Query(ge=-90, le=90),
    lng: float = Query(ge=-180, le=180),
    radiusMeters: int = Query(default=3000, ge=100, le=10_000),
    categories: str = Query(default="walk,cafe"),
    limit: int = Query(default=10, ge=1, le=50),
    session: Session = Depends(get_session),
) -> dict:
    cats = [c.strip() for c in categories.split(",") if c.strip()]
    bad = [c for c in cats if c not in PLACE_CATEGORIES]
    if bad or not cats:
        raise ApiError(422, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", {"fields": ["categories"]})
    pad = radiusMeters * DEG_PER_M * 1.5
    rows = session.scalars(
        select(PetPlace).where(PetPlace.category.in_(cats), PetPlace.lat.between(lat - pad, lat + pad), PetPlace.lng.between(lng - pad * 1.3, lng + pad * 1.3))
    ).all()
    hits = sorted(((distance_m((lat, lng), (r.lat, r.lng)), r) for r in rows), key=lambda h: h[0])
    items = [{**item(r), "distanceKm": round(d / 1000, 1)} for d, r in hits if d <= radiusMeters][:limit]
    return {"items": items, "count": len(items)}
