package com.emop.pause.core.vehicle

import com.emop.pause.core.model.Gear
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** AAOS VehicleProperty 값 변환 (값은 android.car.jar의 상수와 같다: GEAR_PARK=4, STATE_CHARGING=1). */
class AaosMappingTest {
    @Test
    fun `속도 m·s를 km·h 절댓값으로`() {
        assertEquals(100f, AaosMapping.kph(27.7778f), 0.01f)
        assertEquals(18f, AaosMapping.kph(-5f), 0.01f)
    }

    @Test
    fun `기어 비트값`() {
        assertEquals(Gear.PARK, AaosMapping.gear(4))
        assertEquals(Gear.REVERSE, AaosMapping.gear(2))
        assertEquals(Gear.NEUTRAL, AaosMapping.gear(1))
        assertEquals(Gear.DRIVE, AaosMapping.gear(8))
        assertEquals(Gear.DRIVE, AaosMapping.gear(16)) // 1단
        assertEquals(Gear.UNKNOWN, AaosMapping.gear(0))
    }

    @Test
    fun `배터리 Wh를 퍼센트로, 용량 모르면 null`() {
        assertEquals(53, AaosMapping.socPercent(41_340f, 78_000f))
        assertEquals(100, AaosMapping.socPercent(80_000f, 78_000f))
        assertNull(AaosMapping.socPercent(41_340f, null))
        assertNull(AaosMapping.socPercent(null, 78_000f))
    }

    @Test
    fun `충전 상태`() {
        assertEquals(true, AaosMapping.charging(1))
        assertEquals(false, AaosMapping.charging(2))
        assertEquals(false, AaosMapping.charging(3))
        assertNull(AaosMapping.charging(0))
    }
}
