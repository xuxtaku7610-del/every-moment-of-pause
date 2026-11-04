"""도우미 답 근거 검사 (공알 cloud/grounding.py의 생각을 돌봄 카드에 맞게 새로 씀). 표준 라이브러리만, 순수 함수.

모델 답의 ① 숫자(분·회·번·도·℃·ml·g·kg·시간·%·컵·알) ② 음식·약 이름 ③ 처치 동사가 **고른 카드 본문에 있어야** 통과한다.
④ 약 이름·용량·진단 낱말은 카드에 있어도 늘 실패 ⑤ 문장마다 내용어 절반 이상이 카드 한 문장에 있어야 한다(공알 SUPPORT_MIN 0.5).
실패하면 답을 버리고 카드 줄을 그대로 쓴다 — 엄격한 쪽이 안전하다(잘못 거르면 문장 하나를 잃을 뿐, 지어낸 처치는 읽히면 안 된다).
"""
import re

SPACE = re.compile(r"\s+")
NUMBER = re.compile(r"(\d+(?:[.~\-]\d+)?)\s*(분|회|번|도|℃|ml|mL|g|kg|시간|%|컵|알|정|방울)")
FOODS = ("초콜릿", "초콜렛", "포도", "건포도", "양파", "마늘", "자일리톨", "우유", "소금", "소금물", "얼음", "얼음물", "알코올", "커피", "뼈", "껌")
DRUGS = ("진통제", "해열제", "수면제", "진정제", "멀미약", "항생제", "연고", "타이레놀", "아스피린", "이부프로펜", "아세트아미노펜", "약을")
VERBS = ("토하게", "먹이", "먹여", "바르", "발라", "주사", "묶", "담그", "담가", "씻기", "씻겨", "문지르", "주무르", "때리", "흔들", "억지로")
ALWAYS_BAD = re.compile(r"\d+\s*(mg|밀리그램|정|알)\b|진단(입니다|결과|돼요|이에요)|(병|질환)입니다|확률")
ENDINGS = ("해주세요", "하세요", "합니다", "입니다", "해요", "예요", "에요", "어요", "아요", "세요", "에서", "으로", "까지", "부터", "에는",
           "은", "는", "이", "가", "을", "를", "에", "로", "의", "도", "와", "과", "만", "요", "고", "면")
STOP = frozenset({"반려견", "콩이", "경우", "때", "것", "좀", "조금", "바로", "다시", "지금", "먼저", "그리고", "또는", "해요", "있어요"})
SENT = re.compile(r"(?<=[.!?。])\s+|\n+")
SUPPORT_MIN = 0.5


def _compact(s: str) -> str:
    return SPACE.sub("", s)


def _words(s: str) -> list[str]:
    out = []
    for w in re.findall(r"[가-힣A-Za-z]+", s):
        for e in ENDINGS:
            if w.endswith(e) and len(w) - len(e) >= 2:
                w = w[: -len(e)]
                break
        if len(w) >= 2 and w not in STOP:
            out.append(w)
    return out


def support(sentence: str, card_lines: list[str]) -> float:
    words = _words(sentence)
    if not words:
        return 1.0
    return max((sum(w in _compact(l) for w in words) / len(words) for l in card_lines), default=0.0)


def check(answer: str, card_text: str, card_lines: list[str] | None = None) -> tuple[bool, list[str]]:
    """(통과, 빠진 근거 목록)."""
    src = _compact(card_text)
    missing: list[str] = []
    if ALWAYS_BAD.search(answer):
        missing.append("약 용량·진단 표현")
    for value, unit in NUMBER.findall(answer):
        if _compact(value + unit) not in src and value not in src:
            missing.append(value + unit)
    for word in (*FOODS, *DRUGS, *VERBS):
        if word in answer and word not in src:
            missing.append(word)
    lines = card_lines or [card_text]
    for i, s in enumerate(x for x in SENT.split(answer) if x.strip()):
        score = support(s, lines)
        if score < SUPPORT_MIN:
            missing.append(f"문장{i + 1} 근거 부족({score:.2f})")
    return not missing, missing
