"""폰으로 받는 이동 기록 (v10-3 5번, 사용자 결정 11/04) — api_spec §2.8-3.

도착 요약 화면의 [폰으로 기록 받기]: 앱이 이번 여행 요약(타임라인 · 주행 자극 5분 값 · 표시 구간 · 병원에 보여 줄 줄)을 올리면
토큰을 돌려주고, 보호자 폰은 `GET /report/{token}`(API 접두사 밖, 인증 없음) 페이지를 연다. **위치는 받지 않는다** —
본문에 정의되지 않은 필드가 있으면 422. 토큰 128비트, 서버에는 SHA-256 해시만, 24시간 뒤 만료.
그래프는 앱 v10 기준(STYLE_V10 §5)과 같은 축: 가로 출발 뒤 시간, 세로 0~100, 낮음·보통·높음 띠, 흔들림 많은 구간 앰버 면, 휴식 회색 면, 신고 점선.
"""
import html
import secrets
from datetime import datetime, timedelta, timezone
from typing import Literal

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app import pages
from app.api.deps import get_session, hash_token, require_device
from app.errors import ApiError
from app.models import ReportShare

router = APIRouter()
page_router = APIRouter()
TTL_HOURS = 24
STIMULUS_NOTE = "차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님"


class TimelineItem(BaseModel):
    model_config = {"extra": "forbid"}
    kind: Literal["START", "STIMULUS", "REPORT", "REST", "VET", "ARRIVE"]
    time: str = Field(max_length=8)
    title: str = Field(max_length=60)
    detail: str | None = Field(default=None, max_length=120)


class Mark(BaseModel):
    model_config = {"extra": "forbid"}
    kind: Literal["STOP", "SYMPTOM", "HIGH"]
    minute: int = Field(ge=0, le=1440)
    lengthMin: int = Field(default=0, ge=0, le=1440)
    label: str = Field(default="", max_length=30)


class Row(BaseModel):
    model_config = {"extra": "forbid"}
    key: str = Field(max_length=30)
    value: str = Field(max_length=200)


class ReportCreate(BaseModel):
    """올릴 수 있는 값은 이것뿐이다 (위치·경로·좌표 필드는 422)."""
    model_config = {"extra": "forbid"}
    petNames: list[str] = Field(min_length=1, max_length=3)
    date: str = Field(max_length=20)
    destination: str | None = Field(default=None, max_length=40)
    totalMin: int = Field(ge=0, le=1440)
    distance: str | None = Field(default=None, max_length=16)
    stops: int = Field(ge=0, le=50)
    reports: int = Field(ge=0, le=100)
    timeline: list[TimelineItem] = Field(default_factory=list, max_length=40)
    series: list[int] = Field(default_factory=list, max_length=300)
    sampleMin: int = Field(default=5, ge=1, le=30)
    marks: list[Mark] = Field(default_factory=list, max_length=60)
    share: list[Row] = Field(default_factory=list, max_length=12)
    insight: str | None = Field(default=None, max_length=200)


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _aware(t: datetime) -> datetime:
    return t if t.tzinfo else t.replace(tzinfo=timezone.utc)


@router.post("/reports", status_code=201)
def create(body: ReportCreate, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    if any(not (0 <= v <= 100) for v in body.series):
        raise ApiError(422, "BAD_SERIES", "주행 자극 값은 0~100이어야 합니다.")
    token = secrets.token_urlsafe(16)
    now = _now()
    expires = now + timedelta(hours=TTL_HOURS)
    session.add(ReportShare(token_hash=hash_token(token), device_id=device_id, payload=body.model_dump(), created_at=now, expires_at=expires))
    session.commit()
    return {"token": token, "pageUrl": f"/report/{token}", "expiresAt": expires.isoformat(timespec="seconds")}


def _hm(minutes: int) -> str:
    h, m = divmod(minutes, 60)
    return f"{h}시간 {m}분" if h and m else (f"{h}시간" if h else f"{m}분")


def chart_svg(series: list[int], marks: list[dict], sample_min: int = 5, w: int = 360, h: int = 200) -> str:
    """앱 TrendChart와 같은 축의 정지 그림 (SVG). 값이 2개 미만이면 축과 "기록이 아직 없어요"."""
    left, top, bottom = 30, 22, 24
    pw, ph = w - left - 4, h - top - bottom
    total = max(1, (len(series) - 1) * sample_min)

    def x(m: float) -> float:
        return left + pw * max(0.0, min(1.0, m / total))

    def y(v: float) -> float:
        return top + ph * (1 - max(0, min(100, v)) / 100)

    out = [f"<svg viewBox='0 0 {w} {h}' width='100%' role='img' aria-label='주행 자극 그래프' style='display:block'>"]
    out.append(f"<rect x='{left}' y='{y(100):.1f}' width='{pw}' height='{y(70) - y(100):.1f}' fill='var(--warning-soft)'/>")
    out.append(f"<rect x='{left}' y='{y(70):.1f}' width='{pw}' height='{y(40) - y(70):.1f}' fill='var(--bg)'/>")
    for g in (40, 70):
        out.append(f"<line x1='{left}' x2='{left + pw}' y1='{y(g):.1f}' y2='{y(g):.1f}' stroke='var(--border)'/>"
                   f"<text x='{left - 4}' y='{y(g) + 4:.1f}' font-size='11' text-anchor='end' fill='var(--sub)'>{g}</text>")
    out.append(f"<line x1='{left}' x2='{left + pw}' y1='{y(0):.1f}' y2='{y(0):.1f}' stroke='var(--sub)' stroke-opacity='.5'/>")
    if len(series) < 2:
        out.append(f"<text x='{left + pw / 2}' y='{top + ph / 2}' font-size='14' text-anchor='middle' fill='var(--sub)'>기록이 아직 없어요</text></svg>")
        return "".join(out)
    for m in marks:
        if m["minute"] >= total:
            continue
        x0, x1 = x(m["minute"]), max(x(m["minute"] + m.get("lengthMin", 0)), x(m["minute"]) + 3)
        if m["kind"] == "HIGH":
            out.append(f"<rect x='{x0:.1f}' y='{top}' width='{x1 - x0:.1f}' height='{ph}' fill='var(--warning)' fill-opacity='.22'/>")
        elif m["kind"] == "STOP":
            out.append(f"<rect x='{x0:.1f}' y='{top}' width='{x1 - x0:.1f}' height='{ph}' fill='var(--sub)' fill-opacity='.14'/>")
    for m in marks:
        if m["kind"] == "SYMPTOM" and m["minute"] <= total:
            xs = x(m["minute"])
            out.append(f"<line x1='{xs:.1f}' x2='{xs:.1f}' y1='{top}' y2='{top + ph}' stroke='var(--warning-ink)' stroke-dasharray='4 3'/>"
                       f"<text x='{xs:.1f}' y='{top - 6}' font-size='11' text-anchor='middle' fill='var(--text)'>{html.escape(m.get('label', ''))}</text>")
    pts = " ".join(f"{x(i * sample_min):.1f},{y(v):.1f}" for i, v in enumerate(series))
    out.append(f"<polyline points='{pts}' fill='none' stroke='var(--primary)' stroke-width='3' stroke-linejoin='round' stroke-linecap='round'/>")
    pk = max(range(len(series)), key=lambda i: series[i])
    if series[pk] > 0:
        out.append(f"<circle cx='{x(pk * sample_min):.1f}' cy='{y(series[pk]):.1f}' r='4.5' fill='var(--warning)' stroke='var(--surface)' stroke-width='2'/>")
    tick = 30 if total <= 180 else 60
    for t in range(0, total + 1, tick):
        label = "출발" if t == 0 else (f"{t // 60}시간" if t % 60 == 0 else f"{t // 60}:{t % 60:02d}")
        out.append(f"<text x='{x(t):.1f}' y='{h - 6}' font-size='11' text-anchor='middle' fill='var(--sub)'>{label}</text>")
    out.append("</svg>")
    return "".join(out)


def render(row: ReportShare | None, now: datetime | None = None) -> tuple[int, str]:
    now = now or _now()
    if row is None or now >= _aware(row.expires_at):
        body = "<section class='card hero'><p class='title'>기록이 사라졌어요</p><p class='note'>폰으로 받은 기록은 24시간 뒤 사라져요. 차 화면에서 다시 받을 수 있어요.</p></section>"
        return (404 if row is None else 410), pages.page("기록이 사라졌어요", body)
    p = row.payload
    names = "·".join(p["petNames"])
    tiles = [("총 이동 시간", _hm(p["totalMin"]))]
    if p.get("distance"):
        tiles.append(("거리", p["distance"]))
    tiles += [("휴식", f"{p['stops']}번"), ("관찰 신고", f"{p['reports']}건")]
    tile_html = "".join(f"<div class='tile'><div class='label'>{k}</div><div class='value'>{html.escape(v)}</div></div>" for k, v in tiles)
    series = p.get("series") or []
    peak = max(series) if series else None
    high = sum(1 for m in p.get("marks", []) if m["kind"] == "HIGH")
    nums = ""
    if peak is not None:
        nums = (f"<div class='bar'><div><div class='label' style='font-size:14px;color:var(--sub)'>최고</div><div class='big'>{peak}<span style='font-size:16px;color:var(--sub)'> / 100</span></div></div>"
                f"<div><div style='font-size:14px;color:var(--sub)'>흔들림 많은 구간</div><div class='big'>{high}<span style='font-size:16px;color:var(--sub)'>곳</span></div></div></div>")
    timeline = "".join(
        f"<div class='row'><span>{html.escape(t['time'])}</span><span><b>{html.escape(t['title'])}</b>"
        + (f"<br><small style='color:var(--sub)'>{html.escape(t['detail'])}</small>" if t.get("detail") else "") + "</span></div>"
        for t in p.get("timeline", [])
    ) or "<p class='note'>기록이 아직 없어요</p>"
    share = "".join(f"<div class='row'><span>{html.escape(r['key'])}</span><span>{html.escape(r['value'])}</span></div>" for r in p.get("share", []))
    insight = f"<p style='margin:12px 0 0'>{html.escape(p['insight'])}</p>" if p.get("insight") else ""
    dest = f" · {html.escape(p['destination'])}까지" if p.get("destination") else ""
    body = (
        f"<p class='kicker'>{html.escape(p['date'])}{dest}</p><p class='title'>{html.escape(names)} 이동 기록</p>"
        f"<div class='tiles'>{tile_html}</div>"
        f"<section class='card'><p class='title'>주행 자극</p><p class='kicker'>{STIMULUS_NOTE}</p>{nums}"
        f"{chart_svg(series, p.get('marks', []), p.get('sampleMin', 5))}"
        "<p class='note' style='margin-top:8px'>5분 최고값 · 낮음 &lt; 40 ≤ 보통 &lt; 70 ≤ 높음 · 앰버 면 = 흔들림 많은 구간 · 회색 면 = 휴식 · 점선 = 관찰 신고</p></section>"
        f"<section class='card'><p class='title'>오늘의 길</p>{timeline}{insight}</section>"
        + (f"<section class='card'><p class='title'>병원에 보여 줄 내용</p>{share}</section>" if share else "")
        + "<p class='note'>위치는 담지 않아요 · 진단이 아니에요 · 이 기록은 24시간 뒤 사라져요.</p>"
    )
    return 200, pages.page(f"{names} 이동 기록", body)


@page_router.get("/report/{token}", response_class=HTMLResponse)
def page(token: str, request: Request) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        code, text = render(session.get(ReportShare, hash_token(token)))
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})
