"""휴게소 수집 잡 (architecture §3.2 jobs/ingest_reststops).

도로공사 API → 정규화 → rest_stops, rest_stop_facilities. EX_API_KEY가 비어 있으면 외부 호출 없이
app/data/sample_reststops.json을 같은 정규화 경로로 적재하고 로그에 source=sample을 남긴다.
API 호출이 실패하면 기존 데이터를 그대로 둔다.

실행: python -m app.jobs.ingest_reststops
"""
import json
import logging
from collections.abc import Callable
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from sqlalchemy import Engine, delete, select
from sqlalchemy.orm import Session

from app.config import Settings, get_settings
from app.models import FacilityCode, RestStop, RestStopFacility

log = logging.getLogger(__name__)
SAMPLE_PATH = Path(__file__).resolve().parent.parent / "data" / "sample_reststops.json"
STOP_TYPES = {"service_area", "drowsy_shelter"}


@dataclass
class NormalizedStop:
    ex_code: str
    name: str
    stop_type: str
    route_name: str
    direction: str
    lat: float
    lng: float
    phone: str | None = None
    address: str | None = None
    facilities: dict[str, int] = field(default_factory=dict)

    @property
    def id(self) -> str:
        return f"rs_{self.ex_code.lower()}"


@dataclass
class IngestResult:
    source: str  # sample | ex
    ok: bool
    inserted: int = 0
    updated: int = 0
    deleted: int = 0


def fetch_ex(api_key: str) -> dict[str, Any]:
    # TODO(ex-api): 공공데이터포털 활용 승인 후 실제 엔드포인트·응답 형식을 확인해 구현한다.
    raise NotImplementedError("도로공사 API 응답 형식 확인 전")


def load_sample(path: Path = SAMPLE_PATH) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def normalize(payload: dict[str, Any], known_facilities: set[str]) -> list[NormalizedStop]:
    """응답 → NormalizedStop. 좌표·종류가 이상한 항목과 모르는 편의시설 코드는 버린다."""
    stops: dict[str, NormalizedStop] = {}
    for a in payload.get("restAreas", []):
        try:
            lat, lng = float(a["yValue"]), float(a["xValue"])
            code, name = str(a["serviceAreaCode"]).strip(), str(a["serviceAreaName"]).strip()
        except (KeyError, TypeError, ValueError):
            log.warning("ingest skip malformed rest area: %s", a.get("serviceAreaCode"))
            continue
        stop_type = a.get("stopType", "service_area")
        if not code or not name or stop_type not in STOP_TYPES or not (-90 <= lat <= 90 and -180 <= lng <= 180):
            log.warning("ingest skip invalid rest area: %s", code)
            continue
        stops[code] = NormalizedStop(
            ex_code=code, name=name, stop_type=stop_type,
            route_name=str(a.get("routeName") or ""), direction=str(a.get("direction") or ""),
            lat=lat, lng=lng, phone=a.get("telNo"), address=a.get("svarAddr"),
        )
    for f in payload.get("facilities", []):
        stop = stops.get(str(f.get("serviceAreaCode")))
        code = f.get("facilityCode")
        if stop is None or code not in known_facilities:
            continue
        stop.facilities[code] = max(0, int(f.get("quantity", 1)))
    return list(stops.values())


def apply(session: Session, stops: list[NormalizedStop]) -> IngestResult:
    """신규 추가, 변경 반영, 목록에서 빠진 휴게소 삭제."""
    result = IngestResult(source="", ok=True)
    existing = {r.ex_code: r for r in session.scalars(select(RestStop))}
    for s in stops:
        row = existing.pop(s.ex_code, None)
        values = dict(name=s.name, stop_type=s.stop_type, route_name=s.route_name, direction=s.direction,
                      lat=s.lat, lng=s.lng, phone=s.phone, address=s.address)
        if row is None:
            session.add(RestStop(id=s.id, ex_code=s.ex_code, **values))
            result.inserted += 1
        else:
            changed = any(getattr(row, k) != v for k, v in values.items())
            for k, v in values.items():
                setattr(row, k, v)
            if changed:
                result.updated += 1
        session.flush()
        stop_id = row.id if row is not None else s.id
        session.execute(delete(RestStopFacility).where(RestStopFacility.rest_stop_id == stop_id))
        session.add_all(RestStopFacility(rest_stop_id=stop_id, facility_code=c, quantity=q) for c, q in s.facilities.items())
    for row in existing.values():
        session.execute(delete(RestStopFacility).where(RestStopFacility.rest_stop_id == row.id))
        session.delete(row)
        result.deleted += 1
    return result


def ingest(engine: Engine, settings: Settings | None = None,
           fetch: Callable[[str], dict[str, Any]] = fetch_ex,
           sample: Callable[[], dict[str, Any]] = load_sample) -> IngestResult:
    settings = settings or get_settings()
    source = "ex" if settings.ex_api_key else "sample"
    try:
        payload = fetch(settings.ex_api_key) if source == "ex" else sample()
    except Exception as e:  # noqa: BLE001 - 수집 실패는 기존 데이터 유지
        log.warning("ingest source=%s failed, keeping existing data: %s", source, type(e).__name__)
        return IngestResult(source=source, ok=False)
    with Session(engine) as session, session.begin():
        known = set(session.scalars(select(FacilityCode.code)))
        result = apply(session, normalize(payload, known))
    result.source = source
    log.info("ingest source=%s inserted=%d updated=%d deleted=%d", source, result.inserted, result.updated, result.deleted)
    return result


if __name__ == "__main__":
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    ingest(make_engine(get_settings().database_url))
