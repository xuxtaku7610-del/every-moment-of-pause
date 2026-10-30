"""반려동물 동반 장소 세부 (TourAPI detailPetTour2·대표 이미지)

Revision ID: 0004
Revises: 0003
Create Date: 2026-10-30

- pet_places: image_url, space_note(실내·실외를 말하는 원문 문장), size_note(동반 가능 동물 원문), companion(동반 구분 원문).
  모두 NULL 허용 — 글에 없으면 비워 둔다(판정·추측하지 않는다).
- wait_sessions.guardian_returning: 보호자 폰 "돌아가는 중". cabin_temp_start_c: 대기 중 처음 받은 실내 온도(오르는지 비교).
- eta_shares: 동물병원 도착 예정 공유 (F5) — 위치 컬럼 없음, 2시간 만료.
"""
from alembic import op

revision = "0004"
down_revision = "0003"
branch_labels = None
depends_on = None

UPGRADE = [
    "ALTER TABLE pet_places ADD COLUMN image_url VARCHAR(512)",
    "ALTER TABLE pet_places ADD COLUMN space_note VARCHAR(100)",
    "ALTER TABLE pet_places ADD COLUMN size_note VARCHAR(100)",
    "ALTER TABLE pet_places ADD COLUMN large_dog_ok BOOLEAN",
    "ALTER TABLE pet_places ADD COLUMN indoor_ok BOOLEAN",
    "ALTER TABLE pet_places ADD COLUMN companion VARCHAR(64)",
    "ALTER TABLE wait_sessions ADD COLUMN guardian_returning BOOLEAN NOT NULL DEFAULT false",
    "ALTER TABLE wait_sessions ADD COLUMN cabin_temp_start_c DOUBLE PRECISION",
    """
    CREATE TABLE eta_shares (
        token_hash   VARCHAR(64)  PRIMARY KEY,
        device_id    VARCHAR(32)  NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
        pet_name     VARCHAR(32)  NOT NULL,
        vet_name     VARCHAR(128) NOT NULL,
        eta_minutes  INT          NOT NULL CONSTRAINT ck_eta_shares_eta CHECK (eta_minutes BETWEEN 0 AND 600),
        symptoms     VARCHAR(200),
        memo         VARCHAR(200),
        trip_line    VARCHAR(80),
        created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
        expires_at   TIMESTAMPTZ  NOT NULL
    )
    """,
    "CREATE INDEX idx_eta_shares_device ON eta_shares (device_id)",
]


def upgrade() -> None:
    for sql in UPGRADE:
        op.execute(sql)


def downgrade() -> None:
    op.execute("DROP TABLE IF EXISTS eta_shares CASCADE")
    op.execute("ALTER TABLE wait_sessions DROP COLUMN IF EXISTS guardian_returning")
    op.execute("ALTER TABLE wait_sessions DROP COLUMN IF EXISTS cabin_temp_start_c")
    for col in ("companion", "indoor_ok", "large_dog_ok", "size_note", "space_note", "image_url"):
        op.execute(f"ALTER TABLE pet_places DROP COLUMN IF EXISTS {col}")
