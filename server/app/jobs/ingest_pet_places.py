"""반려동물 동반 장소 수집 잡 (architecture §3.2 jobs/ingest_pet_places, CONCEPT F4).

한국관광공사 TourAPI "반려동물 동반여행" 서비스(KorPetTourService2) → 정규화 → pet_places.
- 목록: areaBasedList2 (지역별), 동반 조건: detailPetTour2 (장소마다 1건). 2026-10-30 실제 응답으로 확인
  (tests/fixtures/tour_*.json, 개인정보 없는 일부).
- 호출 수 제한(개발계정 하루 1,000건): 응답을 server/data/cache/tourapi/에 7일 캐시하고, 날짜별 호출 수를 세어 하루 DAILY_BUDGET건을
  넘기지 않는다. 다 못 받은 상세(detailPetTour2)는 다음 날 실행에서 이어 받는다(목록은 먼저 다 받으므로 장소는 빠지지 않는다).
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
# 전국 17개 시·도 (결정 UPGRADE 6). 지역별로 차례로, 하루 호출 예산([DAILY_BUDGET]) 안에서 며칠에 나눠 받는다.
AREAS = {
    "1": "서울", "2": "인천", "3": "대전", "4": "대구", "5": "광주", "6": "부산", "7": "울산", "8": "세종",
    "31": "경기", "32": "강원", "33": "충북", "34": "충남", "35": "경북", "36": "경남", "37": "전북", "38": "전남", "39": "제주",
}
TOUR_AREAS = tuple(AREAS)
TOUR_PAGE_ROWS = 100
CACHE_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "cache" / "tourapi"
CACHE_TTL_SEC = 7 * 24 * 3600
# 개발계정 하루 1,000건 → 여유를 두고 900건. 같은 날 여러 번 실행해도 합이 넘지 않게 캐시 폴더에 날짜별로 센다.
DAILY_BUDGET = 900
CALL_BUDGET = DAILY_BUDGET  # 한 번 실행 상한 (하루 남은 수와 작은 쪽)
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
    # 결정 10/30: 실내/실외·대형견 여부는 추측하지 않는다. 동반 조건 글에 명확히 써 있는 문장만 그대로 둔다.
    space_note: str | None = None  # "실내"·"실외"·"야외"·"테라스"가 들어 있는 원문 문장
    size_note: str | None = None  # 동반 가능 동물 원문 (acmpyPsblCpam, 없으면 견종·크기·무게를 말하는 원문 문장)
    # 필터용 (UPGRADE 6): 응답 글에 **명시**됐을 때만 True/False, 아니면 None("정보 없음")
    large_dog_ok: bool | None = None
    indoor_ok: bool | None = None
    companion: str | None = None  # 동반 구분 원문 ("전구역 동반가능", "일부구역 동반가능")

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
    if content_type == "38":
        return "shop"  # 쇼핑(반려동물 용품점·마켓) — 주행 중 주변 추천 "마켓" (UI v9)
    if content_type in ("14", "28"):
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


_SPACE = re.compile(r"실내|실외|야외|테라스")
_SIZE = re.compile(r"대형견|중형견|소형견|견종|\d+\s*(?:kg|㎏)", re.I)
NOTE_MAX = 100


def sentences(text: str) -> list[str]:
    """원문을 문장 단위로: 줄바꿈·"- "·"* "·마침표 뒤에서 자른다. 글자는 바꾸지 않는다(앞뒤 기호·공백만 뺌)."""
    out = []
    for line in str(text or "").splitlines():
        for part in re.split(r"(?<=[.。])\s+|\s+-\s+", line):
            t = part.strip().lstrip("-*※·• ").strip()
            if t:
                out.append(t)
    return out


def _note(t: str) -> str:
    return t if len(t) <= NOTE_MAX else t[: NOTE_MAX - 1] + "…"


def space_note_of(detail: dict[str, Any] | None) -> str | None:
    """실내/실외: 응답에 필드가 없다. 동반 조건 글에 "실내·실외·야외·테라스"가 쓰인 첫 문장을 그대로. 없으면 None."""
    if not detail:
        return None
    for key in ("acmpyNeedMtr", "etcAcmpyInfo", "acmpyPsblCpam"):  # 동반 조건 글만 (편의시설 목록은 아님)
        for t in sentences(detail.get(key) or ""):
            if _SPACE.search(t):
                return _note(t)
    return None


def size_note_of(detail: dict[str, Any] | None) -> str | None:
    """대형견 등 동반 가능 동물: "동반 가능 동물"(acmpyPsblCpam) 원문, 없으면 견종·크기·무게를 말하는 첫 문장. 판정하지 않는다."""
    if not detail:
        return None
    psbl = " ".join(str(detail.get("acmpyPsblCpam") or "").split())
    if psbl:
        return _note(psbl)
    for t in sentences(detail.get("etcAcmpyInfo") or ""):
        if _SIZE.search(t):
            return _note(t)
    return None


_LARGE_OK = re.compile(r"대형견[^.\n]{0,10}(동반\s*)?가능|전\s*견종|모든\s*견종|견종\s*(무관|제한\s*없)|크기\s*(무관|제한\s*없)")
_LARGE_NO = re.compile(r"대형견[^.\n]{0,12}(불가|제한|금지|안\s*돼|어려)|소형견만|중[·,]?\s*소형견만|중소형견만")
_INDOOR_OK = re.compile(r"실내[^.\n]{0,12}(동반|입장|출입|이용)\s*가능|실내\s*가능")
_INDOOR_NO = re.compile(r"실내[^.\n]{0,16}(불가|제한|금지|어려|안\s*돼)")


def _all_sentences(detail: dict[str, Any] | None) -> list[str]:
    if not detail:
        return []
    return [t for k in ("acmpyTypeCd", "acmpyPsblCpam", "acmpyNeedMtr", "etcAcmpyInfo") for t in sentences(detail.get(k) or "")]


def large_dog_ok_of(detail: dict[str, Any] | None) -> bool | None:
    """대형견 동반: 글에 "대형견 가능·전 견종·견종 무관"이 쓰였으면 True, "대형견 불가·소형견만"이면 False, 그 밖(무게 제한만 등)은 None."""
    ss = _all_sentences(detail)
    if any(_LARGE_NO.search(t) for t in ss):
        return False
    if any(_LARGE_OK.search(t) for t in ss):
        return True
    return None


def indoor_ok_of(detail: dict[str, Any] | None) -> bool | None:
    """실내 동반: 글에 "실내 동반 가능"이 쓰였으면 True, "실내 불가·어려움"이면 False, 그 밖(전구역 동반가능만 등)은 None."""
    ss = _all_sentences(detail)
    if any(_INDOOR_NO.search(t) for t in ss):
        return False
    if any(_INDOOR_OK.search(t) for t in ss):
        return True
    return None


def policy_of(detail: dict[str, Any] | None) -> str | None:
    """필요 사항 원문 ("목줄 착용,이동장(켄넬)사용"). 크기·공간 문장은 따로 둔다."""
    if not detail:
        return None
    return " ".join(str(detail.get("acmpyNeedMtr") or "").split())[:160] or None


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
                space_note=space_note_of(d), size_note=size_note_of(d),
                large_dog_ok=large_dog_ok_of(d), indoor_ok=indoor_ok_of(d),
                companion=(str(d.get("acmpyTypeCd") or "").strip() or None) if d else None,
            )
    return list(out.values())


def normalize_sample(payload: dict[str, Any]) -> list[NormalizedPlace]:
    out = []
    for it in payload.get("items", []):
        if it.get("category") not in PLACE_CATEGORIES:
            continue
        out.append(NormalizedPlace(it["sourceId"], "sample", it["name"], it["category"], float(it["lat"]), float(it["lng"]),
                                   it.get("address"), it.get("petPolicy")))
    return out


class BudgetExceeded(Exception):
    pass


class TourCache:
    """응답 파일 캐시 (serviceKey를 뺀 파라미터로 이름을 만든다). [budget]건을 넘게 부르면 BudgetExceeded."""

    def __init__(self, get: Callable[[str, dict[str, Any]], dict[str, Any]], directory: Path | None = CACHE_DIR,
                 ttl_sec: int = CACHE_TTL_SEC, budget: int = CALL_BUDGET, now: Callable[[], float] = time.time,
                 daily: int = DAILY_BUDGET):
        self.get, self.dir, self.ttl, self.now, self.daily = get, directory, ttl_sec, now, daily
        self.calls = 0
        self.hits = 0
        # 오늘 이미 쓴 호출 수(같은 캐시 폴더의 장부) — 실행 상한과 하루 남은 수 중 작은 쪽까지만 부른다
        self.used_today = self._ledger_read()
        self.budget = max(0, min(budget, daily - self.used_today))

    def _ledger(self) -> Path | None:
        if self.dir is None:
            return None
        day = time.strftime("%Y%m%d", time.localtime(self.now()))
        return self.dir / f"_calls_{day}.txt"

    def _ledger_read(self) -> int:
        p = self._ledger()
        try:
            return int(p.read_text().strip()) if p is not None and p.exists() else 0
        except ValueError:
            return 0

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
        ledger = self._ledger()
        if ledger is not None:
            ledger.parent.mkdir(parents=True, exist_ok=True)
            ledger.write_text(str(self.used_today + self.calls))
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
    missing = sum(1 for p in pages for it in _items(p) if str(it.get("contentid") or "").strip() not in details)
    log.info("tour api calls=%d cache_hits=%d today=%d/%d details_missing=%d", cache.calls, cache.hits,
             cache.used_today + cache.calls, cache.daily, missing)
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
                      image_url=p.image_url, space_note=p.space_note, size_note=p.size_note, companion=p.companion,
                      large_dog_ok=p.large_dog_ok, indoor_ok=p.indoor_ok)
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

    from app.logmask import install

    import sys

    logging.basicConfig(level=logging.INFO)
    install()
    # --cache-only: 7일 캐시에 있는 응답만 쓴다(외부 호출 0건) — 분류 규칙만 바뀌었을 때 다시 적재 (0006 shop)
    if "--cache-only" in sys.argv:
        ingest(make_engine(get_settings().database_url), fetch=lambda key: fetch_tour(key, budget=0))
    else:
        ingest(make_engine(get_settings().database_url))
