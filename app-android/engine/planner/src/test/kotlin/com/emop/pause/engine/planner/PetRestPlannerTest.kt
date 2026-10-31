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

class PetRestPlannerTest {
    private fun fatigue(level: FatigueLevel = FatigueLevel.LOW, cont: Int = 60) = FatigueResult(
        score = when (level) { FatigueLevel.LOW -> 20; FatigueLevel.MEDIUM -> 50; FatigueLevel.HIGH -> 80 },
        level = level,
        topContributors = emptyList(),
        activeFactors = setOf(FatigueFactor.CONTINUOUS_DRIVE),
        signalLost = false,
        continuousDriveMinutes = cont,
    )

    // 100 km/h = 1.667 km/분 → 10km ≈ 6분
    private val kmPerMin = 100.0 / 60
    private val near = RestStop("rs_near", "가람휴게소", StopType.SERVICE_AREA, 15.0, setOf(Facility.PET_ZONE))
    private val nearPlain = RestStop("rs_plain", "나래휴게소", StopType.SERVICE_AREA, 15.0)
    private val far = RestStop("rs_far", "다온휴게소", StopType.SERVICE_AREA, 70.0, setOf(Facility.PET_ZONE, Facility.EV_FAST))
    private val shelter = RestStop("ds_x", "라온졸음쉼터", StopType.DROWSY_SHELTER, 8.0)

    private fun plan(
        stops: List<RestStop>,
        motion: Int = 0,
        level: FatigueLevel = FatigueLevel.LOW,
        needs: List<Need> = emptyList(),
        previous: String? = null,
        traveled: Double = 0.0,
    ) = PetRestPlanner.plan(PetPlannerInput(stops, traveled, kmPerMin, fatigue(level), needs, motion, previousStopId = previous))

    @Test
    fun `쉴 이유가 없으면 제안하지 않는다`() {
        assertEquals(PlanDecision.NoNeed, plan(listOf(near, far), motion = 20))
    }

    @Test
    fun `F2-01 멀미 주의 이상이면 반려견 놀이터가 있는 가까운 곳이 먼 곳보다 높다`() {
        val d = plan(listOf(near, far), motion = 55) as PlanDecision.Suggest
        assertEquals("rs_near", d.stop.id)
        assertTrue(Reason.Motion in d.reasons, d.reasons.toString())
        // 같은 거리라면 놀이터 있는 곳
        val e = plan(listOf(nearPlain, near), motion = 55) as PlanDecision.Suggest
        assertEquals("rs_near", e.stop.id)
    }

    @Test
    fun `F2-02 배변 간격이 다 되면 그 시각에 맞는 곳`() {
        val walkDue = Need(NeedKind.PET_WALK, 0.9, dueInMin = 40)
        val d = plan(listOf(near, far), needs = listOf(walkDue)) as PlanDecision.Suggest
        assertEquals("rs_far", d.stop.id, "40분 뒤(70km) 놀이터 휴게소")
        assertTrue(Reason.PottyDue in d.reasons || d.reasons.any { it is Reason.HasFacility }, d.reasons.toString())
    }

    @Test
    fun `F2-03 이유는 기여도 순 1~2개, 거짓 이유 없음`() {
        val d = plan(listOf(near, far), motion = 60) as PlanDecision.Suggest
        assertTrue(d.reasons.size in 1..2)
        // 놀이터(시설 0.35) > 멀미(가까움 0.85 × 0.6 × 0.2 ≈ 0.10) — 실제 기여도 순
        assertEquals(listOf(Reason.HasFacility(Facility.PET_ZONE, NeedKind.PET_WALK), Reason.Motion), d.reasons)
        // 놀이터 없는 곳을 고르면 놀이터를 이유로 대지 않는다
        val plain = plan(listOf(nearPlain), motion = 60) as PlanDecision.Suggest
        assertFalse(plain.reasons.any { it is Reason.HasFacility }, plain.reasons.toString())
        assertEquals(
            "흔들리는 길을 오래 달렸어요. 9분 뒤 반려견 놀이터가 있는 가람휴게소에서 쉬어 갈까요?",
            RestPhrases.petSuggestion(d, "콩이"),
        )
    }

    @Test
    fun `F2-04 히스테리시스 - 직전 제안을 15% 이상 낫지 않으면 유지`() {
        val a = RestStop("rs_a", "가람휴게소", StopType.SERVICE_AREA, 20.0, setOf(Facility.PET_ZONE))
        val b = RestStop("rs_b", "나래휴게소", StopType.SERVICE_AREA, 24.0, setOf(Facility.PET_ZONE))
        val fresh = plan(listOf(a, b), motion = 50) as PlanDecision.Suggest
        val other = if (fresh.stop.id == "rs_a") "rs_b" else "rs_a"
        val kept = plan(listOf(a, b), motion = 50, previous = other) as PlanDecision.Suggest
        assertEquals(other, kept.stop.id, "비슷한 후보면 직전 제안 유지")
    }

    @Test
    fun `F2-04 나중에 뒤 쿨다운 동안 다시 제안하지 않는다`() {
        val p = SuggestionPolicy()
        p.onDeclined(0, FatigueLevel.LOW)
        assertFalse(p.canSuggest(SuggestionPolicy.RETRY_MS - 1, FatigueLevel.LOW))
        assertTrue(p.canSuggest(SuggestionPolicy.RETRY_MS, FatigueLevel.LOW))
    }

    @Test
    fun `F2-06 운전자 피로 HIGH면 반려견 시설보다 가장 가까운 곳(졸음쉼터 포함)`() {
        val d = plan(listOf(near, far, shelter), motion = 60, level = FatigueLevel.HIGH) as PlanDecision.Suggest
        assertEquals("ds_x", d.stop.id)
        assertTrue(d.fatigueFirst)
        assertEquals(listOf<Reason>(Reason.Fatigue), d.reasons)
    }

    @Test
    fun `앞쪽 후보가 없으면 일반 안내`() {
        val d = plan(listOf(near), motion = 60, traveled = 20.0)
        assertTrue(d is PlanDecision.NoCandidate)
    }

    @Test
    fun `반려견 문구 - 이유 없음, 산책 시간, 피로 우선`() {
        val s = PlanDecision.Suggest(far, 42, 70.0, listOf(Reason.PottyDue), 0.5, false)
        assertEquals("콩이 산책 시간이에요. 42분 뒤 다온휴게소에서 쉬어 갈까요?", RestPhrases.petSuggestion(s, "콩이"))
        val f = PlanDecision.Suggest(shelter, 5, 8.0, listOf(Reason.Fatigue), 1.0, true)
        assertEquals(RestPhrases.suggestion(f), RestPhrases.petSuggestion(f, "콩이"))
        val ev = PlanDecision.Suggest(far, 42, 70.0, listOf(Reason.HasFacility(Facility.EV_FAST, NeedKind.CHARGE)), 0.5, false)
        assertEquals("42분 뒤 급속충전기가 있는 다온휴게소에서 쉬어 갈까요?", RestPhrases.petSuggestion(ev, "콩이"))
    }
}
