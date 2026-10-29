"""반려동물 동반 장소 수집 잡 (architecture §3.2 jobs/ingest_pet_places, CONCEPT F4).

한국관광공사 TourAPI "반려동물 동반여행" 서비스(KorPetTourService2) → 정규화 → pet_places.
- 목록: areaBasedList2 (지역별), 동반 조건: detailPetTour2 (장소마다 1건). 2026-10-30 실제 응답으로 확인
  (tests/fixtures/tour_*.json, 개인정보 없는 일부).
- 호출 수 제한(개발계정 하루 1,000건): 응답을 server/data/cache/tourapi/에 7일 캐시하고, 한 번 실행에 최대 CALL_BUDGET건만 부른다.
키는 환경 변수 TOUR_API_KEY(server/.env)로만 받는다. 비어 있으면 외부 호출 없이 app/data/sample_pet_places.json을
같은 정규화 경로로 적재하고 로그에 source=sample을 남긴다. 수집이 실패하면 기존 데이터를 그대로 둔다.

실행: python -m app.jobs.ingest_pet_places
"""
import hashlib
import json
import logging
import re
import time
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

TOUR_BASE = "https://apis.data.go.kr/B551011/KorPetTourService2"
TOUR_AREAS = ("32",)  # 강원 (강릉·횡성·평창 — 시연 경로 영동선 구간). 2026-10 기준 69곳
TOUR_PAGE_ROWS = 100
CACHE_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "cache" / "tourapi"
CACHE_TTL_SEC = 7 * 24 * 3600
CALL_BUDGET = 300
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
    image_url: str | None = None
    indoor: str | None = None  # indoor | outdoor | both | None(모름)
    large_dog: bool | None = None  # None = 정보 없음
    companion: str | None = None  # 동반 구분 원문 ("전체 동반가능", "일부구역 동반가능" …)

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


def _text(d: dict[str, Any] | None, *keys: str) -> str:
    return " ".join(str(d.get(k) or "") for k in keys) if d else ""


_INDOOR_NO = re.compile(r"실내[^.\n,/]{0,16}(불가|제한|금지|어려)")
_OUTDOOR_NO = re.compile(r"(실외|야외)[^.\n,/]{0,12}(불가|제한|금지)")
_LARGE_NO = re.compile(r"대형견[^.\n,/]{0,12}(불가|제한|금지|어려)|소형견만")
_KG_LIMIT = re.compile(r"(\d+)\s*(?:kg|㎏|키로)\s*(?:이하|미만)", re.I)
LARGE_DOG_KG = 25  # 이 무게 미만까지만 받으면 대형견은 안 되는 것으로 본다
_LARGE_YES = re.compile(r"대형견|견종\s*무관|크기\s*무관|모든\s*견종|전\s*견종")


def indoor_of(detail: dict[str, Any] | None) -> str | None:
    """동반 조건 글에서 실내/실외. 글에 없으면 None(지어내지 않는다)."""
    t = _text(detail, "acmpyTypeCd", "acmpyPsblCpam", "acmpyNeedMtr", "etcAcmpyInfo", "relaPosesFclty")
    inside = "실내" in t and not _INDOOR_NO.search(t)
    outside = any(w in t for w in ("실외", "야외", "테라스")) and not _OUTDOOR_NO.search(t)
    if _INDOOR_NO.search(t):
        outside = True  # "실내 동반 불가" → 실외만
    if inside and outside:
        return "both"
    return "indoor" if inside else "outdoor" if outside else None


def large_dog_of(detail: dict[str, Any] | None) -> bool | None:
    """대형견 가능 여부. "대형견 불가"·"소형견만"·"10kg 이하" → False, "30kg 이하"·"전 견종" → True, 소형·중형만 언급 → False, 없으면 None."""
    t = _text(detail, "acmpyPsblCpam", "acmpyNeedMtr", "etcAcmpyInfo")
    if not t.strip():
        return None
    if _LARGE_NO.search(t):
        return False
    limits = [int(m.group(1)) for m in _KG_LIMIT.finditer(t)]
    if limits:
        return min(limits) >= LARGE_DOG_KG
    if _LARGE_YES.search(t):
        return True
    if "소형견" in t or "중형견" in t:
        return False
    return None


def policy_of(detail: dict[str, Any] | None) -> str | None:
    """화면에 보일 동반 조건 한 줄: 가능 동물 → 필요 사항 순, 160자."""
    if not detail:
        return None
    parts = [str(detail.get(k) or "").strip() for k in ("acmpyPsblCpam", "acmpyNeedMtr")]
    line = " · ".join(" ".join(p.split()) for p in parts if p)
    return line[:160] or None


def https(url: str | None) -> str | None:
    """대표 이미지(tong.visitkorea.or.kr)는 http로 오지만 https도 된다. 앱은 cleartext를 막으므로 https로."""
    if not url:
        return None
    return "https://" + url.split("://", 1)[1] if url.startswith("http://") else url


def normalize_tour(payloads: list[dict[str, Any]], details: dict[str, dict[str, Any]] | None = None) -> list[NormalizedPlace]:
    """[payloads] = areaBasedList2 응답들, [details] = contentid → detailPetTour2 item."""
    details = details or {}
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
            d = details.get(cid)
            out[cid] = NormalizedPlace(
                cid, "tourapi", title, cat, lat, lng, addr, policy_of(d),
                image_url=https(it.get("firstimage") or it.get("firstimage2")),
                indoor=indoor_of(d), large_dog=large_dog_of(d),
                companion=(str(d.get("acmpyTypeCd") or "").strip() or None) if d else None,
            )
    return list(out.values())


def normalize_sample(payload: dict[str, Any]) -> list[NormalizedPlace]:
    out = []
    for it in payload.get("items", []):
        if it.get("category") not in PLACE_CATEGORIES:
            continue
        out.append(NormalizedPlace(it["sourceId"], "sample", it["name"], it["category"], float(it["lat"]), float(it["lng"]),
                                   it.get("address"), it.get("petPolicy"), indoor=it.get("indoor"), large_dog=it.get("largeDog")))
    return out


class BudgetExceeded(Exception):
    pass


class TourCache:
    """응답 파일 캐시 (serviceKey를 뺀 파라미터로 이름을 만든다). [budget]건을 넘게 부르면 BudgetExceeded."""

    def __init__(self, get: Callable[[str, dict[str, Any]], dict[str, Any]], directory: Path | None = CACHE_DIR,
                 ttl_sec: int = CACHE_TTL_SEC, budget: int = CALL_BUDGET, now: Callable[[], float] = time.time):
        self.get, self.dir, self.ttl, self.budget, self.now = get, directory, ttl_sec, budget, now
        self.calls = 0
        self.hits = 0

    def _path(self, op: str, params: dict[str, Any]) -> Path | None:
        if self.dir is None:
            return None
        key = "&".join(f"{k}={params[k]}" for k in sorted(params) if k != "serviceKey")
        return self.dir / f"{op}_{hashlib.sha1(key.encode()).hexdigest()[:16]}.json"

    def __call__(self, op: str, params: dict[str, Any]) -> dict[str, Any]:
        path = self._path(op, params)
        if path is not None and path.exists() and self.now() - path.stat().st_mtime < self.ttl:
            self.hits += 1
            return json.loads(path.read_text(encoding="utf-8"))
        if self.calls >= self.budget:
            raise BudgetExceeded(op)
        self.calls += 1
        data = self.get(f"{TOUR_BASE}/{op}", params)
        if path is not None:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        return data


def _http_get(url: str, params: dict[str, Any]) -> dict[str, Any]:
    import httpx

    # httpx INFO 로그는 요청 URL(serviceKey 포함)을 그대로 찍는다 → 키가 로그에 남지 않게 끈다.
    logging.getLogger("httpx").setLevel(logging.WARNING)
    r = httpx.get(url, params=params, timeout=15)
    r.raise_for_status()
    return r.json()


def fetch_tour(api_key: str, get: Callable[[str, dict[str, Any]], dict[str, Any]] | None = None,
               cache_dir: Path | None = CACHE_DIR, budget: int = CALL_BUDGET) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    """지역별·페이지별 areaBasedList2 + 장소마다 detailPetTour2. [get]을 바꿔 끼우면(테스트) 캐시를 쓰지 않는다."""
    cache = TourCache(get or _http_get, cache_dir if get is None else None, budget=budget)
    base = {"serviceKey": api_key, "MobileOS": "ETC", "MobileApp": "GatchiGaGae", "_type": "json"}
    pages = []
    for area in TOUR_AREAS:
        page = 1
        while True:
            data = cache("areaBasedList2", {**base, "numOfRows": TOUR_PAGE_ROWS, "pageNo": page, "areaCode": area})
            pages.append(data)
            total = int(((data.get("response") or {}).get("body") or {}).get("totalCount") or 0)
            if page * TOUR_PAGE_ROWS >= total:
                break
            page += 1
    details: dict[str, dict[str, Any]] = {}
    for it in (i for p in pages for i in _items(p)):
        cid = str(it.get("contentid") or "").strip()
        if not cid or cid in details:
            continue
        try:
            got = _items(cache("detailPetTour2", {**base, "contentId": cid}))
        except BudgetExceeded:
            log.warning("tour api call budget %d reached; remaining details next run", budget)
            break
        if got:
            details[cid] = got[0]
    log.info("tour api calls=%d cache_hits=%d", cache.calls, cache.hits)
    return pages, details


def load_sample(path: Path = SAMPLE_PATH) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def apply(session: Session, places: list[NormalizedPlace], source: str) -> IngestResult:
    """같은 출처의 행만 바꾼다: 신규 추가, 변경 반영, 목록에서 빠진 것 삭제."""
    result = IngestResult(source=source, ok=True)
    existing = {r.source_id: r for r in session.scalars(select(PetPlace).where(PetPlace.source == source))}
    for p in places:
        row = existing.pop(p.source_id, None)
        values = dict(source=p.source, name=p.name, category=p.category, lat=p.lat, lng=p.lng, address=p.address, pet_policy=p.pet_policy,
                      image_url=p.image_url, indoor=p.indoor, large_dog=p.large_dog, companion=p.companion)
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
           fetch: Callable[[str], tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]] = fetch_tour,
           sample: Callable[[], dict[str, Any]] = load_sample) -> IngestResult:
    settings = settings or get_settings()
    source = "tourapi" if settings.tour_api_key else "sample"
    try:
        places = normalize_tour(*fetch(settings.tour_api_key)) if source == "tourapi" else normalize_sample(sample())
    except Exception as e:  # noqa: BLE001 - 수집 실패는 기존 데이터 유지 (키·응답은 로그에 남기지 않는다)
        log.warning("pet places ingest source=%s failed, keeping existing data: %s", source, type(e).__name__)
        return IngestResult(source=source, ok=False)
    with Session(engine) as session, session.begin():
        result = apply(session, places, source)
        if source == "tourapi" and places:
            # 실제 데이터가 들어오면 샘플("[샘플]")은 내린다. 키를 빼면 다음 실행에서 샘플이 다시 들어온다.
            for row in session.scalars(select(PetPlace).where(PetPlace.source == "sample")):
                session.delete(row)
                result.deleted += 1
    log.info("pet places ingest source=%s inserted=%d updated=%d deleted=%d", source, result.inserted, result.updated, result.deleted)
    return result


if __name__ == "__main__":
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    ingest(make_engine(get_settings().database_url))
