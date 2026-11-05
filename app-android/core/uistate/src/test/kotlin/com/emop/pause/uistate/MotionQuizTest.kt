package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan F8-01: 온보딩 흔들림에 예민한 정도(보호자 응답) — 질문 3개 → 3단계, 다 답해야 결과. */
class MotionQuizTest {
    @Test
    fun `F8-01 답 합계로 3단계를 고른다`() {
        assertEquals(TendencyUi.LOW, MotionQuiz.result(listOf(0, 0, 1)))
        assertEquals(TendencyUi.NORMAL, MotionQuiz.result(listOf(1, 1, 0)))
        assertEquals(TendencyUi.NORMAL, MotionQuiz.result(listOf(2, 1, 0)))
        assertEquals(TendencyUi.HIGH, MotionQuiz.result(listOf(2, 1, 1)))
        assertNull(MotionQuiz.result(listOf(2, null, 1)))
        assertNull(MotionQuiz.result(listOf(2)))
    }

    @Test
    fun `F8-01 온보딩 다음 버튼은 단계 조건을 따른다`() {
        assertTrue(Fixtures.onboarding(1).canNext)
        assertFalse(Fixtures.onboarding(1).copy(pet = Fixtures.pet.copy(problems = listOf("이름을 적어 주세요"))).canNext)
        assertTrue(Fixtures.onboarding(2).canNext)
        assertTrue(Fixtures.onboarding(3).canNext)
        assertFalse(Fixtures.onboarding(3).copy(result = null).canNext)
        assertEquals(3, Fixtures.onboarding(1).quiz.size)
    }
}
