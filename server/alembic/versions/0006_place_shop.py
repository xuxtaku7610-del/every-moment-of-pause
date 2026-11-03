"""반려동물 동반 장소 분류에 shop(TourAPI 쇼핑, 콘텐츠 타입 38) 추가 (UI v9 주행 중 주변 추천 "마켓")

Revision ID: 0006
Revises: 0005
Create Date: 2026-11-03

- 이전에는 쇼핑(38)을 tour로 묶었다. 이미 들어 있는 행은 수집 잡을 캐시로 다시 돌려 고친다
  (`python -m app.jobs.ingest_pet_places --cache-only`, 외부 호출 없음).
"""
from alembic import op

revision = "0006"
down_revision = "0005"
branch_labels = None
depends_on = None

OLD = "('restaurant', 'cafe', 'lodging', 'tour', 'walk')"
NEW = "('restaurant', 'cafe', 'lodging', 'tour', 'walk', 'shop')"


def upgrade() -> None:
    op.execute("ALTER TABLE pet_places DROP CONSTRAINT IF EXISTS ck_pet_places_category")
    op.execute(f"ALTER TABLE pet_places ADD CONSTRAINT ck_pet_places_category CHECK (category IN {NEW})")


def downgrade() -> None:
    op.execute("UPDATE pet_places SET category = 'tour' WHERE category = 'shop'")
    op.execute("ALTER TABLE pet_places DROP CONSTRAINT IF EXISTS ck_pet_places_category")
    op.execute(f"ALTER TABLE pet_places ADD CONSTRAINT ck_pet_places_category CHECK (category IN {OLD})")
