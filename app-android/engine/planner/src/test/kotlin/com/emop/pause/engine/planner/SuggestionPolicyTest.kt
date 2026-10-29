package com.emop.pause.engine.planner

import com.emop.pause.engine.fatigue.FatigueLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SuggestionPolicyTest {
    private val min = 60_000L

    @Test
    fun `P0 나중에 - 10분 뒤 재평가`() {
        val p = SuggestionPolicy()
        assertTrue(p.canSuggest(0, FatigueLevel.MEDIUM))
        p.onDeclined(0)
        assertFalse(p.canSuggest(9 * min, FatigueLevel.MEDIUM))
        assertTrue(p.canSuggest(10 * min, FatigueLevel.MEDIUM))
    }

    @Test
    fun `P1 연속 거절 3회부터 간격 2배, 피로 HIGH면 예외`() {
        val p = SuggestionPolicy()
        p.onDeclined(0)
        p.onDeclined(10 * min)
        p.onDeclined(20 * min)
        assertEquals(3, p.consecutiveDeclines)
        assertFalse(p.canSuggest(30 * min, FatigueLevel.MEDIUM))
        assertTrue(p.canSuggest(40 * min, FatigueLevel.MEDIUM))
        assertTrue(p.canSuggest(30 * min, FatigueLevel.HIGH))
    }

    @Test
    fun `수락하면 거절 횟수 초기화`() {
        val p = SuggestionPolicy()
        repeat(3) { p.onDeclined(it * 10 * min) }
        p.onAccepted()
        assertEquals(0, p.consecutiveDeclines)
        assertTrue(p.canSuggest(20 * min, FatigueLevel.LOW))
    }
}
