"""녹화 원본 자동 촬영 (2026-11-03) — docs/recording.md 장면 순서대로 장면별 mp4.

    python scripts/record_scenes.py                 # 전체 (시작 전 확인: 기기·서버·기본 내비 안내)
    python scripts/record_scenes.py --only 1,7,8    # 일부 장면만 (번호)
    python scripts/record_scenes.py --no-wait       # 기본 내비가 안내 중이 아니어도 멈추지 않고 분할 창 장면을 전체 창으로 찍는다
    python scripts/record_scenes.py --share-base http://192.168.0.10:8000   # 보호자 폰 QR 주소 앞부분 (demo_check.ps1 -DetectIp)

- 장면 이동·입력은 scripts/rehearsal.py의 함수(start·wait_for·tap·nodes)를 그대로 쓴다.
- 차 화면: `adb shell screenrecord --size 1920x1080` → artifacts/recording/<날짜>/NN-<장면>.mp4 (소리 없음, 편집 없음).
- 병원 접수함(1280×800)·보호자 대기 페이지(390×844): Playwright가 있으면 webm 영상, 없으면 설치된 Chrome 헤드리스로
  1초 간격 PNG 묶음(편집에서 PIP로). Playwright는 server/.venv에 있다(11/03) — `server\.venv\Scripts\python scripts/record_scenes.py`.
- 장면 목록·길이·파일은 artifacts/recording/<날짜>/index.md.
"""
import argparse
import datetime as dt
import os
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import rehearsal as r  # noqa: E402

NAV_PKG = "ai.umos.maps.android.navigation.app"
CHROME = Path(os.environ.get("PROGRAMFILES", r"C:\Program Files")) / "Google/Chrome/Application/chrome.exe"
BASE = "http://127.0.0.1:8000"


def run(*args: str) -> str:
    p = subprocess.run(["adb", *args], capture_output=True, env=r.ENV)
    return p.stdout.decode("utf-8", "replace")


class Recorder:
    """장면 하나를 기기 안에서 screenrecord로 찍고 PC로 가져온다 (최대 180초)."""

    def __init__(self, out: Path):
        self.out = out
        self.proc: subprocess.Popen | None = None
        self.name = ""
        self.t0 = 0.0

    def start(self, name: str, limit: int = 170) -> None:
        self.name = name
        run("shell", "rm", "-f", f"/sdcard/rec_{name}.mp4")
        self.proc = subprocess.Popen(
            ["adb", "shell", "screenrecord", "--size", "1920x1080", "--bit-rate", "12000000", "--time-limit", str(limit), f"/sdcard/rec_{name}.mp4"],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=r.ENV,
        )
        self.t0 = time.time()
        time.sleep(1.5)  # 인코더가 첫 프레임을 잡을 때까지

    def stop(self) -> tuple[Path | None, float]:
        secs = time.time() - self.t0
        # SIGINT로 멈춰야 mp4 끝(moov)이 써진다
        run("shell", "pkill", "-INT", "screenrecord")
        if self.proc:
            try:
                self.proc.wait(timeout=15)
            except subprocess.TimeoutExpired:
                self.proc.kill()
        time.sleep(1.5)
        dst = self.out / f"{self.name}.mp4"
        run("pull", f"/sdcard/rec_{self.name}.mp4", str(dst))
        run("shell", "rm", "-f", f"/sdcard/rec_{self.name}.mp4")
        return (dst if dst.exists() and dst.stat().st_size > 0 else None), secs


def web_record(url: str, out: Path, name: str, size: tuple[int, int], seconds: int, action=None) -> str:
    """웹 페이지 녹화: Playwright(webm) → 없으면 Chrome 헤드리스 1초 간격 PNG. 돌려주는 값은 index에 적을 한 줄."""
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        sync_playwright = None
    if sync_playwright:
        vdir = out / f"{name}_video"
        vdir.mkdir(exist_ok=True)
        with sync_playwright() as p:
            b = p.chromium.launch(channel="chrome")
            ctx = b.new_context(viewport={"width": size[0], "height": size[1]}, record_video_dir=str(vdir), record_video_size={"width": size[0], "height": size[1]})
            pg = ctx.new_page()
            pg.goto(url)
            for i in range(seconds):
                if action and i == seconds // 2:
                    action()
                time.sleep(1)
            ctx.close()
            b.close()
        return f"{vdir.name}/ (Playwright webm)"
    if not CHROME.exists():
        return "건너뜀 — Playwright·Chrome 없음"
    fdir = out / f"{name}_frames"
    fdir.mkdir(exist_ok=True)
    # 헤드리스 Chrome은 창 폭을 약 500px 아래로 줄이지 않는다 → 폰 폭(390)은 같은 크기 iframe에 넣어 그 폭으로 배치시킨다
    wrap = fdir / "_frame.html"
    wrap.write_text(f"<html><body style='margin:0'><iframe src='{url}' style='border:0;width:{size[0]}px;height:{size[1]}px'></iframe></body></html>", encoding="utf-8")
    for i in range(seconds):
        if action and i == seconds // 2:
            action()
        subprocess.run([str(CHROME), "--headless=new", "--hide-scrollbars", f"--window-size={size[0]},{size[1]}", "--virtual-time-budget=2500",
                        f"--screenshot={fdir / f'{i:03d}.png'}", wrap.as_uri()], capture_output=True, timeout=30)
    wrap.unlink(missing_ok=True)
    n = len(list(fdir.glob("*.png")))
    return f"{fdir.name}/ PNG {n}장 (Playwright 없음 — Chrome 헤드리스 1초 간격)"


def nav_guiding() -> bool:
    """기본 내비가 안내 중인지: 내비를 앞으로 띄워 "안내 종료"가 보이는지 본다 (docs/recording.md "기본 내비 안내 걸기")."""
    run("shell", "monkey", "-p", NAV_PKG, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(4)
    ok = any("안내 종료" in t for t, _ in r.nodes())
    return ok


def split() -> None:
    run("shell", "monkey", "-p", NAV_PKG, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(3)
    r.start()
    time.sleep(2)
    run("shell", "input", "swipe", "2547", "720", "1700", "720", "800")
    time.sleep(6)


def unsplit() -> None:
    run("shell", "input", "swipe", "1705", "720", "2555", "720", "800")
    time.sleep(3)
    r.start()
    time.sleep(2)


def clinic_url() -> str | None:
    x = subprocess.run(["adb", "logcat", "-d", "-s", "Intake"], capture_output=True, env=r.ENV).stdout.decode("utf-8", "replace")
    m = re.findall(r"clinic=(\S+)", x)
    return m[-1] if m else None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", help="장면 번호 쉼표 목록 (예: 1,7,8)")
    ap.add_argument("--no-wait", action="store_true", help="기본 내비가 안내 중이 아니어도 멈추지 않는다(분할 창 장면은 전체 창으로)")
    ap.add_argument("--share-base", default="", help="보호자 폰 QR 주소 앞부분 (없으면 앱 설정 그대로)")
    a = ap.parse_args()
    out = r.ROOT / "artifacts" / "recording" / dt.date.today().isoformat()
    out.mkdir(parents=True, exist_ok=True)
    r.OUT = out  # rehearsal.shot()이 같은 폴더에 쓰게

    # ── 시작 전 확인 ──
    if "device" not in run("devices").split("List of devices attached")[-1]:
        print("멈춤: adb 기기 없음 — scripts\\demo_check.ps1 -Prepare")
        return 2
    try:
        urllib.request.urlopen(BASE + "/api/v1/health", timeout=3).read()
        server = True
    except Exception:
        server = False
        print("주의: 서버(:8000) 응답 없음 — 병원 사전 접수·보호자 페이지 장면은 화면만 찍힌다")
    guiding = nav_guiding()
    if not guiding:
        msg = ("기본 내비가 안내 중이 아니다. 분할 창 장면(2·3)은 다음 회전 카드가 실제 내비 값이라 안내가 걸려 있어야 한다.\n"
               "  1) 기본 내비 → \"어디로 갈까요?\" → 최근 목록 '강릉씨티호텔' 오른쪽 화살표(바로 안내)\n"
               "  2) 아래 카드에 \"안내 종료\"가 보이면 안내 중")
        if a.no_wait:
            print(msg + "\n  --no-wait: 분할 창 장면을 전체 창으로 찍는다")
        else:
            print(msg)
            input("안내를 걸었으면 Enter (그만두려면 Ctrl+C) ")
            guiding = nav_guiding()
            print("안내 중 확인" if guiding else "여전히 안내 중이 아님 — 분할 창 장면을 전체 창으로 찍는다")
    r.start(sim="pet-trip", onboard="done", scene="none", seek=0, action="pause", speed=1, demo=False, compare=False, emergency="none")
    if a.share_base:
        r.start(shareBase=a.share_base)
    time.sleep(3)

    rec = Recorder(out)
    rows: list[tuple[str, str, float, str]] = []
    want = {int(x) for x in a.only.split(",")} if a.only else None

    def scene(no: int, key: str, title: str, fn) -> None:
        if want and no not in want:
            return
        name = f"{no:02d}-{key}"
        print(f"[{no}] {title} …", flush=True)
        rec.start(name)
        note = ""
        try:
            note = fn() or ""
        except Exception as e:  # 한 장면이 막혀도 다음 장면으로
            note = f"오류: {e}"
        f, secs = rec.stop()
        rows.append((name, title, secs, note if f else "파일 없음" + (f" · {note}" if note else "")))
        print(f"    {secs:.0f}초 {f.name if f else '파일 없음'} {note}", flush=True)

    def s1():  # 출발 체크
        r.start(seek=0, action="pause")
        r.wait_for("출발 전 체크", 20)
        time.sleep(2)
        for label in ("하네스·켄넬", "물", "배변봉투"):
            xy = next((xy for t, xy in r.nodes() if t == label), None)
            if xy:
                r.tap(xy)
                time.sleep(1.2)
        time.sleep(2)
        xy = r.wait_for("이대로 출발", 5)
        if xy:
            r.tap(xy)
        time.sleep(3)

    def s2():  # 분할 창 고속도로
        r.start(seek=1800, action="play", speed=1)
        r.wait_for("다음 쉼표", 20)
        if guiding:
            split()
        time.sleep(10)
        return None if guiding else "전체 창(내비 안내 없음)"

    def s3():  # 고갯길 예보
        r.start(seek=8748, action="play", speed=1)
        ok = r.wait_for("덜 흔들려요", 40)
        time.sleep(8)
        if guiding:
            unsplit()
        return None if ok else "예보가 40초 안에 안 나옴"

    def s4():  # 정차 후 주행 자극 보기 → 고갯길 비교
        r.start(seek=5240, action="play", speed=1)
        r.wait_for("잠깐 다녀올게", 25)
        xy = r.wait_for("주행 자극 보기", 3)
        if not xy:
            # 쉼표 화면 왼쪽 버튼 줄 맨 아래(긴급 도움 밑)라 첫 화면 밖일 수 있다 → 한 번 위로 끌어 올린다
            run("shell", "input", "swipe", "1300", "1100", "1300", "500", "600")
            time.sleep(1.5)
            xy = r.wait_for("주행 자극 보기", 5)
        if xy:
            r.tap(xy)
            time.sleep(6)
            r.start(emergency="none")
        r.start(seek=0, action="pause")
        time.sleep(2)
        r.start(compare=True)
        time.sleep(10)
        r.start(compare=False)
        return None if xy else "주행 자극 보기 버튼 없음"

    def s5():  # 이상 신고 (주행 중 음성)
        subprocess.run(["adb", "logcat", "-c"], env=r.ENV)
        r.start(seek=2400, action="play", speed=1, cabin=24)
        r.wait_for("다음 쉼표", 20)
        time.sleep(2)
        r.start(say="콩이가 축 처졌어")
        ok = r.wait_for("동물병원으로", 15)
        time.sleep(4)
        return None if ok else "응급 안내가 안 뜸"

    def s6():  # 병원 사전 접수 + 접수함(브라우저)
        xy = next((xy for t, xy in r.nodes() if t == "동물병원으로"), None)
        if not xy:
            return "동물병원으로 버튼 없음(5번 장면 뒤에 찍는다)"
        r.tap(xy)
        for _ in range(8):
            time.sleep(2)
            r.start(say="네")
            if r.wait_for("사전 접수 보냄", 3) or r.wait_for("접수함을 열어", 1):
                break
        url = None
        for _ in range(10):
            url = clinic_url()
            if url:
                break
            time.sleep(1)
        if not (url and server):
            time.sleep(5)
            return "접수함 링크 없음(서버 확인)"
        full = BASE + url if url.startswith("/") else url.replace("10.0.2.2", "127.0.0.1")
        reply = lambda: urllib.request.urlopen(urllib.request.Request(full + "/reply?r=ready", method="POST")).read()  # noqa: E731
        web = web_record(full, out, "06b-clinic-inbox", (1280, 800), 12, action=reply)
        r.wait_for("바로 봐요", 20)
        time.sleep(3)
        return f"접수함 {web}"

    def s7():  # 휴게소 문 열림 → 잠깐 대기(새 화면) + 보호자 페이지
        r.start(seek=5240, action="play", speed=1)
        r.wait_for("잠깐 다녀올게", 25)
        time.sleep(14)  # 5252초 운전석 문 → 제안 한 줄
        r.start(seek=5540, action="play", speed=1, say="잠깐 다녀올게")
        ok = r.wait_for("주인이", 25)
        time.sleep(6)
        page = None
        xy = r.wait_for("설정", 5)
        if xy:
            r.tap(xy)
            time.sleep(2)
            page = next((t for t, _ in r.nodes() if "/wait/" in t), None)
            close = r.wait_for("설정 닫기", 3)
            if close:
                r.tap(close)
            time.sleep(3)
        web = ""
        if page and server:
            full = page if page.startswith("http") else BASE + page
            full = re.sub(r"http://[^/]+", BASE, full)
            web = " · 보호자 페이지 " + web_record(full, out, "07b-guardian-wait", (390, 844), 8)
        return (None if ok else "대기 화면이 안 뜸") if not web else ("대기 화면이 안 뜸" if not ok else web.strip(" ·"))

    def s8():  # 정차 중 긴급 도움
        r.start(emergency="none")
        xy = next((xy for t, xy in r.nodes() if t == "돌아왔어요"), None)
        if xy:
            r.tap(xy)
        r.start(seek=5240, action="play", speed=1)
        xy = r.wait_for("긴급 도움", 25)
        if not xy:
            return "긴급 도움 버튼 없음"
        time.sleep(2)
        r.tap(xy)
        xy = r.wait_for("위험 물질 섭취", 10)
        if xy:
            time.sleep(2)
            r.tap(xy)
        ok = r.wait_for("바로 동물병원에 연락해요", 10)
        time.sleep(6)
        r.start(emergency="none")
        return None if ok else "응급 카드가 안 뜸"

    def s9():  # 도착 뒷문 → 콩이 내렸나요?
        r.start(seek=10905, action="play", speed=1)
        ok = r.wait_for("내렸어요", 40)
        time.sleep(4)
        return None if ok else "도착 체크가 안 뜸"

    def s10():  # 리포트
        xy = r.wait_for("내렸어요", 10)
        if xy:
            time.sleep(10)  # 점프 따라잡기 끝난 뒤 (BLOCKED 11/02)
            r.tap(xy)
        ok = r.wait_for("오늘의 길", 15)
        time.sleep(6)
        d = r.wait_for("주행 자극 자세히 보기", 5)
        if d:
            r.tap(d)
            time.sleep(6)
        return None if ok else "리포트가 안 뜸"

    for no, key, title, fn in [
        (1, "depart", "출발 체크", s1),
        (2, "highway-split", "분할 창 고속도로", s2),
        (3, "forecast", "고갯길 예보", s3),
        (4, "stimulus-compare", "정차 후 주행 자극 보기 · 고갯길 비교", s4),
        (5, "distress", "이상 신고 (주행 중 음성)", s5),
        (6, "intake", "병원 사전 접수 (+ 접수함 브라우저)", s6),
        (7, "rest-door-wait", "휴게소 문 열림 · 잠깐 대기 (+ 보호자 페이지)", s7),
        (8, "emergency", "정차 중 긴급 도움", s8),
        (9, "arrival-door", "도착 뒷문 → 콩이 내렸나요?", s9),
        (10, "report", "도착 후 기록 리포트", s10),
    ]:
        scene(no, key, title, fn)

    lines = [f"# 녹화 원본 {dt.date.today().isoformat()}", "",
             f"- 기기 녹화: `adb shell screenrecord --size 1920x1080` (소리 없음, 편집 없음) · 기본 내비 안내: {'예' if guiding else '아니오(분할 창 장면은 전체 창)'} · 서버: {'예' if server else '아니오'}",
             "- 순서·자막은 docs/recording.md. 점프 대기 구간은 편집에서 잘라낸다.", "",
             "| 파일 | 장면 | 길이(초) | 비고 |", "| --- | --- | --- | --- |"]
    # --only로 일부만 다시 찍으면 기존 index의 다른 장면 줄은 그대로 둔다
    old: dict[str, str] = {}
    idx = out / "index.md"
    if idx.exists():
        for ln in idx.read_text(encoding="utf-8").splitlines():
            m = re.match(r"\| (\d\d-[^ ]+)\.mp4 \|", ln)
            if m:
                old[m[1]] = ln
    new = {n: f"| {n}.mp4 | {t} | {s:.0f} | {note} |" for n, t, s, note in rows}
    lines += [v for _, v in sorted({**old, **new}.items())]
    idx.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"\n{out / 'index.md'}")
    return 0 if all(not note.startswith("파일 없음") for *_, note in rows) else 1


if __name__ == "__main__":
    sys.exit(main())
