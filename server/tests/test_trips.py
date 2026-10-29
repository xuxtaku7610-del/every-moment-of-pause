"""test_plan 서버 장 (SV-01~SV-06), F7: 여행 기록·데이터 삭제·요청 ID·레이트 리밋."""
import json
import logging

import pytest
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models import Device, Trip, TripEvent
from tests.conftest import make_client, register

V1 = "/api/v1"


@pytest.fixture
def client(db_engine):
    with make_client(db_engine) as c:
        yield c


# ───────── 여행 ─────────

def _trip(c, h) -> str:
    r = c.post(f"{V1}/trips", json={"startedAt": "2026-11-02T09:00:00+09:00", "originLabel": "서울", "destinationLabel": "부산",
                                    "plannedDistanceKm": 395, "passengerSummary": {"adults": 2, "children": 1, "pets": 1}, "powertrain": "ev"}, headers=h)
    assert r.status_code == 201 and r.json()["status"] == "active"
    return r.json()["tripId"]


EVENTS = [
    {"clientEventId": "e-001", "type": "recommendation_shown", "occurredAt": "2026-11-02T10:42:00+09:00", "payload": {"score": 0.8}},
    {"clientEventId": "e-002", "type": "recommendation_accepted", "occurredAt": "2026-11-02T10:42:05+09:00"},
    {"clientEventId": "e-003", "type": "stop_started", "occurredAt": "2026-11-02T11:00:00+09:00", "payload": {"reason": "charging"}},
    {"clientEventId": "e-004", "type": "stop_ended", "occurredAt": "2026-11-02T11:28:00+09:00"},
    {"clientEventId": "e-005", "type": "fatigue_peak", "occurredAt": "2026-11-02T10:55:00+09:00", "payload": {"value": 74}},
    {"clientEventId": "e-006", "type": "motion_peak", "occurredAt": "2026-11-02T12:10:00+09:00", "payload": {"value": 62}},
]


def test_events_are_idempotent_and_report_is_stable(client):
    # T9-12 [N] 같은 clientEventId 재전송 / T7-4 [E] 중복 업로드 → duplicates, 리포트 불변
    h = register(client)
    tid = _trip(client, h)
    assert client.post(f"{V1}/trips/{tid}/events", json={"events": EVENTS}, headers=h).json() == {"accepted": 6, "duplicates": 0}
    before = client.get(f"{V1}/trips/{tid}/report", headers=h).json()
    assert client.post(f"{V1}/trips/{tid}/events", json={"events": EVENTS[:3]}, headers=h).json() == {"accepted": 0, "duplicates": 3}
    assert client.get(f"{V1}/trips/{tid}/report", headers=h).json() == before
    assert client.patch(f"{V1}/trips/{tid}", json={"status": "ended", "endedAt": "2026-11-02T14:20:00+09:00", "actualDistanceKm": 398.4}, headers=h).status_code == 200
    rep = client.get(f"{V1}/trips/{tid}/report", headers=h).json()
    assert rep["durationMin"] == 320
    assert rep["stops"] == 1
    assert rep["longestContinuousDriveMin"] == 172  # 11:28 → 14:20
    assert rep["fatiguePeak"]["value"] == 74
    assert (rep["suggestionsShown"], rep["suggestionsAccepted"]) == (1, 1)
    assert rep["motionPeak"]["value"] == 62
    assert rep["tips"] and len(rep["tips"]) <= 2


def test_ended_trip_rejects_events_and_second_end(client):
    # T9-13 [E] 종료된 여행에 이벤트 → 409
    h = register(client)
    tid = _trip(client, h)
    body = {"status": "ended", "endedAt": "2026-11-02T14:20:00+09:00"}
    assert client.patch(f"{V1}/trips/{tid}", json=body, headers=h).status_code == 200
    assert client.patch(f"{V1}/trips/{tid}", json=body, headers=h).json()["error"]["code"] == "TRIP_ALREADY_ENDED"
    r = client.post(f"{V1}/trips/{tid}/events", json={"events": EVENTS[:1]}, headers=h)
    assert r.status_code == 409


def test_other_devices_trip_is_404_and_bad_event_type_422(client):
    h1, h2 = register(client), register(client)
    tid = _trip(client, h1)
    assert client.get(f"{V1}/trips/{tid}/report", headers=h2).status_code == 404
    bad = {"events": [{**EVENTS[0], "type": "location_point"}]}  # 좌표 같은 이벤트는 받지 않는다
    assert client.post(f"{V1}/trips/{tid}/events", json=bad, headers=h1).status_code == 422


# ───────── 데이터 삭제 · 요청 ID · 레이트 리밋 ─────────

def test_delete_me_removes_device_and_trips(client, db_engine):
    # G·J P1 내 데이터 삭제 = 서버 기기 토큰 삭제
    h = register(client)
    tid = _trip(client, h)
    client.post(f"{V1}/trips/{tid}/events", json={"events": EVENTS}, headers=h)
    assert client.delete(f"{V1}/devices/me", headers=h).status_code == 204
    with Session(db_engine) as s:
        assert s.scalar(select(func.count()).select_from(Trip)) == 0
        assert s.scalar(select(func.count()).select_from(TripEvent)) == 0
        assert s.scalar(select(func.count()).select_from(Device)) == 0
    assert client.post(f"{V1}/trips", json={"startedAt": "2026-11-02T09:00:00+09:00"}, headers=h).status_code == 401


def test_request_id_echo_and_structured_log(client, caplog):
    caplog.set_level(logging.INFO, logger="pause.access")
    r = client.get(f"{V1}/health", headers={"X-Request-ID": "demo-123"})
    assert r.headers["X-Request-ID"] == "demo-123"
    bad = client.get(f"{V1}/health", headers={"X-Request-ID": "x" * 200})
    assert bad.headers["X-Request-ID"] != "x" * 200
    lines = [json.loads(m) for m in caplog.messages if m.startswith("{")]
    assert lines[0]["requestId"] == "demo-123" and lines[0]["path"] == "/api/v1/health" and "ms" in lines[0]
    assert all("token" not in json.dumps(l).lower() for l in lines)


def test_rate_limit_per_device(db_engine):
    with make_client(db_engine, rate_limit=3) as c:
        h = register(c)
        tid = _trip(c, h)
        codes = [c.get(f"{V1}/trips/{tid}/report", headers=h).status_code for _ in range(3)]
        assert codes == [200, 200, 429]  # 기기 토큰 기준: 여행 1 + 조회 2 = 3 (등록은 IP 기준)
        r = c.get(f"{V1}/trips/{tid}/report", headers=h)
        assert r.json()["error"]["code"] == "RATE_LIMITED" and r.headers["Retry-After"] == "60"
        # /health는 제한하지 않는다
        assert c.get(f"{V1}/health").status_code in (200, 503)


def test_trip_rejects_child_ages(client):
    # 개인정보: 동승자 나이·이름은 받지 않는다 (인원 수만)
    h = register(client)
    r = client.post(f"{V1}/trips", json={"startedAt": "2026-11-02T09:00:00+09:00", "passengerSummary": {"children": [5]}}, headers=h)
    assert r.status_code == 422
    r = client.post(f"{V1}/trips", json={"startedAt": "2026-11-02T09:00:00+09:00", "passengerSummary": {"children": 1, "names": ["콩이"]}}, headers=h)
    assert r.status_code == 422


def test_cleanup_retention(db_engine, client):
    # database.md 보관 정책: 여행 90일, LLM 기록 30일
    from datetime import datetime, timedelta, timezone

    from app.jobs.cleanup import cleanup
    from app.models import LlmCallLog

    h = register(client)
    old = client.post(f"{V1}/trips", json={"startedAt": "2026-01-01T09:00:00+09:00"}, headers=h).json()["tripId"]
    new = client.post(f"{V1}/trips", json={"startedAt": "2026-10-20T09:00:00+09:00"}, headers=h).json()["tripId"]
    client.post(f"{V1}/trips/{old}/events", json={"events": EVENTS[:2]}, headers=h)
    now = datetime(2026, 10, 29, tzinfo=timezone.utc)
    with Session(db_engine) as s, s.begin():
        s.add(LlmCallLog(kind="rest_suggestion", source="template", latency_ms=1, created_at=now - timedelta(days=40)))
        s.add(LlmCallLog(kind="rest_suggestion", source="template", latency_ms=1, created_at=now - timedelta(days=1)))
    r = cleanup(db_engine, now)
    assert (r.trips, r.events, r.llm_logs) == (1, 2, 1)
    with Session(db_engine) as s:
        assert s.get(Trip, old) is None and s.get(Trip, new) is not None


def test_cleanup_idle_devices_after_180_days(db_engine, client):
    from datetime import datetime, timedelta, timezone

    from app.jobs.cleanup import cleanup

    h = register(client)
    _trip(client, h)  # 인증 요청 → last_seen_at 갱신
    now = datetime.now(timezone.utc)
    assert cleanup(db_engine, now).devices == 0
    with Session(db_engine) as s, s.begin():
        for d in s.scalars(select(Device)):
            d.last_seen_at = now - timedelta(days=181)
    r = cleanup(db_engine, now)
    assert r.devices == 1
    with Session(db_engine) as s:
        assert s.scalar(select(func.count()).select_from(Trip)) == 0
