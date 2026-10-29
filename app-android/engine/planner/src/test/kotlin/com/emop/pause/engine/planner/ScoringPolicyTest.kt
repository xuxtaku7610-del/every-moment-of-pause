package com.emop.pause.engine.planner

import com.emop.pause.engine.fatigue.FatigueFactor
import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.fatigue.FatigueResult
import com.emop.pause.engine.needs.Need
import com.emop.pause.engine.needs.NeedKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** COMMERCIAL_READINESS B P0: 가중치 한 곳(ScoringPolicy), 히스테리시스 15%, 제안 간격. */
class ScoringPolicyTest {
    private val p = ScoringPolicy.DEFAULT

    @Test
    fun `T4-2 기본 가중치를 고정한다`() {
        assertEquals(0.35, p.timingWeight)
        assertEquals(0.2, p.fatigueWeight)
        assertEquals(0.35, p.facilityWeight)
        assertEquals(0.1, p.gapWeight)
        assertEquals(0.15, p.hysteresis)
        assertEquals(1.0, p.timingWeight + p.fatigueWeight + p.facilityWeight + p.gapWeight, 1e-9)
    }

    @Test
    fun `T4-2 항목별 점수 - 목표 시각에 맞고 편의시설을 다 갖춘 곳이 최고`() {
        val perfect = p.score(minutesAway = 20.0, targetMin = 20.0, fatigueScore = 50, facilityMatch = 1.0, minutesSinceRest = 90)
        assertEquals(1.0, perfect.timing, 1e-9)
        assertEquals(1.0, perfect.facility, 1e-9)
        assertEquals(0.0, perfect.gapPenalty, 1e-9)
        // fatigue = (1 - 20/60) * 0.5
        assertEquals(1.0 / 3, perfect.fatigue, 1e-9)
        assertEquals(0.35 + 0.35 + 0.2 / 3, perfect.total, 1e-9)

        val late = p.score(65.0, 20.0, 50, 1.0, 90)
        assertEquals(0.0, late.timing, 1e-9)
        assertTrue(late.total < perfect.total)
    }

    @Test
    fun `T4-2 피로가 높을수록 가까운 곳 가점이 커진다`() {
        val nearTired = p.score(10.0, 30.0, 65, 0.0, 90).fatigue
        val nearFresh = p.score(10.0, 30.0, 10, 0.0, 90).fatigue
        val farTired = p.score(50.0, 30.0, 65, 0.0, 90).fatigue
        assertTrue(nearTired > nearFresh)
        assertTrue(nearTired > farTired)
    }

    @Test
    fun `T4-2 방금 쉬었으면 간격 페널티`() {
        val justRested = p.score(5.0, 5.0, 40, 1.0, minutesSinceRest = 0)
        assertEquals(1.0 - 5.0 / 45, justRested.gapPenalty, 1e-9)
        assertEquals(0.0, p.score(5.0, 5.0, 40, 1.0, minutesSinceRest = 40).gapPenalty, 1e-9)
    }

    @Test
    fun `T4-4 히스테리시스 - 15% 미만으로 나은 후보는 직전 제안을 이기지 못한다`() {
        val ranked = listOf("a" to 0.60, "b" to 0.68)
        assertEquals("a", p.stabilize(ranked, previous = "a")) // 0.68 < 0.60 × 1.15 = 0.69
        assertEquals("b", p.stabilize(listOf("a" to 0.60, "b" to 0.70), previous = "a"))
        assertEquals("b", p.stabilize(ranked, previous = null))
        // 직전 제안이 후보에서 빠졌으면(지나침) 최고 후보
        assertEquals("b", p.stabilize(ranked, previous = "gone"))
    }

    // ───────── 플래너에 적용 ─────────

    private fun fatigue(score: Int, level: FatigueLevel, continuous: Int = 90) =
        FatigueResult(score, level, emptyList(), setOf(FatigueFactor.CONTINUOUS_DRIVE), false, continuous)

    private val kmPerMin = 100.0 / 60

    @Test
    fun `T4-4 나중에 뒤 재평가 - 조금 나은 곳이 생겨도 같은 휴게소를 다시 제안`() {
        // 아이 화장실 25분 뒤: A(약 24분, 키즈존)와 B(약 27분, 키즈존) — B가 아주 조금 낫거나 비슷.
        val stops = listOf(
            RestStop("a", "옥산휴게소", StopType.SERVICE_AREA, 40.0, setOf(Facility.KIDS_ZONE)),
            RestStop("b", "죽암휴게소", StopType.SERVICE_AREA, 44.0, setOf(Facility.KIDS_ZONE)),
        )
        val needs = listOf(Need(NeedKind.TOILET, dueInMin = 26, urgency = 0.8))
        fun plan(prev: String?) = RestPlanner.plan(PlannerInput(stops, 0.0, kmPerMin, fatigue(30, FatigueLevel.LOW), needs, previousStopId = prev)) as PlanDecision.Suggest
        val fresh = plan(null).stop.id
        val other = if (fresh == "a") "b" else "a"
        assertEquals(other, plan(other).stop.id)
    }

    @Test
    fun `T4-4 직전 제안을 이미 지났으면 다음 후보`() {
        val stops = listOf(
            RestStop("a", "옥산휴게소", StopType.SERVICE_AREA, 40.0, setOf(Facility.KIDS_ZONE)),
            RestStop("b", "죽암휴게소", StopType.SERVICE_AREA, 70.0, setOf(Facility.KIDS_ZONE)),
        )
        val needs = listOf(Need(NeedKind.TOILET, dueInMin = 10, urgency = 0.9))
        val d = RestPlanner.plan(PlannerInput(stops, 45.0, kmPerMin, fatigue(30, FatigueLevel.LOW), needs, previousStopId = "a")) as PlanDecision.Suggest
        assertEquals("b", d.stop.id)
    }

    @Test
    fun `T4-11 거절 뒤 피로가 HIGH로 오르면 기다리지 않는다`() {
        val sp = SuggestionPolicy()
        sp.onDeclined(0, FatigueLevel.MEDIUM)
        assertEquals(false, sp.canSuggest(60_000, FatigueLevel.MEDIUM))
        assertEquals(true, sp.canSuggest(60_000, FatigueLevel.HIGH))
        // HIGH에서 거절했으면 HIGH여도 10분은 기다린다(반복 경고 방지)
        val sp2 = SuggestionPolicy()
        sp2.onDeclined(0, FatigueLevel.HIGH)
        assertEquals(false, sp2.canSuggest(60_000, FatigueLevel.HIGH))
        assertEquals(true, sp2.canSuggest(SuggestionPolicy.RETRY_MS, FatigueLevel.HIGH))
    }
}
