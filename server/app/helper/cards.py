"""돌봄 카드 읽기 (content/care/*.md + _drafts/*.md). 앱 CareSheet·HelperRouter와 같은 머리말 형식.

서버는 앱이 넘긴 `allowed` 카드만 고른다 — 릴리스 앱은 검수 끝난 카드(reviewed: true)만 넘긴다.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CARD_DIRS = (ROOT / "content" / "care", ROOT / "content" / "care" / "_drafts")


@dataclass(frozen=True)
class Card:
    id: str
    title: str
    category: str | None
    triggers: tuple[str, ...]
    voice: str | None
    action: str | None
    reviewed: bool
    sources: str
    lines: tuple[str, ...] = field(default_factory=tuple)

    @property
    def text(self) -> str:
        """근거 검사·답에 쓰는 카드 본문 (제목 + 줄)."""
        return " ".join((self.title, *self.lines))

    @property
    def embed_text(self) -> str:
        """임베딩용: 제목 + 분류 말 + 본문."""
        return f"{self.title}. {' '.join(self.triggers)}. {' '.join(self.lines)}"


def _list(raw: str | None) -> tuple[str, ...]:
    if not raw:
        return ()
    return tuple(x.strip() for x in raw.strip().strip("[]").split(",") if x.strip())


def parse(file_id: str, text: str) -> Card | None:
    t = text.replace("\r\n", "\n")
    if not t.startswith("---\n"):
        return None
    end = t.find("\n---", 4)
    if end < 0:
        return None
    head = {}
    for line in t[4:end].splitlines():
        if ":" in line:
            k, v = line.split(":", 1)
            head[k.strip()] = v.strip()
    if "title" not in head:
        return None
    lines = tuple(l.strip()[2:].strip() for l in t[end + 4:].splitlines() if l.strip().startswith("- "))
    return Card(
        id=head.get("id") or file_id, title=head["title"], category=head.get("category"), triggers=_list(head.get("triggers")),
        voice=head.get("voice") or None, action=head.get("action") or None, reviewed=head.get("reviewed") == "true",
        sources=head.get("sources", ""), lines=lines,
    )


def load(dirs: tuple[Path, ...] = CARD_DIRS) -> dict[str, Card]:
    out: dict[str, Card] = {}
    for d in dirs:
        if not d.is_dir():
            continue
        for f in sorted(d.glob("care_*.md")):
            c = parse(f.stem.removeprefix("care_"), f.read_text(encoding="utf-8"))
            if c and c.id not in out:
                out[c.id] = c
    return out


EMERGENCY = re.compile(
    r"숨(을|이)?\s*(못|안\s*쉬|가빠|가쁘|막)|숨\s*쉬기\s*(가\s*)?(힘들|어려)|잇몸(이|색이)?\s*(파래|파랗|하얘)|경련|발작|쓰러|기절|의식(이)?\s*(없|잃)"
    r"|피(가|를)\s*(나|흘)|출혈|차에\s*치|치였|쥐약|살충제|부동액|(초콜(릿|렛)|포도|양파|자일리톨)[을를도]?[^,.?!]{0,10}?(먹었|먹은|삼켰)"
)


def emergency(text: str) -> bool:
    """서버 쪽 안전 검사: 앱이 이미 응급을 먼저 보지만, 서버도 응급 말이면 LLM을 부르지 않는다."""
    return bool(EMERGENCY.search(text))
