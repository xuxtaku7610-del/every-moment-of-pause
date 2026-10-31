"""UPGRADE 3 동물병원 사전 접수 + 실시간 도착 알림 (F5-07~10).

병원 접수함 /clinic/{token}: 보호자가 직접 전달한 링크. 반려견 이름·증상·메모·도착 예정만(위치 없음).
앱이 도착 예정을 갱신하면 접수함이 그 값을 보여 주고, 병원이 누른 응답은 앱이 받아 간다.
"""
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy.orm import Session

from app.api import eta
from app.api.deps import hash_token
from app.models import EtaShare
from tests.conftest import make_client, register

V1 = "/api/v1"
BODY = {"petName": "콩이", "vetName": "여주동물병원", "etaMinutes": 14, "symptoms": ["축 처짐"], "memo": "숨이 빨라요",
        "tripLine": "이동 40분 · 멀미 주의 1회(최고 46)", "intake": True}


@pytest.fixture
def client(db_engine):
    with make_client(db_engine) as c:
        yield c


def _intake(client, h, **over):
    r = client.post(f"{V1}/eta", json={**BODY, **over}, headers=h)
    assert r.status_code == 201
    return r.json()


def test_f5_07_intake_makes_separate_clinic_link(client, db_engine):
    h = register(client)
    res = _intake(client, h)
    ct = res["clinicToken"]
    assert res["clinicUrl"] == f"/clinic/{ct}" and ct != res["token"] and len(ct) >= 20
    page = client.get(f"/clinic/{ct}")
    assert page.status_code == 200 and page.headers["cache-control"] == "no-store"
    for text in ("사전 접수", "콩이", "축 처짐", "숨이 빨라요", "멀미 주의 1회", "14분"):
        assert text in page.text
    # 병원 접수함에만 응답 버튼이 있다. 보호자 링크로는 응답할 수 없다.
    assert "/reply?r=ready" in page.text and "/reply?r=" not in client.get(f"/eta/{res['token']}").text
    assert client.post(f"/clinic/{res['token']}/reply?r=ready").status_code == 404
    with Session(db_engine) as s:
        row = s.get(EtaShare, hash_token(res["token"]))
        assert row.clinic_token_hash == hash_token(ct) and row.clinic_seen_at is not None


def test_f5_07_plain_share_has_no_clinic_link(client):
    h = register(client)
    res = _intake(client, h, intake=False)
    assert res["clinicToken"] is None and res["clinicUrl"] is None


def test_f5_08_eta_update_shows_on_clinic_page(client, db_engine):
    h = register(client)
    res = _intake(client, h)
    r = client.put(f"{V1}/eta/{res['token']}", json={"etaMinutes": 6}, headers=h)
    assert r.status_code == 200 and r.json()["etaMinutes"] == 6 and r.json()["reply"] is None
    page = client.get(res["clinicUrl"]).text
    assert "6분" in page and "갱신" in page
    # 도착
    assert client.put(f"{V1}/eta/{res['token']}", json={"etaMinutes": 0, "arrived": True}, headers=h).json()["arrived"] is True
    assert "도착했어요" in client.get(res["clinicUrl"]).text
    # 다른 기기·인증 없음은 못 바꾼다, 위치 필드 422
    other = register(client)
    assert client.put(f"{V1}/eta/{res['token']}", json={"etaMinutes": 3}, headers=other).status_code == 404
    assert client.put(f"{V1}/eta/{res['token']}", json={"etaMinutes": 3}).status_code == 401
    assert client.put(f"{V1}/eta/{res['token']}", json={"etaMinutes": 3, "lat": 37.3}, headers=h).status_code == 422


def test_f5_09_clinic_reply_reaches_app_and_guardian(client):
    h = register(client)
    res = _intake(client, h)
    assert client.get(f"{V1}/eta/{res['token']}", headers=h).json()["seen"] is False
    client.get(res["clinicUrl"])
    r = client.post(f"{res['clinicUrl']}/reply?r=ready", follow_redirects=False)
    assert r.status_code == 303 and r.headers["location"] == res["clinicUrl"]
    st = client.get(f"{V1}/eta/{res['token']}", headers=h).json()
    assert st["reply"] == "ready" and st["seen"] is True and st["replyAt"]
    assert "도착하면 바로 볼게요" in client.get(f"/eta/{res['token']}").text
    assert "도착하면 바로 볼게요" in client.get(res["clinicUrl"]).text
    # 모르는 응답 값은 400, 바꿀 수 있다(마지막 응답이 이긴다)
    assert client.post(f"{res['clinicUrl']}/reply?r=xyz", follow_redirects=False).status_code == 400
    client.post(f"{res['clinicUrl']}/reply?r=busy", follow_redirects=False)
    assert client.get(f"{V1}/eta/{res['token']}", headers=h).json()["reply"] == "busy"


def test_f5_10_clinic_link_expires_with_share(client, db_engine):
    h = register(client)
    res = _intake(client, h)
    with Session(db_engine) as s:
        row = s.get(EtaShare, hash_token(res["token"]))
        later = datetime.now(timezone.utc) + timedelta(minutes=eta.TTL_MIN + 1)
        assert eta.render_clinic(row, later)[0] == 410
    assert client.get("/clinic/unknown").status_code == 404
    assert client.post("/clinic/unknown/reply?r=ready", follow_redirects=False).status_code == 404
