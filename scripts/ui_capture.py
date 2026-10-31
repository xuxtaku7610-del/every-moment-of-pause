"""화면 전후 캡처 (UI v6 재설계 검수용).

    python scripts/ui_capture.py <태그> [화면 ...]      # 예: before, after, it1 — 화면을 빼면 전부
    python scripts/ui_capture.py --pair before after   # artifacts/ui/v6/<화면>-before-after.png (앱 영역만 잘라 나란히)

주행·멀미 예보·쉼표·대기·도착·이동 기록은 **시뮬레이터 실제 값**(pet-trip 장면 점프), 나머지는 화면 계약 Fixtures 장면.
에뮬레이터·디버그 앱·서버(:8000)가 떠 있어야 한다. 나란히 붙이기는 설치된 Chrome 헤드리스를 쓴다(없으면 건너뜀).
"""
import argparse
import os
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import rehearsal as r  # noqa: E402

OUT = r.ROOT / "artifacts" / "ui" / "v6"
# Pleos 런처 안 앱 영역 (2560×1440 원본 기준)
CROP = "1682:1210:853:90"


def sim(seek: int, wait: str | None = None, timeout: float = 30, settle: float = 6, speed: int = 1, **extra: object) -> bool:
    r.start(sim="pet-trip", onboard="done", scene="none", demo=False, compare=False, seek=seek, action="play", speed=speed, **extra)
    ok = True
    if wait:
        ok = r.wait_for(wait, timeout) is not None
    time.sleep(settle)
    return ok


def fixture(name: str) -> bool:
    r.start(scene=name, demo=False)
    time.sleep(3)
    return True


def depart() -> bool:
    r.start(sim="pet-trip", onboard="done", scene="none", demo=False, compare=False, seek=0, action="pause", speed=1)
    return r.wait_for("체크", 20) is not None


def distress() -> bool:
    if not sim(2400, "멀미 지수", settle=2, cabin=31):
        return False
    r.start(say="콩이가 헥헥거려")
    time.sleep(1)
    return r.wait_for("괜찮아졌어요", 15) is not None


def wait_mode() -> bool:
    if not sim(5540, settle=2):
        return False
    r.start(say="잠깐 다녀올게")
    ok = r.wait_for("주인이", 25) is not None
    time.sleep(3)
    return ok


def triplog() -> bool:
    # 점프로 건너뛴 구간은 멀미 기록이 비므로, 고갯길 앞(8,400초)부터 10배속으로 실제로 달려 기록을 채운다(100배속은 1초 표본을 건너뜀)
    if not sim(8400, "내렸어요", timeout=420, settle=1, speed=10):
        return False
    xy = next((xy for t, xy in r.nodes() if t == "내렸어요"), None)
    if xy:
        r.tap(xy)
    ok = r.wait_for("이동 기록", 15) is not None
    time.sleep(3)
    return ok


def compare() -> bool:
    depart()
    time.sleep(2)
    r.start(compare=True)
    time.sleep(3)
    return True


SCREENS = {
    "01-drive": lambda: sim(210, "멀미 지수", settle=8),
    "02-forecast": lambda: sim(8748, "굽은 길", timeout=40, settle=2),
    "03-triplog": triplog,
    "04-pet": lambda: fixture("pet"),
    "05-depart": depart,
    "06-suggest": lambda: sim(3420, "여기서 쉴게요", timeout=75, settle=1),
    "07-distress": distress,
    "08-vet-drive": lambda: fixture("vet_drive"),
    "09-rest": lambda: sim(5240, "잠깐 다녀올게", timeout=25, settle=2),
    "10-wait": wait_mode,
    "11-places": lambda: fixture("places"),
    "12-vets": lambda: fixture("vets"),
    "13-arrival": lambda: sim(10843, "내렸어요", timeout=100, settle=1),
    "14-settings": lambda: fixture("settings"),
    "15-onboarding": lambda: fixture("onboard_1"),
    "16-compare": compare,
}


CHROME = Path(os.environ.get("PROGRAMFILES", r"C:\Program Files")) / "Google/Chrome/Application/chrome.exe"


def compose(images: list[tuple[str, Path]], out: Path, cols: int = 2, scale: float = 0.5) -> None:
    """앱 영역만 잘라 격자로 붙인다(설치된 Chrome 헤드리스로 HTML을 찍는다). 위에 이름표."""
    w, h, label = int(1682 * scale), int(1210 * scale), 44
    cells = "".join(
        f"<div class='c'><p>{name}</p><div class='v'><img src='{path.as_uri()}'></div></div>" for name, path in images
    )
    rows = (len(images) + cols - 1) // cols
    html = (
        "<html><body style='margin:0;background:#fff;font:600 22px sans-serif'>"
        f"<style>.c{{display:inline-block;vertical-align:top;margin:0 20px 20px 0}}p{{margin:8px 0;height:{label - 16}px}}"
        f".v{{width:{w}px;height:{h}px;overflow:hidden;position:relative;border:1px solid #DFE4EA}}"
        f".v img{{position:absolute;left:{-853 * scale}px;top:{-90 * scale}px;width:{2560 * scale}px}}</style>{cells}</body></html>"
    )
    tmp = OUT / "_compose.html"
    tmp.write_text(html, encoding="utf-8")
    size = f"{cols * (w + 22)},{rows * (h + label + 22)}"
    subprocess.run([str(CHROME), "--headless=new", "--hide-scrollbars", "--allow-file-access-from-files", f"--window-size={size}",
                    f"--screenshot={out}", tmp.as_uri()], capture_output=True, timeout=60)
    tmp.unlink(missing_ok=True)


def pair(a: str, b: str) -> None:
    if not CHROME.exists():
        print("Chrome 없음 — 나란히 붙이기 건너뜀")
        return
    for name in SCREENS:
        pa, pb = OUT / f"{name}-{a}.png", OUT / f"{name}-{b}.png"
        if pa.exists() and pb.exists():
            out = OUT / f"{name}-{a}-{b}.png"
            compose([(f"{name} · {a}", pa), (f"{name} · {b}", pb)], out)
            print(out.name)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("tag", nargs="?")
    ap.add_argument("screens", nargs="*")
    ap.add_argument("--pair", nargs=2)
    a = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    if a.pair:
        pair(*a.pair)
        return 0
    names = [n for n in SCREENS if not a.screens or any(n.startswith(s) or s in n for s in a.screens)]
    for n in names:
        ok = SCREENS[n]()
        path = OUT / f"{n}-{a.tag}.png"
        with open(path, "wb") as f:
            f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, env=r.ENV).stdout)
        print(f"{n}: {'OK' if ok else '기대 글자 못 찾음'} → {path.name}", flush=True)
    r.start(scene="none", compare=False)
    return 0


if __name__ == "__main__":
    sys.exit(main())
