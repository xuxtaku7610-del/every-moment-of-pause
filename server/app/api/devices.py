"""POST /devices — api_spec §2.2. 익명 기기 등록, 토큰 발급 (인증 면제). DELETE /devices/me — 내 데이터 삭제."""
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_session, hash_token, require_device
from app.models import Device

router = APIRouter()
KST = timezone(timedelta(hours=9))


class DeviceIn(BaseModel):
    platform: str = Field(min_length=1, max_length=32)
    appVersion: str = Field(min_length=1, max_length=16)


@router.post("/devices", status_code=201)
def register(body: DeviceIn, session: Session = Depends(get_session)) -> dict:
    device_id = f"dv_{secrets.token_hex(6)}"
    token = secrets.token_urlsafe(32)
    session.add(Device(id=device_id, token_hash=hash_token(token), platform=body.platform, app_version=body.appVersion))
    session.commit()
    return {"deviceId": device_id, "token": token, "issuedAt": datetime.now(KST).isoformat(timespec="seconds")}


def delete_device(session: Session, device_id: str) -> None:
    """기기와 그 기기의 여행·이벤트·대기 공유를 지운다. LLM 호출 기록은 기기 연결만 끊는다(SET NULL).
    SQLite 테스트는 FK CASCADE가 꺼져 있을 수 있어 직접 지운다. 커밋은 호출하는 쪽이 한다."""
    from sqlalchemy import delete, update

    from app.models import LlmCallLog, Trip, TripEvent, WaitSession

    trips = [t for (t,) in session.execute(select(Trip.id).where(Trip.device_id == device_id))]
    if trips:
        session.execute(delete(TripEvent).where(TripEvent.trip_id.in_(trips)))
    session.execute(delete(Trip).where(Trip.device_id == device_id))
    session.execute(delete(WaitSession).where(WaitSession.device_id == device_id))
    session.execute(update(LlmCallLog).where(LlmCallLog.device_id == device_id).values(device_id=None))
    session.execute(delete(Device).where(Device.id == device_id))


@router.delete("/devices/me", status_code=204)
def delete_me(device_id: str = Depends(require_device), session: Session = Depends(get_session)) -> None:
    """내 데이터 삭제 (COMMERCIAL_READINESS G·J P1, api_spec §2.14)."""
    delete_device(session, device_id)
    session.commit()
