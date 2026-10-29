"""test_plan F4-01~F4-04, F5-01·F5-02: 반려동물 동반 장소·동물병원."""
import json
from pathlib import Path

import pytest

from app.config import Settings
from app.jobs import ingest_pet_places, load_vets
from tests.conftest import make_client, register

V1 = "/api/v1"
DATA = Path(__file__).resolve().parent / "data"
SCENARIO = Path(__file__).resolve().parents[2] / "scenarios" / "pet-trip.json"


@pytest.fixture
def client(db_engine):
    ingest_pet_places.ingest(db_engine, Settings(tour_api_key=""))
    load_vets.load(db_engine, Settings(), raw_dir=Path("__no_such_dir__"))
    with make_client(db_engine) as c:
        yield c


def _route():
    r = json.loads(SCENARIO.read_text(encoding="utf-8"))["route"]
    return r["points"], r["totalDistanceM"]


def test_f4_01_without_key_loads_sample_and_marks_it(db_engine):
    def boom(_key):
        raise AssertionError("키가 없으면 외부 호출하지 않는다")

    r = ingest_pet_places.ingest(db_engine, Settings(tour_api_key=""), fetch=boom)
    assert (r.source, r.ok) == ("sample", True) and r.inserted >= 20
    again = ingest_pet_places.ingest(db_engine, Settings(tour_api_key=""), fetch=boom)
    assert (again.inserted, again.updated, again.deleted) == (0, 0, 0)


def test_f4_02_buffer_search_in_route_order(client):
    points, total = _route()
    h = register(client)
    body = {"points": points, "bufferMeters": 5000, "routeLengthMeters": total}
    items = client.post(f"{V1}/places/along-route", json=body, headers=h).json()["items"]
    assert items and all(i["sample"] for i in items)
    assert all(i["offRouteKm"] <= 5.0 for i in items)
    along = [i["distanceAlongRouteKm"] for i in items]
    assert along == sorted(along)
    # 좁은 버퍼면 더 적다
    narrow = client.post(f"{V1}/places/along-route", json={**body, "bufferMeters": 1000}, headers=h).json()["items"]
    assert len(narrow) < len(items)
    # 분류 거르기
    cafes = client.post(f"{V1}/places/along-route", json={**body, "categories": ["cafe"]}, headers=h).json()["items"]
    assert cafes and {i["category"] for i in cafes} == {"cafe"}


def test_f4_02_bad_requests(client):
    h = register(client)
    assert client.post(f"{V1}/places/along-route", json={"points": [{"lat": 37.5, "lng": 127.0}]}, headers=h).status_code == 400
    assert client.post(f"{V1}/places/along-route", json={"points": [{"lat": 37.5, "lng": 127.0}] * 2, "bufferMeters": 100}, headers=h).status_code == 422
    assert client.post(f"{V1}/places/along-route", json={"points": [{"lat": 37.5, "lng": 127.0}] * 2}).status_code == 401


def test_f4_03_near_destination_walks_and_cafes(client):
    h = register(client)
    r = client.get(f"{V1}/places/near", params={"lat": 37.7519, "lng": 128.8761}, headers=h).json()
    assert r["count"] >= 2
    assert {i["category"] for i in r["items"]} <= {"walk", "cafe"}
    d = [i["distanceKm"] for i in r["items"]]
    assert d == sorted(d) and d[-1] <= 3.0
    assert client.get(f"{V1}/places/near", params={"lat": 37.75, "lng": 128.87, "categories": "spa"}, headers=h).status_code == 422


def test_f4_04_tour_api_parser():
    payload = json.loads((DATA / "tour_pet_response.json").read_text(encoding="utf-8"))
    places = ingest_pet_places.normalize_tour([payload])
    by_id = {p.source_id: p for p in places}
    assert by_id["900001"].category == "cafe"
    assert by_id["900002"].category == "restaurant"
    assert by_id["900003"].category == "walk"
    assert by_id["900004"].category == "lodging"
    assert "900005" not in by_id  # 좌표 없음
    assert "900006" not in by_id  # 모르는 콘텐츠 타입
    assert all(p.source == "tourapi" for p in places)
    # 결과 없음(items가 빈 문자열)·한 건(dict)도 처리
    assert ingest_pet_places.normalize_tour([{"response": {"body": {"items": ""}}}]) == []
    one = {"response": {"body": {"items": {"item": payload["response"]["body"]["items"]["item"][0]}}}}
    assert len(ingest_pet_places.normalize_tour([one])) == 1


def test_f4_04_fetch_pages_until_total():
    calls = []

    def get(url, params):
        calls.append((url, params["areaCode"], params["pageNo"]))
        return {"response": {"body": {"totalCount": 150, "items": ""}}}

    ingest_pet_places.fetch_tour("KEY", get)
    assert len(calls) == len(ingest_pet_places.TOUR_AREAS) * 2
    assert all(c[0].endswith("/areaBasedList") for c in calls)


def test_f5_01_sample_vets_nearest_first_without_phone(client):
    h = register(client)
    r = client.get(f"{V1}/vets/nearby", params={"lat": 37.7519, "lng": 128.8761, "limit": 3}, headers=h).json()
    assert r["count"] == 3
    d = [i["distanceKm"] for i in r["items"]]
    assert d == sorted(d)
    assert all(i["sample"] and i["phone"] is None for i in r["items"])
    assert "강릉" in r["items"][0]["name"]


def test_f5_02_licensing_csv_open_only_and_bad_rows_skipped(tmp_path, db_engine):
    csv_text = (
        "관리번호,사업장명,영업상태명,도로명전체주소,소재지전화,좌표정보(x),좌표정보(y),위도,경도\n"
        "A-1,가나동물병원,영업/정상,강원 강릉시 가나로 1,033-000-0000,,,37.75,128.88\n"
        "A-2,다라동물병원,폐업,강원 강릉시 다라로 2,,,,37.76,128.89\n"
        "A-3,마바동물병원,영업/정상,강원 강릉시 마바로 3,,,,,\n"
        "A-4,사아동물병원,영업/정상,서울 중구 사아로 4,,198000,452000,,\n"
    )
    (tmp_path / "vets.csv").write_bytes(csv_text.encode("cp949"))
    r = load_vets.load(db_engine, Settings(), raw_dir=tmp_path)
    assert r.source == "licensing"
    assert r.inserted == 2  # A-1(위도·경도), A-4(좌표 변환)
    assert r.skipped == 1   # A-3 좌표 없음
    from sqlalchemy.orm import Session

    from app.models import VetClinic

    with Session(db_engine) as s:
        a4 = s.query(VetClinic).filter_by(license_no="A-4").one()
        assert 37.5 < a4.lat < 37.7 and 126.9 < a4.lng < 127.1
        assert s.query(VetClinic).filter_by(license_no="A-1").one().phone == "033-000-0000"
