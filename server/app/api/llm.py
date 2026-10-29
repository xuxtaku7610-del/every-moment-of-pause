"""POST /llm/phrases — api_spec §2.8. 2.5초 안에 Ollama 문장, 아니면 503 + fallback."""
import asyncio
import logging
import time
from typing import Literal

from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.api.deps import require_device
from app.config import get_settings
from app.errors import ApiError
from app.llm.phrases import SYSTEM_REST, check, rest_fallback, rest_user_prompt
from app.models import LlmCallLog

log = logging.getLogger(__name__)
router = APIRouter()


class RestFacts(BaseModel):
    restStopName: str = Field(min_length=1, max_length=64)
    minutesAway: int = Field(ge=0, le=600)
    reasons: list[str] = Field(default_factory=list, max_length=8)
    fatigueLevel: Literal["low", "medium", "high"] = "low"
    passengers: list[str] = Field(default_factory=list, max_length=8)


class PhraseIn(BaseModel):
    kind: Literal["rest_suggestion"]
    facts: RestFacts
    maxSentences: int = Field(default=2, ge=1, le=2)


def _log_call(request: Request, device_id: str, kind: str, source: str, latency_ms: int, fallback: bool) -> None:
    # 원문은 저장하지 않는다: 종류·지연·폴백 여부만 (architecture §6).
    try:
        with Session(request.app.state.engine) as s, s.begin():
            s.add(LlmCallLog(device_id=device_id, kind=kind, source=source, latency_ms=latency_ms, fallback_used=fallback))
    except Exception as e:  # noqa: BLE001
        log.warning("llm log failed: %s", type(e).__name__)


@router.post("/llm/phrases")
async def phrases(body: PhraseIn, request: Request, device_id: str = Depends(require_device)) -> dict:
    facts = body.facts.model_dump()
    fallback = rest_fallback(facts)
    started = time.monotonic()
    reason: str
    try:
        text = await asyncio.wait_for(
            request.app.state.ollama.chat(SYSTEM_REST, rest_user_prompt(facts)),
            get_settings().llm_timeout_seconds,
        )
        reason = check(text, facts, body.maxSentences) or ""
    except Exception as e:  # noqa: BLE001 - 타임아웃·연결 실패 모두 폴백
        text, reason = "", type(e).__name__
    latency_ms = int((time.monotonic() - started) * 1000)
    if reason:
        log.info("llm phrases fallback reason=%s latency_ms=%d", reason, latency_ms)
        _log_call(request, device_id, body.kind, "template", latency_ms, True)
        raise ApiError(503, "LLM_UNAVAILABLE", "LLM 응답 실패", extra={"fallback": fallback})
    _log_call(request, device_id, body.kind, "ollama", latency_ms, False)
    return {"text": text, "source": "ollama", "latencyMs": latency_ms}


class KidAnswerIn(BaseModel):
    question: str = Field(min_length=1, max_length=100)
    minutesToNextStop: int | None = Field(default=None, ge=0, le=600)
    remainingEpisodes: int | None = Field(default=None, ge=0, le=50)
    nextStopHighlights: list[str] = Field(default_factory=list, max_length=8)
    childAge: int | None = Field(default=None, ge=0, le=18)


@router.post("/llm/kid-answer", dependencies=[Depends(require_device)])
def kid_answer(body: KidAnswerIn) -> dict:
    """'아직 멀었어?' — api_spec §2.9. 아이용 문장은 LLM 없이 템플릿만 쓴다 (CLAUDE.md 규칙 4, 결정 2026-10-29).
    질문 원문은 저장하지 않는다."""
    from app.content import kid_answer as template

    return {"text": template(body.minutesToNextStop, body.remainingEpisodes, body.nextStopHighlights), "source": "template"}
