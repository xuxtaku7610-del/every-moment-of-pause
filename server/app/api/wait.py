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
from fastapi.responses import HTMLResponse, RedirectResponse, Response
from pydantic import BaseModel, Field
from sqlalchemy import update
from sqlalchemy.orm import Session

from app import pages
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
        "returning": bool(row.guardian_returning),
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


# ───────────── 보호자 폰 페이지 (API 접두사 밖, 6번 화면 v5) ─────────────

def _mins(sec: int) -> str:
    m = sec // 60
    return f"{m}분째" if m >= 1 else f"{sec}초째"


def render(row: WaitSession | None, token: str = "") -> tuple[int, str]:
    if row is None or not _live(row):
        body = (
            f"<section class='card hero'>{pages.DOG}<p class='title'>대기가 끝났어요</p>"
            "<p class='note'>끝났거나 만료됐어요. 새 대기를 시작하면 새 주소가 생겨요.</p></section>"
        )
        return (404 if row is None else 410), pages.page("대기가 끝났어요", body)
    s = _status(row)
    overdue = s["state"] == "overdue"
    remain = max(0, s["limitMin"] * 60 - s["elapsedSec"])
    tiles = [("남은 시간", f"{(remain + 59) // 60}분" if not overdue else "지남")]
    # 배터리·실내 온도는 차가 실제로 보낸 값이 있을 때만 (지어내지 않는다).
    if s["batteryPercent"] is not None:
        tiles.append(("배터리", f"{s['batteryPercent']}%"))
    if s["cabinTempC"] is not None:
        tiles.append(("실내 온도", f"{s['cabinTempC']:.0f}℃"))
    tile_html = "".join(f"<div class='tile'><div class='label'>{k}</div><div class='value'>{html.escape(v)}</div></div>" for k, v in tiles)
    banner = "<div class='banner'><b>설정 시간이 지났어요</b>차로 돌아가 주세요.</div>" if overdue else ""
    stale = f"<div class='banner'><b>차와 연결 확인 중</b>마지막 갱신 {html.escape(s['updatedAt'][11:16])}</div>" if s["stale"] else ""
    if row.guardian_returning:
        action = "<div class='ok'>차 화면에 '돌아오는 중'이라고 알렸어요</div>"
    else:
        action = (
            f"<form method='post' action='/wait/{html.escape(token)}/returning'>"
            "<button class='btn' type='submit'>돌아가는 중</button></form>"
        )
    body = (
        f"{banner}{stale}<section class='card hero'>{pages.DOG}"
        f"<p class='title'>{'시간이 지났어요' if overdue else '대기 중이에요'}</p><p class='big'>{_mins(s['elapsedSec'])}</p></section>"
        f"<div class='tiles'>{tile_html}</div>{action}"
        "<p class='note'>위치는 공유하지 않아요. 이 페이지는 보조 안내예요 — 반려견을 차에 오래 혼자 두지 마세요.</p>"
    )
    return 200, pages.page("반려견 대기 중", body, refresh_sec=REFRESH_SEC)


@page_router.get("/wait/{token}", response_class=HTMLResponse)
def page(token: str, request: Request) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        row = session.get(WaitSession, hash_token(token))
        code, text = render(row, token)
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})


@page_router.post("/wait/{token}/returning")
def returning(token: str, request: Request) -> Response:
    """보호자 폰 "돌아가는 중" (인증 없음 — 토큰이 곧 권한). 차는 다음 상태 업로드 응답에서 받는다."""
    with Session(request.app.state.engine) as session:
        row = session.get(WaitSession, hash_token(token))
        if row is None or not _live(row):
            return HTMLResponse(render(row, token)[1], status_code=404 if row is None else 410)
        row.guardian_returning = True
        session.commit()
    return RedirectResponse(f"/wait/{token}", status_code=303)
