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


@lru_cache
def get_settings() -> Settings:
    return Settings()
