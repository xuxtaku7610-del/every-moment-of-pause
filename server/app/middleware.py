"""요청 ID·구조화 로그·기기당 레이트 리밋 (COMMERCIAL_READINESS J P0).

- 요청 ID: 들어온 `X-Request-ID`(영숫자·-·_ 64자 이하)를 쓰거나 새로 만들고, 응답 헤더로 돌려준다.
- 구조화 로그: 요청마다 JSON 한 줄 (요청 ID·메서드·경로·상태·지연 ms·기기 키 앞 8자). 토큰·본문·좌표는 남기지 않는다.
- 레이트 리밋: 기기 토큰(없으면 클라이언트 IP)마다 60초 창 [limit]회. 넘으면 429 RATE_LIMITED. /health는 제외.
"""
import json
import logging
import re
import secrets
import time
from collections import defaultdict, deque

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from app.api.deps import hash_token
from app.errors import error_body

log = logging.getLogger("pause.access")
_SAFE_ID = re.compile(r"^[A-Za-z0-9_-]{1,64}$")


class RequestContextMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, limit: int = 120, window_sec: float = 60.0, clock=time.monotonic) -> None:
        super().__init__(app)
        self.limit = limit
        self.window = window_sec
        self.clock = clock
        self.hits: dict[str, deque[float]] = defaultdict(deque)

    def _key(self, request: Request) -> str:
        token = request.headers.get("x-device-token")
        if token:
            return "d:" + hash_token(token)[:16]
        return "ip:" + (request.client.host if request.client else "unknown")

    def _allow(self, key: str) -> bool:
        now = self.clock()
        q = self.hits[key]
        while q and now - q[0] >= self.window:
            q.popleft()
        if len(q) >= self.limit:
            return False
        q.append(now)
        return True

    async def dispatch(self, request: Request, call_next) -> Response:
        rid = request.headers.get("x-request-id", "")
        if not _SAFE_ID.match(rid):
            rid = secrets.token_hex(8)
        request.state.request_id = rid
        start = time.perf_counter()
        key = self._key(request)
        if not request.url.path.endswith("/health") and not self._allow(key):
            resp: Response = JSONResponse(
                error_body("RATE_LIMITED", "요청이 너무 많습니다. 잠시 뒤 다시 시도해 주세요.", {"retryAfterSec": int(self.window)}),
                status_code=429, headers={"Retry-After": str(int(self.window))},
            )
        else:
            resp = await call_next(request)
        resp.headers["X-Request-ID"] = rid
        log.info(json.dumps({
            "requestId": rid, "method": request.method, "path": request.url.path, "status": resp.status_code,
            "ms": round((time.perf_counter() - start) * 1000, 1), "client": key[:10],
        }, ensure_ascii=False))
        return resp
