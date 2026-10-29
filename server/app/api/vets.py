"""GET /vets/nearby — api_spec §2.7 (CONCEPT F5). 현재 위치는 계산에만 쓰고 저장하지 않는다."""
from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_session, require_device
from app.geo import distance_m
from app.models import VetClinic

router = APIRouter(dependencies=[Depends(require_device)])
# 가까운 곳이 적을 때를 대비해 넓혀 가며 찾는 반경(도). 0.5도 ≈ 55km.
SEARCH_PADS = (0.05, 0.15, 0.5)


@router.get("/vets/nearby")
def nearby(
    lat: float = Query(ge=-90, le=90),
    lng: float = Query(ge=-180, le=180),
    limit: int = Query(default=5, ge=1, le=20),
    session: Session = Depends(get_session),
) -> dict:
    rows: list[VetClinic] = []
    for pad in SEARCH_PADS:
        rows = list(session.scalars(
            select(VetClinic).where(VetClinic.open.is_(True), VetClinic.lat.between(lat - pad, lat + pad), VetClinic.lng.between(lng - pad * 1.3, lng + pad * 1.3))
        ))
        if len(rows) >= limit:
            break
    hits = sorted(((distance_m((lat, lng), (r.lat, r.lng)), r) for r in rows), key=lambda h: h[0])[:limit]
    items = [
        {
            "vetId": r.id, "name": r.name, "address": r.address, "lat": r.lat, "lng": r.lng,
            "distanceKm": round(d / 1000, 1), "phone": r.phone, "sample": r.source == "sample", "source": r.source,
        }
        for d, r in hits
    ]
    return {"items": items, "count": len(items)}
