"""동물병원 적재 잡 (architecture §3.2 jobs/load_vets, CONCEPT F5).

공공데이터 "지방행정 인허가 데이터 — 동물병원" 파일(CSV)을 server/data/raw/에 두면 그 파일로 적재한다.
파일이 없으면 app/data/sample_vets.json(샘플, 전화번호 없음)을 적재한다.
- 실제 파일(2026-10-30, vets.csv, cp949, 10,660행) 확인 결과:
  열 = 관리번호·사업장명·영업상태코드(01=영업/정상)·영업상태명·도로명주소·지번주소·전화번호·좌표정보(X)·좌표정보(Y) …
  좌표 = EPSG:5174(보정 Bessel TM 중부): 강릉동물병원 (367319.9, 474516.5) → 37.758, 128.899 로 주소와 맞는다.
- 영업 중인 곳만(영업상태코드 01, 코드 열이 없으면 영업상태명에 "영업"·"정상"). 좌표가 없거나 변환에 실패한 행은 건너뛴다.
- 영업시간·24시 여부는 데이터에 없다 → 화면에 쓰지 않는다(이름에 "24"가 있어도).
- 반려견 앱이므로 농장 동물 병원은 뺀다. 진료 대상·업태 열이 있으면 그 열이 먼저([KIND_COLUMNS]), 없으면 이름 규칙
  (가축·양계·양돈·축협 등, 2026-10 파일에는 그런 열이 없어 이름 규칙으로 224곳). 뺀 수는 로그 excluded. docs/plan/public_data.md

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
OPEN_CODE = "01"
# 농장 동물(가축·가금·양봉·수산) 병원 이름 — 반려견 안내에서 뺀다. 이름 규칙이라 완벽하지 않다.
LIVESTOCK = re.compile(r"가축|양계|양돈|축협|축산|한우|젖소|낙농|양봉|수산|어류|양식")
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


# 진료 대상을 직접 말하는 열 (지금 파일에는 없다 — 2026-10 vets.csv 25개 열 확인). 있으면 이름 규칙보다 먼저 쓴다.
KIND_COLUMNS = ("진료대상", "진료동물", "취급동물", "업태구분명", "업태")
PET_KIND = re.compile(r"반려|소동물|애완|개|고양이|혼합")
FARM_KIND = re.compile(r"농장|가축|대동물|산업동물|축산|가금|양돈|양계|수산")


def is_livestock(row: dict[str, str], name: str) -> bool:
    """농장 동물 병원인가: 진료 대상 열이 있으면 그 값으로(반려동물 언급이 있으면 남김), 없으면 이름 규칙 [LIVESTOCK]."""
    kind = _col(row, *KIND_COLUMNS)
    if kind is not None:
        return bool(FARM_KIND.search(kind)) and not PET_KIND.search(kind)
    return bool(LIVESTOCK.search(name))


def parse_licensing_csv(text: str, transform: Callable[[float, float], tuple[float, float]] | None) -> tuple[list[NormalizedVet], int]:
    """인허가 CSV → 영업 중 반려동물 병원. (목록, 건너뛴 행 수). 농장 동물 병원 수는 [parse_licensing_csv.excluded]."""
    out: list[NormalizedVet] = []
    skipped = 0
    excluded = 0
    for row in csv.DictReader(io.StringIO(text)):
        code = _col(row, "영업상태코드")
        status = _col(row, "영업상태명", "상세영업상태명") or ""
        if code is not None and code != OPEN_CODE:
            continue
        if code is None and "영업" not in status and "정상" not in status:
            continue
        lic = _col(row, "관리번호", "인허가번호")
        name = _col(row, "사업장명")
        if not lic or not name:
            skipped += 1
            continue
        if is_livestock(row, name):
            excluded += 1
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
        out.append(NormalizedVet(lic, "licensing", name, _col(row, "도로명주소", "도로명전체주소", "지번주소", "소재지전체주소"),
                                 _col(row, "전화번호", "소재지전화"), lat, lng))
    parse_licensing_csv.excluded = excluded  # type: ignore[attr-defined]
    log.info("vets file excluded livestock clinics=%d", excluded)
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
        if source == "licensing" and vets:
            # 실제 인허가 데이터가 들어오면 샘플 병원은 내린다.
            for row in session.scalars(select(VetClinic).where(VetClinic.source == "sample")):
                session.delete(row)
                result.deleted += 1
    result.skipped = skipped
    log.info("vets load source=%s inserted=%d updated=%d deleted=%d skipped=%d", source, result.inserted, result.updated, result.deleted, skipped)
    return result


if __name__ == "__main__":
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    load(make_engine(get_settings().database_url))
