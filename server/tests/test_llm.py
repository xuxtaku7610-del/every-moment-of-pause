"""test_plan §9: /llm/phrases, Ollama 요청 본문, 서버 시작 warm-up"""
import asyncio
import json
import logging
import time

import httpx
import pytest

from app.config import Settings
from app.llm.ollama import OllamaClient
from app.llm.phrases import check
from tests.conftest import make_client, register

URL = "/api/v1/llm/phrases"
FACTS = {"restStopName": "안성휴게소", "minutesAway": 18, "reasons": ["kids_zone", "ev_fast"],
         "fatigueLevel": "medium", "passengers": ["child_5", "dog"]}
BODY = {"kind": "rest_suggestion", "facts": FACTS, "maxSentences": 2}
SETTINGS = Settings(ollama_base_url="http://ollama", ollama_model="gemma4:12b", ollama_keep_alive="30m",
                    ollama_num_predict=60, llm_timeout_seconds=2.5)


def ollama_replying(text: str | None = None, seen: list | None = None, delay: float = 0.0, error: Exception | None = None):
    async def handler(request: httpx.Request) -> httpx.Response:
        if seen is not None:
            seen.append(json.loads(request.content))
        if delay:
            await asyncio.sleep(delay)
        if error is not None:
            raise error
        return httpx.Response(200, json={"message": {"role": "assistant", "content": text}})

    return OllamaClient(SETTINGS, transport=httpx.MockTransport(handler))


def post(db_engine, ollama: OllamaClient) -> httpx.Response:
    with make_client(db_engine, ollama=ollama) as c:
        return c.post(URL, json=BODY, headers=register(c))


def test_phrase_ok_within_limits_and_ollama_request_body(db_engine):
    # [N] 2문장·50자 이내 / [N] 요청 본문: /api/chat, think=false, keep_alive=30m, stream=false, num_predict=60, temperature=0.3
    seen: list = []
    text = "18분 뒤 안성휴게소에 키즈존과 급속충전기가 있어요. 들렀다 갈까요?"
    resp = post(db_engine, ollama_replying(text, seen))
    assert resp.status_code == 200
    assert resp.json()["text"] == text and resp.json()["source"] == "ollama"
    req = seen[0]
    assert req["model"] == "gemma4:12b"
    assert req["stream"] is False and req["think"] is False and req["keep_alive"] == "30m"
    assert req["options"] == {"num_predict": 60, "temperature": 0.3}
    assert [m["role"] for m in req["messages"]] == ["system", "user"]
    assert "안성휴게소" in req["messages"][1]["content"] and "키즈존" in req["messages"][1]["content"]


@pytest.mark.parametrize("text", [
    "18분 뒤 안성휴게소가 있어요. 키즈존도 있어요. 충전도 돼요. 들를까요?",  # 3문장 초과
    "18분 뒤 안성휴게소에는 키즈존과 급속충전기, 그리고 넓은 주차장과 맛있는 식당까지 있어요. 들를까요?",  # 50자 초과
    "18분 뒤 안성휴게소, 무조건 쉬어야 해요.",  # 금칙어
    "곧 휴게소가 나와요. 들를까요?",  # 장소·숫자 누락(지어낸 문장 방지)
])
def test_bad_output_returns_503_with_fallback(db_engine, text):
    # [E] 금칙어·과장 표현·길이 초과 → fallback
    resp = post(db_engine, ollama_replying(text))
    assert resp.status_code == 503
    assert resp.json() == {"error": {"code": "LLM_UNAVAILABLE", "message": "LLM 응답 실패"},
                           "fallback": "18분 뒤 안성휴게소에서 쉬어 갈까요?"}


def test_ollama_down_returns_503_fast(db_engine):
    # [E] Ollama 중단 → 503 + fallback
    resp = post(db_engine, ollama_replying(error=httpx.ConnectError("refused")))
    assert resp.status_code == 503
    assert "fallback" in resp.json()


def test_slow_ollama_times_out_within_2_5_seconds(db_engine):
    # [E] 응답 시간 2.5초 이내
    t0 = time.monotonic()
    resp = post(db_engine, ollama_replying("늦은 답", delay=10))
    elapsed = time.monotonic() - t0
    assert resp.status_code == 503
    assert elapsed < 3.5, elapsed  # 기기 등록·앱 기동 포함 여유


def test_phrase_requires_token_and_valid_kind(db_engine):
    with make_client(db_engine, ollama=ollama_replying("x")) as c:
        assert c.post(URL, json=BODY).status_code == 401
        h = register(c)
        assert c.post(URL, json={**BODY, "kind": "poem"}, headers=h).status_code == 422
        assert c.post(URL, json={**BODY, "maxSentences": 5}, headers=h).status_code == 422


def test_check_rules():
    assert check("18분 뒤 안성휴게소, 들를까요?", FACTS, 2) is None
    assert check("", FACTS, 2) == "empty"
    assert check("18분 뒤 안성휴게소! 들러요! 지금요!", FACTS, 2) == "too_many_sentences"


def test_warm_up_once_on_startup(db_engine):
    # [N] 서버 시작 시 warm-up 1회 (messages=[], keep_alive 포함)
    from fastapi.testclient import TestClient

    from app.api.health import LlmHealth
    from app.main import create_app

    seen: list = []
    app = create_app(engine=db_engine, llm_health=LlmHealth("http://ollama", "m"), ollama=ollama_replying("", seen))
    with TestClient(app):
        for _ in range(50):
            if seen:
                break
            time.sleep(0.02)
    assert len(seen) == 1
    assert seen[0]["messages"] == [] and seen[0]["keep_alive"] == "30m"


def test_server_starts_even_if_warm_up_fails(db_engine, caplog):
    # [N] Ollama가 꺼져 있어도 서버는 정상 기동하고 경고 로그만 남김
    from fastapi.testclient import TestClient

    from app.api.health import LlmHealth
    from app.main import create_app

    caplog.set_level(logging.WARNING)
    app = create_app(engine=db_engine, llm_health=LlmHealth("http://ollama", "m"),
                     ollama=ollama_replying(error=httpx.ConnectError("refused")))
    with TestClient(app) as c:
        for _ in range(50):
            if "warm-up failed" in caplog.text:
                break
            time.sleep(0.02)
        assert c.get("/api/v1/health").status_code == 200
    assert "warm-up failed" in caplog.text
