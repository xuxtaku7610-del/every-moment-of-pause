"""POST /llm/phrases — api_spec §2.9. 2.5초 안에 Ollama 문장, 아니면 503 + fallback.

kind: rest_suggestion(F2 휴게소 제안), trip_summary(F7 이동 기록 요약). 멀미 예보·안전·"내렸나요?"는 앱 템플릿만 쓴다.
"""
import asyncio
import logging
import time
from typing import Literal

from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field, ValidationError
from sqlalchemy.orm import Session

from app.api.deps import require_device
from app.config import get_settings
from app.errors import ApiError
from app.llm.phrases import (
    SYSTEM_REST,
    SYSTEM_SUMMARY,
    check,
    check_summary,
    rest_fallback,
    rest_user_prompt,
    summary_fallback,
    summary_user_prompt,
)
from app.models import LlmCallLog

log = logging.getLogger(__name__)
router = APIRouter()


class RestFacts(BaseModel):
    restStopName: str = Field(min_length=1, max_length=64)
    minutesAway: int = Field(ge=0, le=600)
    reasons: list[str] = Field(default_factory=list, max_length=8)
    fatigueLevel: Literal["low", "medium", "high"] = "low"
    passengers: list[str] = Field(default_factory=list, max_length=8)


class TripFacts(BaseModel):
    """이동 기록 요약 사실. 반려견 이름·증상 메모는 보내지 않는다 (architecture §6)."""
    model_config = {"extra": "forbid"}

    durationMin: int = Field(ge=0, le=24 * 60)
    stops: int = Field(ge=0, le=50)
    motionPeak: int = Field(ge=0, le=100)
    limitMin: int | None = Field(default=None, ge=1, le=24 * 60)


class PhraseIn(BaseModel):
    kind: Literal["rest_suggestion", "trip_summary"]
    facts: dict
    maxSentences: int = Field(default=2, ge=1, le=2)


def _facts(body: PhraseIn) -> dict:
    model = RestFacts if body.kind == "rest_suggestion" else TripFacts
    try:
        return model.model_validate(body.facts).model_dump()
    except ValidationError as e:
        fields = ["facts." + ".".join(str(p) for p in err["loc"]) for err in e.errors()]
        raise ApiError(422, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", {"fields": fields}) from None


def _log_call(request: Request, device_id: str, kind: str, source: str, latency_ms: int, fallback: bool) -> None:
    # 원문은 저장하지 않는다: 종류·지연·폴백 여부만 (architecture §6).
    try:
        with Session(request.app.state.engine) as s, s.begin():
            s.add(LlmCallLog(device_id=device_id, kind=kind, source=source, latency_ms=latency_ms, fallback_used=fallback))
    except Exception as e:  # noqa: BLE001
        log.warning("llm log failed: %s", type(e).__name__)


@router.post("/llm/phrases")
async def phrases(body: PhraseIn, request: Request, device_id: str = Depends(require_device)) -> dict:
    facts = _facts(body)
    rest = body.kind == "rest_suggestion"
    fallback = rest_fallback(facts) if rest else summary_fallback(facts)
    system, prompt = (SYSTEM_REST, rest_user_prompt(facts)) if rest else (SYSTEM_SUMMARY, summary_user_prompt(facts))
    started = time.monotonic()
    reason: str
    try:
        text = await asyncio.wait_for(request.app.state.ollama.chat(system, prompt), get_settings().llm_timeout_seconds)
        reason = (check(text, facts, body.maxSentences) if rest else check_summary(text, facts, body.maxSentences)) or ""
    except Exception as e:  # noqa: BLE001 - 타임아웃·연결 실패 모두 폴백
        text, reason = "", type(e).__name__
    latency_ms = int((time.monotonic() - started) * 1000)
    if reason:
        log.info("llm phrases fallback reason=%s latency_ms=%d", reason, latency_ms)
        _log_call(request, device_id, body.kind, "template", latency_ms, True)
        raise ApiError(503, "LLM_UNAVAILABLE", "LLM 응답 실패", extra={"fallback": fallback})
    _log_call(request, device_id, body.kind, "ollama", latency_ms, False)
    return {"text": text, "source": "ollama", "latencyMs": latency_ms}
