package com.emop.pause.voice

/** 답변 말투. 화자 구분은 하지 않고 질문 표현으로 고른다 (COMMERCIAL_READINESS C P0). */
enum class Tone { KID, ADULT }

/** 들은 말 한 마디의 뜻. */
sealed interface Utterance {
    /** 제안에 대한 응답 (응/나중에/모름). */
    data class Answer(val reply: Reply) : Utterance

    /** "아직 멀었어?" 계열 — 남은 시간 질문. */
    data class AskEta(val tone: Tone) : Utterance

    /** "라디오 틀어 줘" / "라디오 꺼". */
    data class Radio(val on: Boolean) : Utterance
}

/**
 * 운전자·동승자 말 → 뜻. 규칙 기반이라 오프라인에서도 즉시 동작한다.
 * 남은 시간 질문을 먼저 본다("얼마나 남았어?"에는 응답 단어가 없지만, "아직 멀었어?"의 "아"가 다른 규칙과 겹치지 않게).
 */
object VoiceIntent {
    private val etaWords = listOf("멀었", "남았", "언제 도착", "언제 와", "언제 가", "도착해", "얼마나 가", "몇 분", "몇분", "몇 시간", "다 왔", "도착 시간", "남은 시간", "남은 거리")

    /** 어른 말투 표지: 존댓말 어미, 시간·거리 명사. 없으면 아이 말투(반말 질문)로 본다. */
    private val adultMarks = listOf("요", "니까", "습니", "남은 시간", "도착 시간", "도착 예정", "거리", "몇 km", "킬로")

    private val radioOn = listOf("틀어", "켜", "들려", "들을래", "해 줘", "해줘", "시작")
    private val radioOff = listOf("꺼", "그만", "멈춰", "끄")

    fun parse(text: String?): Utterance {
        val t = text?.trim()?.lowercase().orEmpty()
        if (t.isEmpty()) return Utterance.Answer(Reply.UNKNOWN)
        if ("라디오" in t || "이야기" in t) {
            if (radioOff.any { it in t }) return Utterance.Radio(on = false)
            if (radioOn.any { it in t }) return Utterance.Radio(on = true)
        }
        if (etaWords.any { it in t }) {
            return Utterance.AskEta(if (adultMarks.any { it in t }) Tone.ADULT else Tone.KID)
        }
        return Utterance.Answer(ReplyIntent.parse(t))
    }
}
