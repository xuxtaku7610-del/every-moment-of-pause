"""도우미 2단계 (앱 규칙 분류가 못 고른 질문만): 임베딩 검색 → (낮으면) LLM 분류 → 카드만 근거로 2~3문장 설명 → 근거 검사.

공알(cloud/ai_runtime.py)에서 가져온 생각: embeddinggemma 코사인 검색과 임계값(VECTOR_MIN, 우리 카드로 다시 잼), 답 규칙(ANSWER_RULE),
근거 검사 실패 → 원문(카드 줄) 그대로, 모델 실패 → "카드 고르기만"(공알의 작은 모델 대신). 질문 원문은 저장·로그하지 않는다.
"""
from __future__ import annotations

import asyncio
import json
import math
import re
import time
from dataclasses import dataclass

import httpx

from app.config import Settings
from app.helper import grounding
from app.helper.cards import Card, emergency

# 2026-11-04 측정(tools/eval_helper.py, 카드 28장·평가 85문항, embeddinggemma): docs/plan/helper_eval.md "임계값"
VECTOR_MIN = 0.55
ROUTE_MIN_CONFIDENCE = 0.6  # HELPER_PROMPT 3번 (앱도 0.6 미만이면 모름)
ANSWER_RULE = (
    "카드 내용만 근거로 2~3문장 한국어로 답한다. 카드에 없는 숫자·시간·온도·음식·약·처치는 쓰지 않는다. "
    "약 이름·용량은 절대 쓰지 않는다. 진단하지 않는다(\"~병이에요\", \"~일 확률\" 금지). 질문을 되풀이하지 않는다. "
    "카드의 행동을 질문 상황에 맞게 순서대로 말한다. 존댓말 '~해요'체."
)
SYSTEM_EXPLAIN = "너는 반려견 이동 도우미다. 주어진 돌봄 카드 문장만 근거로 짧게 답한다."
SYSTEM_ROUTE = "너는 질문 분류기다. 반드시 JSON 한 줄만 답한다."


@dataclass
class Answer:
    route: str  # emergency | vector | llm | none
    id: str | None
    confidence: float
    answer: str | None
    grounded: bool
    source: str  # ollama | card | none
    missing: list[str]
    latency_ms: int


def cosine(a: list[float], b: list[float]) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    na, nb = math.sqrt(sum(x * x for x in a)), math.sqrt(sum(y * y for y in b))
    return dot / (na * nb) if na and nb else 0.0


class Helper:
    def __init__(self, settings: Settings, cards: dict[str, Card], transport: httpx.AsyncBaseTransport | None = None) -> None:
        self.base = settings.ollama_base_url.rstrip("/")
        self.model = settings.ollama_model
        self.embed_model = settings.embed_model
        self.keep_alive = settings.ollama_keep_alive
        self.timeout = settings.helper_timeout_seconds
        self.cards = cards
        self._transport = transport
        self._vectors: dict[str, list[float]] = {}

    def _client(self, timeout: float) -> httpx.AsyncClient:
        return httpx.AsyncClient(timeout=httpx.Timeout(timeout, connect=2), transport=self._transport)

    async def embed(self, texts: list[str]) -> list[list[float]]:
        async with self._client(30) as c:
            r = await c.post(f"{self.base}/api/embed", json={"model": self.embed_model, "input": texts, "keep_alive": self.keep_alive})
            r.raise_for_status()
            vecs = r.json()["embeddings"]
        if len(vecs) != len(texts) or not all(v and all(isinstance(x, (int, float)) and math.isfinite(x) for x in v) for v in vecs):
            raise ValueError("embedding format")
        return vecs

    async def _card_vectors(self, ids: list[str]) -> dict[str, list[float]]:
        need = [i for i in ids if i not in self._vectors and i in self.cards]
        if need:
            for i, v in zip(need, await self.embed([self.cards[i].embed_text for i in need])):
                self._vectors[i] = v
        return {i: self._vectors[i] for i in ids if i in self._vectors}

    async def search(self, text: str, allowed: list[str]) -> list[tuple[str, float]]:
        """허용된 카드의 코사인 순위 (임베딩 실패면 예외)."""
        vecs = await self._card_vectors(allowed)
        q = (await self.embed([f"질문: {text}"]))[0]
        return sorted(((i, cosine(q, v)) for i, v in vecs.items()), key=lambda x: -x[1])

    async def chat(self, system: str, user: str, num_predict: int, fmt_json: bool = False) -> str:
        body = {
            "model": self.model, "stream": False, "think": False, "keep_alive": self.keep_alive,
            "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
            "options": {"temperature": 0.2, "num_predict": num_predict},
        }
        if fmt_json:
            body["format"] = "json"
        async with self._client(self.timeout) as c:
            r = await c.post(f"{self.base}/api/chat", json=body)
            r.raise_for_status()
            return str(r.json()["message"]["content"]).strip()

    async def llm_route(self, text: str, allowed: list[str]) -> tuple[str | None, float]:
        """allowed 밖·파싱 실패·시간 초과면 (None, 0). confidence < 0.6이면 모름."""
        menu = [{"id": i, "title": self.cards[i].title} for i in allowed if i in self.cards]
        rule = ('카드 제목이 질문에 **직접** 답할 때만 그 id를 고른다. 비슷한 주제일 뿐이거나(예: 수명·품종 상식·사료 추천), '
                '약·용량·사람 약을 묻거나, 날씨·음악 같은 반려견 돌봄 밖 질문이면 id는 null. '
                '{"id": "...", "confidence": 0.0~1.0} 한 줄만.')
        user = json.dumps({"question": text, "cards": menu, "rule": rule}, ensure_ascii=False)
        try:
            raw = await asyncio.wait_for(self.chat(SYSTEM_ROUTE, user, 40, fmt_json=True), self.timeout)
            obj = json.loads(raw)
            cid, conf = obj.get("id"), float(obj.get("confidence", 0))
        except Exception:  # noqa: BLE001 - 타임아웃·JSON 오류 모두 모름
            return None, 0.0
        if cid not in allowed or not (0 <= conf <= 1):
            return None, 0.0
        return (cid, conf) if conf >= ROUTE_MIN_CONFIDENCE else (None, conf)

    async def explain(self, text: str, card: Card) -> tuple[str, bool, str, list[str]]:
        """(답, 근거 통과, 출처, 빠진 것). 실패·근거 부족이면 카드 줄 그대로."""
        quote = " ".join(card.lines[:3])
        user = json.dumps({"question": text, "card": {"title": card.title, "lines": list(card.lines)}, "answer_rule": ANSWER_RULE}, ensure_ascii=False)
        try:
            out = await asyncio.wait_for(self.chat(SYSTEM_EXPLAIN, user, 160), self.timeout)
        except Exception:  # noqa: BLE001
            return quote, False, "card", ["모델 응답 없음"]
        out = re.sub(r"\s+", " ", out).strip()
        ok, missing = grounding.check(out, card.text, list(card.lines))
        sentences = [s for s in re.split(r"(?<=[.!?])\s+", out) if s.strip()]
        if not out or len(sentences) > 3:
            ok, missing = False, missing + ["문장 수"]
        return (out, True, "ollama", []) if ok else (quote, False, "card", missing)

    async def ask(self, text: str, allowed: list[str], explain: bool = True) -> Answer:
        started = time.monotonic()
        allowed = [i for i in allowed if i in self.cards]

        def done(route: str, cid: str | None, conf: float, ans: str | None, grounded: bool, source: str, missing: list[str] | None = None) -> Answer:
            return Answer(route, cid, conf, ans, grounded, source, missing or [], int((time.monotonic() - started) * 1000))

        # 응급 말이면 LLM보다 먼저 (앱이 이미 병원 안내로 보내지만 서버도 모델을 부르지 않는다)
        if emergency(text):
            em = [i for i in allowed if self.cards[i].category == "emergency"]
            cid = None
            try:
                ranked = await self.search(text, em) if em else []
                cid = ranked[0][0] if ranked else None
            except Exception:  # noqa: BLE001
                pass
            card = self.cards.get(cid) if cid else None
            return done("emergency", cid, 1.0, " ".join(card.lines[:3]) if card else None, False, "card")
        cid, conf, route = None, 0.0, "none"
        try:
            ranked = await self.search(text, allowed)
            if ranked and ranked[0][1] >= VECTOR_MIN:
                cid, conf, route = ranked[0][0], ranked[0][1], "vector"
        except Exception:  # noqa: BLE001 - 임베딩이 죽으면 LLM 분류만
            pass
        if cid is None:
            cid, conf = await self.llm_route(text, allowed)
            route = "llm" if cid else "none"
        if cid is None:
            return done("none", None, conf, None, False, "none")
        card = self.cards[cid]
        if not explain or card.action == "VET":
            return done(route, cid, conf, " ".join(card.lines[:3]), False, "card")
        ans, grounded, source, missing = await self.explain(text, card)
        return done(route, cid, conf, ans, grounded, source, missing)
