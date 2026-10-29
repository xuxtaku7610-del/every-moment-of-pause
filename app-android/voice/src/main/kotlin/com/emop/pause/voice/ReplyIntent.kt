package com.emop.pause.voice

/** 제안에 대한 운전자 응답 (flowchart §1 "운전자 응답"). */
enum class Reply { YES, LATER, UNKNOWN }

/**
 * "응/나중에" 의도 인식. 규칙 기반이라 오프라인에서도 즉시 동작한다.
 * 부정·보류 표현을 먼저 보고("안 갈래"에 "갈래"가 들어 있으므로), 그다음 긍정 표현을 본다.
 */
object ReplyIntent {
    private val later = listOf("나중", "이따", "다음에", "아니", "안 ", "안갈", "싫", "괜찮아", "됐어", "그냥 가", "패스", " 노 ")
    private val yes = listOf("응", "어 ", "네", "예", "그래", "좋아", "가자", "갈래", "들르", "들를", "들러", "쉴게", "쉴래", "쉬자", "쉬어 가", "오케이", "콜", "ok", "yes")

    fun parse(text: String?): Reply {
        val t = " " + (text ?: return Reply.UNKNOWN).trim().lowercase() + " "
        if (t.isBlank()) return Reply.UNKNOWN
        if (later.any { it in t }) return Reply.LATER
        if (yes.any { it in t } || t.trim() == "어") return Reply.YES
        return Reply.UNKNOWN
    }
}
