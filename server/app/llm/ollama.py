"""Ollama 네이티브 /api/chat 클라이언트 (api_spec §2.8, architecture §3.3)."""
import logging

import httpx

from app.config import Settings

log = logging.getLogger(__name__)
TEMPERATURE = 0.3
WARMUP_TIMEOUT_SECONDS = 120.0


class OllamaClient:
    def __init__(self, settings: Settings, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self.base_url = settings.ollama_base_url.rstrip("/")
        self.model = settings.ollama_model
        self.keep_alive = settings.ollama_keep_alive
        self.num_predict = settings.ollama_num_predict
        self.timeout = settings.llm_timeout_seconds
        self.transport = transport

    def chat_body(self, system: str, user: str) -> dict:
        return {
            "model": self.model,
            "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}],
            "stream": False,
            "think": False,
            "keep_alive": self.keep_alive,
            "options": {"num_predict": self.num_predict, "temperature": TEMPERATURE},
        }

    async def chat(self, system: str, user: str) -> str:
        """응답 본문 텍스트. 실패·타임아웃은 예외로 올린다(호출 측이 템플릿으로 폴백)."""
        async with httpx.AsyncClient(transport=self.transport, timeout=self.timeout) as client:
            resp = await client.post(f"{self.base_url}/api/chat", json=self.chat_body(system, user))
        resp.raise_for_status()
        return str(resp.json()["message"]["content"]).strip()

    async def warm_up(self) -> bool:
        """모델을 미리 메모리에 올린다. 빈 messages는 생성 없이 로드만 한다. 실패해도 예외를 내지 않는다."""
        body = {"model": self.model, "messages": [], "stream": False, "keep_alive": self.keep_alive}
        try:
            async with httpx.AsyncClient(transport=self.transport, timeout=WARMUP_TIMEOUT_SECONDS) as client:
                resp = await client.post(f"{self.base_url}/api/chat", json=body)
            resp.raise_for_status()
            log.info("ollama warm-up ok model=%s", self.model)
            return True
        except Exception as e:  # noqa: BLE001 - 서버 기동을 막지 않는다
            log.warning("ollama warm-up failed: %s", type(e).__name__)
            return False
