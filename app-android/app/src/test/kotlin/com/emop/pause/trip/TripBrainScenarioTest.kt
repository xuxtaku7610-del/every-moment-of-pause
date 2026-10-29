package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.data.BundledRestStops
import com.emop.pause.data.alongRoute
import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.needs.EvProfile
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.RestPhrases
import com.emop.pause.engine.planner.StopType
import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** M2 완료 기준: 시나리오 재생 중 제안 → "응" → 경유지 추가 흐름을 엔진 수준에서 확인한다. */
class TripBrainScenarioTest {
    private val bundled = BundledRestStops.parse(File("../../server/app/data/sample_reststops.json").readText())

    private data class Shown(val tSec: Int, val decision: PlanDecision, val mode: AppMode, val fatigue: FatigueLevel)

    /** 시나리오를 1초씩 재생하며 제안이 나올 때마다 [reply]로 응답한다(true=응). */
    private fun replay(name: String, setup: TripSetup, reply: (PlanDecision) -> Boolean): Pair<List<Shown>, TripBrain> {
        val scenario = Scenario.parse(File("../../scenarios/$name.json").readText())
        val stops = bundled.alongRoute(scenario.route.points.map { it.toModel() }, scenario.route.totalDistanceM.toDouble())
        val brain = TripBrain(setup) { stops }
        val (h, m) = scenario.startClock.split(":").map { it.toInt() }
        var mode = ModeState()
        val shown = mutableListOf<Shown>()
        for (t in 0..scenario.durationSec) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            val now = t * 1000L
            mode = AppModeReducer.reduce(mode, ModeInput(now, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            val eta = if (f.remainDistanceM != null && f.remainTimeSec != null) EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!) else null
            val s = brain.tick(BrainInput(now, h * 60 + m + t / 60, mode.mode, f.driveState, f.sample, scenario.capabilities, eta, scenario.route.totalDistanceM))
            val d = s.pending ?: continue
            shown += Shown(t, d, mode.mode, s.fatigue!!.level)
            brain.onShown(now)
            if (d is PlanDecision.Suggest) {
                if (reply(d)) brain.onAccepted(d.stop) else brain.onDeclined(now)
            }
        }
        return shown to brain
    }

    private val family = TripSetup(listOf(Passenger.Child(5), Passenger.Dog()), EvProfile())

    @Test
    fun `P0 seoul-busan-family - 충전 정차 전에 아이·충전 니즈 맞춤 휴게소를 제안하고 응하면 그곳으로 간다`() {
        val (shown, _) = replay("seoul-busan-family", family) { true }
        val first = shown.first()
        val d = first.decision as PlanDecision.Suggest
        assertTrue(first.tSec < 5700, "충전 정차(5700초) 전에 제안: ${first.tSec}")
        assertEquals(AppMode.DRIVING, first.mode)
        assertEquals(StopType.SERVICE_AREA, d.stop.type)
        assertTrue(Facility.EV_FAST in d.stop.facilities, "충전 니즈 → 급속충전기 있는 곳만: ${d.stop.name}")
        assertTrue(d.reasons.isNotEmpty())
        println("첫 제안 t=${first.tSec}s: ${RestPhrases.suggestion(d)}")
        // 수락한 휴게소로 가는 동안은 다시 제안하지 않는다.
        val second = shown.getOrNull(1)
        if (second != null) assertTrue(second.tSec > first.tSec + 10 * 60)
        // 모든 제안은 주행 중에만
        assertTrue(shown.all { it.mode == AppMode.DRIVING })
    }

    @Test
    fun `T4-4 T4-11 나중에 - 10분 뒤 재평가, 같은 휴게소를 지났으면 다음 후보`() {
        val (shown, brain) = replay("seoul-busan-family", family) { false }
        assertTrue(shown.size >= 2)
        // 10분 간격. 예외: 거절 뒤 피로가 HIGH로 올라가면 바로 (COMMERCIAL_READINESS B P0)
        val gaps = shown.zipWithNext().filterNot { (a, b) -> b.fatigue == FatigueLevel.HIGH && a.fatigue != FatigueLevel.HIGH }.map { (a, b) -> b.tSec - a.tSec }
        assertTrue(gaps.all { it >= 10 * 60 }, "간격 $gaps")
        assertTrue(brain.policy.consecutiveDeclines >= 3)
        // 연속 거절 3회 이후 간격 2배 (다음 제안 시점에 피로 HIGH면 예외)
        val afterBackoff = shown.drop(2).zipWithNext().filter { (a, b) -> b.fatigue != FatigueLevel.HIGH && a.fatigue != FatigueLevel.HIGH }.map { (a, b) -> b.tSec - a.tSec }
        assertTrue(afterBackoff.all { it >= 20 * 60 }, "백오프 $afterBackoff")
        // 지난 휴게소를 다시 제안하지 않는다
        val suggestions = shown.mapNotNull { it.decision as? PlanDecision.Suggest }
        suggestions.zipWithNext().forEach { (a, b) ->
            if (b.stop.id == a.stop.id) assertTrue(b.kmAway < a.kmAway)
        }
    }

    @Test
    fun `P0 night-solo - 연속 주행 2시간 근처에서 피로 HIGH, 가장 가까운 곳 우선 제안`() {
        val (shown, _) = replay("night-solo", TripSetup()) { false }
        val high = shown.firstOrNull { it.fatigue == FatigueLevel.HIGH }
        assertNotNull(high, "night-solo에서 HIGH 도달: $shown")
        val d = high!!.decision
        if (d is PlanDecision.Suggest) assertTrue(d.fatigueFirst)
        assertTrue(high.tSec in 60 * 60..130 * 60, "HIGH 제안 시각 ${high.tSec / 60}분")
    }

    @Test
    fun `동승자 없음 - 피로만으로 추천 (니즈 타이머 없음)`() {
        val (shown, brain) = replay("no-signal-car", TripSetup()) { true }
        assertTrue(brain.state.needs!!.needs.isEmpty())
        assertTrue(!brain.state.needs!!.chargeInfoAvailable)
        shown.forEach { assertEquals(AppMode.DRIVING, it.mode) }
    }

    @Test
    fun `I 휴게소 목록 로딩 전에는 제안·일반 안내를 내지 않는다 (점프 직후)`() {
        val scenario = Scenario.parse(File("../../scenarios/seoul-busan-family.json").readText())
        var stops = emptyList<com.emop.pause.engine.planner.RestStop>()
        val brain = TripBrain(family) { stops }
        var mode = ModeState()
        var shown: PlanDecision? = null
        for (t in 0..4500) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            mode = AppModeReducer.reduce(mode, ModeInput(t * 1000L, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            val eta = EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!)
            if (t == 4400) stops = bundled.alongRoute(scenario.route.points.map { it.toModel() }, scenario.route.totalDistanceM.toDouble())
            val s = brain.tick(BrainInput(t * 1000L, 600 + t / 60, mode.mode, f.driveState, f.sample, scenario.capabilities, eta, scenario.route.totalDistanceM))
            if (s.pending != null && shown == null) { shown = s.pending; assertTrue(t >= 4400, "로딩 전 t=$t: ${s.pending}") }
        }
        assertTrue(shown is PlanDecision.Suggest, "목록이 오면 바로 제안: $shown")
    }
}
