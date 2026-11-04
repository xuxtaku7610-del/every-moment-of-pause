package com.emop.pause.trip

import com.emop.pause.core.model.Gear
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** test_plan F11-04~07 (v10-1 4번): 자동 시작 조건, 오늘은 안 타요, 안내 빈도, 조용히 모드. */
class GuidancePolicyTest {
    private val today = LocalDate.of(2026, 11, 4)

    @Test
    fun `F11-04 자동 시작 = 목적지 + D단 + 5km·h 이상, 안 타요 날·P단·목적지 없음·이미 시작이면 안 함`() {
        assertTrue(AutoStart.due(notStarted = true, hasDestination = true, gear = Gear.DRIVE, speedKph = 12f, dayOff = false))
        assertTrue(AutoStart.due(true, true, Gear.DRIVE, 5f, false))
        assertFalse(AutoStart.due(true, true, Gear.DRIVE, 4f, false), "아직 출발 전(서행 미만)")
        assertFalse(AutoStart.due(true, true, Gear.DRIVE, null, false), "속도 신호 없음")
        assertFalse(AutoStart.due(true, false, Gear.DRIVE, 30f, false), "기본 내비에 목적지 없음")
        assertFalse(AutoStart.due(true, true, Gear.PARK, 0f, false))
        assertFalse(AutoStart.due(true, true, Gear.REVERSE, 6f, false))
        assertFalse(AutoStart.due(true, true, Gear.DRIVE, 30f, dayOff = true), "오늘은 안 타요")
        assertFalse(AutoStart.due(notStarted = false, hasDestination = true, gear = Gear.DRIVE, speedKph = 30f, dayOff = false), "이미 여행 중")
    }

    @Test
    fun `F11-05 오늘은 안 타요면 응급 포함 아무 안내도 없고, 다음 날이면 다시 안내`() {
        val p = GuidancePolicy().apply { dayOff = today }
        AnnounceKind.entries.forEach { assertFalse(p.allows(it, 0, today, forecastPeak = 90), it.name) }
        assertTrue(p.isDayOff(today))
        val tomorrow = today.plusDays(1)
        assertFalse(p.isDayOff(tomorrow))
        AnnounceKind.entries.forEach { assertTrue(p.allows(it, 0, tomorrow, forecastPeak = 90), it.name) }
    }

    @Test
    fun `F11-06 빈도 - 많이는 낮음 예보까지, 보통은 보통 이상, 적게는 높음만·주변 추천 끔`() {
        val p = GuidancePolicy()
        fun f(peak: Int) = p.allows(AnnounceKind.FORECAST, 0, today, peak)
        p.level = GuidanceLevel.MORE
        assertTrue(f(30) && f(50) && f(80))
        assertTrue(p.allows(AnnounceKind.NEARBY, 0, today))
        p.level = GuidanceLevel.NORMAL
        assertFalse(f(39))
        assertTrue(f(40) && f(80))
        assertTrue(p.allows(AnnounceKind.NEARBY, 0, today))
        p.level = GuidanceLevel.LESS
        assertFalse(f(69))
        assertTrue(f(70))
        assertFalse(p.allows(AnnounceKind.NEARBY, 0, today), "적게 = 주변 추천 끔")
        // 쉼표 추천·응급은 빈도와 상관없이
        GuidanceLevel.entries.forEach { lv ->
            p.level = lv
            assertTrue(p.allows(AnnounceKind.SUGGEST, 0, today) && p.allows(AnnounceKind.EMERGENCY, 0, today), lv.name)
        }
    }

    @Test
    fun `F11-07 조용히 모드 - 응급·쉼표 추천만 말하고 1시간 뒤·정차 때 풀린다`() {
        val p = GuidancePolicy().apply { level = GuidanceLevel.MORE }
        p.startQuiet(1_000)
        assertTrue(p.allows(AnnounceKind.EMERGENCY, 2_000, today))
        assertTrue(p.allows(AnnounceKind.SUGGEST, 2_000, today))
        assertFalse(p.allows(AnnounceKind.FORECAST, 2_000, today, forecastPeak = 90), "높음 예보도 조용히")
        assertFalse(p.allows(AnnounceKind.NEARBY, 2_000, today))
        assertFalse(p.allows(AnnounceKind.OTHER, 2_000, today))
        // 1시간이 지나면 풀린다
        assertTrue(p.allows(AnnounceKind.FORECAST, 1_000 + GuidancePolicy.QUIET_MS, today, forecastPeak = 90))
        // 정차하면 풀린다
        p.startQuiet(5_000)
        assertTrue(p.isQuiet(6_000))
        p.endQuiet()
        assertFalse(p.isQuiet(6_000))
        assertTrue(p.allows(AnnounceKind.NEARBY, 6_000, today))
    }
}
