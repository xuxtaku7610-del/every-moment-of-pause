"""보관 기간 정리 잡 (database.md "보관 정책", 개인정보 안내 화면 "여행 기록은 서버에서 90일 뒤 지워요").

- trips·trip_events: 출발 90일 뒤 삭제
- llm_call_logs: 30일 뒤 삭제
- playlists·playlist_items: 30일 뒤 삭제 (편성 결과는 다시 만들 수 있다)
- devices: 마지막 접속 180일 뒤 삭제 (그 기기의 모든 기록과 함께)
일 1회 실행: cd server → .venv\\Scripts\\python -m app.jobs.cleanup
"""
import logging
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from sqlalchemy import Engine, delete, select
from sqlalchemy.orm import Session

from app.models import Device, LlmCallLog, Playlist, PlaylistItem, Trip, TripEvent

log = logging.getLogger(__name__)
TRIP_DAYS = 90
LOG_DAYS = 30
PLAYLIST_DAYS = 30
DEVICE_DAYS = 180


@dataclass(frozen=True)
class CleanupResult:
    trips: int
    events: int
    llm_logs: int
    playlists: int
    devices: int = 0


def cleanup(engine: Engine, now: datetime | None = None) -> CleanupResult:
    now = now or datetime.now(timezone.utc)
    with Session(engine) as s, s.begin():
        old_trips = list(s.scalars(select(Trip.id).where(Trip.started_at < now - timedelta(days=TRIP_DAYS))))
        events = s.execute(delete(TripEvent).where(TripEvent.trip_id.in_(old_trips))).rowcount if old_trips else 0
        trips = s.execute(delete(Trip).where(Trip.id.in_(old_trips))).rowcount if old_trips else 0
        logs = s.execute(delete(LlmCallLog).where(LlmCallLog.created_at < now - timedelta(days=LOG_DAYS))).rowcount
        old_pl = list(s.scalars(select(Playlist.id).where(Playlist.created_at < now - timedelta(days=PLAYLIST_DAYS))))
        if old_pl:
            s.execute(delete(PlaylistItem).where(PlaylistItem.playlist_id.in_(old_pl)))
        playlists = s.execute(delete(Playlist).where(Playlist.id.in_(old_pl))).rowcount if old_pl else 0
        from app.api.devices import delete_device

        idle = list(s.scalars(select(Device.id).where(Device.last_seen_at < now - timedelta(days=DEVICE_DAYS))))
        for d in idle:
            delete_device(s, d)
    r = CleanupResult(trips or 0, events or 0, logs or 0, playlists or 0, len(idle))
    log.info("cleanup trips=%d events=%d llm_logs=%d playlists=%d devices=%d", r.trips, r.events, r.llm_logs, r.playlists, r.devices)
    return r


if __name__ == "__main__":
    from app.config import get_settings
    from app.db import make_engine

    logging.basicConfig(level=logging.INFO)
    cleanup(make_engine(get_settings().database_url))
