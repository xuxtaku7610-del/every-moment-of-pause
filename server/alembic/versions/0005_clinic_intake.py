"""동물병원 사전 접수 + 실시간 도착 알림 (UPGRADE 3)

Revision ID: 0005
Revises: 0004
Create Date: 2026-10-30

- eta_shares에 병원 접수함 링크(clinic_token_hash, 보호자 링크와 다른 토큰 — 해시만)와
  도착 예정 갱신(eta_at·updated_at·arrived), 병원 응답(clinic_reply·clinic_reply_at), 병원이 열어 본 시각(clinic_seen_at).
- 위치 컬럼은 여전히 없다. 2시간 만료도 그대로.
"""
from alembic import op

revision = "0005"
down_revision = "0004"
branch_labels = None
depends_on = None

UPGRADE = [
    "ALTER TABLE eta_shares ADD COLUMN clinic_token_hash VARCHAR(64)",
    "ALTER TABLE eta_shares ADD COLUMN eta_at TIMESTAMPTZ",
    "ALTER TABLE eta_shares ADD COLUMN updated_at TIMESTAMPTZ",
    "ALTER TABLE eta_shares ADD COLUMN arrived BOOLEAN NOT NULL DEFAULT false",
    "ALTER TABLE eta_shares ADD COLUMN clinic_reply VARCHAR(16)",
    "ALTER TABLE eta_shares ADD COLUMN clinic_reply_at TIMESTAMPTZ",
    "ALTER TABLE eta_shares ADD COLUMN clinic_seen_at TIMESTAMPTZ",
    "ALTER TABLE eta_shares ADD CONSTRAINT ck_eta_shares_reply CHECK (clinic_reply IS NULL OR clinic_reply IN ('received', 'ready', 'busy'))",
    "CREATE UNIQUE INDEX uq_eta_shares_clinic ON eta_shares (clinic_token_hash)",
]


def upgrade() -> None:
    for sql in UPGRADE:
        op.execute(sql)


def downgrade() -> None:
    op.execute("DROP INDEX IF EXISTS uq_eta_shares_clinic")
    op.execute("ALTER TABLE eta_shares DROP CONSTRAINT IF EXISTS ck_eta_shares_reply")
    for col in ("clinic_seen_at", "clinic_reply_at", "clinic_reply", "arrived", "updated_at", "eta_at", "clinic_token_hash"):
        op.execute(f"ALTER TABLE eta_shares DROP COLUMN IF EXISTS {col}")
