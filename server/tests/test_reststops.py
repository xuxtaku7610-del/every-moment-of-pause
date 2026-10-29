"""test_plan §9 (T9-4·T9-5·T9-6): /devices, /reststops/along-route, /reststops/{id}"""
import pytest

from app.jobs.ingest_reststops import ingest
from app.config import Settings
from tests.conftest import make_client, register

# scenarios/seoul-busan-family.json 경로
ROUTE = [
    (37.4602, 127.0417), (37.2411, 127.1776), (36.8065, 127.1522), (36.3504, 127.3845),
    (36.1398, 128.1136), (35.8714, 128.6014), (35.1796, 129.0756),
]
URL = "/api/v1/reststops/along-route"


def body(points=ROUTE, **kw):
    return {"points": [{"lat": a, "lng": b} for a, b in points], **kw}


@pytest.fixture
def client(db_engine):
    ingest(db_engine, Settings(ex_api_key=""))
    with make_client(db_engine) as c:
        yield c


def test_devices_issue_token_and_requests_without_token_are_401(client):
    # [N] /devices 토큰 발급, 토큰 없는 요청 401
    resp = client.post(URL, json=body())
    assert resp.status_code == 401
    assert resp.json()["error"]["code"] == "INVALID_DEVICE_TOKEN"
    assert client.post(URL, json=body(), headers={"X-Device-Token": "nope"}).status_code == 401
    assert client.post(URL, json=body(), headers=register(client)).status_code == 200


def test_along_route_sorted_and_excludes_outside_corridor(client):
    # T9-5 [N] 경로 방향 앞쪽만, 거리순 정렬, 반경 밖 제외
    data = client.post(URL, json=body(), headers=register(client)).json()
    names = [i["name"] for i in data["items"]]
    assert data["count"] == 19
    along = [i["distanceAlongRouteKm"] for i in data["items"]]
    assert along == sorted(along)
    first = data["items"][0]
    assert first["name"] == "서울만남의광장휴게소"
    assert (first["routeName"], first["direction"]) == ("경부선", "부산")
    assert first["facilities"] == ["ev_fast", "ev_slow", "kids_zone", "nursing_room"]
    assert first["evChargers"] == {"fast": 4, "slow": 1}
    assert first["type"] == "service_area"
    assert names[-1] == "통도사휴게소"


def test_along_route_excludes_opposite_direction(client):
    # T9-5 반대 방향(서울방향) 휴게소 제외, 거꾸로 가면 서울방향만 (COMMERCIAL_READINESS B P0)
    h = register(client)
    south = [i["name"] for i in client.post(URL, json=body(), headers=h).json()["items"]]
    assert "죽전휴게소" not in south and "천안호두휴게소" not in south
    north = [i["name"] for i in client.post(URL, json=body(list(reversed(ROUTE))), headers=h).json()["items"]]
    assert north == ["신탄진휴게소", "천안호두휴게소", "입장거봉포도휴게소", "죽전휴게소"]


def test_along_route_scales_to_route_length(client):
    # 직선 좌표열(약 341km)을 내비 총거리 395km 척도로 — 앱 RouteGeo와 같은 규칙
    h = register(client)
    raw = {i["name"]: i["distanceAlongRouteKm"] for i in client.post(URL, json=body(), headers=h).json()["items"]}
    scaled = {i["name"]: i["distanceAlongRouteKm"] for i in client.post(URL, json=body(routeLengthMeters=395_000), headers=h).json()["items"]}
    assert abs(scaled["죽암휴게소"] - 150) <= 1
    assert raw["죽암휴게소"] < 135


def test_along_route_only_ahead_of_first_point(client):
    # 대전 근처에서 출발하면 그 앞(서울~대전) 휴게소는 나오지 않는다.
    data = client.post(URL, json=body(ROUTE[3:]), headers=register(client)).json()
    names = [i["name"] for i in data["items"]]
    assert "안성휴게소" not in names and "죽암휴게소" not in names
    assert "금강휴게소" in names
    assert data["items"][0]["distanceAlongRouteKm"] < 20


def test_along_route_type_filter_and_wider_corridor(client):
    h = register(client)
    shelters = client.post(URL, json=body(types=["drowsy_shelter"]), headers=h).json()["items"]
    assert {i["type"] for i in shelters} == {"drowsy_shelter"}
    assert len(shelters) == 4
    narrow = client.post(URL, json=body(corridorMeters=100), headers=h).json()
    assert narrow["count"] == 19  # 샘플은 경로에서 60m
    assert client.post(URL, json=body(corridorMeters=99), headers=h).status_code == 422


def test_along_route_needs_two_points_and_valid_range(client):
    # [E] 좌표 1개 → 400, 위경도 범위 밖 → 422
    h = register(client)
    one = client.post(URL, json=body(ROUTE[:1]), headers=h)
    assert one.status_code == 400
    assert one.json()["error"]["code"] == "INVALID_REQUEST"
    bad = client.post(URL, json={"points": [{"lat": 91, "lng": 127}, {"lat": 37, "lng": 127}]}, headers=h)
    assert bad.status_code == 422
    assert bad.json()["error"]["code"] == "VALIDATION_ERROR"


def test_rest_stop_detail_and_404(client):
    h = register(client)
    item = client.post(URL, json=body(), headers=h).json()["items"][0]
    d = client.get(f"/api/v1/reststops/{item['restStopId']}", headers=h)
    assert d.status_code == 200
    assert d.json()["name"] == item["name"]
    assert {"phone", "address", "themes"} <= d.json().keys()
    missing = client.get("/api/v1/reststops/rs_999", headers=h)
    assert missing.status_code == 404
    assert missing.json()["error"] == {
        "code": "REST_STOP_NOT_FOUND", "message": "해당 휴게소를 찾을 수 없습니다.", "detail": {"restStopId": "rs_999"},
    }
