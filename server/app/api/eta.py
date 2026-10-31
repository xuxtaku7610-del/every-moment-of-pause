"""동물병원 도착 예정 공유 (F5, V5 §5).

동물병원 안내 중 앱이 "도착 예정 공유"를 누르면 무작위 토큰 링크를 만든다. 링크는 보호자 폰으로 보내고,
보호자가 병원·가족에게 직접 전달한다(앱·서버가 병원에 자동으로 보내지 않는다).
**위치는 받지 않는다** — 반려견 이름·병원 이름·도착 예정 분·증상 메모만. 토큰은 128비트, 서버에는 해시만, 2시간 만료.

사전 접수 (UPGRADE 3, F5-07~10): `intake=true`면 병원 접수함 링크 `/clinic/{token}`을 따로 만든다(보호자 링크와 다른 토큰).
- 앱은 가는 동안 `PUT /eta/{token}`으로 도착 예정 분을 갱신하고(위치 대신 분만), 도착하면 `arrived=true`.
- 병원은 접수함에서 응답 하나를 누른다: 접수 확인 / 도착하면 바로 볼게요 / 지금은 어려워요. 앱은 `GET /eta/{token}`으로 받아 간다.
- 접수함 링크도 보호자가 직접 전달한다(문자·전화로 불러 주기). 서버는 병원에 아무것도 보내지 않는다.
"""
import html
import math
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app import pages
from app.api.deps import get_session, hash_token, require_device
from app.errors import ApiError
from app.models import EtaShare

router = APIRouter()
page_router = APIRouter()
TTL_MIN = 120


class EtaIn(BaseModel):
    """lat·lng·location 같은 필드는 422 (위치 저장 금지)."""
    model_config = {"extra": "forbid"}

    petName: str = Field(min_length=1, max_length=32)
    vetName: str = Field(min_length=1, max_length=128)
    etaMinutes: int = Field(ge=0, le=600)
    symptoms: list[str] = Field(default_factory=list, max_length=8)
    # 증상 메모(받아쓴 글)·오늘 이동 한 줄 — 앱에서 사용자가 체크했을 때만 온다 (UPGRADE 3)
    memo: str | None = Field(default=None, max_length=200)
    tripLine: str | None = Field(default=None, max_length=80)
    # 병원 사전 접수 (UPGRADE 3): 병원 접수함 링크를 함께 만든다
    intake: bool = False


class EtaUpdate(BaseModel):
    """가는 동안 도착 예정 갱신. 위치 필드는 422."""
    model_config = {"extra": "forbid"}

    etaMinutes: int = Field(ge=0, le=600)
    arrived: bool = False


# 병원 응답 (접수함 버튼). 진단·처치 문구는 넣지 않는다.
REPLIES = {
    "received": "접수 확인했어요",
    "ready": "도착하면 바로 볼게요",
    "busy": "지금은 어려워요 · 다른 병원을 찾아 주세요",
}


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _aware(t: datetime) -> datetime:
    return t if t.tzinfo else t.replace(tzinfo=timezone.utc)


@router.post("/eta", status_code=201)
def create(body: EtaIn, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    token = secrets.token_urlsafe(16)
    now = _now()
    expires = now + timedelta(minutes=TTL_MIN)
    symptoms = ", ".join(s.strip()[:20] for s in body.symptoms if s.strip())[:200] or None
    clinic = secrets.token_urlsafe(16) if body.intake else None
    session.add(EtaShare(token_hash=hash_token(token), device_id=device_id, pet_name=body.petName.strip(), vet_name=body.vetName.strip(),
                         eta_minutes=body.etaMinutes, symptoms=symptoms, memo=(body.memo or "").strip() or None,
                         trip_line=(body.tripLine or "").strip() or None, created_at=now, expires_at=expires,
                         clinic_token_hash=hash_token(clinic) if clinic else None, eta_at=now + timedelta(minutes=body.etaMinutes),
                         updated_at=now, arrived=False))
    session.commit()
    return {"token": token, "pageUrl": f"/eta/{token}", "expiresAt": expires.isoformat(timespec="seconds"),
            "clinicToken": clinic, "clinicUrl": f"/clinic/{clinic}" if clinic else None}


def _own(session: Session, token: str, device_id: str) -> EtaShare:
    row = session.get(EtaShare, hash_token(token))
    if row is None or row.device_id != device_id or _now() >= _aware(row.expires_at):
        raise ApiError(404, "NOT_FOUND", "공유를 찾을 수 없어요.")
    return row


def _iso(t: datetime | None) -> str | None:
    return _aware(t).isoformat(timespec="seconds") if t else None


def _status(row: EtaShare) -> dict:
    return {"etaMinutes": row.eta_minutes, "arrived": bool(row.arrived), "reply": row.clinic_reply,
            "replyAt": _iso(row.clinic_reply_at), "seen": row.clinic_seen_at is not None}


@router.put("/eta/{token}")
def update(token: str, body: EtaUpdate, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    """도착 예정 갱신 (F5-08). 응답에 병원 응답을 실어 앱이 따로 묻지 않아도 되게 한다."""
    row = _own(session, token, device_id)
    now = _now()
    row.eta_minutes = 0 if body.arrived else body.etaMinutes
    row.eta_at = now + timedelta(minutes=row.eta_minutes)
    row.updated_at = now
    row.arrived = bool(row.arrived) or body.arrived
    session.commit()
    return _status(row)


@router.get("/eta/{token}")
def status(token: str, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    """병원 응답·열람 여부 (F5-09). 앱이 15초마다 묻는다."""
    return _status(_own(session, token, device_id))


def render(row: EtaShare | None, now: datetime | None = None) -> tuple[int, str]:
    now = now or _now()
    if row is None or now >= _aware(row.expires_at):
        body = "<section class='card hero'><p class='title'>공유가 끝났어요</p><p class='note'>도착 예정 공유는 2시간 뒤 사라져요.</p></section>"
        return (404 if row is None else 410), pages.page("공유가 끝났어요", body)
    left, kst = _left(row, now)
    rows = [("병원", row.vet_name), ("도착 예정", "도착했어요" if row.arrived else f"{kst:%H:%M} (지금부터 약 {left}분)")]
    if row.clinic_reply:
        rows.append(("병원 응답", REPLIES[row.clinic_reply]))
    if row.symptoms:
        rows.append(("증상", row.symptoms))
    if row.memo:
        rows.append(("증상 메모", row.memo))
    if row.trip_line:
        rows.append(("오늘 이동", row.trip_line))
    row_html = "".join(f"<div class='row'><span>{k}</span><span>{html.escape(v)}</span></div>" for k, v in rows)
    body = (
        f"<section class='card hero'>{pages.DOG}<p class='title'>{html.escape(row.pet_name)} · 동물병원으로 가는 중</p>"
        f"<p class='big'>{'도착했어요' if row.arrived else f'{left}분 뒤 도착'}</p></section><section class='card'>{row_html}</section>"
        "<p class='note'>보호자가 직접 보낸 링크예요. 위치는 공유하지 않고 도착 예정 시각만 보여요. 2시간 뒤 사라져요.</p>"
    )
    return 200, pages.page(f"{row.pet_name} 도착 예정", body, refresh_sec=30)


def _left(row: EtaShare, now: datetime) -> tuple[int, datetime]:
    arrive = _aware(row.eta_at) if row.eta_at else _aware(row.created_at) + timedelta(minutes=row.eta_minutes)
    left = max(0, math.ceil((arrive - now).total_seconds() / 60))  # 도착 예정은 올려서 (늦게 말하지 않게)
    return left, arrive.astimezone(timezone(timedelta(hours=9)))


def render_clinic(row: EtaShare | None, now: datetime | None = None, token: str = "") -> tuple[int, str]:
    """병원 접수함 (F5-07): 도착 예정(15초마다 새로 고침)·증상·메모·오늘 이동 + 응답 버튼 3개."""
    now = now or _now()
    if row is None or now >= _aware(row.expires_at):
        body = "<section class='card hero'><p class='title'>접수가 끝났어요</p><p class='note'>사전 접수는 2시간 뒤 사라져요.</p></section>"
        return (404 if row is None else 410), pages.page("접수가 끝났어요", body)
    left, kst = _left(row, now)
    ago = max(0, int((now - _aware(row.updated_at or row.created_at)).total_seconds() // 60))
    head = "도착했어요" if row.arrived else f"{left}분 뒤 도착"
    sub = "보호자가 병원에 도착했어요" if row.arrived else f"{kst:%H:%M} 도착 예정 · {'방금' if ago == 0 else f'{ago}분 전'} 갱신"
    rows = [("반려견", row.pet_name)]
    if row.symptoms:
        rows.append(("증상", row.symptoms))
    if row.memo:
        rows.append(("증상 메모", row.memo))
    if row.trip_line:
        rows.append(("오늘 이동", row.trip_line))
    row_html = "".join(f"<div class='row'><span>{k}</span><span>{html.escape(v)}</span></div>" for k, v in rows)
    chosen = f"<p class='ok'>보낸 응답: {REPLIES[row.clinic_reply]}</p>" if row.clinic_reply else ""
    buttons = "".join(
        f"<form method='post' action='/clinic/{token}/reply?r={k}'><button class='btn{' alt' if k == 'busy' else ''}'>{v}</button></form>"
        for k, v in REPLIES.items()
    )
    body = (
        f"<section class='card hero'><p class='kicker'>사전 접수 · {html.escape(row.vet_name)}</p>"
        f"<p class='title'>{html.escape(row.pet_name)} 보호자가 오는 중</p><p class='big'>{head}</p><p class='note'>{sub}</p></section>"
        f"<section class='card'>{row_html}</section>{chosen}<section>{buttons}</section>"
        "<p class='note'>보호자가 직접 보낸 링크예요. 위치는 받지 않고 도착 예정 시각만 보여요. 진단 정보가 아니에요. 2시간 뒤 사라져요.</p>"
    )
    return 200, pages.page(f"{row.pet_name} 사전 접수", body, refresh_sec=15)


def _clinic_row(session: Session, token: str) -> EtaShare | None:
    return session.scalar(select(EtaShare).where(EtaShare.clinic_token_hash == hash_token(token)))


@page_router.get("/clinic/{token}", response_class=HTMLResponse)
def clinic_page(token: str, request: Request) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        row = _clinic_row(session, token)
        if row is not None and row.clinic_seen_at is None and _now() < _aware(row.expires_at):
            row.clinic_seen_at = _now()
            session.commit()
        code, text = render_clinic(row, token=token)
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})


@page_router.post("/clinic/{token}/reply")
def clinic_reply(token: str, r: str, request: Request):
    """병원 응답 (F5-09). 폼 POST → 303으로 접수함으로 돌아간다(새로 고침해도 다시 보내지 않게)."""
    with Session(request.app.state.engine) as session:
        row = _clinic_row(session, token)
        if row is None or _now() >= _aware(row.expires_at):
            return HTMLResponse(pages.page("접수가 끝났어요", "<p class='note'>접수가 끝났어요.</p>"), status_code=404)
        if r not in REPLIES:
            return HTMLResponse(pages.page("다시 눌러 주세요", "<p class='note'>알 수 없는 응답이에요.</p>"), status_code=400)
        row.clinic_reply = r
        row.clinic_reply_at = _now()
        session.commit()
    return RedirectResponse(f"/clinic/{token}", status_code=303)


@page_router.get("/eta/{token}", response_class=HTMLResponse)
def page(token: str, request: Request) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        code, text = render(session.get(EtaShare, hash_token(token)))
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})
