"""test_plan F3-07·F3-08·F3-09, SV-05: 대기 공유와 보호자 폰 페이지."""
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.api.deps import hash_token
from app.models import WaitSession
from tests.conftest import make_client, register

V1 = "/api/v1"


@pytest.fixture
def client(db_engine):
    with make_client(db_engine) as c:
        yield c


def _start(c, h, limit=10):
    r = c.post(f"{V1}/wait", json={"limitMin": limit}, headers=h)
    assert r.status_code == 201
    return r.json()


def test_f3_07_start_update_and_phone_page(client):
    h = register(client)
    w = _start(client, h)
    token = w["token"]
    assert len(token) >= 20 and w["pageUrl"] == f"/wait/{token}"
    body = {"state": "waiting", "elapsedSec": 185, "limitMin": 10, "batteryPercent": 61, "cabinTempC": None}
    assert client.put(f"{V1}/wait/{token}", json=body, headers=h).json()["elapsedSec"] == 185
    js = client.get(f"{V1}/wait/{token}").json()  # 인증 없이 (토큰이 곧 권한)
    assert (js["state"], js["batteryPercent"], js["cabinTempC"], js["stale"]) == ("waiting", 61, None, False)
    page = client.get(f"/wait/{token}")
    assert page.status_code == 200 and "text/html" in page.headers["content-type"]
    # v5 폰 페이지: 남은 시간, 배터리는 값이 있을 때만, 실내 온도는 없으면 칸 자체를 그리지 않는다.
    assert "3분째" in page.text and "61%" in page.text and "남은 시간" in page.text and "7분" in page.text
    assert "실내 온도" not in page.text and "돌아가는 중" in page.text
    assert "http-equiv='refresh' content='5'" in page.text and "noindex" in page.text
    assert page.headers["cache-control"] == "no-store"


def test_f3_03_overdue_page_warns(client):
    h = register(client)
    token = _start(client, h)["token"]
    client.put(f"{V1}/wait/{token}", json={"state": "overdue", "elapsedSec": 610, "limitMin": 10, "batteryPercent": 60, "cabinTempC": 24.4}, headers=h)
    page = client.get(f"/wait/{token}").text
    assert "설정 시간이 지났어요" in page and "24℃" in page and "지남" in page


def test_f3_08_location_fields_rejected_and_token_hashed(client, db_engine):
    h = register(client)
    token = _start(client, h)["token"]
    for extra in ({"lat": 37.5}, {"lng": 127.0}, {"location": "x"}):
        body = {"state": "waiting", "elapsedSec": 1, "limitMin": 10, **extra}
        assert client.put(f"{V1}/wait/{token}", json=body, headers=h).status_code == 422
    assert client.post(f"{V1}/wait", json={"limitMin": 10, "lat": 37.5}, headers=h).status_code == 422
    with Session(db_engine) as s:
        rows = list(s.scalars(select(WaitSession)))
        assert [r.token_hash for r in rows] == [hash_token(token)]  # 토큰 원문은 저장하지 않는다
        assert not any(hasattr(r, "lat") or hasattr(r, "lng") for r in rows)


def test_f3_08_expired_and_ended_are_410(client, db_engine):
    h = register(client)
    token = _start(client, h)["token"]
    with Session(db_engine) as s, s.begin():
        s.get(WaitSession, hash_token(token)).expires_at = datetime.now(timezone.utc) - timedelta(minutes=1)
    assert client.get(f"{V1}/wait/{token}").status_code == 410
    assert client.get(f"/wait/{token}").status_code == 410
    body = {"state": "waiting", "elapsedSec": 1, "limitMin": 10}
    assert client.put(f"{V1}/wait/{token}", json=body, headers=h).json()["error"]["code"] == "WAIT_EXPIRED"
    t2 = _start(client, h)["token"]
    assert client.delete(f"{V1}/wait/{t2}", headers=h).status_code == 204
    assert client.get(f"/wait/{t2}").status_code == 410
    assert client.get("/wait/no-such-token").status_code == 404


def test_one_active_wait_per_device_and_others_cannot_write(client):
    h1, h2 = register(client), register(client)
    first = _start(client, h1)["token"]
    second = _start(client, h1)["token"]
    assert client.get(f"{V1}/wait/{first}").status_code == 410  # 새로 만들면 이전 것은 끝
    body = {"state": "waiting", "elapsedSec": 1, "limitMin": 10}
    assert client.put(f"{V1}/wait/{second}", json=body, headers=h2).status_code == 404
    assert client.post(f"{V1}/wait", json={"limitMin": 10}).status_code == 401


def test_sv_05_delete_me_removes_waits_and_cleanup_after_expiry(client, db_engine):
    from app.jobs.cleanup import cleanup

    h = register(client)
    _start(client, h)
    now = datetime.now(timezone.utc)
    assert cleanup(db_engine, now).waits == 0
    assert cleanup(db_engine, now + timedelta(hours=2, days=1, minutes=1)).waits == 1
    _start(client, h)
    assert client.delete(f"{V1}/devices/me", headers=h).status_code == 204
    with Session(db_engine) as s:
        assert s.scalar(select(func.count()).select_from(WaitSession)) == 0


def test_f3_10_phone_returning_reaches_car(client):
    # 보호자 폰 "돌아가는 중" → 차의 다음 상태 업로드 응답에 returning = true (차 화면 문구가 바뀐다)
    h = register(client)
    token = _start(client, h)["token"]
    body = {"state": "waiting", "elapsedSec": 60, "limitMin": 10}
    assert client.put(f"{V1}/wait/{token}", json=body, headers=h).json()["returning"] is False
    r = client.post(f"/wait/{token}/returning", follow_redirects=False)
    assert r.status_code == 303 and r.headers["location"] == f"/wait/{token}"
    assert client.put(f"{V1}/wait/{token}", json=body, headers=h).json()["returning"] is True
    assert "차 화면에" in client.get(f"/wait/{token}").text
    assert client.post("/wait/nope/returning").status_code == 404


def test_f3_12_phone_page_heat_banner_and_no_made_up_temperature(client, monkeypatch):
    # UPGRADE 2: 대기 중 실내 온도가 오르면 폰 페이지에 배너. 온도를 모르면 숫자 없이(여름이면) 일반 문구만.
    from app.api import wait as wait_api

    h = register(client)
    token = _start(client, h)["token"]
    put = lambda temp, sec: client.put(f"{V1}/wait/{token}", json={"state": "waiting", "elapsedSec": sec, "limitMin": 10, "cabinTempC": temp}, headers=h)
    put(23.4, 30)
    assert "오르고 있어요" not in client.get(f"/wait/{token}").text
    put(25.6, 90)
    page = client.get(f"/wait/{token}").text
    assert "실내 온도가 오르고 있어요" in page and "23℃ → 26℃" in page
    # 온도 신호가 없는 대기: 온도 칸 없음, 여름(7월)엔 일반 문구, 겨울엔 아무것도
    token2 = _start(client, h)["token"]
    client.put(f"{V1}/wait/{token2}", json={"state": "waiting", "elapsedSec": 30, "limitMin": 10}, headers=h)
    monkeypatch.setattr(wait_api, "_month", lambda: 7)
    summer = client.get(f"/wait/{token2}").text
    assert "실내 온도" not in summer and "여름에는 차 안이 빨리 더워져요" in summer and "℃" not in summer
    monkeypatch.setattr(wait_api, "_month", lambda: 12)
    assert "여름에는" not in client.get(f"/wait/{token2}").text
