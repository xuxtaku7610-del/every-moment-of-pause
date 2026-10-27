"""초기 스키마 (database.md §2 DDL)

Revision ID: 0001
Revises:
Create Date: 2026-10-27

PostgreSQL 전용 DDL(JSONB, BIGSERIAL, TIMESTAMPTZ)을 그대로 쓴다. 마이그레이션은 PostgreSQL에서만 검증한다.
"""
from alembic import op

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None

UPGRADE = [
    """
    CREATE TABLE devices (
        id            VARCHAR(32) PRIMARY KEY,
        token_hash    VARCHAR(128) NOT NULL,
        platform      VARCHAR(32)  NOT NULL,
        app_version   VARCHAR(16)  NOT NULL,
        created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
        last_seen_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    )
    """,
    """
    CREATE TABLE rest_stops (
        id          VARCHAR(32)  PRIMARY KEY,
        ex_code     VARCHAR(32)  NOT NULL UNIQUE,
        name        VARCHAR(64)  NOT NULL,
        stop_type   VARCHAR(16)  NOT NULL CHECK (stop_type IN ('service_area', 'drowsy_shelter')),
        route_name  VARCHAR(64)  NOT NULL,
        direction   VARCHAR(32)  NOT NULL,
        lat         DOUBLE PRECISION NOT NULL,
        lng         DOUBLE PRECISION NOT NULL,
        phone       VARCHAR(32),
        address     VARCHAR(255),
        updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    )
    """,
    "CREATE INDEX idx_rest_stops_route ON rest_stops (route_name, direction)",
    "CREATE INDEX idx_rest_stops_latlng ON rest_stops (lat, lng)",
    """
    CREATE TABLE facility_codes (
        code      VARCHAR(32) PRIMARY KEY,
        label_ko  VARCHAR(64) NOT NULL
    )
    """,
    """
    CREATE TABLE rest_stop_facilities (
        rest_stop_id   VARCHAR(32) NOT NULL REFERENCES rest_stops(id) ON DELETE CASCADE,
        facility_code  VARCHAR(32) NOT NULL REFERENCES facility_codes(code),
        quantity       INTEGER     NOT NULL DEFAULT 1 CHECK (quantity >= 0),
        PRIMARY KEY (rest_stop_id, facility_code)
    )
    """,
    """
    CREATE TABLE content_episodes (
        id            VARCHAR(32)  PRIMARY KEY,
        topic         VARCHAR(32)  NOT NULL CHECK (topic IN ('proverb', 'common_sense', 'safety', 'manners')),
        title         VARCHAR(128) NOT NULL,
        script        TEXT         NOT NULL,
        quiz          JSONB,
        duration_sec  INTEGER      NOT NULL CHECK (duration_sec BETWEEN 30 AND 900),
        min_age       INTEGER      NOT NULL DEFAULT 4,
        max_age       INTEGER      NOT NULL DEFAULT 12,
        reviewed      BOOLEAN      NOT NULL DEFAULT FALSE,
        created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
        CHECK (min_age <= max_age)
    )
    """,
    "CREATE INDEX idx_episodes_lookup ON content_episodes (topic, reviewed, min_age, max_age)",
    """
    CREATE TABLE missions (
        id            VARCHAR(32)  PRIMARY KEY,
        context       VARCHAR(16)  NOT NULL CHECK (context IN ('rest_stop', 'charging')),
        title         VARCHAR(64)  NOT NULL,
        instructions  TEXT         NOT NULL,
        duration_sec  INTEGER      NOT NULL,
        min_age       INTEGER      NOT NULL DEFAULT 4,
        reviewed      BOOLEAN      NOT NULL DEFAULT FALSE
    )
    """,
    """
    CREATE TABLE playlists (
        id          VARCHAR(32) PRIMARY KEY,
        device_id   VARCHAR(32) NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
        target_sec  INTEGER     NOT NULL,
        total_sec   INTEGER     NOT NULL,
        created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
    )
    """,
    """
    CREATE TABLE playlist_items (
        playlist_id  VARCHAR(32) NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
        position     INTEGER     NOT NULL,
        episode_id   VARCHAR(32) NOT NULL REFERENCES content_episodes(id),
        PRIMARY KEY (playlist_id, position)
    )
    """,
    """
    CREATE TABLE trips (
        id                   VARCHAR(32) PRIMARY KEY,
        device_id            VARCHAR(32) NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
        status               VARCHAR(16) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'ended')),
        origin_label         VARCHAR(64),
        destination_label    VARCHAR(64),
        planned_distance_km  NUMERIC(7,1),
        actual_distance_km   NUMERIC(7,1),
        powertrain           VARCHAR(8) CHECK (powertrain IN ('ev', 'ice', 'hev')),
        passenger_summary    JSONB       NOT NULL DEFAULT '{}'::jsonb,
        started_at           TIMESTAMPTZ NOT NULL,
        ended_at             TIMESTAMPTZ
    )
    """,
    "CREATE INDEX idx_trips_device ON trips (device_id, started_at DESC)",
    """
    CREATE TABLE trip_events (
        id               BIGSERIAL   PRIMARY KEY,
        trip_id          VARCHAR(32) NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
        client_event_id  VARCHAR(64) NOT NULL,
        event_type       VARCHAR(32) NOT NULL CHECK (event_type IN (
                            'recommendation_shown', 'recommendation_accepted', 'recommendation_declined',
                            'stop_started', 'stop_ended', 'fatigue_peak', 'kid_question',
                            'pause_activity_started', 'pause_activity_ended')),
        rest_stop_id     VARCHAR(32) REFERENCES rest_stops(id),
        payload          JSONB       NOT NULL DEFAULT '{}'::jsonb,
        occurred_at      TIMESTAMPTZ NOT NULL,
        received_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
        UNIQUE (trip_id, client_event_id)
    )
    """,
    "CREATE INDEX idx_trip_events_trip ON trip_events (trip_id, occurred_at)",
    """
    CREATE TABLE llm_call_logs (
        id             BIGSERIAL   PRIMARY KEY,
        device_id      VARCHAR(32) REFERENCES devices(id) ON DELETE SET NULL,
        kind           VARCHAR(32) NOT NULL,
        source         VARCHAR(16) NOT NULL CHECK (source IN ('ollama', 'template')),
        latency_ms     INTEGER     NOT NULL,
        fallback_used  BOOLEAN     NOT NULL DEFAULT FALSE,
        created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
    )
    """,
    """
    INSERT INTO facility_codes (code, label_ko) VALUES
        ('nursing_room', '수유실'),
        ('kids_zone', '키즈존'),
        ('pet_zone', '반려동물 놀이터'),
        ('ev_fast', '전기차 급속충전기'),
        ('ev_slow', '전기차 완속충전기'),
        ('shower', '샤워실'),
        ('accessible_toilet', '장애인 화장실'),
        ('sleep_room', '수면실')
    """,
]

TABLES = [
    "llm_call_logs", "trip_events", "trips", "playlist_items", "playlists", "missions",
    "content_episodes", "rest_stop_facilities", "facility_codes", "rest_stops", "devices",
]


def upgrade() -> None:
    for stmt in UPGRADE:
        op.execute(stmt)


def downgrade() -> None:
    for table in TABLES:
        op.execute(f"DROP TABLE IF EXISTS {table} CASCADE")
