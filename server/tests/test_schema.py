"""database.md §2: 모델(SQLite 단위)과 Alembic 마이그레이션(PostgreSQL)."""
import pytest
from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, inspect

from app.models import Base

EXPECTED_TABLES = {
    "devices", "rest_stops", "facility_codes", "rest_stop_facilities",
    "trips", "trip_events", "llm_call_logs", "pet_places", "vet_clinics", "wait_sessions", "eta_shares",
    "report_shares", "photo_uploads",
}


def test_models_create_on_sqlite(sqlite_engine):
    Base.metadata.create_all(sqlite_engine)
    assert set(inspect(sqlite_engine).get_table_names()) == EXPECTED_TABLES


@pytest.mark.pg
def test_alembic_upgrade_head_creates_expected_tables(pg_url):
    cfg = Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", pg_url)
    command.downgrade(cfg, "base")
    command.upgrade(cfg, "head")

    engine = create_engine(pg_url)
    try:
        tables = set(inspect(engine).get_table_names()) - {"alembic_version"}
        assert tables == EXPECTED_TABLES
        with engine.connect() as conn:
            assert conn.exec_driver_sql("SELECT count(*) FROM facility_codes").scalar() == 8
    finally:
        engine.dispose()


@pytest.mark.pg
def test_models_match_migration(pg_url):
    # 마이그레이션 SQL과 SQLAlchemy 모델의 테이블·컬럼이 같아야 한다.
    cfg = Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", pg_url)
    command.upgrade(cfg, "head")
    engine = create_engine(pg_url)
    try:
        insp = inspect(engine)
        for table in Base.metadata.sorted_tables:
            db_cols = {c["name"] for c in insp.get_columns(table.name)}
            assert db_cols == {c.name for c in table.columns}, table.name
    finally:
        engine.dispose()
