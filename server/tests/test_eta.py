"""V5 §5 동물병원 도착 예정 공유: 위치 없음, 토큰 해시, 2시간 만료."""
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy.orm import Session

from app.api import eta
from app.api.deps import hash_token
from app.models import EtaShare
from tests.conftest import make_client, register

V1 = "/api/v1"


@pytest.fixture
def client(db_engine):
    with make_client(db_engine) as c:
        yield c


def test_f5_05_eta_share_page_without_location(client, db_engine):
    h = register(client)
    body = {"petName": "콩이", "vetName": "강릉동물병원", "etaMinutes": 12, "symptoms": ["구토"], "memo": "구토 두 번, 물은 조금", "tripLine": "주행 2시간 10분(휴식 제외) · 주행 자극 최고 46 · 관찰 신고 없음"}
    r = client.post(f"{V1}/eta", json=body, headers=h)
    assert r.status_code == 201
    token = r.json()["token"]
    assert r.json()["pageUrl"] == f"/eta/{token}" and len(token) >= 20
    page = client.get(f"/eta/{token}")
    assert page.status_code == 200 and "콩이" in page.text and "강릉동물병원" in page.text and "구토 두 번, 물은 조금" in page.text and "주행 자극 최고 46" in page.text
    assert "12분 뒤 도착" in page.text and "위치는 공유하지 않고" in page.text and page.headers["cache-control"] == "no-store"
    with Session(db_engine) as s:
        assert s.get(EtaShare, hash_token(token)) is not None and s.get(EtaShare, token) is None
    # 위치 필드는 받지 않는다, 인증 필요
    assert client.post(f"{V1}/eta", json={**body, "lat": 37.7}, headers=h).status_code == 422
    assert client.post(f"{V1}/eta", json=body).status_code == 401


def test_f5_05_eta_share_expires_after_two_hours(client, db_engine):
    h = register(client)
    token = client.post(f"{V1}/eta", json={"petName": "콩이", "vetName": "병원", "etaMinutes": 5}, headers=h).json()["token"]
    with Session(db_engine) as s:
        row = s.get(EtaShare, hash_token(token))
        later = datetime.now(timezone.utc) + timedelta(minutes=eta.TTL_MIN + 1)
        assert eta.render(row, later)[0] == 410
    assert client.get("/eta/unknown").status_code == 404
