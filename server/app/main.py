from fastapi import FastAPI
from sqlalchemy import Engine

from app.api import health
from app.api.health import LlmHealth
from app.config import VERSION, get_settings
from app.db import make_engine

API_PREFIX = "/api/v1"


def create_app(engine: Engine | None = None, llm_health: LlmHealth | None = None) -> FastAPI:
    settings = get_settings()
    app = FastAPI(title="쉼표 API", version=VERSION)
    app.state.engine = engine or make_engine(settings.database_url)
    app.state.llm_health = llm_health or LlmHealth(settings.ollama_base_url, settings.ollama_model)
    app.include_router(health.router, prefix=API_PREFIX)
    return app


app = create_app()
