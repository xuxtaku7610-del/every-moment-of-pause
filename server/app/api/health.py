"""GET /health — api_spec §2.1. 인증·rate limit 면제."""
import asyncio
import logging
import time
from collections.abc import Callable

import httpx
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
from sqlalchemy import Engine, text

from app.config import VERSION

log = logging.getLogger(__name__)
router = APIRouter()

DB_TIMEOUT_SECONDS = 1.0
LLM_TIMEOUT_SECONDS = 1.0
LLM_CACHE_SECONDS = 30.0


def _select_one(engine: Engine) -> None:
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))


async def check_db(engine: Engine) -> str:
    """SELECT 1 성공이면 ok, 실패하거나 1초를 넘기면 down."""
    try:
        await asyncio.wait_for(asyncio.to_thread(_select_one, engine), DB_TIMEOUT_SECONDS)
        return "ok"
    except Exception as e:  # noqa: BLE001 - 어떤 실패든 down
        log.warning("health db down: %s", type(e).__name__)
        return "down"


class LlmHealth:
    """Ollama /api/tags 확인. 결과는 30초 캐시한다.

    - 1초 안에 응답하고 모델이 목록에 있으면 ok
    - 연결은 됐지만 모델이 없거나 응답이 느리면 degraded
    - 연결 자체가 안 되면 down
    """

    def __init__(
        self,
        base_url: str,
        model: str,
        transport: httpx.AsyncBaseTransport | None = None,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.transport = transport
        self.clock = clock
        self._cached: tuple[float, str] | None = None

    async def status(self) -> str:
        now = self.clock()
        if self._cached and now - self._cached[0] < LLM_CACHE_SECONDS:
            return self._cached[1]
        result = await self._probe()
        self._cached = (now, result)
        return result

    async def _probe(self) -> str:
        try:
            async with httpx.AsyncClient(transport=self.transport, timeout=LLM_TIMEOUT_SECONDS) as client:
                resp = await client.get(f"{self.base_url}/api/tags")
        except (httpx.ConnectError, httpx.ConnectTimeout):
            return "down"
        except httpx.TimeoutException:
            return "degraded"
        except httpx.HTTPError:
            return "down"
        if resp.status_code != 200:
            return "degraded"
        names = {m.get("name") for m in resp.json().get("models", [])} | {
            m.get("model") for m in resp.json().get("models", [])
        }
        wanted = {self.model, f"{self.model}:latest"}
        return "ok" if names & wanted else "degraded"


@router.get("/health")
async def health(request: Request) -> JSONResponse:
    db, llm = await asyncio.gather(check_db(request.app.state.engine), request.app.state.llm_health.status())
    if db == "down":
        status = "down"
    elif llm != "ok":
        status = "degraded"
    else:
        status = "ok"
    body = {"status": status, "db": db, "llm": llm, "version": VERSION}
    return JSONResponse(body, status_code=503 if db == "down" else 200)
