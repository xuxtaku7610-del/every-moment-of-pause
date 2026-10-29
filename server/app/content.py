"""콘텐츠 편성과 아이 답변 템플릿 — 앱 `engine:content` PlaylistPlanner·`voice` EtaAnswer와 같은 규칙."""
from dataclasses import dataclass

STEP = 5  # 초. 에피소드 길이도 5초 단위로 만든다.


@dataclass(frozen=True)
class Item:
    id: str
    duration_sec: int


def plan_playlist(items: list[Item], target_sec: int) -> list[Item]:
    """목표 이하에서 가장 가까운 조합 (0/1 배낭). 합이 같으면 편수가 적은 쪽, 그다음 입력 순서."""
    if target_sec <= 0:
        return []
    cap = target_sec // STEP
    w = [(i.duration_sec + STEP - 1) // STEP for i in items]
    best = [0] * (cap + 1)
    count = [0] * (cap + 1)
    take = [[False] * (cap + 1) for _ in items]
    for i, wi in enumerate(w):
        for c in range(cap, wi - 1, -1):
            s, n = best[c - wi] + wi, count[c - wi] + 1
            if s > best[c] or (s == best[c] and n < count[c]):
                best[c], count[c], take[i][c] = s, n, True
    chosen, c = [], cap
    for i in range(len(items) - 1, -1, -1):
        if take[i][c]:
            chosen.append(items[i])
            c -= w[i]
    return list(reversed(chosen))


def _duration(minutes: int) -> str:
    h, m = divmod(max(0, minutes), 60)
    if h == 0:
        return f"{m}분"
    return f"{h}시간" if m == 0 else f"{h}시간 {m}분"


def kid_answer(minutes_to_next_stop: int | None, remaining_episodes: int | None, highlights: list[str]) -> str:
    """'아직 멀었어?' 아이 답 — 검수된 고정 문장 틀만 쓴다. 아이용 문장은 실시간 LLM으로 만들지 않는다 (CLAUDE.md 규칙 4)."""
    place = "키즈존 있는 휴게소" if "kids_zone" in highlights else "다음 휴게소"
    if remaining_episodes and remaining_episodes > 0:
        return f"라디오 {remaining_episodes}편만 들으면 {place}에 도착해!"
    if minutes_to_next_stop is None:
        return "조금만 더 가면 돼. 창밖에 뭐가 보이는지 같이 찾아볼까?"
    if minutes_to_next_stop <= 10:
        return f"거의 다 왔어! {minutes_to_next_stop}분만 기다리면 {place}야."
    return f"{_duration(minutes_to_next_stop)} 뒤에 {place}에서 쉬어 가자. 조금만 기다려 줘!"
