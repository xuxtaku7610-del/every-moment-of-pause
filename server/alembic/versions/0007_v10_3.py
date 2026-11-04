"""v10-3 편의 기능: 보호자 2명(돌아가는 중 누른 사람), 폰으로 받는 이동 기록, 반려견 사진 QR 업로드

Revision ID: 0007
Revises: 0006
Create Date: 2026-11-04

- wait_sessions.returning_by: 보호자 1·2 (이름은 받지 않는다)
- report_shares: 앱이 만든 기록 요약 JSON(타임라인·흔들림 값·병원 요약), 위치 없음, 24시간
- photo_uploads: 사진 1장(5MB 이하) 임시 보관, 차가 받아 가면 바이트를 지운다, 10분
"""
import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision = "0007"
down_revision = "0006"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("wait_sessions", sa.Column("returning_by", sa.Integer(), nullable=True))
    op.create_table(
        "report_shares",
        sa.Column("token_hash", sa.String(64), primary_key=True),
        sa.Column("device_id", sa.String(32), sa.ForeignKey("devices.id", ondelete="CASCADE"), nullable=False),
        sa.Column("payload", postgresql.JSONB(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("idx_report_shares_device", "report_shares", ["device_id"])
    op.create_table(
        "photo_uploads",
        sa.Column("token_hash", sa.String(64), primary_key=True),
        sa.Column("device_id", sa.String(32), sa.ForeignKey("devices.id", ondelete="CASCADE"), nullable=False),
        sa.Column("image", sa.LargeBinary(), nullable=True),
        sa.Column("content_type", sa.String(16), nullable=True),
        sa.Column("uploaded_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("fetched_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.CheckConstraint("content_type IS NULL OR content_type IN ('image/jpeg', 'image/png', 'image/webp')", name="ck_photo_uploads_type"),
    )
    op.create_index("idx_photo_uploads_device", "photo_uploads", ["device_id"])


def downgrade() -> None:
    op.drop_index("idx_photo_uploads_device", table_name="photo_uploads")
    op.drop_table("photo_uploads")
    op.drop_index("idx_report_shares_device", table_name="report_shares")
    op.drop_table("report_shares")
    op.drop_column("wait_sessions", "returning_by")
