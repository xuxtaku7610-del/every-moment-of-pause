"""돌봄 카드 자료 후보 모으기 (HELPER_PROMPT 2번) — 개발 PC에서 사람이 돌리는 도구. **앱·서버 실행 중(차 안)에는 쓰지 않는다.**

    cd server
    .venv\\Scripts\\python tools\\collect_care_sources.py --list                      # 목록만 (네트워크 없음)
    .venv\\Scripts\\python tools\\collect_care_sources.py --only water,potty          # 고른 것만
    .venv\\Scripts\\python tools\\collect_care_sources.py --summarize                 # 인용으로만 한국어 요약 후보(로컬 Ollama)

규칙
- 아래 SOURCES에 적은 주소만 연다(링크를 따라가지 않는다). 공식 자료만: Merck Veterinary Manual(반려인용)·RSPCA.
- 사이트마다 robots.txt를 먼저 읽고 막힌 주소는 건너뛴다. 요청 사이 DELAY_SECONDS(기본 5초) 쉰다. User-Agent에 목적을 밝힌다.
- 결과 = `content/care/_drafts/_candidates/<slug>.md` (aapt가 `_`로 시작하는 폴더를 빼므로 앱 assets에 들어가지 않는다):
  요약 후보 2~4줄 + 원문 인용 1~2개(각 240자 이하) + 주소 + 연 날짜. **원문 전체는 저장·업로드하지 않는다**(메모리에서 인용만 고르고 버린다).
- 후보는 늘 `reviewed: false`. 카드로 옮기는 것은 사람이 인용과 대조한 뒤(content/care/README.md "검수 방법").
- 약 이름·용량·사람 약이 들어간 문장은 인용에서 뺀다.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.robotparser
from datetime import date
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit

import httpx

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "content" / "care" / "_drafts" / "_candidates"
UA = "gatigage-care-review/1.0 (manual review of pet travel care cards; one request per listed page)"
DELAY_SECONDS = 5.0
QUOTE_MAX = 240
ALLOWED_HOSTS = ("www.merckvetmanual.com", "www.rspca.org.uk")
NO_QUOTE = re.compile(r"\b\d+\s*(mg|ml|mL|tablet|tablets)\b|paracetamol|ibuprofen|aspirin|acetaminophen|human medicine|dose|dosage", re.I)

# slug, 주소, 인용을 고를 영어 낱말 (카드 주제)
SOURCES: list[tuple[str, str, tuple[str, ...]]] = [
    ("water", "https://www.rspca.org.uk/adviceandwelfare/pets/general/holiday", ("water", "drink")),
    ("departure_prep", "https://www.rspca.org.uk/adviceandwelfare/pets/general/holiday", ("before", "journey", "travel")),
    ("heatstroke", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health/heatstroke", ("cool", "heatstroke", "vet")),
    ("wait_heat", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health/dogsinhotcars", ("car", "hot", "minutes")),
    ("hot_walk", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health/hotwalks", ("walk", "pavement", "hot")),
    ("first_aid", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health/firstaid", ("vet", "calm", "bleeding")),
    ("poisoning", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health/poisoning", ("vet", "poison", "sick")),
    ("senior", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health/seniordogs", ("older", "walk", "joint")),
    ("walking", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/walking", ("lead", "walk")),
    ("potty", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/training/toilettraining", ("toilet", "sniff", "circling")),
    ("barking", "https://www.rspca.org.uk/adviceandwelfare/pets/dogs/behaviour/barking", ("bark",)),
    ("brachy", "https://www.rspca.org.uk/adviceandwelfare/pets/genetics/caring/dogs", ("breath", "flat", "heat")),
    ("emergency", "https://www.merckvetmanual.com/special-pet-topics/emergencies/what-to-do-in-a-dog-or-cat-emergency", ("veterinarian", "emergency")),
    ("minor_injury", "https://www.merckvetmanual.com/special-pet-topics/emergencies/minor-injuries-and-accidents", ("sting", "swelling", "veterinarian")),
    ("motion", "https://www.merckvetmanual.com/dog-owners/brain-spinal-cord-and-nerve-disorders-of-dogs/motion-sickness-in-dogs", ("motion", "drool", "vomit")),
    ("vomit", "https://www.merckvetmanual.com/dog-owners/digestive-disorders-of-dogs/vomiting-in-dogs", ("vomit", "veterinarian")),
    ("travel_kit", "https://www.merckvetmanual.com/multimedia/table/pet-travel-kit", ("water", "leash", "first")),
]


class _Text(HTMLParser):
    """본문 글자만 (script·style·nav·footer 제외). 메모리에만 두고 파일로 쓰지 않는다."""

    SKIP = {"script", "style", "nav", "footer", "header", "noscript", "svg"}

    def __init__(self) -> None:
        super().__init__()
        self.depth = 0
        self.parts: list[str] = []

    def handle_starttag(self, tag, attrs):  # noqa: ANN001
        if tag in self.SKIP:
            self.depth += 1

    def handle_endtag(self, tag):  # noqa: ANN001
        if tag in self.SKIP and self.depth:
            self.depth -= 1

    def handle_data(self, data):  # noqa: ANN001
        if not self.depth and data.strip():
            self.parts.append(data.strip())


def sentences(html: str) -> list[str]:
    p = _Text()
    p.feed(html)
    text = re.sub(r"\s+", " ", " ".join(p.parts))
    out = (s.strip().lstrip("|").strip() for s in re.split(r"(?<=[.!?])\s+", text))
    return [s for s in out if 40 <= len(s) <= QUOTE_MAX]


def pick_quotes(sents: list[str], words: tuple[str, ...], n: int = 2) -> list[str]:
    scored = sorted(((sum(w in s.lower() for w in words), i, s) for i, s in enumerate(sents) if not NO_QUOTE.search(s)), key=lambda x: (-x[0], x[1]))
    return [s for score, _, s in scored if score > 0][:n]


def allowed_by_robots(url: str, cache: dict[str, urllib.robotparser.RobotFileParser], client: httpx.Client) -> bool:
    host = urlsplit(url).netloc
    if host not in cache:
        rp = urllib.robotparser.RobotFileParser()
        try:
            r = client.get(f"https://{host}/robots.txt")
            rp.parse(r.text.splitlines() if r.status_code == 200 else [])
        except httpx.HTTPError:
            rp.parse(["User-agent: *", "Disallow: /"])  # 못 읽으면 막힌 것으로
        cache[host] = rp
        time.sleep(DELAY_SECONDS)
    return cache[host].can_fetch(UA, url)


def summarize(quotes: list[str]) -> list[str]:
    """로컬 Ollama로 인용만 근거로 한국어 2~4줄 후보. 실패하면 빈 목록(사람이 쓴다)."""
    sys.path.insert(0, str(ROOT / "server"))
    from app.config import get_settings  # noqa: PLC0415

    s = get_settings()
    prompt = ("아래 영어 인용만 근거로 반려견 보호자용 한국어 문장 2~4개를 '- '로 시작하는 줄로 쓴다. 인용에 없는 숫자·약·처치는 쓰지 않는다. "
              "진단하지 않는다. '~해요'체.\n\n" + "\n".join(quotes))
    try:
        r = httpx.post(f"{s.ollama_base_url.rstrip('/')}/api/chat", timeout=60, json={
            "model": s.ollama_model, "stream": False, "think": False, "options": {"temperature": 0.2, "num_predict": 200},
            "messages": [{"role": "user", "content": prompt}]})
        lines = [l.strip()[2:].strip() for l in r.json()["message"]["content"].splitlines() if l.strip().startswith("- ")]
        return lines[:4]
    except Exception:  # noqa: BLE001
        return []


def write_candidate(slug: str, url: str, quotes: list[str], summary: list[str]) -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    today = date.today().isoformat()
    body = summary or ["(요약 필요: 아래 인용을 보고 사람이 2~4줄로 써요)"]
    text = "\n".join([
        "---",
        f"id: {slug}",
        f"title: (후보) {slug}",
        "reviewed: false",
        f"sources: {url} ({today} 자동 수집 후보, 사용자 대조 전)",
        "---",
        *[f"- {l}" for l in body[:4]],
        "",
        "## 인용 (원문 일부, 카드에는 넣지 않음)",
        *[f"> {q}" for q in quotes],
        "",
    ])
    p = OUT / f"{slug}.md"
    p.write_text(text, encoding="utf-8")
    return p


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--list", action="store_true", help="주소 목록만 (네트워크 없음)")
    ap.add_argument("--only", default="", help="slug 쉼표 목록")
    ap.add_argument("--summarize", action="store_true", help="로컬 Ollama로 인용 요약 후보")
    ap.add_argument("--delay", type=float, default=DELAY_SECONDS)
    a = ap.parse_args(argv)
    todo = [s for s in SOURCES if not a.only or s[0] in a.only.split(",")]
    if a.list:
        for slug, url, _ in todo:
            print(slug, url)
        return 0
    globals()["DELAY_SECONDS"] = max(a.delay, 2.0)  # 2초보다 짧게는 못 줄인다
    robots: dict = {}
    report = []
    with httpx.Client(headers={"User-Agent": UA}, timeout=20, follow_redirects=False) as c:
        for slug, url, words in todo:
            if urlsplit(url).netloc not in ALLOWED_HOSTS:
                report.append((slug, "허용 밖 주소"))
                continue
            if not allowed_by_robots(url, robots, c):
                report.append((slug, "robots.txt 차단 — 건너뜀"))
                continue
            try:
                r = c.get(url)
            except httpx.HTTPError as e:
                report.append((slug, f"요청 실패 {type(e).__name__}"))
                continue
            finally:
                time.sleep(DELAY_SECONDS)
            if r.status_code != 200 or "html" not in r.headers.get("content-type", ""):
                report.append((slug, f"HTTP {r.status_code} {r.headers.get('content-type', '')[:20]}"))
                continue
            quotes = pick_quotes(sentences(r.text), words)
            del r  # 원문은 여기서 버린다
            if not quotes:
                report.append((slug, "맞는 문장 없음"))
                continue
            path = write_candidate(slug, url, quotes, summarize(quotes) if a.summarize else [])
            report.append((slug, f"후보 {path.relative_to(ROOT).as_posix()}"))
    print(json.dumps(report, ensure_ascii=False, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
