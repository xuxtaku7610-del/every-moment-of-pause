package com.emop.pause.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** COMMERCIAL_READINESS D P0: CarUxRestrictions 플래그 해석 (비트값은 android.car.jar 상수와 같다). */
class UxLimitsTest {
    @Test
    fun `T10-1 플래그 비트`() {
        val l = UxLimits(UxLimits.NO_VIDEO or UxLimits.LIMIT_STRING_LENGTH, 80)
        assertTrue(l.noVideo)
        assertTrue(l.limitStringLength)
        assertFalse(l.noKeyboard)
        assertFalse(l.noSetup)
        assertEquals(80, l.maxStringLength)
        val full = UxLimits(UxLimits.FULLY_RESTRICTED)
        assertTrue(full.noVideo && full.noKeyboard && full.noSetup && full.limitStringLength && full.noTextMessage)
    }

    @Test
    fun `시뮬레이터 근사 - 정차는 제한 없음, 주행은 영상·키보드·설정 금지`() {
        assertEquals(UxLimits.NONE, UxLimits.approximate(DriveState.PARKED))
        val d = UxLimits.approximate(DriveState.DRIVING)
        assertTrue(d.noVideo && d.noKeyboard && d.noSetup && d.limitStringLength)
        assertEquals(UxLimits.FULLY_RESTRICTED, UxLimits.approximate(DriveState.RESTRICTED).active)
    }
}
