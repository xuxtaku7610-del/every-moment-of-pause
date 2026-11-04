"""반려견 사진 QR 업로드 (v10-3 2번, 사용자 결정 11/04 — QR 업로드만) — api_spec §2.8-4.

1) 차(앱): `POST /api/v1/photos` → 토큰·`/upload/<토큰>` 주소 → 화면에 QR.
2) 보호자 폰: `GET /upload/<토큰>` 페이지에서 사진 1장 고르기 → **폰 브라우저가 긴 변 1024px JPEG로 줄여** `POST /upload/<토큰>`(본문 = 이미지).
   서버는 크기(5MB 이하)·형식(JPEG·PNG·WebP 앞 바이트)만 확인한다 — 서버에 이미지 라이브러리를 두지 않는다.
3) 차: `GET /api/v1/photos/<토큰>`을 몇 초마다 — 올라오기 전 204, 올라오면 이미지 바이트 200 후 **서버에서 지운다**(받은 시각만 남김).
토큰 128비트, 서버에는 SHA-256 해시만, 10분 만료. 사진은 반려견 프로필 사진으로만 쓰고 다른 데 보내지 않는다(처리방침).
"""
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, Response
from sqlalchemy.orm import Session

from app import pages
from app.api.deps import get_session, hash_token, require_device
from app.errors import ApiError
from app.models import PhotoUpload

router = APIRouter()
page_router = APIRouter()
TTL_MINUTES = 10
MAX_BYTES = 5 * 1024 * 1024
MAGIC = {
    "image/jpeg": (b"\xff\xd8\xff",),
    "image/png": (b"\x89PNG\r\n\x1a\n",),
    "image/webp": (b"RIFF",),
}


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _aware(t: datetime) -> datetime:
    return t if t.tzinfo else t.replace(tzinfo=timezone.utc)


def _live(row: PhotoUpload | None, now: datetime | None = None) -> bool:
    return row is not None and (now or _now()) < _aware(row.expires_at)


def kind_of(data: bytes, declared: str | None) -> str | None:
    """앞 바이트로 형식 확인 — 선언한 형식과 맞아야 한다(WebP는 RIFF....WEBP)."""
    ct = (declared or "").split(";")[0].strip().lower()
    if ct not in MAGIC or not any(data.startswith(m) for m in MAGIC[ct]):
        return None
    if ct == "image/webp" and data[8:12] != b"WEBP":
        return None
    return ct


@router.post("/photos", status_code=201)
def start(session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> dict:
    token = secrets.token_urlsafe(16)
    now = _now()
    expires = now + timedelta(minutes=TTL_MINUTES)
    session.add(PhotoUpload(token_hash=hash_token(token), device_id=device_id, created_at=now, expires_at=expires))
    session.commit()
    return {"token": token, "pageUrl": f"/upload/{token}", "expiresAt": expires.isoformat(timespec="seconds")}


@router.get("/photos/{token}")
def fetch(token: str, session: Session = Depends(get_session), device_id: str = Depends(require_device)) -> Response:
    row = session.get(PhotoUpload, hash_token(token))
    if row is None or row.device_id != device_id:
        raise ApiError(404, "PHOTO_NOT_FOUND", "사진 업로드를 찾을 수 없습니다.")
    if row.fetched_at is not None:
        raise ApiError(410, "PHOTO_TAKEN", "이미 받아 간 사진입니다.")
    if not _live(row):
        raise ApiError(410, "PHOTO_EXPIRED", "만료된 사진 업로드입니다.")
    if row.image is None:
        return Response(status_code=204)
    data, ct = row.image, row.content_type or "image/jpeg"
    # 차가 받아 갔으면 서버의 사진은 바로 지운다 (받은 시각만 남김)
    row.image, row.fetched_at = None, _now()
    session.commit()
    return Response(content=data, media_type=ct, headers={"Cache-Control": "no-store"})


# 폰 브라우저에서 긴 변 1024px JPEG(품질 0.85)로 줄여 본문 그대로 올린다 (서버는 이미지를 열지 않는다)
UPLOAD_JS = """
(function(){
var f=document.getElementById('file'),b=document.getElementById('send'),m=document.getElementById('msg'),pv=document.getElementById('pv');
var blob=null;
f.onchange=function(){var file=f.files[0];if(!file)return;m.textContent='사진을 줄이는 중…';
var img=new Image();img.onload=function(){var s=Math.min(1,1024/Math.max(img.width,img.height));var c=document.createElement('canvas');
c.width=Math.round(img.width*s);c.height=Math.round(img.height*s);c.getContext('2d').drawImage(img,0,0,c.width,c.height);
c.toBlob(function(bl){blob=bl;pv.src=URL.createObjectURL(bl);pv.style.display='block';b.disabled=false;m.textContent='이 사진을 보낼까요?';},'image/jpeg',0.85);};
img.onerror=function(){m.textContent='이 파일은 열 수 없어요. 다른 사진을 골라 주세요.';};img.src=URL.createObjectURL(file);};
b.onclick=function(){if(!blob)return;b.disabled=true;m.textContent='보내는 중…';
fetch(location.pathname,{method:'POST',headers:{'Content-Type':'image/jpeg'},body:blob}).then(function(r){
if(r.ok){m.textContent='보냈어요. 차 화면에 사진이 곧 보여요.';f.disabled=true;}else{r.json().then(function(j){m.textContent=(j.error&&j.error.message)||'보내지 못했어요.';}).catch(function(){m.textContent='보내지 못했어요.';});b.disabled=false;}
}).catch(function(){m.textContent='연결이 끊겼어요. 다시 눌러 주세요.';b.disabled=false;});};
})();
"""


def render(row: PhotoUpload | None) -> tuple[int, str]:
    if not _live(row) or row.uploaded_at is not None:
        done = row is not None and row.uploaded_at is not None
        title = "사진을 보냈어요" if done else "주소가 만료됐어요"
        note = "차 화면에서 사진을 확인해 주세요." if done else "차 화면에서 [폰으로 사진 올리기]를 다시 누르면 새 QR이 생겨요. 주소는 10분 동안만 열려요."
        return (200 if done else 410 if row else 404), pages.page(title, f"<section class='card hero'><p class='title'>{title}</p><p class='note'>{note}</p></section>")
    body = (
        "<section class='card hero'><p class='title'>반려견 사진 올리기</p>"
        "<p class='note' style='margin-top:8px'>사진 1장 · 폰에서 작게 줄여 보내요 · 차가 받으면 서버에서 지워요</p>"
        "<img id='pv' alt='고른 사진' style='display:none;max-width:100%;border-radius:12px;margin:12px auto 0'>"
        "<input id='file' type='file' accept='image/*' style='margin-top:16px;max-width:100%'>"
        "<button id='send' class='btn' type='button' disabled>보내기</button><p id='msg' class='note'>사진을 골라 주세요.</p></section>"
        "<noscript><p class='note'>이 페이지는 자바스크립트가 필요해요.</p></noscript>"
        "<p class='note'>반려견 프로필 사진으로만 써요. 위치·다른 사진은 보내지 않아요.</p>"
    )
    return 200, pages.page("반려견 사진 올리기", body, script=UPLOAD_JS)


@page_router.get("/upload/{token}", response_class=HTMLResponse)
def page(token: str, request: Request) -> HTMLResponse:
    with Session(request.app.state.engine) as session:
        code, text = render(session.get(PhotoUpload, hash_token(token)))
    return HTMLResponse(text, status_code=code, headers={"Cache-Control": "no-store"})


@page_router.post("/upload/{token}")
async def upload(token: str, request: Request) -> Response:
    """보호자 폰이 올린 사진 1장 (인증 없음 — 토큰이 곧 권한). 5MB 넘거나 이미지가 아니면 거절, 두 번째 사진은 409."""
    declared = request.headers.get("content-length")
    if declared and declared.isdigit() and int(declared) > MAX_BYTES:
        raise ApiError(413, "PHOTO_TOO_LARGE", "사진은 5MB 이하만 올릴 수 있어요.")
    data = await request.body()
    if len(data) > MAX_BYTES:
        raise ApiError(413, "PHOTO_TOO_LARGE", "사진은 5MB 이하만 올릴 수 있어요.")
    ct = kind_of(data, request.headers.get("content-type"))
    if ct is None:
        raise ApiError(415, "PHOTO_TYPE", "JPEG·PNG·WebP 사진만 올릴 수 있어요.")
    with Session(request.app.state.engine) as session:
        row = session.get(PhotoUpload, hash_token(token))
        if row is None:
            raise ApiError(404, "PHOTO_NOT_FOUND", "주소를 찾을 수 없어요.")
        if not _live(row):
            raise ApiError(410, "PHOTO_EXPIRED", "주소가 만료됐어요. 차 화면에서 새 QR을 만들어 주세요.")
        if row.uploaded_at is not None:
            raise ApiError(409, "PHOTO_ALREADY", "사진은 한 장만 올릴 수 있어요.")
        row.image, row.content_type, row.uploaded_at = data, ct, _now()
        session.commit()
    return Response(status_code=204)

