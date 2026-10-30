"""동물병원 도착 예정 공유 (F5, V5 §5).

동물병원 안내 중 앱이 "도착 예정 공유"를 누르면 무작위 토큰 링크를 만든다. 링크는 보호자 폰으로 보내고,
보호자가 병원·가족에게 직접 전달한다(앱·서버가 병원에 자동으로 보내지 않는다).
**위치는 받지 않는다** — 반려견 이름·병원 이름·도착 예정 분·증상 메모만. 토큰은 128비트, 서버에는 해시만, 2시간 만료.
"""
import html
import math
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app import pages
from app.api.deps import get_session, hash_token, require_device
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
    session.add(EtaShare(token_hash=hash_token(token), device_id=device_id, pet_name=body.petName.strip(), vet_name=body.vetName.strip(),
                         eta_minutes=body.etaMinutes, symptoms=symptoms, memo=(body.memo or "").strip() or None,
                         trip_line=(body.tripLine or "").strip() or None, created_at=now, expires_at=expires))
    session.commit()
    return {"token": token, "pageUrl": f"/eta/{token}", "expiresAt": expires.isoformat(timespec="seconds")}


def render(row: EtaShare | None, now: datetime | None = None) -> tuple[int, str]:
    now = now or _now()
    if row is None or now >= _aware(row.expires_at):
        body = "<section class='card hero'><p class='title'>공유가 끝났어요</p><p class='note'>도착 예정 공유는 2시간 뒤 사라져요.</p></section>"
        return (404 if row is None else 410), pages.page("공유가 끝났어요", body)
    arrive = _aware(row.created_at) + timedelta(minutes=row.eta_minutes)
    left = max(0, math.ceil((arrive - now).total_seconds() / 60))  # 도착 예정은 올려서 (늦게 말하지 않게)
    kst = arrive.astimezone(timezone(timedelta(hours=9)))
    rows = [("병원", row.vet_name), ("도착 예정", f"{kst:%H:%M} (지금부터 약 {left}분)")]
    if row.symptoms:
        rows.append(("증상", row.symptoms))
    if row.memo:
        rows.append(("증상 메모", row.memo))
    if row.trip_line:
        rows.append(("오늘 이동", row.trip_line))
    row_html = "".join(f"<div class='row'><span>{k}</span><span>{html.escape(v)}</span></div>" for k, v in rows)
    body = (
        f"<section class='card hero'>{pages.DOG}<p class='title'>{html.escape(row.pet_name)} · 동물병원으로 가는 중</p>"
        f"<p class='big'>{left}분 뒤 도착</p></section><section class='card'>{row_html}</section>"
        "<p class='note'>보호자가 직접 보낸 링크예요. 위치는 공유하지 않고 도착 예정 시각만 보여요. 2시간 뒤 사라져요.</p>"
    )
    return 200, pages.page(f"{row.pet_name} 도착 예정", body, refresh_sec=30)


@page_router.get("/eta/{token}", response_class=HTMLResponse)
def page(token: str, request: Request) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        code, text = render(session.get(EtaShare, hash_token(token)))
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})
