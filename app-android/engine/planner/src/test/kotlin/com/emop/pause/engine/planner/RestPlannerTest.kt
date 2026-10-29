package com.emop.pause.engine.planner

import com.emop.pause.engine.fatigue.FatigueFactor
import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.fatigue.FatigueResult
import com.emop.pause.engine.needs.Need
import com.emop.pause.engine.needs.NeedKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RestPlannerTest {
    private fun fatigue(level: FatigueLevel) = FatigueResult(
        score = when (level) { FatigueLevel.LOW -> 20; FatigueLevel.MEDIUM -> 50; FatigueLevel.HIGH -> 80 },
        level = level,
        topContributors = emptyList(),
        activeFactors = setOf(FatigueFactor.CONTINUOUS_DRIVE),
        signalLost = false,
        continuousDriveMinutes = 60,
    )

    // 100 km/h = 1.667 km/분
    private val kmPerMin = 100.0 / 60
    private val stops = listOf(
        RestStop("rs_a", "가람휴게소", StopType.SERVICE_AREA, 20.0, setOf(Facility.EV_SLOW)),
        RestStop("ds_b", "나래졸음쉼터", StopType.DROWSY_SHELTER, 25.0),
        RestStop("rs_c", "다온휴게소", StopType.SERVICE_AREA, 30.0, setOf(Facility.KIDS_ZONE, Facility.EV_FAST, Facility.PET_ZONE)),
        RestStop("rs_d", "라온휴게소", StopType.SERVICE_AREA, 60.0, setOf(Facility.EV_FAST)),
        RestStop("rs_e", "마루휴게소", StopType.SERVICE_AREA, 150.0, setOf(Facility.KIDS_ZONE, Facility.EV_FAST)),
    )

    private fun plan(level: FatigueLevel, needs: List<Need> = emptyList(), traveledKm: Double = 0.0, s: List<RestStop> = stops) =
        RestPlanner.plan(PlannerInput(s, traveledKm, kmPerMin, fatigue(level), needs))

    @Test
    fun `피로 낮고 니즈도 여유 있으면 제안하지 않는다`() {
        assertEquals(PlanDecision.NoNeed, plan(FatigueLevel.LOW, listOf(Need(NeedKind.TOILET, 0.3, 60))))
    }

    @Test
    fun `P0 아이 화장실·충전 니즈가 급하면 키즈존과 급속충전기가 있는 휴게소를 이유와 함께 추천`() {
        val needs = listOf(Need(NeedKind.TOILET, 0.8, 18), Need(NeedKind.CHARGE, 0.5, 45, reachKm = 75.0))
        val d = plan(FatigueLevel.LOW, needs) as PlanDecision.Suggest
        assertEquals("rs_c", d.stop.id)
        assertEquals(18, d.minutesAway)
        assertEquals(
            listOf(Reason.HasFacility(Facility.KIDS_ZONE, NeedKind.TOILET), Reason.HasFacility(Facility.EV_FAST, NeedKind.CHARGE)),
            d.reasons,
        )
        assertEquals("18분 뒤 다온휴게소, 키즈존과 급속충전기가 있어요. 들렀다 갈까요?", RestPhrases.suggestion(d))
    }

    @Test
    fun `P0 충전 니즈가 있으면 급속충전기가 없거나 배터리로 못 가는 곳은 후보에서 뺀다`() {
        val needs = listOf(Need(NeedKind.CHARGE, 0.8, 15, reachKm = 25.0))
        val d = plan(FatigueLevel.LOW, needs)
        // rs_c(30km)는 도달 불가, rs_a는 완속만 → 후보 없음
        assertTrue(d is PlanDecision.NoCandidate, "$d")
    }

    @Test
    fun `충전 가능한 마지막 휴게소가 30분 안이면 긴급도가 낮아도 지금 제안한다`() {
        // 45km 안의 급속충전 휴게소는 rs_c(18분)뿐 → 놓치면 충전할 곳이 없다.
        val needs = listOf(Need(NeedKind.CHARGE, 0.1, 27, reachKm = 45.0))
        val d = plan(FatigueLevel.LOW, needs) as PlanDecision.Suggest
        assertEquals("rs_c", d.stop.id)
        assertEquals(PlanDecision.NoNeed, plan(FatigueLevel.LOW, listOf(Need(NeedKind.CHARGE, 0.1, 80, reachKm = 70.0))))
    }

    @Test
    fun `P0 피로 HIGH면 편의시설과 무관하게 가장 가까운 곳 - 같은 거리면 졸음쉼터`() {
        val d = plan(FatigueLevel.HIGH, listOf(Need(NeedKind.PET_WALK, 0.9, 5)), traveledKm = 5.0) as PlanDecision.Suggest
        assertEquals("rs_a", d.stop.id)
        assertTrue(d.fatigueFirst)
        assertEquals(listOf(Reason.Fatigue), d.reasons)

        val tie = stops + RestStop("ds_t", "바람졸음쉼터", StopType.DROWSY_SHELTER, 20.0)
        val d2 = plan(FatigueLevel.HIGH, traveledKm = 5.0, s = tie) as PlanDecision.Suggest
        assertEquals("ds_t", d2.stop.id)
        assertEquals("9분 뒤 바람졸음쉼터, 잠깐 쉬어 가요. 졸음은 참을수록 위험해요.", RestPhrases.suggestion(d2))
    }

    @Test
    fun `피로 MEDIUM이면 졸음쉼터가 아닌 20분 근처 휴게소`() {
        val d = plan(FatigueLevel.MEDIUM) as PlanDecision.Suggest
        assertEquals(StopType.SERVICE_AREA, d.stop.type)
        assertEquals("rs_c", d.stop.id) // 니즈가 없으면 시간 점수만: 18분이 목표 20분에 가장 가깝다
        assertEquals("18분 뒤 다온휴게소, 잠깐 쉬어 갈까요?", RestPhrases.suggestion(d))
    }

    @Test
    fun `P0 이미 지난 휴게소와 2km 안쪽은 제외한다`() {
        val d = plan(FatigueLevel.HIGH, traveledKm = 21.0) as PlanDecision.Suggest
        assertEquals("ds_b", d.stop.id)
        val d2 = plan(FatigueLevel.HIGH, traveledKm = 24.0) as PlanDecision.Suggest
        assertEquals("rs_c", d2.stop.id)
    }

    @Test
    fun `P1 경로 앞쪽에 후보가 없으면 다음 휴게소 거리로 일반 안내`() {
        val far = stops.dropLast(1) + RestStop("rs_f", "바다휴게소", StopType.SERVICE_AREA, 200.0)
        val d = plan(FatigueLevel.MEDIUM, traveledKm = 61.0, s = far) as PlanDecision.NoCandidate
        assertEquals(139.0, d.nextStopKm!!, 1e-9)
        assertEquals("다음 휴게소까지 139 km, 졸음이 오면 갓길 대신 졸음쉼터를 이용하세요.", RestPhrases.noCandidate(d))
        val none = plan(FatigueLevel.HIGH, traveledKm = 200.0) as PlanDecision.NoCandidate
        assertEquals(null, none.nextStopKm)
    }

    @Test
    fun `P1 계산 100ms 이내 (후보 300개)`() {
        val many = (1..300).map { RestStop("rs_$it", "휴게소$it", StopType.SERVICE_AREA, it * 1.3, setOf(Facility.KIDS_ZONE)) }
        val needs = listOf(Need(NeedKind.TOILET, 0.9, 10), Need(NeedKind.PET_WALK, 0.8, 20))
        repeat(20) { plan(FatigueLevel.MEDIUM, needs, s = many) } // 워밍업
        val t0 = System.nanoTime()
        plan(FatigueLevel.MEDIUM, needs, s = many)
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 100)
    }

    @Test
    fun `조사 - 받침에 따라 과·와, 이·가`() {
        assertEquals("키즈존과 급속충전기", RestPhrases.joinWithAnd(listOf("키즈존", "급속충전기")))
        assertEquals("급속충전기와 수유실", RestPhrases.joinWithAnd(listOf("급속충전기", "수유실")))
        val d = PlanDecision.Suggest(stops[2], 5, 8.0, listOf(Reason.HasFacility(Facility.KIDS_ZONE, NeedKind.TOILET)), 1.0, false)
        assertEquals("5분 뒤 다온휴게소, 키즈존이 있어요. 들렀다 갈까요?", RestPhrases.suggestion(d))
        assertFalse(RestPhrases.waypointFailed(d).isBlank())
    }

    @Test
    fun `T4-13 이유는 실제 점수에 크게 기여한 요인 순서 - 기여 0인 요인은 말하지 않음`() {
        val toilet = Need(NeedKind.TOILET, 0.9, 20)
        val pet = Need(NeedKind.PET_WALK, 0.1, 100)
        val b = ScoringPolicy.DEFAULT.score(10.0, 20.0, fatigueScore = 90, facilityMatch = 1.0, minutesSinceRest = 100)
        val weights = mapOf(NeedKind.TOILET to 0.9, NeedKind.PET_WALK to 0.2)
        val r = RestPlanner.reasonsOf(b, listOf(pet, toilet), weights, 1.1, ScoringPolicy.DEFAULT)
        // 화장실(0.35×0.82=0.29) > 피로(0.2×0.75=0.15) > 반려견(0.06) → 상위 2개
        assertEquals(listOf(Reason.HasFacility(Facility.KIDS_ZONE, NeedKind.TOILET), Reason.Fatigue), r)
        // 맞는 편의시설도 없고 피로 기여도 0 → 피로(쉬어 갈 때)만
        val none = ScoringPolicy.DEFAULT.score(60.0, 20.0, fatigueScore = 0, facilityMatch = 0.0, minutesSinceRest = 100)
        assertEquals(listOf(Reason.Fatigue), RestPlanner.reasonsOf(none, emptyList(), emptyMap(), 0.0, ScoringPolicy.DEFAULT))
    }
}
