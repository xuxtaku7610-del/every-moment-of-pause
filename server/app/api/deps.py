"""공통 의존성: DB 세션, 기기 토큰 인증 (api_spec §0)."""
import hashlib
from datetime import datetime, timedelta, timezone
from collections.abc import Iterator

from fastapi import Header, Request
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.errors import ApiError
from app.models import Device


def get_session(request: Request) -> Iterator[Session]:
    with Session(request.app.state.engine) as session:
        yield session


def hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def require_device(request: Request, x_device_token: str | None = Header(default=None)) -> str:
    """X-Device-Token 확인. 서버에는 토큰 해시만 저장한다 (architecture §6)."""
    if not x_device_token:
        raise ApiError(401, "INVALID_DEVICE_TOKEN", "기기 토큰이 없습니다.")
    with Session(request.app.state.engine) as session:
        device = session.scalar(select(Device).where(Device.token_hash == hash_token(x_device_token)))
        if device is None:
            raise ApiError(401, "INVALID_DEVICE_TOKEN", "기기 토큰이 올바르지 않습니다.")
        # 보관 정책(마지막 접속 180일)용. 쓰기를 줄이려고 하루에 한 번만 갱신한다.
        now = datetime.now(timezone.utc)
        seen = device.last_seen_at if device.last_seen_at is None or device.last_seen_at.tzinfo else device.last_seen_at.replace(tzinfo=timezone.utc)
        if seen is None or now - seen > timedelta(days=1):
            device.last_seen_at = now
            session.commit()
        return device.id
