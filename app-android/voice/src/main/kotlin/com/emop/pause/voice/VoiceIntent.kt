package com.emop.pause.voice

/** 들은 말 한 마디의 뜻. */
sealed interface Utterance {
    /** 제안에 대한 응답 (응/나중에/모름). "여기서 쉴게요"는 응. */
    data class Answer(val reply: Reply) : Utterance

    /** "잠깐 다녀올게" — 정차 중 대기 모드 시작 (CONCEPT F3). */
    data object StartWait : Utterance
}

/**
 * 운전자 말 → 뜻. 규칙 기반이라 오프라인에서도 즉시 동작한다.
 * 대기 모드 시작 표현을 먼저 본다("다녀올게"에는 응답 단어가 없지만 "갈게"류와 겹치지 않게).
 */
object VoiceIntent {
    private val waitWords = listOf("다녀올게", "다녀 올게", "갔다 올게", "갔다올게", "금방 올게", "잠깐 나갔다", "대기 모드", "기다려 줘", "기다려줘")

    fun parse(text: String?): Utterance {
        val t = text?.trim()?.lowercase().orEmpty()
        if (t.isEmpty()) return Utterance.Answer(Reply.UNKNOWN)
        if (waitWords.any { it in t }) return Utterance.StartWait
        return Utterance.Answer(ReplyIntent.parse(t))
    }
}
