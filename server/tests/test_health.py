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


def test_sv_08_logs_never_contain_service_key(caplog, db_engine):
    # 결정 10/30: httpx 등이 요청 URL을 로그에 찍어도 serviceKey 값은 *** 로 가린다 (app/logmask.py, create_app에서 설치)
    import logging

    from tests.conftest import make_client

    caplog.set_level(logging.INFO)
    with make_client(db_engine) as c:  # 앱을 만들면 마스킹이 설치된다
        c.get("/api/v1/health?serviceKey=SECRET4")
        url = httpx.URL("https://apis.data.go.kr/B551011/KorPetTourService2/areaBasedList2?serviceKey=SECRET%2Babc&MobileOS=ETC&_type=json")
        logging.getLogger("httpx").info('HTTP Request: %s %s "%s %d %s"', "GET", url, "HTTP/1.1", 200, "OK")
        logging.getLogger("app.jobs").warning("failed for https://x.example/a?ServiceKey=SECRET2&b=1")
    assert "SECRET" not in caplog.text
    assert "serviceKey=***" in caplog.text and "ServiceKey=***" in caplog.text
    assert "MobileOS=ETC" in caplog.text  # 다른 쿼리 값은 그대로


def test_sv_08_ingest_job_logs_mask_key(caplog, tmp_path):
    # 수집 잡이 실제 httpx로 부를 때도: 요청 실패 로그·httpx 로그 어디에도 키가 없다
    import logging

    from app import logmask
    from app.jobs import ingest_pet_places

    logmask.install()
    caplog.set_level(logging.DEBUG)

    def transport(request: httpx.Request) -> httpx.Response:
        logging.getLogger("httpx").info("HTTP Request: GET %s", request.url)
        return httpx.Response(500, request=request)

    with pytest.raises(httpx.HTTPStatusError):
        with httpx.Client(transport=httpx.MockTransport(transport)) as cl:
            cl.get(ingest_pet_places.TOUR_BASE + "/areaBasedList2", params={"serviceKey": "TOPSECRET", "areaCode": "32"}).raise_for_status()
    assert "TOPSECRET" not in caplog.text and "serviceKey=***" in caplog.text
