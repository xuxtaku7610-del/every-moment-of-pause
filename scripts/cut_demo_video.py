"""시연 영상 3분 편집본 만들기 (2026-11-05).

    server\\.venv\\Scripts\\python scripts/cut_demo_video.py [--rec artifacts/recording/2026-11-05]

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


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--rec", default=str(ROOT / "artifacts" / "recording" / "2026-11-05"))
    a = ap.parse_args()
    rec = Path(a.rec)
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


if __name__ == "__main__":
    sys.exit(main())
