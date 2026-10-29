import asyncio
import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI
from sqlalchemy import Engine

from app import errors
from app.api import content, devices, health, llm, reststops, trips
from app.middleware import RequestContextMiddleware
from app.api.health import LlmHealth
from app.config import VERSION, get_settings
from app.db import make_engine
from app.llm.ollama import OllamaClient

API_PREFIX = "/api/v1"
log = logging.getLogger(__name__)


def create_app(
    engine: Engine | None = None,
    llm_health: LlmHealth | None = None,
    ollama: OllamaClient | None = None,
    warm_up: bool = True,
    rate_limit: int | None = None,
) -> FastAPI:
    settings = get_settings()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        # Ollama warm-up은 백그라운드로: 실패하거나 느려도 서버 기동을 막지 않는다 (architecture §3.3).
        task = asyncio.create_task(app.state.ollama.warm_up()) if warm_up else None
        app.state.warm_up_task = task
        yield
        if task and not task.done():
            task.cancel()

    app = FastAPI(title="쉼표 API", version=VERSION, lifespan=lifespan)
    app.state.engine = engine or make_engine(settings.database_url)
    app.state.llm_health = llm_health or LlmHealth(settings.ollama_base_url, settings.ollama_model)
    app.state.ollama = ollama or OllamaClient(settings)
    errors.install(app)
    app.add_middleware(RequestContextMiddleware, limit=rate_limit or settings.rate_limit_per_minute)
    for module in (health, devices, reststops, llm, content, trips):
        app.include_router(module.router, prefix=API_PREFIX)
    return app


app = create_app()
