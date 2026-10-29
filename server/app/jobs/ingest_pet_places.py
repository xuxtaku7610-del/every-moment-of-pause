"""반려동물 동반 장소 수집 잡 (architecture §3.2 jobs/ingest_pet_places, CONCEPT F4).

한국관광공사 TourAPI "반려동물 동반여행" 서비스 → 정규화 → pet_places.
키는 환경 변수 TOUR_API_KEY(server/.env)로만 받는다. 비어 있으면 외부 호출 없이 app/data/sample_pet_places.json을
같은 정규화 경로로 적재하고 로그에 source=sample을 남긴다. 수집이 실패하면 기존 데이터를 그대로 둔다.

실행: python -m app.jobs.ingest_pet_places
"""
import json
import logging
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from sqlalchemy import Engine, select
from sqlalchemy.orm import Session

from app.config import Settings, get_settings
from app.models import PLACE_CATEGORIES, PetPlace

log = logging.getLogger(__name__)
SAMPLE_PATH = Path(__file__).resolve().parent.parent / "data" / "sample_pet_places.json"

# TODO(tour-api): 키 발급 후 실제 엔드포인트·필드를 확인한다. 아래는 TourAPI 국문 서비스(KorService)와 같은 응답 형식을 가정한다.
TOUR_BASE = "https://apis.data.go.kr/B551011/KorPetTourService"
TOUR_AREAS = ("1", "31", "32")  # 서울, 경기, 강원 (시연 경로)
TOUR_PAGE_ROWS = 100
CAFE_CAT3 = "A05020900"  # TourAPI 분류: 카페/전통찻집


@dataclass
class NormalizedPlace:
    source_id: str
    source: str  # tourapi | sample
    name: str
    category: str
    lat: float
    lng: float
    address: str | None
    pet_policy: str | None

    @property
    def id(self) -> str:
        prefix = "pp" if self.source == "tourapi" else "ps"
        return f"{prefix}_{self.source_id.lower().replace('-', '')[:28]}"


@dataclass
class IngestResult:
    source: str
    ok: bool
    inserted: int = 0
    updated: int = 0
    deleted: int = 0


def category_of(content_type: str, cat1: str | None, cat3: str | None) -> str | None:
    """TourAPI 콘텐츠 타입 → 같이가개 분류. 모르는 타입은 None(버림)."""
    if content_type == "39":
        return "cafe" if cat3 == CAFE_CAT3 else "restaurant"
    if content_type == "32":
        return "lodging"
    if content_type == "12":
        return "walk" if cat1 == "A01" else "tour"  # A01 = 자연(공원·산책로)
    if content_type in ("14", "28", "38"):
        return "tour"
    return None


def _items(payload: dict[str, Any]) -> list[dict[str, Any]]:
    """TourAPI 응답의 item 목록. 결과가 없으면 items가 빈 문자열, 하나면 dict로 온다."""
    items = ((payload.get("response") or {}).get("body") or {}).get("items")
    if not items:
        return []
    item = items.get("item") if isinstance(items, dict) else None
    if isinstance(item, dict):
        return [item]
    return list(item or [])


def normalize_tour(payloads: list[dict[str, Any]]) -> list[NormalizedPlace]:
    out: dict[str, NormalizedPlace] = {}
    for p in payloads:
        for it in _items(p):
            try:
                lat, lng = float(it["mapy"]), float(it["mapx"])
                cid, title = str(it["contentid"]).strip(), str(it["title"]).strip()
            except (KeyError, TypeError, ValueError):
                continue
            cat = category_of(str(it.get("contenttypeid", "")), it.get("cat1"), it.get("cat3"))
            if not cid or not title or cat is None or not (33 <= lat <= 39 and 124 <= lng <= 132):
                continue
            addr = " ".join(x for x in (it.get("addr1"), it.get("addr2")) if x) or None
            policy = it.get("acmpyPsblCpam") or it.get("acmpyNeedMtr") or None  # detailPetTour 필드(있으면)
            out[cid] = NormalizedPlace(cid, "tourapi", title, cat, lat, lng, addr, policy)
    return list(out.values())


def normalize_sample(payload: dict[str, Any]) -> list[NormalizedPlace]:
    out = []
    for it in payload.get("items", []):
        if it.get("category") not in PLACE_CATEGORIES:
            continue
        out.append(NormalizedPlace(it["sourceId"], "sample", it["name"], it["category"], float(it["lat"]), float(it["lng"]),
                                   it.get("address"), it.get("petPolicy")))
    return out


def fetch_tour(api_key: str, get: Callable[[str, dict[str, Any]], dict[str, Any]] | None = None) -> list[dict[str, Any]]:
    """지역별·페이지별로 areaBasedList를 부른다. [get]은 테스트에서 바꿔 끼운다."""
    if get is None:
        import httpx

        def get(url: str, params: dict[str, Any]) -> dict[str, Any]:
            r = httpx.get(url, params=params, timeout=10)
            r.raise_for_status()
            return r.json()

    pages = []
    for area in TOUR_AREAS:
        page = 1
        while True:
            params = {"serviceKey": api_key, "MobileOS": "ETC", "MobileApp": "GatchiGaGae", "_type": "json",
                      "numOfRows": TOUR_PAGE_ROWS, "pageNo": page, "areaCode": area}
            data = get(f"{TOUR_BASE}/areaBasedList", params)
            pages.append(data)
            total = int(((data.get("response") or {}).get("body") or {}).get("totalCount") or 0)
            if page * TOUR_PAGE_ROWS >= total:
                break
            page += 1
    return pages


def load_sample(path: Path = SAMPLE_PATH) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def apply(session: Session, places: list[NormalizedPlace], source: str) -> IngestResult:
    """같은 출처의 행만 바꾼다: 신규 추가, 변경 반영, 목록에서 빠진 것 삭제."""
    result = IngestResult(source=source, ok=True)
    existing = {r.source_id: r for r in session.scalars(select(PetPlace).where(PetPlace.source == source))}
    for p in places:
        row = existing.pop(p.source_id, None)
        values = dict(source=p.source, name=p.name, category=p.category, lat=p.lat, lng=p.lng, address=p.address, pet_policy=p.pet_policy)
        if row is None:
            session.add(PetPlace(id=p.id, source_id=p.source_id, **values))
            result.inserted += 1
        else:
            if any(getattr(row, k) != v for k, v in values.items()):
                result.updated += 1
            for k, v in values.items():
                setattr(row, k, v)
    for row in existing.values():
        session.delete(row)
        result.deleted += 1
    return result


def ingest(engine: Engine, settings: Settings | None = None,
           fetch: Callable[[str], list[dict[str, Any]]] = fetch_tour,
           sample: Callable[[], dict[str, Any]] = load_sample) -> IngestResult:
    settings = settings or get_settings()
    source = "tourapi" if settings.tour_api_key else "sample"
    try:
        places = normalize_tour(fetch(settings.tour_api_key)) if source == "tourapi" else normalize_sample(sample())
    except Exception as e:  # noqa: BLE001 - 수집 실패는 기존 데이터 유지 (키·응답은 로그에 남기지 않는다)
        log.warning("pet places ingest source=%s failed, keeping existing data: %s", source, type(e).__name__)
        return IngestResult(source=source, ok=False)
    with Session(engine) as session, session.begin():
        result = apply(session, places, source)
    log.info("pet places ingest source=%s inserted=%d updated=%d deleted=%d", source, result.inserted, result.updated, result.deleted)
    return result


if __name__ == "__main__":
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    ingest(make_engine(get_settings().database_url))
