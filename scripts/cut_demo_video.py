"""시연 영상 편집본 만들기 (v1 2026-11-05, v2 2026-11-08).

    server\\.venv\\Scripts\\python scripts/cut_demo_video.py --version v2 [--rec artifacts/recording/2026-11-05]
    server\\.venv\\Scripts\\python scripts/cut_demo_video.py --version v2 --split-all   # tts/all.mp3 → 장면별 mp3
    server\\.venv\\Scripts\\python scripts/cut_demo_video.py --version v1

v2 (AI 음성 + 문장 자막 + 앱 확대):
- 재료: artifacts/video/tts/01.mp3 ~ 11_close.mp3 + timing.json(문장 시각). 음성을 한 파일(all.mp3, all.txt 순서)로
  받았으면 --split-all 이 무음 구간 + 글자 수 비례 정렬(동적 계획법)로 문장을 찾아 장면별로 자른다.
- 장면 길이 = 음성 + 앞뒤 0.4초. 앱은 왼쪽 차량 패널·시스템 바를 잘라 1.3~1.5배로 키우고 장면마다 말하는 카드 쪽에 맞춘다
  (V2_SCENES의 view). 폰 장면(5·7·10)은 차 화면 왼쪽 + 폰 오른쪽.
- 결과: gatigage_demo_v2.mp4 · .srt · cut_list_v2.md · youtube_description.txt (artifacts/video/, 저장소 밖).

v1:

- 재료: record_scenes.py가 찍은 장면 mp4 10개 + 폰 Playwright webm 3개(장면 5·7·10).
- 결과(모두 artifacts/video/, 저장소 밖): gatigage_demo_v1.mp4(자막), gatigage_demo_v1_nosub.mp4,
  gatigage_demo_v1.srt, cut_list.md, narration_script.md.
- ffmpeg는 server/.venv의 imageio-ffmpeg 바이너리를 쓴다(전역 설치 없음).
- 자른 구간은 아래 SCENES 표(초, 원본 기준). 원본을 다시 찍으면 구간을 다시 고른다.
- 폰 장면(5·7·10): 차 화면을 1440×810으로 줄여 왼쪽, 폰(390×844 영상)을 오른쪽에 세로로 — 차 화면 버튼을 가리지 않게.
  폰은 영상 높이 안에 들어가야 해서 폭이 425px(화면 폭 22%)다.
"""
import argparse
import re
import shutil
import subprocess
import sys
from pathlib import Path

import imageio_ffmpeg

ROOT = Path(__file__).resolve().parents[1]
FF = imageio_ffmpeg.get_ffmpeg_exe()
FONT_DIR = ROOT / "app-android" / "app" / "src" / "main" / "res" / "font"
W, H, FPS, XF = 1920, 1080, 30, 0.3

TITLE = "같이가개 — 반려견과 차로 가는 모든 순간"
DISCLAIMER = "주행 자극은 차량 흔들림 기준 · 반려견 측정 아님"
GITHUB = "github.com/xuxtaku7610-del/every-moment-of-pause"

# no, 원본, 전체 화면 구간, 폰 겹침 구간, 폰 webm 폴더, 자막(줄 목록), 내레이션
SCENES = [
    (1, "01-depart.mp4", [(4, 19)], [], None,
     ["타기 전, 오늘 탈 아이와 준비물부터"],
     "같이가개는 차에 타기 전부터 챙깁니다. 오늘 같이 타는 아이를 고르고, 하네스와 켄넬, 물, 배변봉투 세 가지를 확인하면 바로 출발할 수 있어요."),
    (2, "02-nav-forecast.mp4", [(9, 26), (33, 35.3)], [], None,
     ["길 안내는 기본 내비, 콩이는 뒤에서", DISCLAIMER],
     "길 안내는 차의 기본 내비가 맡습니다. 같이가개는 뒤에서 조용히 앞길의 굽은 길을 미리 살펴, 차가 얼마나 흔들릴지 계산해요. 그리고 굽은 길 앞에서만 음성과 알림 한 줄로 천천히 가자고 알려 줍니다."),
    (3, "03-split-talk.mp4", [(12, 32)], [], None,
     ["돌아보지 말고, 눌러서 말로 알려요"],
     "운전 중에는 뒤를 돌아보지 마세요. 화면 아래 버튼을 한 번 누르고 콩이가 헥헥거린다고 말하면 됩니다. 같이가개가 실내 온도를 같이 보고, 에어컨을 낮추라고 하면서 가장 가까운 쉴 곳을 찾아 줘요."),
    (4, "04-potty-suggest.mp4", [(2, 22)], [], None,
     ["배변 시간에 맞춰 놀이터 휴게소로"],
     "지난 배변 시각과 지금까지 운전한 시간을 보고, 배변 시간이 다가오면 반려견 놀이터가 있는 휴게소를 먼저 권합니다. 운전자는 응, 한마디만 하면 되고, 그 휴게소가 바로 경유지로 들어가요."),
    (5, "05-rest-door-wait.mp4", [(6, 14)], [(27, 38), (45, 51)], "05b-guardian-wait_video",
     ["문을 열면 잠깐 대기를 권해요", "보호자 폰엔 경과 시간과 실내 온도"],
     "휴게소에서 운전석 문이 열리면 잠깐 대기 모드를 권합니다. 대기 중에는 차 화면이 바깥 사람에게 보이는 큰 안내판이 되고, 보호자 폰에는 지난 시간과 배터리, 실내 온도가 함께 떠요. 위치는 보내지 않고, 폰에서 돌아가는 중을 누르면 차 화면에도 보여요."),
    (6, "06-helper-rest.mp4", [(8, 18)], [], None,
     ["멈춰 있을 땐 콩이 도우미에게"],
     "멈춰 있을 땐 콩이 도우미에게 물어보세요. 쉬할 데 있냐고 물으면 놀이터 휴게소를 찾아 경유지로 넣어 줘요."),
    (7, "07-helper-emergency-intake.mp4", [(7, 16)], [(18, 24), (32, 42)], "07b-clinic-inbox_video",
     ["위급하면 병원이 먼저 알게", "시연용 제휴 병원 사전 접수"],
     "주행 중에 콩이가 초콜릿을 먹은 것 같다고 말하면, 다른 답보다 응급 안내가 먼저 나옵니다. 가까운 동물병원으로 길을 바꾸고, 네라고 하면 시연용 제휴 병원에 증상과 도착 예정 시각을 미리 보내요. 병원이 확인한 답은 다시 차 화면으로 돌아옵니다."),
    (8, "08-stimulus-compare.mp4", [(15, 23), (27, 35.8)], [], None,
     ["안내대로 가면 흔들림이 줄어요", DISCLAIMER],
     "차를 세우면 오늘 길의 흔들림 그래프를 볼 수 있어요. 대관령 옛길로 비교해 보면, 안내대로 천천히 갈 때 7분쯤 더 걸리지만 가장 큰 흔들림은 55에서 30으로 줄어요."),
    (9, "09-nearby.mp4", [(31, 33), (34, 41.8)], [], None,
     ["목적지 앞, 같이 들를 곳 하나"],
     "목적지가 가까워지면 일반 도로에서만, 반려견과 같이 들를 만한 곳을 하나 골라 권합니다."),
    (10, "10-arrival-summary.mp4", [(8, 13)], [(29, 40)], "10b-phone-report_video",
     ["뒷문이 열리면, 내렸는지 한 번 더", "오늘 기록은 QR로 폰에"],
     "도착해서 뒷문이 열리면 콩이가 내렸는지 한 번 더 물어봅니다. 오늘 이동 기록은 도착 요약의 QR로 보호자 폰에 받아 가고, 하루가 지나면 사라져요."),
]
CLOSING_SEC = 5.0
CLOSING_NARRATION = "같이가개. 반려견과 차로 가는 모든 순간, 끝까지 같이."


def ff(*args: str, cwd: Path | None = None) -> None:
    r = subprocess.run([FF, "-hide_banner", "-loglevel", "error", "-y", *args], cwd=cwd)
    if r.returncode:
        sys.exit(f"ffmpeg 실패: {' '.join(args[:6])} …")


def duration(p: Path) -> float:
    out = subprocess.run([FF, "-hide_banner", "-i", str(p)], capture_output=True, text=True, errors="replace").stderr
    h, m, s = re.search(r"Duration: (\d+):(\d+):([\d.]+)", out).groups()
    return int(h) * 3600 + int(m) * 60 + float(s)


def part_chain(idx: str, parts, tag: str) -> tuple[str, str]:
    """원본 구간들을 잘라 이어 붙이는 필터 조각."""
    f, labels = [], []
    for k, (a, b) in enumerate(parts):
        lab = f"{tag}{k}"
        f.append(f"[{idx}:v]trim={a}:{b},setpts=PTS-STARTPTS,fps={FPS},scale={W}:{H},setsar=1,format=yuv420p[{lab}]")
        labels.append(f"[{lab}]")
    out = f"{tag}c"
    f.append(f"{''.join(labels)}concat=n={len(labels)}:v=1:a=0[{out}]")
    return ";".join(f), out


def build_scene(rec: Path, work: Path, sc) -> Path:
    no, src, full, pip, web, *_ = sc
    out = work / f"s{no:02d}.mp4"
    args = ["-i", str(rec / src)]
    graph = []
    g, full_out = part_chain("0", full, "f")
    graph.append(g)
    final = full_out
    if pip:
        webm = next((rec / web).glob("*.webm"))
        args += ["-i", str(webm)]
        g, car = part_chain("0", pip, "p")
        graph.append(g)
        pip_len = sum(b - a for a, b in pip)
        graph.append(f"color=c=0xEEF3FB:s={W}x{H}:r={FPS}:d={pip_len}[bg]")
        graph.append(f"[{car}]scale=1440:810,setsar=1[cars]")
        graph.append(f"[bg][cars]overlay=20:81:shortest=1[t]")
        graph.append(f"[1:v]fps={FPS},scale=421:916,setsar=1,pad=425:920:2:2:color=0xC9D3E1,"
                     f"tpad=stop_mode=clone:stop_duration=60[ph]")
        graph.append(f"[t][ph]overlay=1475:26:shortest=1,format=yuv420p[pp]")
        graph.append(f"[{full_out}][pp]concat=n=2:v=1:a=0[all]")
        final = "all"
    args += ["-filter_complex", ";".join(graph), "-map", f"[{final}]", "-an",
             "-c:v", "libx264", "-preset", "veryfast", "-crf", "14", "-r", str(FPS), "-pix_fmt", "yuv420p", str(out)]
    ff(*args)
    return out


def build_closing(work: Path, last: Path) -> Path:
    """마지막 장면 끝 화면을 흐리게 깔고 앱 이름·차량용 앱·GitHub 주소."""
    still = work / "last.png"
    ff("-sseof", "-0.2", "-i", str(last), "-frames:v", "1", str(still))
    (work / "end1.txt").write_text("같이가개", encoding="utf-8")
    (work / "end2.txt").write_text("Pleos Connect 차량용 앱", encoding="utf-8")
    (work / "end3.txt").write_text(GITHUB, encoding="utf-8")
    (work / "end4.txt").write_text(DISCLAIMER + " · © OpenStreetMap contributors · 한국관광공사", encoding="utf-8")
    vf = ("boxblur=12:2,eq=brightness=0.06,drawbox=x=0:y=0:w=iw:h=ih:color=0xEEF3FB@0.78:t=fill,"
          "drawtext=fontfile=fb.otf:textfile=end1.txt:fontsize=150:fontcolor=0x182230:x=(w-tw)/2:y=330,"
          "drawtext=fontfile=fs.otf:textfile=end2.txt:fontsize=52:fontcolor=0x3478E5:x=(w-tw)/2:y=530,"
          "drawtext=fontfile=fr.otf:textfile=end3.txt:fontsize=40:fontcolor=0x2B3545:x=(w-tw)/2:y=620,"
          "drawtext=fontfile=fr.otf:textfile=end4.txt:fontsize=26:fontcolor=0x5F6B7B:x=(w-tw)/2:y=980,format=yuv420p")
    out = work / "s11.mp4"
    ff("-loop", "1", "-t", str(CLOSING_SEC), "-i", "last.png", "-vf", vf, "-r", str(FPS),
       "-c:v", "libx264", "-preset", "veryfast", "-crf", "14", "-pix_fmt", "yuv420p", out.name, cwd=work)
    return out


def srt_time(t: float) -> str:
    ms = int(round(t * 1000))
    return f"{ms // 3600000:02d}:{ms // 60000 % 60:02d}:{ms // 1000 % 60:02d},{ms % 1000:03d}"


def main_v1(rec: Path) -> int:
    out = ROOT / "artifacts" / "video"
    work = out / "_work"
    work.mkdir(parents=True, exist_ok=True)
    for name, f in (("fb.otf", "pretendard_extrabold.otf"), ("fs.otf", "pretendard_semibold.otf"), ("fr.otf", "pretendard_regular.otf")):
        shutil.copy(FONT_DIR / f, work / name)

    segs = []
    for sc in SCENES:
        print(f"장면 {sc[0]} …", flush=True)
        segs.append(build_scene(rec, work, sc))
    segs.append(build_closing(work, segs[-1]))
    lens = [duration(p) for p in segs]

    # 장면 시작 시각(크로스페이드 0.3초 겹침)
    starts, t = [], 0.0
    for i, d in enumerate(lens):
        starts.append(t)
        t += d - (XF if i < len(lens) - 1 else 0)
    total = t

    # 1) 이어 붙이기(자막 없음) — 고화질 중간본
    args = []
    for p in segs:
        args += ["-i", str(p)]
    g, prev = [], "0:v"
    for i in range(1, len(segs)):
        lab = f"x{i}"
        g.append(f"[{prev}][{i}:v]xfade=transition=fade:duration={XF}:offset={starts[i]:.3f}[{lab}]")
        prev = lab
    master = work / "master.mp4"
    ff(*args, "-filter_complex", ";".join(g), "-map", f"[{prev}]", "-c:v", "libx264", "-preset", "veryfast",
       "-crf", "12", "-r", str(FPS), "-pix_fmt", "yuv420p", str(master))

    # 2) 자막 신호(첫 3초는 제목)
    cues: list[tuple[float, float, list[str]]] = [(0.0, 3.0, [TITLE])]
    for i, sc in enumerate(SCENES):
        s = 3.0 if i == 0 else starts[i] + XF / 2
        e = starts[i + 1] + XF / 2
        cues.append((s, e - 0.05, sc[5]))
    srt = []
    filt = []
    for n, (s, e, lines) in enumerate(cues, 1):
        srt += [str(n), f"{srt_time(s)} --> {srt_time(e)}", *lines, ""]
        two = len(lines) > 1
        band_h = 112 if two else 104  # 앱 화면 아래 끝(y≈975) 밑 시스템 바 위에만 — 앱 버튼을 가리지 않게
        en = f"enable='between(t,{s:.3f},{e:.3f})'"
        filt.append(f"drawbox=x=0:y={H - band_h}:w={W}:h={band_h}:color=black@0.55:t=fill:{en}")
        for k, line in enumerate(lines):
            tf = f"sub{n:02d}_{k}.txt"
            (work / tf).write_text(line, encoding="utf-8")
            small = line == DISCLAIMER
            size = 30 if small else (40 if two else 46)
            y = (H - band_h + 9) if k == 0 else (H - band_h + (64 if small else 60))
            if not two:
                y = H - band_h + 26
            color = "0xDDE6F3" if small else "white"
            filt.append(f"drawtext=fontfile=fs.otf:textfile={tf}:fontsize={size}:fontcolor={color}:x=(w-tw)/2:y={y}:{en}")
    (out / "gatigage_demo_v1.srt").write_text("\n".join(srt), encoding="utf-8")
    (work / "subs.txt").write_text(",\n".join(filt), encoding="utf-8")

    enc = ["-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-r", str(FPS), "-pix_fmt", "yuv420p",
           "-c:a", "aac", "-b:a", "128k", "-shortest", "-movflags", "+faststart"]
    silent = ["-f", "lavfi", "-i", "anullsrc=r=48000:cl=stereo"]
    ff("-i", "master.mp4", *silent, "-map", "0:v", "-map", "1:a", *enc, str(out / "gatigage_demo_v1_nosub.mp4"), cwd=work)
    # 자막 필터는 파일에서 읽는다(-/vf, 한국어는 textfile로)
    ff("-i", "master.mp4", *silent, "-/vf", "subs.txt", "-map", "0:v", "-map", "1:a", *enc, str(out / "gatigage_demo_v1.mp4"), cwd=work)

    # 3) cut_list.md · narration_script.md
    rows = ["# 3분 편집본 자른 목록 (gatigage_demo_v1)", "",
            f"- 원본: `{rec.relative_to(ROOT).as_posix()}/` · 만든 스크립트: `scripts/cut_demo_video.py` · 전환 0.3초 크로스페이드 · 30fps 1920×1080",
            f"- 총 길이 **{total:.1f}초** · 첫 3초 제목 자막 · 마지막 {CLOSING_SEC:.0f}초 닫는 화면(앱 이름·Pleos Connect 차량용 앱·GitHub)", "",
            "| # | 원본 | 잘라 쓴 구간(초) | 폰 겹침 | 영상 안 시작 | 끝 | 길이 |", "| --- | --- | --- | --- | --- | --- | --- |"]
    for i, sc in enumerate(SCENES):
        no, src, full, pip, web, *_ = sc
        cut = " + ".join(f"{a:g}–{b:g}" for a, b in full + pip)
        ph = f"{web}/ (구간 {' + '.join(f'{a:g}–{b:g}' for a, b in pip)} 동안)" if pip else "—"
        rows.append(f"| {no} | {src} | {cut} | {ph} | {starts[i]:.1f} | {starts[i] + lens[i]:.1f} | {lens[i]:.1f} |")
    rows.append(f"| 닫음 | 10번 끝 화면 정지 | — | — | {starts[-1]:.1f} | {total:.1f} | {lens[-1]:.1f} |")
    (out / "cut_list.md").write_text("\n".join(rows) + "\n", encoding="utf-8")

    nar = ["# 내레이션 대본 (gatigage_demo_v1)", "",
           "- 장면 시작 시각에 맞춰 읽는다. 글자 수는 공백 뺀 수, 초당 4~5글자 안쪽이면 장면 안에 끝난다.",
           "- 녹음은 무음 영상 위에 얹는다(편집기에서 이 시각에 맞춰 놓기).", "",
           "| # | 시작(초) | 길이(초) | 글자 | 초당 | 읽을 문장 |", "| --- | --- | --- | --- | --- | --- |"]
    for i, sc in enumerate(SCENES):
        text = sc[6]
        chars = len(text.replace(" ", ""))
        nar.append(f"| {sc[0]} | {starts[i]:.1f} | {lens[i]:.1f} | {chars} | {chars / lens[i]:.1f} | {text} |")
    chars = len(CLOSING_NARRATION.replace(" ", ""))
    nar.append(f"| 닫음 | {starts[-1]:.1f} | {lens[-1]:.1f} | {chars} | {chars / lens[-1]:.1f} | {CLOSING_NARRATION} |")
    (out / "narration_script.md").write_text("\n".join(nar) + "\n", encoding="utf-8")

    print(f"총 {total:.1f}초")
    for i, d in enumerate(lens):
        print(f"  {i + 1:2d} 시작 {starts[i]:6.1f} 길이 {d:5.1f}")
    return 0


# ─────────────────────────────── v2 ───────────────────────────────
VIDEO = ROOT / "artifacts" / "video"
TTS = VIDEO / "tts"
BG = "0xF4F6F6"            # 앱 배경색(녹화에서 잰 값) — 확대 후 남는 곳을 채운다
CHIP_BLUE = "0x3478E5"
SUB_SIZE, CHIP_SIZE, NOTE_SIZE = 54, 32, 28
LEAD = 0.4                 # 장면 시작 → 음성 시작, 음성 끝 → 장면 끝
CLOSE_MIN = 5.0
TTS_NAMES = ["01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11_close"]
TTS_COUNTS = [4, 5, 6, 5, 8, 3, 6, 5, 3, 4, 3]   # all.txt 장면별 문장 수
GITHUB_URL = "https://" + GITHUB
RELEASE_URL = GITHUB_URL + "/releases/tag/v1.0.0"
TYPECAST_CHARACTER = "○○○"
NOTE_V2 = "차량 흔들림 기준 · 반려견 측정 아님"
APP_X, APP_W, APP_TOP, APP_BOTTOM = 640, 1280, 68, 975   # 녹화 안 앱 영역(왼쪽 차량 패널·상태 바·시스템 바 제외)
CAR_W = 1430               # 폰 장면의 차 화면 폭(1280 → 1.117배)
SUB_DEFAULT = (960, 1000)  # 자막 상자 가운데 x, 아래 끝 y(화면 아래에서 80px 위)


def app(y0: int, y1: int, s: float = 1.3, sub=SUB_DEFAULT):
    """앱 영역의 세로 y0~y1(녹화 좌표)만 s배로 키워 위쪽에 붙이고 남는 곳은 앱 배경색."""
    assert APP_TOP <= y0 < y1 <= APP_BOTTOM and APP_W * s <= W + 0.5 and (y1 - y0) * s <= H + 0.5, (y0, y1, s)
    return {"kind": "app", "y0": y0, "y1": y1, "s": s, "sub": sub}


def pip(y1: int, sub=(20 + CAR_W // 2, 1000)):
    """폰 겹치기: 차 앱 화면(위 ~ y1)을 왼쪽, 폰 영상을 오른쪽."""
    return {"kind": "pip", "y0": APP_TOP, "y1": y1, "sub": sub}


# 장면: 제목 칩, 원본, [(시작, 끝, 화면)], 폰 webm 폴더, 끝 정지(초). 구간 길이 합 + 정지 = 음성 + 0.8초.
# 화면 좌표는 녹화(1920×1080) 기준. 확인한 위치는 cut_list_v2.md 참고.
V2_SCENES = [
    {"title": "① 출발 전 체크", "src": "01-depart.mp4",
     "parts": [(8.5, 19.64, app(135, 862))]},
    {"title": "② 굽은 길 미리 알림", "src": "02-nav-forecast.mp4",
     "parts": [(9.0, 19.9, app(68, 788, 1.5)), (33.4, 35.3, app(68, 788, 1.5))], "hold": 4.47},
    {"title": "③ 말로 알리기", "src": "03-split-talk.mp4",
     # 22.9~27.4는 분할 창을 끌어 좌우가 오가는 중 → 건너뜀. 카드는 왼쪽 칸이라 자막은 오른쪽 내비 칸 위(안내 상자 위).
     "parts": [(11.5, 22.8, app(68, 788, 1.5, sub=(1440, 960))), (27.4, 34.27, app(68, 788, 1.5, sub=(1440, 960)))]},
    {"title": "④ 배변 시간 쉼표", "src": "04-potty-suggest.mp4",
     "parts": [(4.5, 19.82, app(135, 845, 1.5, sub=(960, 915)))]},
    {"title": "⑤ 휴게소 대기", "src": "05-rest-door-wait.mp4", "web": "05b-guardian-wait_video",
     "parts": [(6.0, 12.0, app(140, 805)), (27.0, 37.0, pip(850)), (45.0, 51.72, pip(850))]},
    {"title": "⑥ 콩이 도우미", "src": "06-helper-rest.mp4",
     "parts": [(8.6, 17.81, app(140, 922, sub=(1150, 990)))]},
    {"title": "⑦ 응급 · 사전 접수", "src": "07-helper-emergency-intake.mp4", "web": "07b-clinic-inbox_video",
     "parts": [(7.6, 15.5, app(135, 800)), (17.0, 23.0, pip(845)), (34.0, 40.15, pip(845))]},
    # 안내 글은 위쪽, 비교 화면 제목(왼쪽)과 닫기(오른쪽) 사이 x≈1200.
    {"title": "⑧ 차량 흔들림 비교", "src": "08-stimulus-compare.mp4", "note": NOTE_V2, "note_x": 1200,
     "parts": [(17.3, 24.9, app(140, 845)), (27.4, 35.84, app(105, 860))], "hold": 0.7},
    {"title": "⑨ 목적지 앞 추천", "src": "09-nearby.mp4",
     "parts": [(32.6, 33.4, app(68, 788, 1.5)), (35.3, 43.53, app(68, 788, 1.5))]},
    {"title": "⑩ 도착 · 하차 확인", "src": "10-arrival-summary.mp4", "web": "10b-phone-report_video",
     "parts": [(8.5, 13.0, app(135, 855, 1.5)), (29.3, 36.2, pip(850))]},
]


# ── 음성 한 파일 → 장면별 mp3 ──
def _rms_db(mp3: Path) -> list[float]:
    import array
    import math
    raw = subprocess.run([FF, "-v", "error", "-i", str(mp3), "-ac", "1", "-ar", "16000", "-f", "s16le", "-"],
                         capture_output=True, check=True).stdout
    x = array.array("h", raw[: len(raw) // 2 * 2])
    hop = 160  # 10ms
    return [10 * math.log10(sum(v * v for v in x[k * hop:(k + 1) * hop]) / hop / 32768 ** 2 + 1e-12)
            for k in range(len(x) // hop)]


def align_sentences(mp3: Path, lines: list[str], th: float = -40.0) -> list[tuple[float, float]]:
    """무음 구간(-40dB 이하 0.1초+) 중 문장 경계를 고른다: 문장 길이가 글자 수 비례에 가깝고 경계 무음이 길수록 좋게."""
    db = _rms_db(mp3)
    n = len(db)
    runs, i = [], 0
    while i < n:
        if db[i] < th:
            j = i
            while j < n and db[j] < th:
                j += 1
            runs.append((i / 100, j / 100))
            i = j
        else:
            i += 1
    total = n / 100
    start = runs[0][1] if runs and runs[0][0] <= 0.02 else 0.0
    end = runs[-1][0] if runs and runs[-1][1] >= total - 0.02 else total
    pts = [(start, start)] + [r for r in runs if r[1] - r[0] >= 0.1 and start < r[0] and r[1] < end] + [(end, end)]
    ch = [len(s.replace(" ", "")) for s in lines]
    L, P, INF = len(lines), len(pts), float("inf")

    def solve(rate: float):
        dp = [[INF] * P for _ in range(L + 1)]
        bk = [[-1] * P for _ in range(L + 1)]
        dp[0][0] = 0.0
        for k in range(1, L + 1):
            exp = rate * ch[k - 1] + 0.15
            for b in range(1, P):
                if (k == L) != (b == P - 1):
                    continue
                gap = 0.0 if b == P - 1 else pts[b][1] - pts[b][0]
                for a in range(max(0, b - 25), b):
                    if dp[k - 1][a] == INF:
                        continue
                    d = pts[b][0] - pts[a][1]
                    if d <= 0.3:
                        continue
                    c = dp[k - 1][a] + ((d - exp) / exp) ** 2 - 3.0 * gap
                    if c < dp[k][b]:
                        dp[k][b], bk[k][b] = c, a
        if dp[L][P - 1] == INF:
            sys.exit("문장 정렬 실패: 무음 구간이 문장 수보다 적다")
        b, seq = P - 1, []
        for k in range(L, 0, -1):
            a = bk[k][b]
            seq.append((pts[a][1], pts[b][0]))
            b = a
        return seq[::-1]

    rate = (end - start - 0.45 * (L - 1)) / sum(ch)
    for _ in range(4):
        seq = solve(rate)
        rate = sum(b - a for a, b in seq) / sum(ch)
    return seq


def split_all() -> None:
    import json
    lines = [s for s in (TTS / "all.txt").read_text(encoding="utf-8").splitlines() if s.strip()]
    if len(lines) != sum(TTS_COUNTS):
        sys.exit(f"all.txt 문장 {len(lines)}개 ≠ 장면별 합 {sum(TTS_COUNTS)}")
    seq = align_sentences(TTS / "all.mp3", lines)
    timing, i = {}, 0
    for name, cnt in zip(TTS_NAMES, TTS_COUNTS):
        sents = list(zip(seq[i:i + cnt], lines[i:i + cnt]))
        i += cnt
        a, b = max(0.0, sents[0][0][0] - 0.08), sents[-1][0][1] + 0.15
        ff("-i", str(TTS / "all.mp3"), "-ss", f"{a:.3f}", "-to", f"{b:.3f}", "-c:a", "libmp3lame", "-b:a", "128k",
           str(TTS / f"{name}.mp3"))
        timing[name] = {"src": [round(a, 3), round(b, 3)],
                        "sentences": [{"text": t, "start": round(s - a, 3), "end": round(e - a, 3)} for (s, e), t in sents]}
        print(f"{name}: {b - a:5.2f}초 · {cnt}문장")
    (TTS / "timing.json").write_text(json.dumps(timing, ensure_ascii=False, indent=1), encoding="utf-8")


# ── 장면 만들기 ──
def _app_chain(src_lab: str, a: float, b: float, v: dict, out: str) -> str:
    h_src = v["y1"] - v["y0"]
    w2, h2 = round(APP_W * v["s"] / 2) * 2, round(h_src * v["s"] / 2) * 2
    return (f"[{src_lab}]trim={a}:{b},setpts=PTS-STARTPTS,crop={APP_W}:{h_src}:{APP_X}:{v['y0']},"
            f"scale={w2}:{h2}:flags=lanczos,setsar=1,pad={W}:{H}:{(W - w2) // 2}:0:color={BG},format=yuv420p[{out}]")


def build_scene_v2(rec: Path, work: Path, idx: int, sc: dict) -> Path:
    out = work / f"v2_s{idx + 1:02d}.mp4"
    parts = sc["parts"]
    kinds = [p[2]["kind"] for p in parts]
    assert kinds == sorted(kinds, key=lambda k: k == "pip"), "폰 구간은 장면 끝에 모아 둔다"
    args = ["-i", str(rec / sc["src"])]
    g = [f"[0:v]fps={FPS},split={len(parts)}" + "".join(f"[r{k}]" for k in range(len(parts)))]
    seg_labels, pip_labels = [], []
    for k, (a, b, v) in enumerate(parts):
        if v["kind"] == "app":
            g.append(_app_chain(f"r{k}", a, b, v, f"a{k}"))
            seg_labels.append(f"[a{k}]")
        else:
            h_src = v["y1"] - v["y0"]
            g.append(f"[r{k}]trim={a}:{b},setpts=PTS-STARTPTS,crop={APP_W}:{h_src}:{APP_X}:{v['y0']},"
                     f"scale={CAR_W}:{round(h_src * CAR_W / APP_W / 2) * 2}:flags=lanczos,setsar=1[c{k}]")
            pip_labels.append(f"[c{k}]")
    if pip_labels:
        webm = next((rec / sc["web"]).glob("*.webm"))
        args += ["-i", str(webm)]
        pip_len = sum(b - a for a, b, v in parts if v["kind"] == "pip")
        g.append(f"{''.join(pip_labels)}concat=n={len(pip_labels)}:v=1:a=0[cars]")
        g.append(f"color=c={BG}:s={W}x{H}:r={FPS}:d={pip_len:.3f}[pbg]")
        g.append("[pbg][cars]overlay=20:40:shortest=1[pt]")
        g.append(f"[1:v]fps={FPS},scale=421:916:flags=lanczos,setsar=1,pad=425:920:2:2:color=0xC9D3E1,"
                 f"tpad=stop_mode=clone:stop_duration=60[ph]")
        g.append("[pt][ph]overlay=1475:80:shortest=1,format=yuv420p[pp]")
        seg_labels.append("[pp]")
    hold = sc.get("hold", 0.0)
    tail = f",tpad=stop_mode=clone:stop_duration={hold}" if hold else ""
    g.append(f"{''.join(seg_labels)}concat=n={len(seg_labels)}:v=1:a=0{tail}[vout]")
    ff(*args, "-filter_complex", ";".join(g), "-map", "[vout]", "-an", "-c:v", "libx264", "-preset", "veryfast",
       "-crf", "14", "-r", str(FPS), "-pix_fmt", "yuv420p", str(out))
    return out


def build_closing_v2(work: Path, last: Path, length: float) -> Path:
    still = work / "v2_last.png"
    ff("-sseof", "-0.2", "-i", str(last), "-frames:v", "1", str(still))
    texts = {"e1": "같이가개", "e2": "Pleos Connect 차량용 앱", "e3": GITHUB,
             "e4": "음성: Typecast AI 가상 연기자", "e5": "© OpenStreetMap contributors · 한국관광공사"}
    for k, t in texts.items():
        (work / f"v2_{k}.txt").write_text(t, encoding="utf-8")
    vf = ("boxblur=12:2,eq=brightness=0.06,drawbox=x=0:y=0:w=iw:h=ih:color=0xEEF3FB@0.82:t=fill,"
          "drawtext=fontfile=fx.otf:textfile=v2_e1.txt:fontsize=150:fontcolor=0x182230:x=(w-tw)/2:y=250,"
          "drawtext=fontfile=fs.otf:textfile=v2_e2.txt:fontsize=52:fontcolor=0x3478E5:x=(w-tw)/2:y=450,"
          "drawtext=fontfile=fr.otf:textfile=v2_e3.txt:fontsize=40:fontcolor=0x2B3545:x=(w-tw)/2:y=540,"
          "drawtext=fontfile=fr.otf:textfile=v2_e4.txt:fontsize=30:fontcolor=0x5F6B7B:x=(w-tw)/2:y=680,"
          "drawtext=fontfile=fr.otf:textfile=v2_e5.txt:fontsize=30:fontcolor=0x5F6B7B:x=(w-tw)/2:y=726,format=yuv420p")
    out = work / "v2_s11.mp4"
    ff("-loop", "1", "-t", f"{length:.3f}", "-i", still.name, "-vf", vf, "-r", str(FPS),
       "-c:v", "libx264", "-preset", "veryfast", "-crf", "14", "-pix_fmt", "yuv420p", out.name, cwd=work)
    return out


# ── 자막 ──
def _text_w(s: str, size: int) -> float:
    w = 0.0
    for c in s:
        if c == " ":
            w += 0.27
        elif ord(c) < 0x2000:
            w += 0.58
        else:
            w += 0.96
    return w * size


def wrap2(s: str, size: int, max_w: float) -> list[str]:
    """한 줄 우선, 넘치면 가운데에 가까운 띄어쓰기에서 두 줄."""
    if _text_w(s, size) <= max_w or " " not in s:
        return [s]
    spaces = [i for i, c in enumerate(s) if c == " "]
    cut = min(spaces, key=lambda i: abs(_text_w(s[:i], size) - _text_w(s[i + 1:], size)))
    return [s[:cut], s[cut + 1:]]


def _esc_enable(s: float, e: float) -> str:
    return f"enable='between(t,{s:.3f},{e:.3f})'"


def main_v2(rec: Path) -> int:
    import json
    work = VIDEO / "_work"
    work.mkdir(parents=True, exist_ok=True)
    fonts = (("fx.otf", "pretendard_extrabold.otf"), ("fb.otf", "pretendard_bold.otf"),
             ("fs.otf", "pretendard_semibold.otf"), ("fr.otf", "pretendard_regular.otf"))
    for name, f in fonts:
        shutil.copy(FONT_DIR / f, work / name)
    timing = json.loads((TTS / "timing.json").read_text(encoding="utf-8"))
    mp3s = [TTS / f"{n}.mp3" for n in TTS_NAMES]
    for p in mp3s:
        if not p.exists():
            sys.exit(f"음성 파일 없음: {p} (--split-all 먼저)")
    alens = [duration(p) for p in mp3s]

    # 1) 장면 영상 — 길이는 음성 + 앞뒤 LEAD
    segs = []
    for i, sc in enumerate(V2_SCENES):
        want = alens[i] + 2 * LEAD
        have = sum(b - a for a, b, _ in sc["parts"]) + sc.get("hold", 0.0)
        if abs(want - have) > 0.15:
            print(f"  ! 장면 {i + 1}: 구간 {have:.2f}초 ≠ 음성+{2 * LEAD:.1f} {want:.2f}초", flush=True)
        print(f"장면 {i + 1} …", flush=True)
        segs.append(build_scene_v2(rec, work, i, sc))
    close_len = max(CLOSE_MIN, alens[-1] + 2 * LEAD)
    segs.append(build_closing_v2(work, segs[-1], close_len))
    lens = [duration(p) for p in segs]
    starts, t = [], 0.0
    for i, d in enumerate(lens):
        starts.append(t)
        t += d - (XF if i < len(lens) - 1 else 0)
    total = t
    if total > 210:
        print(f"  ! 총 {total:.1f}초 — 3분 30초 초과", flush=True)

    # 2) 이어 붙이기
    args = []
    for p in segs:
        args += ["-i", str(p)]
    g, prev = [], "0:v"
    for i in range(1, len(segs)):
        g.append(f"[{prev}][{i}:v]xfade=transition=fade:duration={XF}:offset={starts[i]:.3f}[x{i}]")
        prev = f"x{i}"
    master = work / "v2_master.mp4"
    ff(*args, "-filter_complex", ";".join(g), "-map", f"[{prev}]", "-c:v", "libx264", "-preset", "veryfast",
       "-crf", "12", "-r", str(FPS), "-pix_fmt", "yuv420p", str(master))

    # 3) 음성 트랙: 장면 시작 + LEAD, -16 LUFS(2회 loudnorm), 배경음악이 있으면 -28dB + 덕킹
    a_in, a_g = [], []
    for i, p in enumerate(mp3s):
        a_in += ["-i", str(p)]
        ms = int(round((starts[i] + LEAD) * 1000))
        a_g.append(f"[{i}:a]aresample=48000,aformat=channel_layouts=stereo,adelay={ms}:all=1[a{i}]")
    a_g.append(f"{''.join(f'[a{i}]' for i in range(len(mp3s)))}amix=inputs={len(mp3s)}:normalize=0,"
               f"apad,atrim=0:{total:.3f}[voice]")
    voice_wav = work / "v2_voice.wav"
    ff(*a_in, "-filter_complex", ";".join(a_g), "-map", "[voice]", "-c:a", "pcm_s16le", str(voice_wav))
    meas = subprocess.run([FF, "-hide_banner", "-i", str(voice_wav), "-af",
                           "loudnorm=I=-16:TP=-1.5:LRA=11:print_format=json", "-f", "null", "-"],
                          capture_output=True, text=True, errors="replace").stderr
    m = json.loads(meas[meas.rindex("{"):meas.rindex("}") + 1])
    ln = (f"loudnorm=I=-16:TP=-1.5:LRA=11:measured_I={m['input_i']}:measured_TP={m['input_tp']}:"
          f"measured_LRA={m['input_lra']}:measured_thresh={m['input_thresh']}:offset={m['target_offset']}:linear=true")
    bgm = sorted((VIDEO / "bgm").glob("*.mp3")) if (VIDEO / "bgm").exists() else []
    audio = work / "v2_audio.wav"
    if bgm:
        ff("-i", str(voice_wav), "-stream_loop", "-1", "-i", str(bgm[0]), "-filter_complex",
           f"[0:a]{ln},asplit[v][sc];[1:a]aresample=48000,aformat=channel_layouts=stereo,volume=-28dB,"
           f"atrim=0:{total:.3f},afade=t=out:st={total - 2:.3f}:d=2[b];"
           f"[b][sc]sidechaincompress=threshold=0.03:ratio=6:attack=20:release=400[bd];"
           f"[v][bd]amix=inputs=2:normalize=0[mix]", "-map", "[mix]", "-c:a", "pcm_s16le", str(audio))
    else:
        ff("-i", str(voice_wav), "-af", ln + ",aresample=48000", "-c:a", "pcm_s16le", str(audio))

    # 4) 자막·장면 칩·흔들림 안내
    cues = []  # (시작, 끝, 줄들, 가운데 x, 아래 y)
    for i, name in enumerate(TTS_NAMES):
        sc = V2_SCENES[i] if i < len(V2_SCENES) else None
        sents = timing[name]["sentences"]
        bounds, acc = [], 0.0  # 장면 안 구간 경계 → 자막 위치
        if sc:
            for a, b, v in sc["parts"]:
                acc += b - a
                bounds.append((acc, v["sub"]))
        for k, s in enumerate(sents):
            cs = starts[i] + LEAD + s["start"]
            ce = starts[i] + LEAD + s["end"] + 0.12
            if k + 1 < len(sents):
                ce = min(ce, starts[i] + LEAD + sents[k + 1]["start"] - 0.04)
            rel = cs - starts[i]
            cx, bottom = next((sub for edge, sub in bounds if rel < edge), bounds[-1][1] if bounds else SUB_DEFAULT)
            max_w = 1500 if cx == 960 else 2 * min(cx, W - cx) - 120
            cues.append((cs, ce, wrap2(s["text"], SUB_SIZE, max_w), cx, bottom))
    srt, filt = [], []
    for n, (s, e, lines, cx, bottom) in enumerate(cues, 1):
        srt += [str(n), f"{srt_time(s)} --> {srt_time(e)}", *lines, ""]
        tf = f"v2_sub{n:02d}.txt"
        (work / tf).write_text("\n".join(lines), encoding="utf-8")
        filt.append(f"drawtext=fontfile=fb.otf:textfile={tf}:fontsize={SUB_SIZE}:fontcolor=white:borderw=3:bordercolor=black:"
                    f"box=1:boxcolor=black@0.7:boxborderw=16|30:line_spacing=12:text_align=C:"
                    f"x={cx}-tw/2:y={bottom - 16}-th:{_esc_enable(s, e)}")
    for i, sc in enumerate(V2_SCENES):
        tf = f"v2_chip{i + 1:02d}.txt"
        (work / tf).write_text(sc["title"], encoding="utf-8")
        s0 = starts[i] + (0.0 if i == 0 else XF)
        filt.append(f"drawtext=fontfile=fb.otf:textfile={tf}:fontsize={CHIP_SIZE}:fontcolor=white:box=1:"
                    f"boxcolor={CHIP_BLUE}:boxborderw=12|20:x=40:y=28:{_esc_enable(s0, s0 + 2.5)}")
        if sc.get("note"):
            nf = f"v2_note{i + 1:02d}.txt"
            (work / nf).write_text(sc["note"], encoding="utf-8")
            filt.append(f"drawtext=fontfile=fs.otf:textfile={nf}:fontsize={NOTE_SIZE}:fontcolor=white:box=1:"
                        f"boxcolor=0x182230@0.72:boxborderw=8|16:x={sc.get('note_x', W // 2)}-tw/2:y=24:"
                        f"{_esc_enable(starts[i] + XF, starts[i + 1])}")
    (VIDEO / "gatigage_demo_v2.srt").write_text("\n".join(srt), encoding="utf-8")
    (work / "v2_subs.txt").write_text(",\n".join(filt), encoding="utf-8")
    ff("-i", master.name, "-i", audio.name, "-/vf", "v2_subs.txt", "-map", "0:v", "-map", "1:a",
       "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-r", str(FPS), "-pix_fmt", "yuv420p",
       "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-shortest", "-movflags", "+faststart",
       str(VIDEO / "gatigage_demo_v2.mp4"), cwd=work)

    # 5) cut_list_v2.md · youtube_description.txt
    def mmss(x: float) -> str:
        return f"{int(x) // 60:02d}:{int(x) % 60:02d}"

    rows = ["# 시연 영상 v2 자른 목록 (gatigage_demo_v2)", "",
            f"- 원본: `{rec.relative_to(ROOT).as_posix()}/` · 음성: `artifacts/video/tts/` (Typecast, 장면별 mp3 + timing.json)"
            f" · 스크립트: `scripts/cut_demo_video.py --version v2`",
            f"- 총 **{total:.1f}초** ({mmss(total)}) · 30fps 1920×1080 · 장면 전환 {XF}초 · 장면 = 음성 + 앞뒤 {LEAD}초 · 끝 화면 {close_len:.1f}초",
            "- 화면: `app y0–y1 ×s` = 녹화 앱 영역(x 640–1920)의 세로 y0–y1을 s배(위쪽 정렬, 남는 곳 앱 배경색) · "
            "`폰` = 차 앱 화면(위 ~ y1) 1.117배 왼쪽 + 폰 오른쪽", "",
            "| # | 장면 | 원본 | 잘라 쓴 구간(초) · 화면 | 끝 정지 | 음성 | 시작 | 길이 |", "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    for i, sc in enumerate(V2_SCENES):
        cuts = []
        for a, b, v in sc["parts"]:
            view = f"app {v['y0']}–{v['y1']} ×{v['s']}" if v["kind"] == "app" else f"폰({sc['web']}) 차 위~{v['y1']}"
            cuts.append(f"{a:g}–{b:g} {view}")
        rows.append(f"| {i + 1} | {sc['title']} | {sc['src']} | {'<br>'.join(cuts)} | {sc.get('hold', 0):g} | "
                    f"{alens[i]:.1f} | {starts[i]:.1f} | {lens[i]:.1f} |")
    rows.append(f"| 닫음 | 끝 화면 | 10번 끝 프레임 흐림 | — | — | {alens[-1]:.1f} | {starts[-1]:.1f} | {lens[-1]:.1f} |")
    (VIDEO / "cut_list_v2.md").write_text("\n".join(rows) + "\n", encoding="utf-8")

    chapters = []  # YouTube 챕터는 10초 이상 — 짧은 장면은 앞 챕터에 합친다
    for i, sc in enumerate(V2_SCENES):
        end = starts[i + 1]
        name = sc["title"][2:]
        if chapters and end - starts[i] < 10:
            chapters[-1][1] += " · " + name
        else:
            chapters.append([starts[i], name])
    chapters[0][0] = 0.0
    desc = ["같이가개 — 반려견과 차로 가는 모든 순간을 챙기는 Pleos Connect 차량용 앱 시연 영상입니다.", "",
            *(f"{mmss(s)} {n}" for s, n in chapters), f"{mmss(starts[-1])} 마무리", "",
            f"GitHub: {GITHUB_URL}", f"Release(APK): {RELEASE_URL}", "",
            "출처",
            "이 콘텐츠의 목소리는 \"타입캐스트\"로 만들었어요. 고품질의 더 다양한 목소리를 타입캐스트(https://typecast.ai/kr)에서 만나보세요.",
            f"이 콘텐츠는 인공지능 가상 연기자 서비스, 타입캐스트를 활용하여 제작되었습니다. 사용 캐릭터: {TYPECAST_CHARACTER} https://typecast.ai/"]
    if bgm:
        desc.append(f"배경음악: {bgm[0].stem} (출처를 여기에 적는다)")
    desc += ["지도·도로 데이터: © OpenStreetMap contributors (ODbL)",
             "반려동물 동반 여행지 데이터: 한국관광공사 (공공데이터포털)"]
    (VIDEO / "youtube_description.txt").write_text("\n".join(desc) + "\n", encoding="utf-8")

    print(f"총 {total:.1f}초")
    for i, d in enumerate(lens):
        print(f"  {i + 1:2d} 시작 {starts[i]:6.1f} 길이 {d:5.2f}")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", choices=("v1", "v2"), default="v2")
    ap.add_argument("--rec", default=str(ROOT / "artifacts" / "recording" / "2026-11-05"))
    ap.add_argument("--split-all", action="store_true", help="v2: tts/all.mp3 + all.txt → 장면별 mp3 + timing.json")
    a = ap.parse_args()
    if a.version == "v1":
        return main_v1(Path(a.rec))
    if a.split_all:
        split_all()
        return 0
    return main_v2(Path(a.rec))


if __name__ == "__main__":
    sys.exit(main())
