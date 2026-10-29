package com.emop.pause.trip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** COMMERCIAL_READINESS E P0 / test_plan T6-2: 기상 알람은 시스템 알람으로 (앱이 죽어도). */
class NapAlarmPlanTest {
    @Test
    fun `T6-2 실차는 남은 시간 그대로, 시뮬레이션은 배속만큼 빨리`() {
        assertEquals(1_000L + 18 * 60_000L, NapAlarmPlan.triggerAt(1_000, 18 * 60, simSpeed = 1, ticking = true))
        assertEquals(1_000L + 18 * 6_000L, NapAlarmPlan.triggerAt(1_000, 18 * 60, simSpeed = 10, ticking = true))
        assertNull(NapAlarmPlan.triggerAt(1_000, 600, 1, ticking = false))
        assertEquals(1_000L, NapAlarmPlan.triggerAt(1_000, -5, 1, true))
    }

    @Test
    fun `다시 걸기 - 2초 넘게 달라지거나 멈췄을 때만`() {
        assertTrue(NapAlarmPlan.needsReschedule(null, 10_000))
        assertFalse(NapAlarmPlan.needsReschedule(10_000, 11_500))
        assertTrue(NapAlarmPlan.needsReschedule(10_000, 13_000))
        assertTrue(NapAlarmPlan.needsReschedule(10_000, null))
        assertFalse(NapAlarmPlan.needsReschedule(null, null))
    }
}
