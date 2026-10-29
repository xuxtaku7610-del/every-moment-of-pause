from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict

VERSION = "0.1.0"


class Settings(BaseSettings):
    """server/.env와 환경 변수에서 읽는 설정. 기본값은 server/.env.example과 같다."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    database_url: str = "postgresql+psycopg://pause:pause@127.0.0.1:5432/pause"
    ex_api_key: str = ""

    ollama_base_url: str = "http://127.0.0.1:11434"
    ollama_model: str = "gemma4:12b"
    ollama_keep_alive: str = "30m"
    ollama_num_predict: int = 60
    llm_timeout_seconds: float = 2.5
    llm_max_sentences: int = 2
    # 기기당 분당 요청 수 (J P0). 앱은 출발 시·제안 때만 부르므로 넉넉하다.
    rate_limit_per_minute: int = 120


@lru_cache
def get_settings() -> Settings:
    return Settings()
