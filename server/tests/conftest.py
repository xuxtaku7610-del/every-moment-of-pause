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


FACILITY_CODES = {
    "nursing_room": "수유실", "kids_zone": "키즈존", "pet_zone": "반려동물 놀이터", "ev_fast": "전기차 급속충전기",
    "ev_slow": "전기차 완속충전기", "shower": "샤워실", "accessible_toilet": "장애인 화장실", "sleep_room": "수면실",
}


@pytest.fixture
def db_engine(sqlite_engine):
    """모델 테이블 + 편의시설 코드표(마이그레이션 시드와 같음)가 있는 SQLite."""
    from sqlalchemy.orm import Session

    from app.models import Base, FacilityCode

    Base.metadata.create_all(sqlite_engine)
    with Session(sqlite_engine) as s, s.begin():
        s.add_all(FacilityCode(code=c, label_ko=label) for c, label in FACILITY_CODES.items())
    return sqlite_engine


def make_client(engine, **kwargs):
    from fastapi.testclient import TestClient

    from app.api.health import LlmHealth
    from app.main import create_app

    app = create_app(engine=engine, llm_health=LlmHealth("http://ollama", "m"), warm_up=False, **kwargs)
    return TestClient(app)


def register(client) -> dict[str, str]:
    resp = client.post("/api/v1/devices", json={"platform": "pleos-connect", "appVersion": "0.1.0"})
    assert resp.status_code == 201
    return {"X-Device-Token": resp.json()["token"]}
