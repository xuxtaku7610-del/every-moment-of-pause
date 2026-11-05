"""README 대표 이미지 docs/readme/banner.png 다시 만들기 (2026-11-05).

    server\\.venv\\Scripts\\python scripts/make_banner.py          # docs/readme/drive.png·forecast.png → banner.png

- 1600×760, 배경 #EEF3FB, 오른쪽 위 연한 원 #E2EBFA, 왼쪽 글(Pretendard, app-android/app/src/main/res/font/),
  오른쪽에 두 화면(둥근 모서리·옅은 그림자)을 겹쳐 놓고 아래 캡션.
- 그림 도구(Pillow)를 새로 설치하지 않으려고 HTML을 Playwright(설치된 Chrome)로 찍는다 — Playwright는 server/.venv에 있다.
- 결과 PNG에는 텍스트·출처 메타데이터 청크를 남기지 않는다(IHDR·IDAT·IEND 등 그림 청크만 다시 쓴다).
"""
import struct
import sys
import tempfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FONT = ROOT / "app-android" / "app" / "src" / "main" / "res" / "font"
SRC = ROOT / "docs" / "readme"
OUT = SRC / "banner.png"
W, H = 1600, 760

# 화면 두 장: 화면 위쪽만 보여 준다(분할 창 폭 기준 비율). (left, top, width, 보이는 높이)
DRIVE = (868, 70, 384, 560)
FORECAST = (1228, 128, 344, 502)

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
         box-shadow: 0 14px 40px rgba(24, 34, 48, 0.12), 0 2px 6px rgba(24, 34, 48, 0.06); }}
.shot img {{ display: block; width: 100%; }}
.cap {{ position: absolute; font-size: 24px; font-weight: 400; color: #5F6B7B; text-align: center; letter-spacing: -0.3px; }}
</style></head><body>
<div class="circle"></div>
<div class="text">
  <div class="kicker">Pleos Connect 차량용 앱</div>
  <div class="name">같이가개</div>
  <div class="sub">반려견과 차로 가는 모든 순간</div>
  <ul><li>굽은 길을 미리 알려 덜 흔들리게</li><li>필요할 때만 조용히, 말로 묻고 답하게</li><li>잠깐 둘 때, 아플 때, 내릴 때까지</li></ul>
</div>
<div class="shot" style="left:{d[0]}px; top:{d[1]}px; width:{d[2]}px; height:{d[3]}px"><img src="{drive}"></div>
<div class="shot" style="left:{c[0]}px; top:{c[1]}px; width:{c[2]}px; height:{c[3]}px"><img src="{forecast}"></div>
<div class="cap" style="left:{d[0]}px; width:{dcap}px; top:{capy}px">주행 중 · 내비 옆 분할 창</div>
<div class="cap" style="left:{c[0]}px; width:{c[2]}px; top:{capy}px">굽은 길 예보</div>
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

    for p in (SRC / "drive.png", SRC / "forecast.png"):
        if not p.exists():
            print(f"없음: {p}")
            return 2
    html = HTML.format(f=FONT.as_uri(), W=W, H=H, d=DRIVE, c=FORECAST, dcap=FORECAST[0] - DRIVE[0],
                       capy=max(DRIVE[1] + DRIVE[3], FORECAST[1] + FORECAST[3]) + 22,
                       drive=(SRC / "drive.png").as_uri(), forecast=(SRC / "forecast.png").as_uri())
    with tempfile.TemporaryDirectory() as tmp:
        page_file = Path(tmp) / "banner.html"
        page_file.write_text(html, encoding="utf-8")
        with sync_playwright() as pw:
            b = pw.chromium.launch(channel="chrome", args=["--allow-file-access-from-files"])
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
