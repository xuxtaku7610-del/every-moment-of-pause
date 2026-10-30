package com.emop.pause.engine.pet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** UPGRADE 2 (test_plan F3-12·F6-05): 실내 온도 단계 — 값이 없으면 지어내지 않고 계절 문구만. */
class CabinHeatTest {
    @Test
    fun `F6-05 26도 이상 주의, 30도 이상 강한 경고, 값이 없으면 단계도 없음`() {
        assertEquals(CabinHeat.Level.OK, CabinHeat.level(24.0f))
        assertEquals(CabinHeat.Level.CAUTION, CabinHeat.level(26.0f))
        assertEquals(CabinHeat.Level.CAUTION, CabinHeat.level(29.9f))
        assertEquals(CabinHeat.Level.STRONG, CabinHeat.level(30.0f))
        assertNull(CabinHeat.level(null))
    }

    @Test
    fun `F3-12 대기 중 2도 이상 오르거나 주의 온도에 닿으면 오르는 중, 모르면 아님`() {
        assertTrue(CabinHeat.rising(22.0f, 24.0f))
        assertTrue(CabinHeat.rising(25.0f, 26.0f))
        assertFalse(CabinHeat.rising(22.0f, 23.5f))
        assertTrue(CabinHeat.rising(null, 27.0f)) // 시작 값을 몰라도 이미 주의 온도면 알린다
        assertFalse(CabinHeat.rising(24.0f, null))
    }

    @Test
    fun `F3-12 온도를 모르면 6~9월에만 일반 문구, 숫자 없음`() {
        assertEquals("여름에는 차 안이 빨리 더워져요", CabinHeat.seasonNote(null, 7))
        assertNull(CabinHeat.seasonNote(null, 11))
        assertNull(CabinHeat.seasonNote(24.0f, 7)) // 값이 있으면 일반 문구 대신 숫자
    }

    @Test
    fun `F3-12 대기 모드는 처음 받은 온도를 기억하고 오르면 표시`() {
        val w = WaitMode()
        val parked = WaitInput(0, com.emop.pause.core.model.DriveState.PARKED, com.emop.pause.core.model.Gear.PARK, cabinTempC = null)
        w.start(parked)
        w.tick(parked.copy(nowMs = 1_000, cabinTempC = 23.0f))
        assertEquals(23.0f, w.status.cabinTempStartC)
        assertFalse(w.status.heatRising)
        w.tick(parked.copy(nowMs = 60_000, cabinTempC = 25.5f))
        assertEquals(23.0f, w.status.cabinTempStartC)
        assertTrue(w.status.heatRising)
    }
}
