"""화면 전후 캡처 (UI v6 재설계 검수용).

    python scripts/ui_capture.py <태그> [화면 ...]      # 예: before, after, it1 — 화면을 빼면 전부
    python scripts/ui_capture.py --pair before after   # artifacts/ui/v6/<화면>-before-after.png (앱 영역만 잘라 나란히)
    python scripts/ui_capture.py dark --dark 01 02      # 다크 테마 (cmd uimode night yes, 끝나면 되돌림)
    python scripts/ui_capture.py half --split half 01   # 실제 분할 창 1/2(1280dp)·1/3(853dp) — am task resize
    python scripts/ui_capture.py --out v7 --size 1920x1080 w1920 03   # 화면 크기 바꿔 찍기 (adb wm size, 끝나면 항상 reset)
    python scripts/ui_capture.py --out v8 --nav nav 01 02          # 실제 분할 창: 같이가개(왼쪽 1/2) + Pleos 기본 내비(오른쪽), 끝나면 풀기
    python scripts/ui_capture.py --out v7 --sheet sheet.png 03-triplog-before 03-triplog-after   # 찍은 그림을 앱 영역만 잘라 격자로

주행·주행 자극 예보·쉼표·대기·도착·기록 리포트는 **시뮬레이터 실제 값**(pet-trip 장면 점프), 나머지는 화면 계약 Fixtures 장면.
에뮬레이터·디버그 앱·서버(:8000)가 떠 있어야 한다. 나란히 붙이기는 설치된 Chrome 헤드리스를 쓴다(없으면 건너뜀).
"""
import argparse
import json
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
# 분할 창 오른쪽 끝(px): Pleos 앱 영역 x=853부터. 1/2 = 1280dp, 1/3 = 853dp (docs/BLOCKED.md 10/30)
SPLIT_RIGHT = {"half": 2133, "third": 1706}
FULL_RIGHT = 2535
# 화면 폭별 앱 영역(x, y, w, h). 이 AVD 런처는 wm size로 바꿔도 왼쪽 차량 영역(853)·위 막대(90)·아래 조작 막대(140)를
# 그대로 두고 앱 작업 영역만 안 고친다 → 1920×1080에서는 보이는 영역(853,90)-(1920,940)으로 작업을 맞춘다 (v7 검수 10/31).
APP_AREA = {2560: (853, 90, 1682, 1210), 1920: (853, 90, 1067, 850)}


def task_id() -> str | None:
    out = subprocess.run(["adb", "shell", "dumpsys", "activity", "activities"], capture_output=True, text=True, encoding="utf-8", errors="replace", env=r.ENV).stdout
    for line in out.splitlines():
        if "Task{" in line and "com.emop.pause" in line and " #" in line:
            return line.split(" #")[1].split()[0]
    return None


def resize(right: int) -> None:
    tid = task_id()
    if tid:
        subprocess.run(["adb", "shell", "am", "task", "resize", tid, "853", "90", str(right), "1300"], capture_output=True, env=r.ENV)
        time.sleep(3)


NAV_PKG = "ai.umos.maps.android.navigation.app"


def nav_split() -> None:
    """런처 분할: 기본 내비를 띄운 뒤 같이가개를 앞으로, 앱 영역 오른쪽 끝 손잡이(⋮)를 왼쪽으로 끈다 (10/31 확인)."""
    subprocess.run(["adb", "shell", "monkey", "-p", NAV_PKG, "-c", "android.intent.category.LAUNCHER", "1"], capture_output=True, env=r.ENV)
    time.sleep(4)
    r.start()
    time.sleep(2)
    subprocess.run(["adb", "shell", "input", "swipe", "2547", "720", "1700", "720", "800"], capture_output=True, env=r.ENV)
    # 창 크기가 바뀐 뒤 다시 그릴 때까지 (4초면 바뀌는 중에 찍힐 때가 있다)
    time.sleep(8)


def nav_unsplit() -> None:
    """분할 풀기: 가운데 손잡이를 오른쪽 끝으로 끈 뒤 같이가개를 다시 앞으로."""
    subprocess.run(["adb", "shell", "input", "swipe", "1705", "720", "2555", "720", "800"], capture_output=True, env=r.ENV)
    time.sleep(3)
    r.start()
    time.sleep(2)


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
    if not sim(2400, "다음 쉼표", settle=2, cabin=31):
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
    # 도착 장면으로 바로 점프: 건너뛴 구간은 엔진이 1초씩 따라잡은 값으로 이동 기록을 채운다(2026-10-31, 처음부터 달린 것과 같은 숫자)
    if not sim(10843, "내렸어요", timeout=100, settle=1):
        return False
    xy = next((xy for t, xy in r.nodes() if t == "내렸어요"), None)
    if xy:
        r.tap(xy)
    ok = r.wait_for("병원에 보여 줄 내용", 15) is not None
    time.sleep(3)
    return ok


def compare() -> bool:
    depart()
    time.sleep(2)
    r.start(compare=True)
    time.sleep(3)
    return True


SCREENS = {
    "01-drive": lambda: sim(1800, "다음 쉼표", settle=8),
    "02-forecast": lambda: sim(8748, "덜 흔들려요", timeout=40, settle=2),
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
    "12b-vets-call": lambda: fixture("vets_call"),
    "17-stimulus": lambda: fixture("stimulus"),
    "13-arrival": lambda: sim(10843, "내렸어요", timeout=100, settle=1),
    "14-settings": lambda: fixture("settings"),
    "15-onboarding": lambda: fixture("onboard_1"),
    "16-compare": compare,
}


CHROME = Path(os.environ.get("PROGRAMFILES", r"C:\Program Files")) / "Google/Chrome/Application/chrome.exe"


def screen_width(path: Path) -> int:
    meta = path.with_suffix(".json")
    if meta.exists():
        return json.loads(meta.read_text(encoding="utf-8")).get("width", 2560)
    return 2560


def compose(images: list[tuple[str, Path]], out: Path, cols: int = 2, scale: float = 0.5) -> None:
    """앱 영역만 잘라 격자로 붙인다(설치된 Chrome 헤드리스로 HTML을 찍는다). 위에 이름표. 화면 폭이 달라도 칸 크기는 같다."""
    w, h, label = int(1682 * scale), int(1210 * scale), 44

    def cell(name: str, path: Path) -> str:
        # 실제 픽셀 비율 그대로(같은 배율 scale) — 1920 앱 영역은 더 작게 보인다
        sw = screen_width(path)
        x, y, aw, ah = APP_AREA.get(sw, APP_AREA[2560])
        return (f"<div class='c'><p>{name}</p><div class='v' style='width:{aw * scale}px;height:{ah * scale}px'><img src='{path.as_uri()}' "
                f"style='left:{-x * scale}px;top:{-y * scale}px;width:{sw * scale}px'></div></div>")

    cells = "".join(cell(n, p) for n, p in images)
    rows = (len(images) + cols - 1) // cols
    html = (
        "<html><body style='margin:0;background:#fff;font:600 22px sans-serif'>"
        f"<style>.c{{display:inline-block;vertical-align:top;margin:0 20px 20px 0}}p{{margin:8px 0;height:{label - 16}px}}"
        f".v{{width:{w}px;height:{h}px;overflow:hidden;position:relative;border:1px solid #DFE4EA}}"
        f".v img{{position:absolute}}</style>{cells}</body></html>"
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
    ap.add_argument("--split", choices=list(SPLIT_RIGHT))
    ap.add_argument("--dark", action="store_true")
    ap.add_argument("--out", default="v6", help="artifacts/ui/<이름>")
    ap.add_argument("--size", help="예: 1920x1080 — adb shell wm size로 바꿔 찍고 끝나면 wm size reset")
    ap.add_argument("--sheet", help="찍지 않고 screens(파일 이름, .png 생략)를 한 장으로: 2칸 격자")
    ap.add_argument("--nav", action="store_true", help="런처 분할 창(같이가개 + 기본 내비)으로 찍는다")
    a = ap.parse_args()
    global OUT
    OUT = r.ROOT / "artifacts" / "ui" / a.out
    OUT.mkdir(parents=True, exist_ok=True)
    if a.pair:
        pair(*a.pair)
        return 0
    if a.sheet:
        # 첫 이름은 위치 인자 tag로 들어간다
        compose([(n, OUT / f"{n}.png") for n in [a.tag, *a.screens] if n], OUT / a.sheet)
        print(a.sheet)
        return 0
    names = [n for n in SCREENS if not a.screens or any(n.startswith(s) or s in n for s in a.screens)]
    width = int(a.size.split("x")[0]) if a.size else 2560
    split_on = False
    try:
        if a.size:
            subprocess.run(["adb", "shell", "wm", "size", a.size], capture_output=True, env=r.ENV)
            time.sleep(6)
        if a.dark:
            subprocess.run(["adb", "shell", "cmd", "uimode", "night", "yes"], capture_output=True, env=r.ENV)
            time.sleep(2)
        for n in names:
            ok = SCREENS[n]()
            # 장면에 들어간 뒤 분할한다 (분할한 채 am start로 장면을 열면 런처가 분할을 푼다).
            # 답을 기다리는 쉼표 추천은 분할하는 10초 사이에 끝나므로 --split third(같은 1/3 폭)로 찍는다.
            if a.nav:
                nav_split()
                split_on = True
            if a.size and width in APP_AREA:
                x, y, w, h = APP_AREA[width]
                tid = task_id()
                if tid:
                    subprocess.run(["adb", "shell", "am", "task", "resize", tid, str(x), str(y), str(x + w), str(y + h)], capture_output=True, env=r.ENV)
                    time.sleep(4)
            if a.split:
                resize(SPLIT_RIGHT[a.split])
            path = OUT / f"{n}-{a.tag}.png"
            with open(path, "wb") as f:
                f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, env=r.ENV).stdout)
            path.with_suffix(".json").write_text(json.dumps({"width": width}), encoding="utf-8")
            print(f"{n}: {'OK' if ok else '기대 글자 못 찾음'} → {path.name}", flush=True)
            if split_on:
                nav_unsplit()
                split_on = False
    finally:
        if split_on:
            nav_unsplit()
        if a.split:
            resize(FULL_RIGHT)
        if a.dark:
            subprocess.run(["adb", "shell", "cmd", "uimode", "night", "no"], capture_output=True, env=r.ENV)
        if a.size:
            subprocess.run(["adb", "shell", "wm", "size", "reset"], capture_output=True, env=r.ENV)
            print("wm size reset:", subprocess.run(["adb", "shell", "wm", "size"], capture_output=True, text=True, env=r.ENV).stdout.strip(), flush=True)
            time.sleep(6)
            resize(FULL_RIGHT)
    r.start(scene="none", compare=False)
    return 0


if __name__ == "__main__":
    sys.exit(main())
