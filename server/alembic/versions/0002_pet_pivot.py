"""같이가개 전환: 라디오·미션 테이블 삭제, 여행 이벤트 종류 변경 (docs/plan/REUSE_MAP.md)

Revision ID: 0002
Revises: 0001
Create Date: 2026-10-29

- content_episodes·missions·playlists·playlist_items 삭제 (라디오·원정대 제외)
- trip_events.event_type: kid_question 삭제, motion_peak·wait_started·wait_ended 추가
"""
from alembic import op

revision = "0002"
down_revision = "0001"
branch_labels = None
depends_on = None

OLD_TYPES = (
    "recommendation_shown", "recommendation_accepted", "recommendation_declined",
    "stop_started", "stop_ended", "fatigue_peak", "kid_question",
    "pause_activity_started", "pause_activity_ended",
)
NEW_TYPES = (
    "recommendation_shown", "recommendation_accepted", "recommendation_declined",
    "stop_started", "stop_ended", "fatigue_peak", "motion_peak",
    "pause_activity_started", "pause_activity_ended", "wait_started", "wait_ended",
)


def _check(types: tuple[str, ...]) -> str:
    return "event_type IN (" + ", ".join(f"'{t}'" for t in types) + ")"


def upgrade() -> None:
    for table in ("playlist_items", "playlists", "missions", "content_episodes"):
        op.execute(f"DROP TABLE IF EXISTS {table} CASCADE")
    op.execute("ALTER TABLE trip_events DROP CONSTRAINT IF EXISTS trip_events_event_type_check")
    op.execute("ALTER TABLE trip_events DROP CONSTRAINT IF EXISTS ck_trip_events_event_type")
    op.execute("DELETE FROM trip_events WHERE event_type = 'kid_question'")
    op.execute(f"ALTER TABLE trip_events ADD CONSTRAINT ck_trip_events_event_type CHECK ({_check(NEW_TYPES)})")


def downgrade() -> None:
    op.execute("ALTER TABLE trip_events DROP CONSTRAINT IF EXISTS ck_trip_events_event_type")
    op.execute("DELETE FROM trip_events WHERE event_type IN ('motion_peak', 'wait_started', 'wait_ended')")
    op.execute(f"ALTER TABLE trip_events ADD CONSTRAINT ck_trip_events_event_type CHECK ({_check(OLD_TYPES)})")
    op.execute(
        """
        CREATE TABLE content_episodes (
            id VARCHAR(32) PRIMARY KEY, topic VARCHAR(32) NOT NULL, title VARCHAR(128) NOT NULL, script TEXT NOT NULL,
            quiz JSONB, duration_sec INT NOT NULL, min_age INT NOT NULL DEFAULT 4, max_age INT NOT NULL DEFAULT 12,
            reviewed BOOLEAN NOT NULL DEFAULT false, created_at TIMESTAMPTZ NOT NULL DEFAULT now())
        """
    )
    op.execute(
        """
        CREATE TABLE missions (
            id VARCHAR(32) PRIMARY KEY, context VARCHAR(16) NOT NULL, title VARCHAR(64) NOT NULL, instructions TEXT NOT NULL,
            duration_sec INT NOT NULL, min_age INT NOT NULL DEFAULT 4, reviewed BOOLEAN NOT NULL DEFAULT false)
        """
    )
    op.execute(
        """
        CREATE TABLE playlists (
            id VARCHAR(32) PRIMARY KEY, device_id VARCHAR(32) NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
            target_sec INT NOT NULL, total_sec INT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now())
        """
    )
    op.execute(
        """
        CREATE TABLE playlist_items (
            playlist_id VARCHAR(32) NOT NULL REFERENCES playlists(id) ON DELETE CASCADE, position INT NOT NULL,
            episode_id VARCHAR(32) NOT NULL REFERENCES content_episodes(id), PRIMARY KEY (playlist_id, position))
        """
    )
