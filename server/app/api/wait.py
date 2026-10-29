"""대기 공유 (F3) — api_spec §2.8.

앱이 P단에서 "잠깐 다녀올게"를 시작하면 토큰을 받고 10초마다 상태를 올린다. 보호자 폰은 `GET /wait/{token}`
(API 접두사 밖, 인증 없음) 페이지를 연다. 위치는 받지도 저장하지도 않는다 — 요청 본문에 정의되지 않은 필드가 있으면 422.
토큰은 128비트 무작위, 서버에는 SHA-256 해시만 저장, 2시간 뒤 만료.
"""
import html
import secrets
from datetime import datetime, timedelta, timezone
from typing import Literal

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, Response
from pydantic import BaseModel, Field
from sqlalchemy import update
from sqlalchemy.orm import Session

from app.api.deps import get_session, hash_token, require_device
from app.config import get_settings
from app.errors import ApiError
from app.models import WaitSession

router = APIRouter()
page_router = APIRouter()
STALE_SEC = 30
REFRESH_SEC = 5


class WaitStart(BaseModel):
    model_config = {"extra": "forbid"}

    limitMin: int = Field(default=10, ge=1, le=120)


class WaitUpdate(BaseModel):
    """올릴 수 있는 값은 이것뿐이다. lat·lng·location 같은 필드는 422 (위치 저장 금지, test_plan F3-08)."""
    model_config = {"extra": "forbid"}

    state: Literal["waiting", "overdue", "ended"]
    elapsedSec: int = Field(ge=0, le=86_400)
    limitMin: int = Field(ge=1, le=120)
    batteryPercent: int | None = Field(default=None, ge=0, le=100)
    cabinTempC: float | None = Field(default=None, ge=-40, le=80)


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _aware(t: datetime) -> datetime:
    return t if t.tzinfo else t.replace(tzinfo=timezone.utc)


def _find(session: Session, token: str) -> WaitSession:
    row = session.get(WaitSession, hash_token(token))
    if row is None:
        raise ApiError(404, "WAIT_NOT_FOUND", "대기 공유를 찾을 수 없습니다.")
    return row


def _live(row: WaitSession) -> bool:
    return row.state != "ended" and _now() < _aware(row.expires_at)


def _status(row: WaitSession) -> dict:
    updated = _aware(row.updated_at)
    return {
        "state": row.state,
        "elapsedSec": row.elapsed_sec,
        "limitMin": row.limit_min,
        "batteryPercent": row.battery_percent,
        "cabinTempC": row.cabin_temp_c,
        "updatedAt": updated.isoformat(timespec="seconds"),
        "stale": (_now() - updated).total_seconds() > STALE_SEC,
    }


@router.post("/wait", status_code=201)
def start(body: WaitStart, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    """기기당 동시에 하나: 새로 만들면 그 기기의 이전 대기 공유는 끝낸다."""
    session.execute(update(WaitSession).where(WaitSession.device_id == device_id, WaitSession.state != "ended").values(state="ended"))
    token = secrets.token_urlsafe(16)
    now = _now()
    expires = now + timedelta(minutes=get_settings().wait_ttl_minutes)
    session.add(WaitSession(token_hash=hash_token(token), device_id=device_id, limit_min=body.limitMin, created_at=now, updated_at=now, expires_at=expires))
    session.commit()
    return {"token": token, "pageUrl": f"/wait/{token}", "expiresAt": expires.isoformat(timespec="seconds")}


@router.put("/wait/{token}")
def put(token: str, body: WaitUpdate, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    row = _find(session, token)
    if row.device_id != device_id:
        raise ApiError(404, "WAIT_NOT_FOUND", "대기 공유를 찾을 수 없습니다.")
    if not _live(row):
        raise ApiError(410, "WAIT_EXPIRED", "끝났거나 만료된 대기 공유입니다.")
    row.state, row.elapsed_sec, row.limit_min = body.state, body.elapsedSec, body.limitMin
    row.battery_percent, row.cabin_temp_c, row.updated_at = body.batteryPercent, body.cabinTempC, _now()
    session.commit()
    return _status(row)


@router.delete("/wait/{token}", status_code=204)
def end(token: str, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> Response:
    row = _find(session, token)
    if row.device_id != device_id:
        raise ApiError(404, "WAIT_NOT_FOUND", "대기 공유를 찾을 수 없습니다.")
    row.state, row.updated_at = "ended", _now()
    session.commit()
    return Response(status_code=204)


@router.get("/wait/{token}")
def status(token: str, session: Session = Depends(get_session)) -> dict:
    """보호자 폰용 상태 JSON (인증 없음 — 토큰이 곧 권한)."""
    row = _find(session, token)
    if not _live(row):
        raise ApiError(410, "WAIT_EXPIRED", "끝났거나 만료된 대기 공유입니다.")
    return _status(row)


# ───────────── 보호자 폰 페이지 (API 접두사 밖) ─────────────

def _mins(sec: int) -> str:
    m = sec // 60
    return f"{m}분째" if m >= 1 else f"{sec}초째"


def render(row: WaitSession | None) -> tuple[int, str]:
    if row is None or not _live(row):
        title, body, tone = "대기가 끝났어요", "<p class='big'>대기가 끝났거나 만료됐어요</p><p>새 대기를 시작하면 새 주소가 생겨요.</p>", "ended"
        return (404 if row is None else 410), _page(title, body, tone, refresh=False)
    s = _status(row)
    temp = f"실내 {s['cabinTempC']:.0f}℃" if s["cabinTempC"] is not None else "실내 온도 정보 없음"
    battery = f"배터리 {s['batteryPercent']}%" if s["batteryPercent"] is not None else "배터리 정보 없음"
    overdue = s["state"] == "overdue"
    head = "시간 초과 — 차로 돌아가 주세요" if overdue else "반려견이 차에서 기다리는 중"
    stale = f"<p class='warn'>차와 연결 확인 중 (마지막 갱신 {html.escape(s['updatedAt'][11:16])})</p>" if s["stale"] else ""
    body = (
        f"<p class='head'>{head}</p>"
        f"<p class='big'>{_mins(s['elapsedSec'])}</p>"
        f"<p>설정 {s['limitMin']}분 · {battery}</p>"
        f"<p>{temp}</p>{stale}"
        "<p class='note'>위치는 공유하지 않아요. 실내 온도는 참고용이에요.</p>"
    )
    return 200, _page(head, body, "overdue" if overdue else "waiting", refresh=True)


def _page(title: str, body: str, tone: str, refresh: bool) -> str:
    meta = f"<meta http-equiv='refresh' content='{REFRESH_SEC}'>" if refresh else ""
    alert = "<script>try{navigator.vibrate&&navigator.vibrate([300,200,300])}catch(e){}</script>" if tone == "overdue" else ""
    return (
        "<!doctype html><html lang='ko'><head><meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width,initial-scale=1'><meta name='robots' content='noindex,nofollow'>"
        f"{meta}<title>{html.escape(title)} · 같이가개</title><style>"
        "body{margin:0;font-family:system-ui,sans-serif;background:#f6f4ef;color:#1d1d1b;padding:24px;line-height:1.4}"
        "body.overdue{background:#b3261e;color:#fff}body.ended{background:#e8e6e1}"
        ".head{font-size:26px;font-weight:700}.big{font-size:64px;font-weight:800;margin:12px 0}"
        "p{font-size:22px;margin:8px 0}.warn{color:#8a5a00}.overdue .warn{color:#ffe082}.note{font-size:16px;opacity:.7;margin-top:24px}"
        f"</style></head><body class='{tone}'>{body}{alert}</body></html>"
    )


@page_router.get("/wait/{token}", response_class=HTMLResponse)
def page(token: str, request: Request) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        row = session.get(WaitSession, hash_token(token))
        code, text = render(row)
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})

