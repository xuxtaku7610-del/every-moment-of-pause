package com.emop.pause.trip

import com.emop.pause.uistate.MotionAnswerUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** UPGRADE 4 (F7-05): 음성 답 해석 — "안 했어요"는 아니요(부정 먼저). */
class MotionAnswerParserTest {
    @Test
    fun `F7-05 음성 답 해석`() {
        assertEquals(MotionAnswerUi.YES, MotionAnswerParser.parse("네"))
        assertEquals(MotionAnswerUi.YES, MotionAnswerParser.parse("응 토했어"))
        assertEquals(MotionAnswerUi.LITTLE, MotionAnswerParser.parse("조금이요"))
        assertEquals(MotionAnswerUi.NO, MotionAnswerParser.parse("아니요"))
        assertEquals(MotionAnswerUi.NO, MotionAnswerParser.parse("안 했어요"))
        assertEquals(MotionAnswerUi.NO, MotionAnswerParser.parse("괜찮았어"))
        assertNull(MotionAnswerParser.parse("음 글쎄"))
        assertNull(MotionAnswerParser.parse(null))
    }
}
