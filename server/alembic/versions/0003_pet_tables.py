"""같이가개 공용 데이터·대기 공유 테이블 (database.md §2)

Revision ID: 0003
Revises: 0002
Create Date: 2026-10-29

- pet_places: 반려동물 동반 장소 (F4)
- vet_clinics: 동물병원 (F5)
- wait_sessions: 대기 공유 (F3) — 위치 컬럼 없음
"""
from alembic import op

revision = "0003"
down_revision = "0002"
branch_labels = None
depends_on = None

UPGRADE = [
    """
    CREATE TABLE pet_places (
        id          VARCHAR(32)  PRIMARY KEY,
        source_id   VARCHAR(64)  NOT NULL UNIQUE,
        source      VARCHAR(16)  NOT NULL CONSTRAINT ck_pet_places_source CHECK (source IN ('tourapi', 'sample')),
        name        VARCHAR(128) NOT NULL,
        category    VARCHAR(16)  NOT NULL CONSTRAINT ck_pet_places_category
                        CHECK (category IN ('restaurant', 'cafe', 'lodging', 'tour', 'walk')),
        lat         DOUBLE PRECISION NOT NULL,
        lng         DOUBLE PRECISION NOT NULL,
        address     VARCHAR(256),
        pet_policy  TEXT,
        updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    )
    """,
    "CREATE INDEX idx_pet_places_latlng ON pet_places (lat, lng)",
    """
    CREATE TABLE vet_clinics (
        id          VARCHAR(32)  PRIMARY KEY,
        license_no  VARCHAR(64)  NOT NULL UNIQUE,
        source      VARCHAR(16)  NOT NULL CONSTRAINT ck_vet_clinics_source CHECK (source IN ('licensing', 'sample')),
        name        VARCHAR(128) NOT NULL,
        address     VARCHAR(256),
        phone       VARCHAR(32),
        lat         DOUBLE PRECISION NOT NULL,
        lng         DOUBLE PRECISION NOT NULL,
        open        BOOLEAN      NOT NULL DEFAULT true,
        updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    )
    """,
    "CREATE INDEX idx_vet_clinics_latlng ON vet_clinics (lat, lng)",
    """
    CREATE TABLE wait_sessions (
        token_hash       VARCHAR(64) PRIMARY KEY,
        device_id        VARCHAR(32) NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
        state            VARCHAR(16) NOT NULL DEFAULT 'waiting'
                            CONSTRAINT ck_wait_sessions_state CHECK (state IN ('waiting', 'overdue', 'ended')),
        limit_min        INT         NOT NULL CONSTRAINT ck_wait_sessions_limit CHECK (limit_min BETWEEN 1 AND 120),
        elapsed_sec      INT         NOT NULL DEFAULT 0,
        battery_percent  INT,
        cabin_temp_c     DOUBLE PRECISION,
        created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
        updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
        expires_at       TIMESTAMPTZ NOT NULL
    )
    """,
    "CREATE INDEX idx_wait_sessions_device ON wait_sessions (device_id)",
]


def upgrade() -> None:
    for sql in UPGRADE:
        op.execute(sql)


def downgrade() -> None:
    for table in ("wait_sessions", "vet_clinics", "pet_places"):
        op.execute(f"DROP TABLE IF EXISTS {table} CASCADE")
