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
        # v10-3: 누가 눌렀는지 — 이름 대신 보호자 1·2
        "returningBy": row.returning_by,
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
    if row.cabin_temp_start_c is None and body.cabinTempC is not None:
        row.cabin_temp_start_c = body.cabinTempC
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

# 실내 온도 단계 — 앱 engine/pet CabinHeat와 같은 값 (근거는 그 파일 주석)
CAUTION_C = 26.0
RISE_C = 2.0
SUMMER = range(6, 10)


def _month() -> int:
    return datetime.now(timezone(timedelta(hours=9))).month


def heat_rising(start: float | None, now: float | None) -> bool:
    return now is not None and (now >= CAUTION_C or (start is not None and now - start >= RISE_C))


def _mins(sec: int) -> str:
    m = sec // 60
    return f"{m}분째" if m >= 1 else f"{sec}초째"


def guardian(g: str | int | None) -> int:
    """같은 링크를 보호자 2명이 연다 (v10-3): `?g=2`면 보호자 2, 그 밖은 보호자 1."""
    return 2 if str(g) == "2" else 1


def _who(n: int) -> str:
    return "보호자 1이" if n == 1 else "보호자 2가"


# 실내 온도가 오르면(차 신호가 있을 때만) 큰 경고 + 진동·소리. 브라우저 정책상 소리·진동은 사용자가 한 번 눌러 켠다(localStorage에 기억).
# 5초마다 같은 주소를 받아 #wait만 바꾼다 — 새로 고침으로 소리 허용이 풀리지 않게.
WAIT_JS = """
(function(){
var KEY='pause-wait-alert',ctx=null,btn=document.getElementById('snd');
function on(){return localStorage.getItem(KEY)==='on';}
function label(){if(!btn)return;btn.setAttribute('aria-pressed',on());btn.textContent=on()?'소리·진동 켜짐':'경고 소리·진동 켜기';}
function unlock(){try{ctx=ctx||new (window.AudioContext||window.webkitAudioContext)();if(ctx.state==='suspended')ctx.resume();}catch(e){}}
function alarm(){if(!on())return;try{if(navigator.vibrate)navigator.vibrate([400,200,400]);}catch(e){}
if(!ctx||ctx.state!=='running')return;for(var i=0;i<3;i++){var o=ctx.createOscillator(),g=ctx.createGain();o.frequency.value=i%2?1046:880;
g.gain.value=0.25;o.connect(g);g.connect(ctx.destination);var t=ctx.currentTime+i*0.3;o.start(t);o.stop(t+0.2);}}
if(btn)btn.onclick=function(){if(on()){localStorage.setItem(KEY,'off');}else{unlock();localStorage.setItem(KEY,'on');setTimeout(alarm,60);}label();};
if(on())unlock();label();
var box=document.getElementById('wait');
if(box&&box.dataset.hot==='1')setTimeout(alarm,300);
setInterval(function(){fetch(location.href,{cache:'no-store'}).then(function(r){return r.text();}).then(function(t){
var nb=new DOMParser().parseFromString(t,'text/html').getElementById('wait');if(!nb){location.reload();return;}
var heated=nb.dataset.hot==='1'&&(!box||box.dataset.hot!=='1');box.replaceWith(nb);box=nb;if(heated)alarm();}).catch(function(){});},""" + str(REFRESH_SEC * 1000) + """);
})();
"""


def render(row: WaitSession | None, token: str = "", g: int = 1) -> tuple[int, str]:
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
    heat = ""
    hot = heat_rising(row.cabin_temp_start_c, s["cabinTempC"])
    if hot:
        # v10-3: 온도가 오르면 크게 (차가 보낸 측정값이 있을 때만 — 지어내지 않는다)
        start = f"{row.cabin_temp_start_c:.0f}℃ → " if row.cabin_temp_start_c is not None else ""
        heat = (
            "<div class='banner hot'><b>실내 온도가 오르고 있어요</b>"
            f"<span class='big'>{start}{s['cabinTempC']:.0f}℃</span><br>바로 차로 돌아가 주세요.</div>"
        )
    elif s["cabinTempC"] is None:
        # 온도 신호가 없으면 경과 시간으로만 안내한다
        summer = " 여름에는 차 안이 빨리 더워져요." if _month() in SUMMER else ""
        heat = f"<p class='note'>이 차는 온도 정보를 보내지 않아 시간으로만 알려요 — {_mins(s['elapsedSec'])}예요.{summer}</p>"
    if row.guardian_returning:
        by = row.returning_by
        who = (f"{_who(by)} 돌아가는 중 · " if by in (1, 2) else "") + "차 화면에 '돌아오는 중'이라고 알렸어요"
        action = f"<div class='ok'>{who}</div>"
    else:
        action = (
            f"<form method='post' action='/wait/{html.escape(token)}/returning?g={g}'>"
            f"<button class='btn' type='submit'>돌아가는 중 (보호자 {g})</button></form>"
        )
    inner = (
        f"<div id='wait' data-hot='{1 if hot else 0}'>{banner}{heat}{stale}<section class='card hero'>{pages.DOG}"
        f"<p class='title'>{'시간이 지났어요' if overdue else '대기 중이에요'}</p><p class='big'>{_mins(s['elapsedSec'])}</p></section>"
        f"<div class='tiles'>{tile_html}</div>{action}</div>"
    )
    body = (
        f"<div class='bar'><p class='kicker'>보호자 {g} 화면</p>"
        "<button id='snd' class='snd' type='button' aria-pressed='false'>경고 소리·진동 켜기</button></div>"
        f"{inner}"
        "<p class='note'>위치는 공유하지 않아요. 보호자 2명까지 같은 주소로 볼 수 있어요. 이 페이지는 보조 안내예요 — 반려견을 차에 오래 혼자 두지 마세요.</p>"
    )
    return 200, pages.page("반려견 대기 중", body, refresh_sec=REFRESH_SEC, script=WAIT_JS)


@page_router.get("/wait/{token}", response_class=HTMLResponse)
def page(token: str, request: Request, g: str | None = None) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        row = session.get(WaitSession, hash_token(token))
        code, text = render(row, token, guardian(g))
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})


@page_router.post("/wait/{token}/returning")
def returning(token: str, request: Request, g: str | None = None) -> Response:
    """보호자 폰 "돌아가는 중" (인증 없음 — 토큰이 곧 권한). 차는 다음 상태 업로드 응답에서 받는다. 먼저 누른 보호자(1·2)를 남긴다."""
    who = guardian(g)
    with Session(request.app.state.engine) as session:
        row = session.get(WaitSession, hash_token(token))
        if row is None or not _live(row):
            return HTMLResponse(render(row, token, who)[1], status_code=404 if row is None else 410)
        if not row.guardian_returning:
            row.guardian_returning, row.returning_by = True, who
        session.commit()
    suffix = "?g=2" if who == 2 else ""
    return RedirectResponse(f"/wait/{token}{suffix}", status_code=303)
