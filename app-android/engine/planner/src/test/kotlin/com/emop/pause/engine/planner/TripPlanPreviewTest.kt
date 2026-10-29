package com.emop.pause.engine.planner

import com.emop.pause.engine.needs.Passenger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan 1장 F1 [N][P0] "5시간 입력 → 첫 휴식 예정 시각이 앞당겨짐" — 출발 전 휴식 계획. */
class TripPlanPreviewTest {
    private val kmPerMin = 100.0 / 60
    private val stops = listOf(
        RestStop("a", "A휴게소", StopType.SERVICE_AREA, 60.0, setOf(Facility.EV_SLOW)),
        RestStop("b", "B휴게소", StopType.SERVICE_AREA, 120.0, setOf(Facility.KIDS_ZONE, Facility.EV_FAST)),
        RestStop("c", "C졸음쉼터", StopType.DROWSY_SHELTER, 140.0),
        RestStop("d", "D휴게소", StopType.SERVICE_AREA, 150.0, setOf(Facility.EV_FAST)),
        RestStop("e", "E휴게소", StopType.SERVICE_AREA, 190.0, setOf(Facility.KIDS_ZONE)),
        RestStop("f", "F휴게소", StopType.SERVICE_AREA, 290.0, setOf(Facility.KIDS_ZONE, Facility.EV_FAST, Facility.PET_ZONE)),
        RestStop("g", "G휴게소", StopType.SERVICE_AREA, 330.0, setOf(Facility.EV_FAST)),
    )

    @Test
    fun `혼자 7시간 자고 400km면 2시간 간격 안의 휴게소 2곳`() {
        val p = TripPlanPreview.plan(stops, 400.0, kmPerMin, emptyList(), ev = false, sleepHours = 7f)
        assertEquals(listOf("e", "g"), p.stops.map { it.stop.id })
        assertEquals(0, p.firstAdvancedMin)
        assertTrue(p.stops.all { it.stop.type == StopType.SERVICE_AREA })
    }

    @Test
    fun `P0 수면 5시간이면 첫 휴식을 20분 앞당긴다`() {
        val rested = TripPlanPreview.plan(stops, 400.0, kmPerMin, emptyList(), ev = false, sleepHours = 7f)
        val tired = TripPlanPreview.plan(stops, 400.0, kmPerMin, emptyList(), ev = false, sleepHours = 5f)
        assertEquals(20, tired.firstAdvancedMin)
        assertTrue(tired.stops.first().etaMin < rested.stops.first().etaMin)
    }

    @Test
    fun `5세 아이와 전기차면 90분 안에서 급속충전·키즈존이 있는 곳`() {
        val p = TripPlanPreview.plan(stops, 400.0, kmPerMin, listOf(Passenger.Child(5)), ev = true, sleepHours = null)
        assertEquals(90, p.intervalMin)
        assertEquals("b", p.stops.first().stop.id)
        assertEquals(listOf(Facility.KIDS_ZONE, Facility.EV_FAST), p.stops.first().matched)
    }

    @Test
    fun `짧은 여행은 쉼표 없이`() {
        assertEquals(emptyList<PlannedStop>(), TripPlanPreview.plan(stops, 100.0, kmPerMin, emptyList(), ev = false, sleepHours = 7f).stops)
    }

    @Test
    fun `H 주행 중 다시 계획 - 지금 위치 앞쪽에서, 첫 쉼표는 남은 시간 안에`() {
        // 160km 지점, 아이 화장실 20분 뒤 → 첫 쉼표는 160km 앞 + 20분(약 33km) 안 = E(190km)
        val p = TripPlanPreview.plan(stops, 400.0, kmPerMin, listOf(Passenger.Child(5)), ev = false, sleepHours = null, fromKm = 160.0, firstGapMin = 20)
        val first = p.stops.first().stop
        assertTrue(first.distanceAlongRouteKm > 160.0 && first.distanceAlongRouteKm <= 160.0 + 20 * kmPerMin, "${first.name} ${first.distanceAlongRouteKm}")
        assertTrue(p.stops.all { it.stop.distanceAlongRouteKm > 160.0 })
        assertEquals("e", first.id)
    }

    @Test
    fun `H 다시 계획할 때 남은 시간 안에 휴게소가 없으면 가장 가까운 앞쪽 휴게소`() {
        // 125km, 10분 뒤가 급함 → 10분(약 17km) 안엔 졸음쉼터(140)만 → 가장 가까운 휴게소 D(150)
        val p = TripPlanPreview.plan(stops, 400.0, kmPerMin, listOf(Passenger.Child(5)), ev = false, sleepHours = null, fromKm = 125.0, firstGapMin = 10)
        assertEquals("d", p.stops.first().stop.id)
    }
}
