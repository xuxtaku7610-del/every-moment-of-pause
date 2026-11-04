"""보관 기간 정리 잡 (database.md "보관 정책", 개인정보 안내 화면 "여행 기록은 서버에서 90일 뒤 지워요").

- trips·trip_events: 출발 90일 뒤 삭제
- llm_call_logs: 30일 뒤 삭제
- wait_sessions: 만료 1일 뒤 삭제 (대기 공유, F3)
- devices: 마지막 접속 180일 뒤 삭제 (그 기기의 모든 기록과 함께)
일 1회 실행: cd server → .venv\\Scripts\\python -m app.jobs.cleanup
"""
import logging
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from sqlalchemy import Engine, delete, select
from sqlalchemy.orm import Session

from app.models import Device, EtaShare, LlmCallLog, PhotoUpload, ReportShare, Trip, TripEvent, WaitSession

log = logging.getLogger(__name__)
TRIP_DAYS = 90
LOG_DAYS = 30
DEVICE_DAYS = 180
WAIT_KEEP_DAYS = 1


@dataclass(frozen=True)
class CleanupResult:
    trips: int
    events: int
    llm_logs: int
    devices: int = 0
    waits: int = 0


def cleanup(engine: Engine, now: datetime | None = None) -> CleanupResult:
    now = now or datetime.now(timezone.utc)
    with Session(engine) as s, s.begin():
        old_trips = list(s.scalars(select(Trip.id).where(Trip.started_at < now - timedelta(days=TRIP_DAYS))))
        events = s.execute(delete(TripEvent).where(TripEvent.trip_id.in_(old_trips))).rowcount if old_trips else 0
        trips = s.execute(delete(Trip).where(Trip.id.in_(old_trips))).rowcount if old_trips else 0
        logs = s.execute(delete(LlmCallLog).where(LlmCallLog.created_at < now - timedelta(days=LOG_DAYS))).rowcount
        waits = s.execute(delete(WaitSession).where(WaitSession.expires_at < now - timedelta(days=WAIT_KEEP_DAYS))).rowcount
        waits += s.execute(delete(EtaShare).where(EtaShare.expires_at < now - timedelta(days=WAIT_KEEP_DAYS))).rowcount or 0
        # v10-3: 폰 기록(24시간)은 만료 1일 뒤, 사진 업로드(10분)는 만료되면 바로 지운다(사진은 남기지 않는다)
        waits += s.execute(delete(ReportShare).where(ReportShare.expires_at < now - timedelta(days=WAIT_KEEP_DAYS))).rowcount or 0
        waits += s.execute(delete(PhotoUpload).where(PhotoUpload.expires_at < now)).rowcount or 0
        from app.api.devices import delete_device

        idle = list(s.scalars(select(Device.id).where(Device.last_seen_at < now - timedelta(days=DEVICE_DAYS))))
        for d in idle:
            delete_device(s, d)
    r = CleanupResult(trips or 0, events or 0, logs or 0, len(idle), waits or 0)
    log.info("cleanup trips=%d events=%d llm_logs=%d devices=%d waits=%d", r.trips, r.events, r.llm_logs, r.devices, r.waits)
    return r


if __name__ == "__main__":
    from app.config import get_settings
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    cleanup(make_engine(get_settings().database_url))
