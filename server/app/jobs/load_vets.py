"""동물병원 적재 잡 (architecture §3.2 jobs/load_vets, CONCEPT F5).

공공데이터 "지방행정 인허가 데이터 — 동물병원" 파일(CSV)을 server/data/raw/에 두면 그 파일로 적재한다.
파일이 없으면 app/data/sample_vets.json(샘플, 전화번호 없음)을 적재한다.
- 영업 중인 곳만(영업상태명에 "영업" 또는 "정상"), 좌표가 없거나 변환에 실패한 행은 건너뛴다.
- 좌표: 위도·경도 열이 있으면 그대로, 없으면 좌표정보(X·Y)를 VET_SOURCE_CRS(기본 EPSG:5174)에서 WGS84로 바꾼다(pyproj).
  TODO(vet-file): 실제 파일을 받으면 열 이름·좌표계를 확인한다.

실행: python -m app.jobs.load_vets
"""
import csv
import io
import json
import logging
import re
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from sqlalchemy import Engine, select
from sqlalchemy.orm import Session

from app.config import Settings, get_settings
from app.models import VetClinic

log = logging.getLogger(__name__)
SAMPLE_PATH = Path(__file__).resolve().parent.parent / "data" / "sample_vets.json"
RAW_DIR = Path(__file__).resolve().parent.parent.parent / "data" / "raw"


@dataclass
class NormalizedVet:
    license_no: str
    source: str  # licensing | sample
    name: str
    address: str | None
    phone: str | None
    lat: float
    lng: float
    open: bool = True

    @property
    def id(self) -> str:
        key = re.sub(r"[^0-9A-Za-z]", "", self.license_no).lower()
        return f"{'vt' if self.source == 'licensing' else 'vs'}_{key[-28:]}"


@dataclass
class LoadResult:
    source: str
    ok: bool
    inserted: int = 0
    updated: int = 0
    deleted: int = 0
    skipped: int = 0


def _key(h: str) -> str:
    return re.sub(r"\s", "", h).lower()


def _col(row: dict[str, str], *names: str) -> str | None:
    keys = {_key(k): v for k, v in row.items() if k is not None}
    for n in names:
        v = keys.get(_key(n))
        if v is not None and str(v).strip():
            return str(v).strip()
    return None


def _decode(raw: bytes) -> str:
    for enc in ("utf-8-sig", "cp949"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def make_transform(crs: str) -> Callable[[float, float], tuple[float, float]] | None:
    """(x, y) → (lat, lng). pyproj가 없으면 None (위도·경도 열이 있는 파일만 적재)."""
    try:
        from pyproj import Transformer
    except ImportError:
        return None
    t = Transformer.from_crs(crs, "EPSG:4326", always_xy=True)

    def f(x: float, y: float) -> tuple[float, float]:
        lng, lat = t.transform(x, y)
        return lat, lng

    return f


def parse_licensing_csv(text: str, transform: Callable[[float, float], tuple[float, float]] | None) -> tuple[list[NormalizedVet], int]:
    """인허가 CSV → 영업 중 병원. (목록, 건너뛴 행 수)."""
    out: list[NormalizedVet] = []
    skipped = 0
    for row in csv.DictReader(io.StringIO(text)):
        status = _col(row, "영업상태명", "상세영업상태명") or ""
        if "영업" not in status and "정상" not in status:
            continue
        lic = _col(row, "관리번호", "인허가번호")
        name = _col(row, "사업장명")
        if not lic or not name:
            skipped += 1
            continue
        lat = lng = None
        try:
            if _col(row, "위도") and _col(row, "경도"):
                lat, lng = float(_col(row, "위도")), float(_col(row, "경도"))
            elif transform and _col(row, "좌표정보(x)", "좌표정보x") and _col(row, "좌표정보(y)", "좌표정보y"):
                lat, lng = transform(float(_col(row, "좌표정보(x)", "좌표정보x")), float(_col(row, "좌표정보(y)", "좌표정보y")))
        except (TypeError, ValueError):
            lat = lng = None
        if lat is None or lng is None or not (33 <= lat <= 39 and 124 <= lng <= 132):
            skipped += 1
            continue
        out.append(NormalizedVet(lic, "licensing", name, _col(row, "도로명전체주소", "소재지전체주소"), _col(row, "소재지전화", "전화번호"), lat, lng))
    return out, skipped


def normalize_sample(payload: dict[str, Any]) -> list[NormalizedVet]:
    return [NormalizedVet(it["licenseNo"], "sample", it["name"], it.get("address"), None, float(it["lat"]), float(it["lng"]), bool(it.get("open", True)))
            for it in payload.get("items", [])]


def apply(session: Session, vets: list[NormalizedVet], source: str) -> LoadResult:
    result = LoadResult(source=source, ok=True)
    existing = {r.license_no: r for r in session.scalars(select(VetClinic).where(VetClinic.source == source))}
    for v in vets:
        row = existing.pop(v.license_no, None)
        values = dict(source=v.source, name=v.name, address=v.address, phone=v.phone, lat=v.lat, lng=v.lng, open=v.open)
        if row is None:
            session.add(VetClinic(id=v.id, license_no=v.license_no, **values))
            result.inserted += 1
        else:
            if any(getattr(row, k) != val for k, val in values.items()):
                result.updated += 1
            for k, val in values.items():
                setattr(row, k, val)
    for row in existing.values():
        session.delete(row)
        result.deleted += 1
    return result


def load(engine: Engine, settings: Settings | None = None, raw_dir: Path = RAW_DIR, sample_path: Path = SAMPLE_PATH) -> LoadResult:
    settings = settings or get_settings()
    files = sorted(raw_dir.glob("*.csv")) if raw_dir.exists() else []
    if files:
        source = "licensing"
        transform = make_transform(settings.vet_source_crs)
        vets: list[NormalizedVet] = []
        skipped = 0
        for f in files:
            got, sk = parse_licensing_csv(_decode(f.read_bytes()), transform)
            vets += got
            skipped += sk
        if transform is None:
            log.warning("pyproj 없음: 위도·경도 열이 없는 행은 건너뜀")
    else:
        source = "sample"
        vets, skipped = normalize_sample(json.loads(sample_path.read_text(encoding="utf-8"))), 0
    with Session(engine) as session, session.begin():
        result = apply(session, vets, source)
    result.skipped = skipped
    log.info("vets load source=%s inserted=%d updated=%d deleted=%d skipped=%d", source, result.inserted, result.updated, result.deleted, skipped)
    return result


if __name__ == "__main__":
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    load(make_engine(get_settings().database_url))
