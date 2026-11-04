package com.emop.pause.voice

/** 들은 말 한 마디의 뜻. */
sealed interface Utterance {
    /** 제안에 대한 응답 (응/나중에/모름). "여기서 쉴게요"는 응. */
    data class Answer(val reply: Reply) : Utterance

    /** "잠깐 다녀올게" — 정차 중 대기 모드 시작 (CONCEPT F3). */
    data object StartWait : Utterance

    /** "조용히 해 줘" — 조용히 모드(응급·쉼표 추천만, 1시간 또는 정차 때까지, v10-1). */
    data object Quiet : Utterance

    /** "오늘은 안 타요" — 그날 운행 동안 안내를 모두 끈다 (v10-1). */
    data object DayOff : Utterance
}

/**
 * 운전자 말 → 뜻. 규칙 기반이라 오프라인에서도 즉시 동작한다.
 * 대기 모드 시작 표현을 먼저 본다("다녀올게"에는 응답 단어가 없지만 "갈게"류와 겹치지 않게).
 */
object VoiceIntent {
    private val waitWords = listOf("다녀올게", "다녀 올게", "갔다 올게", "갔다올게", "금방 올게", "잠깐 나갔다", "대기 모드", "기다려 줘", "기다려줘")

    private val quietWords = listOf("조용히", "조용 히", "그만 말해", "말 좀 그만")
    private val dayOffWords = listOf("안 타", "안타요", "안 태워", "혼자 가", "혼자 타")

    fun parse(text: String?): Utterance {
        val t = text?.trim()?.lowercase().orEmpty()
        if (t.isEmpty()) return Utterance.Answer(Reply.UNKNOWN)
        if (waitWords.any { it in t }) return Utterance.StartWait
        if (quietWords.any { it in t }) return Utterance.Quiet
        if (dayOffWords.any { it in t }) return Utterance.DayOff
        return Utterance.Answer(ReplyIntent.parse(t))
    }
}
