"""제안·기록 요약 문장 프롬프트·검사·템플릿 (api_spec §2.9)."""
import re

FACILITY_KO = {
    "nursing_room": "수유실", "kids_zone": "키즈존", "pet_zone": "반려견 놀이터", "ev_fast": "급속충전기",
    "ev_slow": "완속충전기", "shower": "샤워실", "accessible_toilet": "장애인 화장실", "sleep_room": "수면실",
    # 같이가개 반려견 이유 (앱 PhraseProvider: Reason.Motion → motion, Reason.PottyDue → potty)
    "motion": "흔들리는 길을 오래 달림", "potty": "반려견 배변·산책 시간",
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


# ───────────── 이동 기록 요약 (F7) ─────────────

SYSTEM_SUMMARY = (
    "너는 반려견 보호자에게 오늘 차 이동 기록을 요약해 주는 비서다. 한국어 2문장 이내, 60자 이내. "
    "주어진 숫자만 쓰고 지어내지 않는다. 주행 자극은 차량 흔들림 값이지 반려견을 잰 값이 아니다 — "
    "반려견의 몸 상태·병명·치료를 말하지 않는다. 문장만 출력한다."
)
SUMMARY_MAX_CHARS = 60
# 기록 요약은 "최고값"을 말해야 하므로 "최고"는 뺀다. 주행 자극을 반려견 상태로 말하지 않게 "멀미"도 막는다 (2026-10-31 재정비).
SUMMARY_BANNED = tuple(b for b in BANNED if b != "최고") + ("진단", "질병", "병에", "치료", "약을", "멀미")


def duration_ko(minutes: int) -> str:
    h, m = divmod(max(0, minutes), 60)
    return f"{m}분" if h == 0 else (f"{h}시간" if m == 0 else f"{h}시간 {m}분")


def summary_user_prompt(facts: dict) -> str:
    limit = duration_ko(facts["limitMin"]) if facts.get("limitMin") else "아직 모름"
    return (
        f"이동 시간: {duration_ko(facts['durationMin'])}\n휴식: {facts['stops']}번\n"
        f"주행 자극 최고값(0~100, 차량 흔들림): {facts['motionPeak']}\n지난 여행 보통 도달 시간: {limit}"
    )


def summary_fallback(facts: dict) -> str:
    base = f"이번 이동은 {duration_ko(facts['durationMin'])}, 휴식 {facts['stops']}번, 주행 자극 최고 {facts['motionPeak']}였어요."
    return base + (f" 지난 여행들은 출발 약 {duration_ko(facts['limitMin'])} 뒤 주행 자극이 보통 이상이 됐어요." if facts.get("limitMin") else "")


def check_summary(text: str, facts: dict, max_sentences: int) -> str | None:
    if not text:
        return "empty"
    if len(text) > SUMMARY_MAX_CHARS:
        return "too_long"
    if sentence_count(text) > max_sentences:
        return "too_many_sentences"
    if any(b in text for b in SUMMARY_BANNED):
        return "banned_word"
    if str(facts["motionPeak"]) not in text:
        return "missing_number"
    return None


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
