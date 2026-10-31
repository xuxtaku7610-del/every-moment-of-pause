"""시연 리허설 자동 실행 (docs/demo_script.md 순서, 1배속) — PT 핵심 3장면 + 여는·닫는 장면.

에뮬레이터에 디버그 앱이 설치되어 있고 서버(:8000)가 떠 있어야 한다. 장면마다 기대 글자가 화면에 나올 때까지 기다리고
artifacts/rehearsal/에 캡처, 장면별 걸린 시간을 artifacts/rehearsal/timing.md로 남긴다.
병원 접수함은 노트북이 병원 역할: 접수함 페이지를 열고(Playwright가 있으면 캡처) "도착하면 바로 볼게요"를 누른다.

    python scripts/rehearsal.py [--phone]     # --phone: 접수함 페이지를 Playwright(설치된 Chrome)로 함께 캡처
    python scripts/rehearsal.py --all         # 예비 장면(쉼표 추천·잠깐 대기)까지
"""
import urllib.request
import argparse
import os
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artifacts" / "rehearsal"
PKG = "com.emop.pause/.MainActivity"
ENV = {**os.environ, "MSYS_NO_PATHCONV": "1"}


def adb(*args: str, out: bool = False) -> str:
    r = subprocess.run(["adb", *args], capture_output=True, env=ENV)
    return r.stdout.decode("utf-8", "replace") if out else ""


def start(**extras: object) -> None:
    cmd = ["shell", "am", "start", "-n", PKG]
    for k, v in extras.items():
        if isinstance(v, bool):
            cmd += ["--ez", k, str(v).lower()]
        elif isinstance(v, int):
            cmd += ["--ei", k, str(v)]
        else:
            cmd += ["--es", k, f"'{v}'" if " " in str(v) else str(v)]
    adb(*cmd)


def nodes() -> list[tuple[str, tuple[int, int]]]:
    adb("shell", "uiautomator", "dump", "/sdcard/u.xml")
    x = adb("shell", "cat", "/sdcard/u.xml", out=True)
    found = []
    for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
        found.append((m[1], ((int(m[2]) + int(m[4])) // 2, (int(m[3]) + int(m[5])) // 2)))
    return found


def wait_for(text: str, timeout: float) -> tuple[int, int] | None:
    end = time.time() + timeout
    while time.time() < end:
        for t, xy in nodes():
            if text in t:
                return xy
        time.sleep(1)
    return None


def tap(xy: tuple[int, int]) -> None:
    adb("shell", "input", "tap", str(xy[0]), str(xy[1]))


def shot(name: str) -> None:
    with open(OUT / f"{name}.png", "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, env=ENV).stdout)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--phone", action="store_true")
    ap.add_argument("--all", action="store_true")
    a = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    rows: list[tuple[str, float, str]] = []
    t0 = time.time()

    def step(name: str, fn) -> None:
        s = time.time()
        note = fn() or "OK"
        rows.append((name, time.time() - s, note))
        print(f"{name}: {time.time() - s:.1f}s {note}", flush=True)

    def depart():
        start(sim="pet-trip", onboard="done", scene="none", seek=0, action="pause", speed=1, demo=False)
        if not wait_for("출발 전 체크", 20):
            return "막힘: 출발 체크 화면이 안 뜸"
        shot("01-depart")
        for label in ("하네스·켄넬", "물", "배변봉투"):
            xy = next((xy for t, xy in nodes() if t == label), None)
            if xy:
                tap(xy)
        shot("01b-depart-ready")
        xy = wait_for("이대로 출발", 5)
        if xy:
            tap(xy)
        return None

    def highway():
        start(seek=1800, action="play", speed=1)
        if not wait_for("멀미 지수", 20):
            return "막힘: 주행 화면이 안 뜸"
        shot("02-highway")
        return None

    def logcat_clinic() -> str | None:
        x = subprocess.run(["adb", "logcat", "-d", "-s", "Intake"], capture_output=True, env=ENV).stdout.decode("utf-8", "replace")
        m = re.findall(r"clinic=(/clinic/[A-Za-z0-9_-]+)", x)
        return m[-1] if m else None

    def compare():
        # PT 1: 고갯길 예보 → 고갯길 비교 크게 (55 → 30, +7분 12초)
        start(seek=8748, action="play", speed=1)
        if not wait_for("예상 최고", 40):
            return "막힘: 예보가 40초 안에 안 나옴"
        shot("03-forecast")
        # 비교 화면은 정차 중에만 열린다 → 출발 전(0초, P단)으로 돌아가 연다
        start(seek=0, action="pause")
        time.sleep(2)
        start(compare=True)
        # 비교 화면은 Canvas라 uiautomator 글자로 잡히지 않는다 → 3초 뒤 캡처(눈으로 확인)
        time.sleep(3)
        shot("04-compare")
        start(compare=False)
        return None

    def intake():
        # PT 2: "콩이가 축 처졌어" → 동물병원으로 → 사전 접수 → 접수함 실시간 도착 → 병원 응답이 차 화면·음성으로
        adb("logcat", "-c")
        start(seek=2400, action="play", speed=1, cabin=24)
        if not wait_for("멀미 지수", 20):
            return "막힘: 주행 화면이 안 뜸"
        start(say="콩이가 축 처졌어")
        if not wait_for("괜찮아졌어요", 15):
            return "막힘: 반려견 안내가 안 뜸"
        shot("05-emergency")
        xy = next((xy for t, xy in nodes() if t == "동물병원으로"), None)
        if not xy:
            return "막힘: 동물병원으로 버튼 없음"
        tap(xy)
        if not wait_for("사전 접수", 15):
            return "막힘: 사전 접수 표시 없음"
        shot("06-going-vet")
        url = None
        for _ in range(10):
            url = logcat_clinic()
            if url:
                break
            time.sleep(1)
        if not url:
            return "막힘: 접수함 링크 로그 없음"
        base = "http://127.0.0.1:8000"
        if a.phone:
            clinic_shot(base + url, "07-clinic-inbox")
        else:
            urllib.request.urlopen(base + url).read()
        urllib.request.urlopen(urllib.request.Request(base + url + "/reply?r=ready", method="POST")).read()
        if not wait_for("바로 봐요", 30):
            return "막힘: 병원 응답이 차 화면에 안 옴"
        shot("08-clinic-reply")
        if a.phone:
            clinic_shot(base + url, "08b-clinic-replied")
        return None

    def clinic_shot(url: str, name: str) -> None:
        try:
            from playwright.sync_api import sync_playwright
        except ImportError:
            # Playwright가 없으면 설치된 Chrome 헤드리스로 (390×844 폰 크기)
            chrome = Path(os.environ.get("PROGRAMFILES", r"C:\Program Files")) / "Google/Chrome/Application/chrome.exe"
            if chrome.exists():
                subprocess.run([str(chrome), "--headless=new", "--hide-scrollbars", "--force-device-scale-factor=2", "--window-size=480,900",
                                f"--screenshot={OUT / (name + '.png')}", url], capture_output=True, timeout=60)
            else:
                urllib.request.urlopen(url).read()
            return
        with sync_playwright() as p:
            b = p.chromium.launch(channel="chrome")
            pg = b.new_page(viewport={"width": 390, "height": 844}, device_scale_factor=2)
            pg.goto(url)
            pg.screenshot(path=str(OUT / f"{name}.png"), full_page=True)
            b.close()

    def calibrate():
        # PT 3: 쓸수록 맞춰짐 — 지난 여행 답 3개(시연용으로 미리 넣음) + 오늘 "네" → 4회째, 민감도 보통 → 조금 높음
        start(calib="yes,yes,little")
        start(seek=10843, action="play", speed=1)
        xy = wait_for("내렸어요", 100)
        if not xy:
            return "막힘: 도착 체크가 100초 안에 안 나옴"
        shot("09-arrival")
        tap(xy)
        if not wait_for("멀미했어요", 15):
            return "막힘: 멀미 질문이 안 뜸"
        shot("10-ask-motion")
        yes = next((xy for t, xy in nodes() if t == "네"), None)
        if yes:
            tap(yes)
        if not wait_for("맞춤 정도", 10):
            return "막힘: 맞춤 정도 줄이 안 뜸"
        time.sleep(2)
        shot("11-calibrated")
        start(calib="reset")
        return None

    def distress():
        # UPGRADE 7: 고속도로 중 "콩이가 헥헥거려" + 여름 실내 31℃(장면 값) → 1줄 안내 + 가장 가까운 정차 장소
        start(seek=2400, action="play", speed=1, cabin=31)
        if not wait_for("멀미 지수", 20):
            return "막힘: 주행 화면이 안 뜸"
        start(say="콩이가 헥헥거려")
        if not wait_for("괜찮아졌어요", 15):
            return "막힘: 반려견 안내가 안 뜸"
        shot("02b-distress")
        xy = next((xy for t, xy in nodes() if t == "괜찮아졌어요"), None)
        if xy:
            tap(xy)
        return None

    def suggest():
        start(seek=3420, action="play", speed=1)
        if not wait_for("여기서 쉴게요", 75):
            return "막힘: 쉼표 추천이 75초 안에 안 나옴"
        shot("03-suggest")
        start(say="응")
        wait_for("경로에", 10)
        shot("04-accepted")
        return None

    def rest():
        start(seek=5240, action="play", speed=1)
        if not wait_for("잠깐 다녀올게", 25):
            return "막힘: 쉼표 화면이 안 뜸"
        shot("05-rest")
        start(seek=5540, action="play", speed=1, say="잠깐 다녀올게")
        if not wait_for("주인이", 25):
            return "막힘: 대기 화면이 안 뜸"
        time.sleep(3)
        shot("06-wait")
        if a.phone:
            url = next((t.split("보호자 폰: ")[1].split(" ")[0] for t, _ in nodes() if "보호자 폰:" in t), None)
            if url:
                phone(url)
        return None

    def phone(url: str) -> None:
        try:
            from playwright.sync_api import sync_playwright
        except ImportError:
            return
        full = url if url.startswith("http") else "http://127.0.0.1:8000" + url
        full = full.replace("10.0.2.2", "127.0.0.1")
        with sync_playwright() as p:
            b = p.chromium.launch(channel="chrome")
            pg = b.new_page(viewport={"width": 390, "height": 844}, device_scale_factor=2)
            pg.goto(full)
            pg.screenshot(path=str(OUT / "06b-phone-wait.png"))
            b.close()

    def forecast():
        start(seek=8748, action="play", speed=1)
        if not wait_for("예상 최고", 40):
            return "막힘: 예보가 40초 안에 안 나옴"
        shot("07-forecast")
        start(demo=True)
        time.sleep(4)
        shot("08-compare-panel")
        start(demo=False)
        return None

    def walk():
        start(seek=9660, action="play", speed=1)
        # 실제 데이터에서는 목적지 3km 안 가장 가까운 동반 장소가 산책로가 아닐 수 있다(카페면 "쉬어 가기") → 앞부분으로 확인
        if not wait_for("곧 도착해요", 40):
            return "막힘: 도착 전 제안이 40초 안에 안 나옴"
        shot("09-arrival-walk")
        return None

    def arrival():
        start(seek=10843, action="play", speed=1)
        xy = wait_for("내렸어요", 100)
        if not xy:
            return "막힘: 도착 체크가 100초 안에 안 나옴"
        shot("10-arrival")
        tap(xy)
        if not wait_for("멀미 지수 추이", 15):
            return "막힘: 이동 기록이 안 뜸"
        time.sleep(3)
        shot("11-triplog")
        return None

    scenes = [("출발 체크", depart), ("고속도로", highway), ("PT1 고갯길 비교", compare), ("PT2 이상 → 병원 사전 접수", intake),
              ("PT3 쓸수록 맞춰짐", calibrate)]
    if a.all:
        scenes += [("(예비) 콩이가 헥헥거려", distress), ("(예비) 쉼표 추천", suggest), ("(예비) 휴게소·잠깐 대기", rest)]
    for name, fn in scenes:
        step(name, fn)
    total = time.time() - t0
    lines = ["| 장면 | 걸린 시간(초) | 결과 |", "| --- | --- | --- |"] + [f"| {n} | {d:.0f} | {r} |" for n, d, r in rows]
    lines.append(f"| **합계** | **{total:.0f}** | |")
    (OUT / "timing.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    return 0 if all(r == "OK" for _, _, r in rows) else 1


if __name__ == "__main__":
    sys.exit(main())
