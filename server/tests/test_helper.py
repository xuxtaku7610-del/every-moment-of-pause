"""test_plan SV-20~27 (콩이 도우미 2단계): /helper/ask·/llm/route, 근거 검사, 질문 원문 비저장.

Ollama는 MockTransport로 흉내 낸다(/api/embed = 낱말 기반 가짜 벡터, /api/chat = 분류 JSON 또는 설명 문장).
"""
import json
import logging

import httpx
import pytest
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.config import Settings
from app.helper import cards as card_store
from app.helper import grounding
from app.helper.service import Helper
from app.models import LlmCallLog
from tests.conftest import make_client, register

SETTINGS = Settings(ollama_base_url="http://ollama", ollama_model="gemma4:12b", ollama_keep_alive="30m", helper_timeout_seconds=1.0)
WATER = """---
id: water
title: 이동 중 물 주기
category: care
triggers: [물 얼마나]
voice: 쉬는 곳에서 물을 조금씩 줘요.
action: NEXT_REST
reviewed: true
sources: RSPCA
---
- 쉬는 곳에서 물을 조금씩 자주 줘요.
- 달리는 동안에는 그릇을 두지 않아요.
"""
TOXIN = """---
id: toxin
title: 먹으면 안 되는 것을 먹었어요
category: emergency
triggers: [초콜릿]
action: VET
reviewed: true
sources: Merck
---
- 바로 동물병원에 연락해요.
- 억지로 토하게 하지 않아요.
"""
CARDS = {c.id: c for c in (card_store.parse("water", WATER), card_store.parse("toxin", TOXIN))}
# 가짜 임베딩 축: 물 · 초콜릿 · 날씨
AXES = ("물", "초콜릿", "날씨")


def vec(text: str) -> list[float]:
    v = [1.0 if a in text else 0.0 for a in AXES]
    return v + [0.0 if any(v) else 1.0]  # 어느 축에도 없으면 따로 떨어진 축 (코사인 0)


def fake_ollama(route: dict | None = None, answer: str = "", seen: list | None = None, embed_fail: bool = False):
    async def handler(req: httpx.Request) -> httpx.Response:
        body = json.loads(req.content)
        if seen is not None:
            seen.append((req.url.path, body))
        if req.url.path == "/api/embed":
            if embed_fail:
                return httpx.Response(500)
            return httpx.Response(200, json={"embeddings": [vec(t) for t in body["input"]]})
        content = json.dumps(route or {"id": None, "confidence": 0}) if body.get("format") == "json" else answer
        return httpx.Response(200, json={"message": {"role": "assistant", "content": content}})

    return httpx.MockTransport(handler)


def helper(**kw) -> Helper:
    return Helper(SETTINGS, CARDS, transport=fake_ollama(**kw))


@pytest.mark.anyio
async def test_sv20_emergency_never_calls_chat():
    # [P0] 응급 말이면 LLM(chat)을 부르지 않고 응급 카드 줄
    seen: list = []
    a = await helper(seen=seen).ask("콩이가 초콜릿 먹었어", list(CARDS))
    assert a.route == "emergency" and a.id == "toxin" and a.source == "card"
    assert not [p for p, _ in seen if p == "/api/chat"]


@pytest.mark.anyio
async def test_sv21_vector_pick_then_grounded_explain():
    a = await helper(answer="쉬는 곳에서 물을 조금씩 자주 줘요.").ask("물은?", ["water"])
    assert (a.route, a.id, a.source, a.grounded) == ("vector", "water", "ollama", True)


@pytest.mark.anyio
async def test_sv22_ungrounded_answer_falls_back_to_card_lines():
    # 카드에 없는 숫자·음식 → 모델 답을 버리고 카드 줄 그대로
    a = await helper(answer="물은 200ml씩 30분마다 얼음물로 줘요.").ask("물은?", ["water"])
    assert a.source == "card" and not a.grounded
    assert a.answer == " ".join(CARDS["water"].lines[:3])


@pytest.mark.anyio
async def test_sv23_llm_route_outside_allowed_or_low_confidence_is_unknown():
    # 임베딩이 낮으면 LLM 분류 — 허용 밖 id·확신 0.6 미만은 모름
    assert (await helper(route={"id": "secret", "confidence": 0.9}).ask("오늘 산책 어때", ["water"])).id is None
    assert (await helper(route={"id": "water", "confidence": 0.4}).ask("오늘 산책 어때", ["water"])).id is None
    a = await helper(route={"id": "water", "confidence": 0.8}, answer="쉬는 곳에서 물을 조금씩 자주 줘요.").ask("목마르면?", ["water"])
    assert a.route == "llm" and a.id == "water"


@pytest.mark.anyio
async def test_sv24_embed_down_uses_llm_route_only():
    a = await helper(embed_fail=True, route={"id": None, "confidence": 0}).ask("물은?", ["water"])
    assert a.route == "none" and a.id is None


@pytest.mark.parametrize("answer,ok", [
    ("쉬는 곳에서 물을 조금씩 자주 줘요.", True),
    ("쉬는 곳에서 물을 10분마다 줘요.", False),  # 카드에 없는 숫자
    ("쉬는 곳에서 물에 소금을 타서 줘요.", False),  # 카드에 없는 음식
    ("진통제 1알을 먹여요.", False),  # 약·용량은 늘 실패
    ("탈수 질환입니다.", False),  # 진단 표현
])
def test_sv25_grounding_unit(answer, ok):
    c = CARDS["water"]
    assert grounding.check(answer, c.text, list(c.lines))[0] is ok


def test_sv26_api_auth_and_no_question_text_logged(db_engine, caplog):
    # [P0] 질문 원문은 DB(llm_call_logs)와 로그에 남지 않는다 · 토큰 없으면 401
    q = "콩이 물은 얼마나 줘야 해 비밀문장"
    with make_client(db_engine) as c:
        c.app.state.helper = helper(answer="쉬는 곳에서 물을 조금씩 자주 줘요.")
        assert c.post("/api/v1/helper/ask", json={"text": q, "allowed": ["water"]}).status_code == 401
        with caplog.at_level(logging.DEBUG):
            r = c.post("/api/v1/helper/ask", json={"text": q, "allowed": ["water"]}, headers=register(c))
        assert r.status_code == 200
        body = r.json()
        assert body["id"] == "water" and body["grounded"] is True and body["route"] == "vector"
        r2 = c.post("/api/v1/llm/route", json={"text": q, "allowed": ["water"]}, headers=register(c))
        assert r2.status_code == 200 and r2.json()["id"] == "water"
    assert "비밀문장" not in caplog.text
    with Session(db_engine) as s:
        logs = s.scalars(select(LlmCallLog)).all()
    assert {l.kind for l in logs} == {"helper", "helper_route"}
    assert all("비밀문장" not in (l.kind + l.source) for l in logs)


def test_sv27_input_limits(db_engine):
    with make_client(db_engine) as c:
        h = register(c)
        assert c.post("/api/v1/helper/ask", json={"text": "", "allowed": ["water"]}, headers=h).status_code == 422
        assert c.post("/api/v1/helper/ask", json={"text": "가" * 201, "allowed": ["water"]}, headers=h).status_code == 422
        assert c.post("/api/v1/helper/ask", json={"text": "물", "allowed": []}, headers=h).status_code == 422


def test_sv28_repo_cards_parse_and_drafts_unreviewed():
    cards = card_store.load()
    assert len(cards) >= 20
    drafts = {f.stem.removeprefix("care_") for f in (card_store.ROOT / "content" / "care" / "_drafts").glob("care_*.md")}
    assert drafts and all(not cards[i].reviewed for i in drafts)
    for c in cards.values():
        assert 2 <= len(c.lines) <= 4, c.id
        assert not grounding.ALWAYS_BAD.search(c.text), c.id


def test_sv29_collect_tool_robots_quotes_no_full_text(tmp_path, monkeypatch):
    # 자료 수집 도구: robots.txt 차단은 건너뜀 · 약/용량 문장은 인용 제외 · 후보는 _candidates(앱 assets 밖)에 인용 2개만
    import tools.collect_care_sources as t

    monkeypatch.setattr(t, "DELAY_SECONDS", 0)
    monkeypatch.setattr(t, "OUT", tmp_path / "_candidates")
    body = "<html><nav>menu water water</nav><p>" + " ".join([
        "Offer your dog small amounts of water at every stop on a long journey.",
        "Give 10 mg of paracetamol to a dog with water if it seems unwell today.",
        "Never leave a dog alone in a parked car, even with the window down a little.",
    ] + ["Filler sentence about something else entirely, which is long enough." for _ in range(30)]) + "</p><script>water()</script></html>"

    def handler(req: httpx.Request) -> httpx.Response:
        if req.url.path == "/robots.txt":
            return httpx.Response(200, text="User-agent: *\nDisallow: /private\n")
        return httpx.Response(200, text=body, headers={"content-type": "text/html"})

    with httpx.Client(transport=httpx.MockTransport(handler)) as c:
        robots: dict = {}
        assert t.allowed_by_robots("https://www.rspca.org.uk/dogs", robots, c)
        assert not t.allowed_by_robots("https://www.rspca.org.uk/private/x", robots, c)
        quotes = t.pick_quotes(t.sentences(c.get("https://www.rspca.org.uk/dogs").text), ("water", "journey"))
    assert quotes == ["Offer your dog small amounts of water at every stop on a long journey."]
    p = t.write_candidate("water", "https://www.rspca.org.uk/dogs", quotes, [])
    out = p.read_text(encoding="utf-8")
    assert p.parent.name == "_candidates" and "reviewed: false" in out and "Filler" not in out and "paracetamol" not in out
