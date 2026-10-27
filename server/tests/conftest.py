import os

import pytest
from sqlalchemy import create_engine, text
from sqlalchemy.engine import make_url
from sqlalchemy.pool import StaticPool

TEST_DATABASE_URL = os.environ.get(
    "TEST_DATABASE_URL", "postgresql+psycopg://pause:pause@127.0.0.1:5432/pause_test"
)


@pytest.fixture
def sqlite_engine():
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    yield engine
    engine.dispose()


@pytest.fixture(scope="session")
def pg_url() -> str:
    """PostgreSQL 테스트 DB URL. 연결할 수 없으면 @pytest.mark.pg 테스트를 skip한다."""
    url = make_url(TEST_DATABASE_URL)
    admin = create_engine(url.set(database="postgres"), isolation_level="AUTOCOMMIT", connect_args={"connect_timeout": 2})
    try:
        with admin.connect() as conn:
            exists = conn.execute(text("SELECT 1 FROM pg_database WHERE datname = :n"), {"n": url.database}).scalar()
            if not exists:
                conn.execute(text(f'CREATE DATABASE "{url.database}"'))
    except Exception as e:  # noqa: BLE001
        pytest.skip(f"PostgreSQL 연결 불가: {type(e).__name__}")
    finally:
        admin.dispose()
    return url.render_as_string(hide_password=False)
