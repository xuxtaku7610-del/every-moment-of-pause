"""콩이 도우미 2단계 — api_spec §2.9-1. 앱 규칙 분류(HelperRouter)가 못 고른 질문만 온다.

- `POST /helper/ask` {text, allowed[카드 id], explain} → 임베딩 검색(embeddinggemma) → 낮으면 LLM 분류 → 카드만 근거로 2~3문장 → 근거 검사.
- `POST /llm/route` {text, allowed} → {id, confidence} (분류만, HELPER_PROMPT 3번 계약).
질문 원문은 **저장하지도 로그에 남기지도 않는다** — 호출 기록은 종류·출처·지연·대체 여부만(llm_call_logs, 기존 정책).
`allowed`는 앱이 정한다(릴리스 앱은 검수 끝난 카드만 넘긴다). 서버가 죽거나 늦으면 앱은 1단계 결과("잘 몰라요" 또는 카드)로 답한다.
"""
import logging

from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.api.deps import require_device
from app.helper import cards as card_store
from app.helper.service import Helper
from app.models import LlmCallLog

log = logging.getLogger(__name__)
router = APIRouter()


class AskIn(BaseModel):
    model_config = {"extra": "forbid"}
    text: str = Field(min_length=1, max_length=200)
    allowed: list[str] = Field(min_length=1, max_length=60)
    explain: bool = True


class RouteIn(BaseModel):
    model_config = {"extra": "forbid"}
    text: str = Field(min_length=1, max_length=200)
    allowed: list[str] = Field(min_length=1, max_length=60)


def helper(request: Request) -> Helper:
    h = getattr(request.app.state, "helper", None)
    if h is None:
        from app.config import get_settings
        h = Helper(get_settings(), card_store.load())
        request.app.state.helper = h
    return h


def _log(request: Request, device_id: str, kind: str, source: str, latency_ms: int, fallback: bool) -> None:
    try:
        with Session(request.app.state.engine) as s, s.begin():
            s.add(LlmCallLog(device_id=device_id, kind=kind, source=source, latency_ms=latency_ms, fallback_used=fallback))
    except Exception as e:  # noqa: BLE001
        log.warning("helper log failed: %s", type(e).__name__)


@router.post("/helper/ask")
async def ask(body: AskIn, request: Request, device_id: str = Depends(require_device)) -> dict:
    a = await helper(request).ask(body.text, body.allowed, body.explain)
    # llm_call_logs.source는 ollama|template만 — 모델 문장을 썼으면 ollama, 카드 줄·모름이면 template
    _log(request, device_id, "helper", "ollama" if a.source == "ollama" else "template", a.latency_ms, a.source != "ollama")
    # 원문·답 문장은 로그에 남기지 않는다 (경로·카드 id·지연만)
    log.info("helper route=%s id=%s grounded=%s latency_ms=%d", a.route, a.id, a.grounded, a.latency_ms)
    return {"route": a.route, "id": a.id, "confidence": round(a.confidence, 3), "answer": a.answer, "grounded": a.grounded, "source": a.source, "latencyMs": a.latency_ms}


@router.post("/llm/route")
async def route(body: RouteIn, request: Request, device_id: str = Depends(require_device)) -> dict:
    a = await helper(request).ask(body.text, body.allowed, explain=False)
    _log(request, device_id, "helper_route", "ollama" if a.route == "llm" else "template", a.latency_ms, a.id is None)
    return {"id": a.id, "confidence": round(a.confidence, 3)}
