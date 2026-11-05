"""README 대표 이미지 docs/readme/banner.png 다시 만들기 (2026-11-05).

    server\\.venv\\Scripts\\python scripts/make_banner.py          # docs/readme/suggest.png·nearby.png → banner.png

- 1600×760, 배경 #EEF3FB, 오른쪽 위 연한 원 #E2EBFA, 왼쪽 글(Pretendard, app-android/app/src/main/res/font/),
  오른쪽에 쉼표 추천과 주변 추천 두 화면(둥근 모서리·옅은 그림자)을 나란히 놓고 아래 캡션.
- 그림 도구(Pillow)를 새로 설치하지 않으려고 HTML을 Playwright로 찍는다 — Playwright는 server/.venv에 있다.
- 결과 PNG에는 텍스트·출처 메타데이터 청크를 남기지 않는다(IHDR·IDAT·IEND 등 그림 청크만 다시 쓴다).
"""
import struct
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FONT = ROOT / "app-android" / "app" / "src" / "main" / "res" / "font"
SRC = ROOT / "docs" / "readme"
OUT = SRC / "banner.png"
W, H = 1600, 760

# 화면 두 장: 카드가 있는 위쪽만 보여 준다(원본 847×1210 중 위 1040px). (left, top, width, 보이는 높이)
FIRST = (790, 92, 384, 472)    # suggest.png
SECOND = (1196, 150, 360, 442)  # nearby.png

HTML = """<!doctype html><html><head><meta charset="utf-8"><style>
@font-face {{ font-family: P; font-weight: 400; src: url('{f}/pretendard_regular.otf'); }}
@font-face {{ font-family: P; font-weight: 600; src: url('{f}/pretendard_semibold.otf'); }}
@font-face {{ font-family: P; font-weight: 800; src: url('{f}/pretendard_extrabold.otf'); }}
html, body {{ margin: 0; width: {W}px; height: {H}px; overflow: hidden; background: #EEF3FB; font-family: P, sans-serif; }}
.circle {{ position: absolute; left: 920px; top: -400px; width: 1100px; height: 1100px; border-radius: 50%; background: #E2EBFA; }}
.text {{ position: absolute; left: 90px; top: 148px; color: #182230; }}
.kicker {{ color: #3478E5; font-size: 30px; font-weight: 600; letter-spacing: -0.3px; }}
.name {{ font-size: 112px; font-weight: 800; letter-spacing: -3px; line-height: 1; margin-top: 26px; }}
.sub {{ font-size: 44px; font-weight: 600; letter-spacing: -1px; margin-top: 30px; }}
ul {{ list-style: none; padding: 0; margin: 40px 0 0 0; }}
li {{ font-size: 32px; font-weight: 400; color: #2B3545; letter-spacing: -0.5px; margin-bottom: 20px; display: flex; align-items: center; }}
li::before {{ content: ""; width: 12px; height: 12px; border-radius: 50%; background: #3478E5; margin-right: 18px; flex: none; }}
.shot {{ position: absolute; border-radius: 26px; overflow: hidden; background: #F5F6F8;
         box-shadow: 0 14px 40px rgba(24, 34, 48, 0.14), 0 2px 6px rgba(24, 34, 48, 0.06); }}
.shot img {{ display: block; width: 100%; }}
.cap {{ position: absolute; font-size: 24px; font-weight: 400; color: #5F6B7B; text-align: center; letter-spacing: -0.3px; }}
</style></head><body>
<div class="circle"></div>
<div class="text">
  <div class="kicker">Pleos Connect 차량용 앱</div>
  <div class="name">같이가개</div>
  <div class="sub">반려견과 차로 가는 모든 순간</div>
  <ul><li>굽은 길을 미리 알려 덜 흔들리게</li><li>쉴 곳과 들를 곳을 말로 묻고 답하게</li><li>잠깐 둘 때, 아플 때, 내릴 때까지</li></ul>
</div>
<div class="shot" style="left:{a[0]}px; top:{a[1]}px; width:{a[2]}px; height:{a[3]}px"><img src="{first}"></div>
<div class="shot" style="left:{b[0]}px; top:{b[1]}px; width:{b[2]}px; height:{b[3]}px"><img src="{second}"></div>
<div class="cap" style="left:{a[0]}px; width:{acap}px; top:{capy}px">쉼표 추천</div>
<div class="cap" style="left:{b[0]}px; width:{b[2]}px; top:{capy}px">주변 추천</div>
</body></html>"""


def strip_png(path: Path) -> None:
    """그림에 필요한 청크만 남긴다(tEXt·iTXt·eXIf·caBX 등 메타데이터 제거)."""
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    keep = {b"IHDR", b"PLTE", b"tRNS", b"IDAT", b"IEND", b"sRGB", b"gAMA", b"pHYs"}
    out, i = [data[:8]], 8
    while i < len(data):
        n = struct.unpack(">I", data[i:i + 4])[0]
        kind = data[i + 4:i + 8]
        if kind in keep:
            out.append(data[i:i + 12 + n])
        i += 12 + n
    path.write_bytes(b"".join(out))


def main() -> int:
    from playwright.sync_api import sync_playwright

    global OUT
    if len(sys.argv) > 2 and sys.argv[1] == "--out":  # 시험용: 다른 곳에 쓰기
        OUT = Path(sys.argv[2])

    first, second = SRC / "suggest.png", SRC / "nearby.png"
    for p in (first, second):
        if not p.exists():
            print(f"없음: {p}")
            return 2
    html = HTML.format(f=FONT.as_uri(), W=W, H=H, a=FIRST, b=SECOND, acap=SECOND[0] - FIRST[0],
                       capy=max(FIRST[1] + FIRST[3], SECOND[1] + SECOND[3]) + 22,
                       first=first.as_uri(), second=second.as_uri())
    with tempfile.TemporaryDirectory() as tmp:
        page_file = Path(tmp) / "banner.html"
        page_file.write_text(html, encoding="utf-8")
        with sync_playwright() as pw:
            args = ["--allow-file-access-from-files"]
            try:
                b = pw.chromium.launch(channel="chrome", args=args)
            except Exception:
                b = pw.chromium.launch(args=args)
            pg = b.new_page(viewport={"width": W, "height": H}, device_scale_factor=1)
            pg.goto(page_file.as_uri())
            pg.evaluate("document.fonts.ready")
            pg.wait_for_timeout(500)
            pg.screenshot(path=str(OUT), clip={"x": 0, "y": 0, "width": W, "height": H})
            b.close()
    strip_png(OUT)
    print(f"{OUT} {W}×{H}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
