"""test_plan §9: /health"""
import httpx
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine

from app.api.health import LlmHealth
from app.main import API_PREFIX, create_app

MODEL = "gemma4:12b"


def tags_transport(models: list[str], counter: list[int] | None = None) -> httpx.MockTransport:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/api/tags"
        if counter is not None:
            counter[0] += 1
        return httpx.Response(200, json={"models": [{"name": m, "model": m} for m in models]})

    return httpx.MockTransport(handler)


def raising_transport(exc: type[Exception]) -> httpx.MockTransport:
    def handler(request: httpx.Request) -> httpx.Response:
        raise exc("mock", request=request)

    return httpx.MockTransport(handler)


def get_health(engine, llm: LlmHealth) -> httpx.Response:
    with TestClient(create_app(engine=engine, llm_health=llm, warm_up=False)) as client:
        return client.get(f"{API_PREFIX}/health")


def test_health_ok_without_token(sqlite_engine):
    # [N] /health 토큰 없이 200, 둘 다 정상이면 status: ok
    resp = get_health(sqlite_engine, LlmHealth("http://ollama", MODEL, transport=tags_transport([MODEL])))
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok", "db": "ok", "llm": "ok", "version": "0.1.0"}


def test_health_db_down_returns_503(sqlite_engine):
    # [E] DB 연결 실패 → 503, db: down, status: down
    bad = create_engine("postgresql+psycopg://x:x@127.0.0.1:1/x", connect_args={"connect_timeout": 1})
    resp = get_health(bad, LlmHealth("http://ollama", MODEL, transport=tags_transport([MODEL])))
    assert resp.status_code == 503
    assert resp.json()["db"] == "down"
    assert resp.json()["status"] == "down"


@pytest.mark.parametrize(
    ("transport", "expected"),
    [
        (raising_transport(httpx.ConnectError), "down"),
        (raising_transport(httpx.ReadTimeout), "degraded"),
        (tags_transport(["llama3:8b"]), "degraded"),
    ],
    ids=["no-response", "slow", "model-missing"],
)
def test_health_llm_states(sqlite_engine, transport, expected):
    # [E] Ollama 응답 없음 → 200, llm: down, status: degraded / 모델 없음 → llm: degraded
    resp = get_health(sqlite_engine, LlmHealth("http://ollama", MODEL, transport=transport))
    assert resp.status_code == 200
    assert resp.json()["llm"] == expected
    assert resp.json()["status"] == "degraded"


async def test_llm_health_cached_for_30_seconds():
    # [E] 30초 캐시 동작
    calls = [0]
    now = [1000.0]
    llm = LlmHealth("http://ollama", MODEL, transport=tags_transport([MODEL], calls), clock=lambda: now[0])
    assert await llm.status() == "ok"
    now[0] += 29
    assert await llm.status() == "ok"
    assert calls[0] == 1
    now[0] += 2
    await llm.status()
    assert calls[0] == 2
