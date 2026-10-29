"""제안 문장 프롬프트·검사·템플릿 (api_spec §2.8)."""
import re

FACILITY_KO = {
    "nursing_room": "수유실", "kids_zone": "키즈존", "pet_zone": "반려동물 놀이터", "ev_fast": "급속충전기",
    "ev_slow": "완속충전기", "shower": "샤워실", "accessible_toilet": "장애인 화장실", "sleep_room": "수면실",
}
MAX_CHARS = 50
# 과장·단정·공포 조장 표현. 운전 중 문장에 쓰지 않는다.
BANNED = ("무조건", "반드시", "절대", "100%", "최고", "완벽", "사고가 납", "죽", "위험합니다!", "!!")

SYSTEM_REST = (
    "너는 운전자에게 휴게소 휴식을 제안하는 차량 음성 비서다. "
    "한국어로 최대 2문장, 50자 이내로 말한다. 숫자와 장소 이름을 먼저 말하고, 마지막은 들를지 묻는 질문으로 끝낸다. "
    "주어진 사실에 없는 시설이나 정보는 절대 덧붙이지 않는다. 과장하거나 겁주지 않는다. 문장만 출력한다."
)


def rest_user_prompt(facts: dict) -> str:
    reasons = ", ".join(FACILITY_KO.get(r, r) for r in facts.get("reasons", [])) or "없음"
    passengers = ", ".join(facts.get("passengers", [])) or "없음"
    return (
        f"휴게소: {facts['restStopName']}\n도착까지: {facts['minutesAway']}분\n"
        f"이유(편의시설): {reasons}\n피로 수준: {facts.get('fatigueLevel', 'low')}\n동승자: {passengers}"
    )


def rest_fallback(facts: dict) -> str:
    return f"{facts['minutesAway']}분 뒤 {facts['restStopName']}에서 쉬어 갈까요?"


def sentence_count(text: str) -> int:
    return len([s for s in re.split(r"(?<=[.?!])\s+", text.strip()) if s])


def check(text: str, facts: dict, max_sentences: int) -> str | None:
    """통과하면 None, 아니면 실패 이유."""
    if not text:
        return "empty"
    if len(text) > MAX_CHARS:
        return "too_long"
    if sentence_count(text) > max_sentences:
        return "too_many_sentences"
    if any(b in text for b in BANNED):
        return "banned_word"
    if facts["restStopName"] not in text:
        return "missing_place"
    if str(facts["minutesAway"]) not in text:
        return "missing_number"
    return None
