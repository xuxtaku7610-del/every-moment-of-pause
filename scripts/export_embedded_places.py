"""서버 DB의 실제 데이터 → 앱 내장 데이터 (UI v9 마무리, 제출 APK가 서버 없이도 주변 추천·가장 가까운 병원을 보이게).

    server\\.venv\\Scripts\\python scripts\\export_embedded_places.py      # DB는 서버 설정(DATABASE_URL)을 그대로 쓴다

- 반려동물 동반 장소: 한국관광공사 TourAPI 반려동물 동반여행(KorPetTourService2) 적재분 전부. 이름·분류·좌표·주소·동반 조건 원문 요약.
  사진 주소(image_url)는 넣지 않는다(앱은 오프라인이면 사진을 못 받는다).
- 동물병원: 행정안전부 지방행정 인허가 데이터(동물_동물병원) 중 영업 중(open)인 곳. 이름·좌표·주소·전화(사업장 대표 번호).
- 둘 다 공공데이터포털 "이용허락범위 제한 없음"(2026-11-03 확인, docs/PROGRESS.md). 출처 표기는 파일 머리와 앱 설정 credits.
- 키·토큰·개인 정보 칸은 DB에 없고, 내보낸 뒤 serviceKey·이메일·주민번호 꼴 문자열이 없는지 검사한다.
- 결과: app-android/data/embedded/pet_places.json · vets.json (data 모듈 빌드가 assets로 묶는다). 휴게소는 샘플 그대로.
"""
import json
import re
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "server"))

from sqlalchemy import select  # noqa: E402
from sqlalchemy.orm import Session  # noqa: E402

from app.config import get_settings  # noqa: E402
from app.db import make_engine  # noqa: E402
from app.models import PetPlace, VetClinic  # noqa: E402

OUT = ROOT / "app-android" / "data" / "embedded"
PLACE_SOURCE = "한국관광공사 TourAPI 반려동물 동반여행 (공공데이터포털 이용허락범위 제한 없음)"
VET_SOURCE = "행정안전부 지방행정 인허가 데이터 동물_동물병원 (공공데이터포털 이용허락범위 제한 없음)"
BAD = [re.compile(p, re.I) for p in (r"servicekey", r"[\w.+-]+@[\w-]+\.[\w.]+", r"\d{6}-[1-4]\d{6}", r"apis\.data\.go\.kr")]


def short(v: str | None, n: int = 100) -> str | None:
    if not v:
        return None
    v = " ".join(v.split())
    return v if len(v) <= n else v[: n - 1] + "…"


def main() -> int:
    engine = make_engine(get_settings().database_url)
    with Session(engine) as s:
        places = s.scalars(select(PetPlace).where(PetPlace.source == "tourapi").order_by(PetPlace.source_id)).all()
        vets = s.scalars(select(VetClinic).where(VetClinic.source == "licensing", VetClinic.open.is_(True)).order_by(VetClinic.license_no)).all()
    if not places or not vets:
        print(f"실제 데이터가 없음: 장소 {len(places)} · 병원 {len(vets)} — 수집 잡을 먼저 돌린다", file=sys.stderr)
        return 1
    today = date.today().isoformat()
    p_doc = {
        "source": PLACE_SOURCE, "exported": today, "count": len(places),
        "items": [
            {k: v for k, v in {
                "sourceId": p.source_id, "name": p.name, "category": p.category, "lat": round(p.lat, 6), "lng": round(p.lng, 6),
                "address": p.address, "petPolicy": short(p.pet_policy), "spaceNote": p.space_note, "sizeNote": p.size_note,
                "companion": p.companion, "largeDogOk": p.large_dog_ok, "indoorOk": p.indoor_ok, "sample": False,
            }.items() if v is not None}
            for p in places
        ],
    }
    v_doc = {
        "source": VET_SOURCE, "exported": today, "count": len(vets),
        "items": [
            {k: v for k, v in {
                "licenseNo": v.license_no, "name": v.name, "lat": round(v.lat, 6), "lng": round(v.lng, 6),
                "address": v.address, "phone": v.phone, "open": True, "sample": False,
            }.items() if v is not None}
            for v in vets
        ],
    }
    OUT.mkdir(parents=True, exist_ok=True)
    for name, doc in (("pet_places.json", p_doc), ("vets.json", v_doc)):
        text = json.dumps(doc, ensure_ascii=False, separators=(",", ":"))
        hits = [b.pattern for b in BAD if b.search(text)]
        if hits:
            print(f"{name}: 키·개인 정보 꼴 문자열 발견 {hits} — 내보내지 않음", file=sys.stderr)
            return 1
        (OUT / name).write_text(text, encoding="utf-8")
        print(f"{name}: {doc['count']}건, {len(text.encode('utf-8')):,} bytes")
    cats: dict[str, int] = {}
    for p in places:
        cats[p.category] = cats.get(p.category, 0) + 1
    print("장소 분류:", cats)
    return 0


if __name__ == "__main__":
    sys.exit(main())
