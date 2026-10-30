package com.emop.pause.trip

import com.emop.pause.uistate.MotionAnswerUi

/** 도착 뒤 "오늘 멀미했어요?" 음성 답 → 보정 답 (UPGRADE 4). 부정을 먼저 본다("안 했어요"에 "했어"가 들어 있으므로). 모르면 null → 화면 버튼. */
object MotionAnswerParser {
    fun parse(text: String?): MotionAnswerUi? {
        val t = text?.replace(" ", "")?.takeIf { it.isNotEmpty() } ?: return null
        return when {
            listOf("아니", "안했", "괜찮", "멀쩡").any { it in t } -> MotionAnswerUi.NO
            listOf("조금", "약간", "살짝").any { it in t } -> MotionAnswerUi.LITTLE
            listOf("네", "예", "응", "했어", "했다", "토했").any { it in t } -> MotionAnswerUi.YES
            else -> null
        }
    }
}
