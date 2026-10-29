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


def test_f4_04_fetch_pages_until_total_then_details():
    calls = []

    def get(url, params):
        op = url.rsplit("/", 1)[1]
        calls.append(op)
        if op == "areaBasedList2":
            item = {"contentid": str(params["pageNo"]), "title": "x", "mapx": "128.9", "mapy": "37.7", "contenttypeid": "39"}
            return {"response": {"body": {"totalCount": 150, "items": {"item": [item]}}}}
        return {"response": {"body": {"items": {"item": [{"contentid": params["contentId"], "acmpyTypeCd": "전구역 동반가능"}]}}}}

    pages, details = ingest_pet_places.fetch_tour("KEY", get)
    assert calls.count("areaBasedList2") == len(ingest_pet_places.TOUR_AREAS) * 2
    assert calls.count("detailPetTour2") == len(details) == 2
    assert len(pages) == 2


FIX = Path(__file__).resolve().parent / "fixtures"


def _real():
    lst = json.loads((FIX / "tour_area_list.json").read_text(encoding="utf-8"))
    det = json.loads((FIX / "tour_pet_details.json").read_text(encoding="utf-8"))["items"]
    return lst, det


def test_f4_04_real_response_mapping():
    # 실제 응답(일부): 동반 구분·대형견·실내/실외·대표 이미지
    lst, det = _real()
    by = {p.source_id: p for p in ingest_pet_places.normalize_tour([lst], det)}
    assert len(by) == 7 and all(p.source == "tourapi" for p in by.values())
    assert by["3536831"].companion == "전구역 동반가능"
    assert by["1947118"].large_dog is False  # 소형견(10kg이하)
    assert by["2757631"].large_dog is True   # 30kg 이하
    assert by["2734012"].large_dog is False  # 5kg미만 소형견
    assert by["125779"].large_dog is True    # 전 견종
    assert by["3537206"].large_dog is None and by["3537206"].pet_policy is None  # 정보 없음 → 지어내지 않음
    assert by["2665662"].indoor == "outdoor"  # "실내 출입 어려운 점" → 실외만
    assert by["3536831"].indoor is None
    assert by["125779"].image_url.startswith("https://tong.visitkorea.or.kr/")
    assert by["3537206"].image_url is None
    assert by["125779"].category == "walk" and by["2757631"].category == "lodging"


def test_f4_04_cache_and_call_budget(tmp_path):
    n = []

    def get(url, params):
        n.append(url)
        return {"response": {"body": {"items": ""}}}

    cache = ingest_pet_places.TourCache(get, tmp_path, budget=2)
    params = {"serviceKey": "SECRET", "contentId": "1"}
    cache("detailPetTour2", params)
    cache("detailPetTour2", {**params, "serviceKey": "OTHER"})  # 키가 달라도 같은 캐시
    assert len(n) == 1 and cache.hits == 1
    assert all("SECRET" not in f.read_text(encoding="utf-8") and "SECRET" not in f.name for f in tmp_path.iterdir())
    cache("detailPetTour2", {**params, "contentId": "2"})
    with pytest.raises(ingest_pet_places.BudgetExceeded):
        cache("detailPetTour2", {**params, "contentId": "3"})
    expired = ingest_pet_places.TourCache(get, tmp_path, ttl_sec=-1)  # 언제나 만료
    expired("detailPetTour2", params)
    assert len(n) == 3


def test_f4_04_real_ingest_replaces_sample_and_keeps_key_out_of_logs(db_engine, caplog):
    import logging

    caplog.set_level(logging.INFO)
    ingest_pet_places.ingest(db_engine, Settings(tour_api_key=""))
    lst, det = _real()
    r = ingest_pet_places.ingest(db_engine, Settings(tour_api_key="SECRET"), fetch=lambda _k: ([lst], det))
    assert (r.source, r.inserted) == ("tourapi", 7)
    assert "SECRET" not in caplog.text
    from sqlalchemy.orm import Session

    from app.models import PetPlace

    with Session(db_engine) as s:
        row = s.get(PetPlace, "pp_125779")
        assert row.large_dog is True and row.image_url and row.companion == "전구역 동반가능"


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
        "A-5,행복가축병원,영업/정상,강원 횡성군 가축로 5,,,,37.49,127.98\n"
        "A-6,횡성축협동물병원,영업/정상,강원 횡성군 축협로 6,,,,37.49,127.98\n"
    )
    (tmp_path / "vets.csv").write_bytes(csv_text.encode("cp949"))
    r = load_vets.load(db_engine, Settings(), raw_dir=tmp_path)
    assert r.source == "licensing"
    assert r.inserted == 2  # A-1(위도·경도), A-4(좌표 변환). A-5·A-6 농장 동물 병원은 뺀다
    assert r.skipped == 1   # A-3 좌표 없음
    from sqlalchemy.orm import Session

    from app.models import VetClinic

    with Session(db_engine) as s:
        a4 = s.query(VetClinic).filter_by(license_no="A-4").one()
        assert 37.5 < a4.lat < 37.7 and 126.9 < a4.lng < 127.1
        assert s.query(VetClinic).filter_by(license_no="A-1").one().phone == "033-000-0000"


def test_f5_02_real_licensing_file_columns_and_crs(tmp_path, db_engine):
    # 실제 인허가 파일 일부(cp949, 전화번호 비움): 영업상태코드 01만, EPSG:5174 → WGS84, 좌표 없는 행 건너뜀, 샘플은 내림
    load_vets.load(db_engine, Settings(), raw_dir=Path("__no_such_dir__"))
    (tmp_path / "vets.csv").write_bytes((FIX / "vets_licensing_sample.csv").read_bytes())
    r = load_vets.load(db_engine, Settings(), raw_dir=tmp_path)
    # 영업 3곳 중 강릉축협동물병원·강원양돈축산업협동조합동물병원은 농장 동물 병원이라 뺀다
    assert (r.source, r.inserted, r.skipped) == ("licensing", 1, 1)
    assert r.deleted >= 10  # 샘플 병원
    from sqlalchemy.orm import Session

    from app.models import VetClinic

    with Session(db_engine) as s:
        rows = s.query(VetClinic).all()
        assert {v.source for v in rows} == {"licensing"}
        gn = next(v for v in rows if v.name == "강릉동물병원")
        assert abs(gn.lat - 37.758) < 0.002 and abs(gn.lng - 128.899) < 0.002
        assert gn.address.startswith("강원특별자치도 강릉시")
