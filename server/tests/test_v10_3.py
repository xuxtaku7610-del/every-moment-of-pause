"""test_plan SV-20~24 (v10-3): 폰으로 받는 이동 기록, 반려견 사진 QR 업로드, 대기 보호자 2명·온도 경고."""
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy.orm import Session

from app.api.deps import hash_token
from app.models import PhotoUpload, ReportShare
from tests.conftest import make_client, register

V1 = "/api/v1"
JPEG = b"\xff\xd8\xff\xe0" + b"\x00" * 64
REPORT = {
    "petNames": ["콩이", "보리"], "date": "11월 4일", "destination": "강릉", "totalMin": 180, "distance": "230km", "stops": 1, "reports": 1,
    "timeline": [{"kind": "START", "time": "0:00", "title": "출발", "detail": "오전 9:01"}, {"kind": "REPORT", "time": "0:39", "title": "헐떡임 신고"}],
    "series": [10, 12, 20, 45, 55, 30], "sampleMin": 5,
    "marks": [{"kind": "HIGH", "minute": 15, "lengthMin": 10}, {"kind": "SYMPTOM", "minute": 5, "label": "헐떡임"}, {"kind": "STOP", "minute": 20, "lengthMin": 5, "label": "휴식"}],
    "share": [{"key": "관찰 신고", "value": "헐떡임 1회"}], "insight": "출발 2시간 뒤 주행 자극이 보통 이상이 됐어요.",
}


@pytest.fixture
def client(db_engine):
    with make_client(db_engine) as c:
        yield c


def test_sv_20_report_share_page_and_no_location(client, db_engine):
    h = register(client)
    r = client.post(f"{V1}/reports", json=REPORT, headers=h)
    assert r.status_code == 201
    token = r.json()["token"]
    assert r.json()["pageUrl"] == f"/report/{token}"
    page = client.get(f"/report/{token}")
    assert page.status_code == 200 and page.headers["cache-control"] == "no-store"
    t = page.text
    assert "콩이·보리 이동 기록" in t and "3시간" in t and "230km" in t and "헐떡임 1회" in t and "<svg" in t and "polyline" in t
    assert "반려견 측정 아님" in t and "위치는 담지 않아요" in t and "24시간" in t
    # 위치·좌표 필드는 받지 않는다
    for extra in ({"lat": 37.5}, {"route": [[37.5, 127.0]]}, {"location": "x"}):
        assert client.post(f"{V1}/reports", json={**REPORT, **extra}, headers=h).status_code == 422
    assert client.post(f"{V1}/reports", json={**REPORT, "series": [10, 120]}, headers=h).status_code == 422
    assert client.post(f"{V1}/reports", json=REPORT).status_code == 401
    # 토큰 원문은 저장하지 않는다, 24시간 뒤 만료
    with Session(db_engine) as s:
        row = s.get(ReportShare, hash_token(token))
        assert row is not None and token not in str(row.payload)
        row.expires_at = datetime.now(timezone.utc) - timedelta(seconds=1)
        s.commit()
    assert client.get(f"/report/{token}").status_code == 410
    assert client.get("/report/nope").status_code == 404


def test_sv_21_report_chart_empty_series():
    from app.api.reports import chart_svg
    assert "기록이 아직 없어요" in chart_svg([10], [])
    svg = chart_svg([0, 40, 80], [{"kind": "HIGH", "minute": 5, "lengthMin": 5}])
    assert "polyline" in svg and "fill-opacity='.22'" in svg


def test_sv_22_photo_upload_once_then_deleted_after_car_fetch(client, db_engine):
    h = register(client)
    r = client.post(f"{V1}/photos", headers=h)
    assert r.status_code == 201
    token = r.json()["token"]
    assert r.json()["pageUrl"] == f"/upload/{token}"
    page = client.get(f"/upload/{token}")
    assert page.status_code == 200 and "반려견 사진 올리기" in page.text and "type='file'" in page.text
    # 올리기 전 차가 물으면 204
    assert client.get(f"{V1}/photos/{token}", headers=h).status_code == 204
    # 형식·크기 검사
    assert client.post(f"/upload/{token}", content=b"not an image", headers={"Content-Type": "image/jpeg"}).status_code == 415
    assert client.post(f"/upload/{token}", content=JPEG, headers={"Content-Type": "image/gif"}).status_code == 415
    big = b"\xff\xd8\xff" + b"\x00" * (5 * 1024 * 1024)
    assert client.post(f"/upload/{token}", content=big, headers={"Content-Type": "image/jpeg"}).status_code == 413
    assert client.post(f"/upload/{token}", content=JPEG, headers={"Content-Type": "image/jpeg"}).status_code == 204
    assert client.post(f"/upload/{token}", content=JPEG, headers={"Content-Type": "image/jpeg"}).status_code == 409
    assert "사진을 보냈어요" in client.get(f"/upload/{token}").text
    # 다른 기기는 못 받는다
    assert client.get(f"{V1}/photos/{token}", headers=register(client)).status_code == 404
    got = client.get(f"{V1}/photos/{token}", headers=h)
    assert got.status_code == 200 and got.content == JPEG and got.headers["content-type"] == "image/jpeg"
    # 받아 간 뒤 서버의 사진은 지운다
    with Session(db_engine) as s:
        row = s.get(PhotoUpload, hash_token(token))
        assert row.image is None and row.fetched_at is not None
    assert client.get(f"{V1}/photos/{token}", headers=h).status_code == 410


def test_sv_23_photo_upload_expires_in_10_minutes(client, db_engine):
    h = register(client)
    token = client.post(f"{V1}/photos", headers=h).json()["token"]
    with Session(db_engine) as s:
        row = s.get(PhotoUpload, hash_token(token))
        assert (row.expires_at.replace(tzinfo=timezone.utc) if row.expires_at.tzinfo is None else row.expires_at) - datetime.now(timezone.utc) <= timedelta(minutes=10)
        row.expires_at = datetime.now(timezone.utc) - timedelta(seconds=1)
        s.commit()
    assert client.get(f"/upload/{token}").status_code == 410
    assert client.post(f"/upload/{token}", content=JPEG, headers={"Content-Type": "image/jpeg"}).status_code == 410
    assert client.get(f"{V1}/photos/{token}", headers=h).status_code == 410


def test_sv_24_wait_two_guardians_and_heat_alert(client):
    h = register(client)
    token = client.post(f"{V1}/wait", json={"limitMin": 10}, headers=h).json()["token"]
    body = {"state": "waiting", "elapsedSec": 60, "limitMin": 10, "cabinTempC": 23.0}
    client.put(f"{V1}/wait/{token}", json=body, headers=h)
    p2 = client.get(f"/wait/{token}?g=2").text
    assert "보호자 2 화면" in p2 and "돌아가는 중 (보호자 2)" in p2 and "returning?g=2" in p2 and "data-hot='0'" in p2
    assert "경고 소리·진동 켜기" in p2
    r = client.post(f"/wait/{token}/returning?g=2", follow_redirects=False)
    assert r.status_code == 303 and r.headers["location"] == f"/wait/{token}?g=2"
    assert client.put(f"{V1}/wait/{token}", json=body, headers=h).json()["returningBy"] == 2
    # 보호자 1이 나중에 눌러도 먼저 누른 사람(2)이 남는다
    client.post(f"/wait/{token}/returning?g=1", follow_redirects=False)
    assert "보호자 2가 돌아가는 중" in client.get(f"/wait/{token}").text
    # 온도가 오르면 큰 경고(data-hot=1)
    client.put(f"{V1}/wait/{token}", json={**body, "cabinTempC": 26.5}, headers=h)
    hot = client.get(f"/wait/{token}").text
    assert "data-hot='1'" in hot and "banner hot" in hot and "26℃" in hot


def test_sv_25_delete_device_and_cleanup_remove_reports_and_photos(client, db_engine):
    from app.jobs.cleanup import cleanup
    h = register(client)
    token = client.post(f"{V1}/reports", json=REPORT, headers=h).json()["token"]
    ptoken = client.post(f"{V1}/photos", headers=h).json()["token"]
    client.post(f"/upload/{ptoken}", content=JPEG, headers={"Content-Type": "image/jpeg"})
    # 만료된 사진 업로드는 정리 작업이 바로 지운다
    with Session(db_engine) as s:
        s.get(PhotoUpload, hash_token(ptoken)).expires_at = datetime.now(timezone.utc) - timedelta(seconds=1)
        s.commit()
    cleanup(db_engine)
    with Session(db_engine) as s:
        assert s.get(PhotoUpload, hash_token(ptoken)) is None
        assert s.get(ReportShare, hash_token(token)) is not None
    # "내 데이터 삭제" = 기기와 기록 링크를 함께 지운다
    assert client.delete(f"{V1}/devices/me", headers=h).status_code == 204
    with Session(db_engine) as s:
        assert s.get(ReportShare, hash_token(token)) is None
