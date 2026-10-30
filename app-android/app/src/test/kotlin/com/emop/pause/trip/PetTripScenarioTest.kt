package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.data.BundledRestStops
import com.emop.pause.data.alongRoute
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.motion.TbtRouteShape
import com.emop.pause.engine.motion.GuidanceFromShape
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.PetTripPreview
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.presentation.DemoScenes
import com.emop.pause.sim.Gpx
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** CONCEPT 7장 시연 흐름을 엔진 수준에서 끝까지 재생한다 (pet-trip, test_plan F1·F2·DM). */
class PetTripScenarioTest {
    private val scenario = Scenario.parse(File("../../scenarios/pet-trip.json").readText())
    private val bundled = BundledRestStops.parse(File("../../server/app/data/sample_reststops.json").readText())

    private data class Run(val suggestions: List<Pair<Int, PlanDecision.Suggest>>, val forecasts: List<Pair<Int, MotionForecast>>, val motionAt: Map<Int, Int>)

    private fun replay(guidanceOnly: Boolean = false): Run {
        val route = scenario.route
        val stops = bundled.alongRoute(route.points.map { it.toModel() }, route.totalDistanceM.toDouble())
        val shape = RouteShape.fromLatLng(Gpx.parse(File("../../scenarios/${route.shapeGpx}").readText()))
        // guidanceOnly: GPX 대신 GPX에서 만든 가짜 내비 안내 + 진행 방위만 (UPGRADE 5, 발표 모드 "회전 안내")
        val brain = if (guidanceOnly) TripBrain(PresetStore.fromScenario(scenario.setup), TbtRouteShape(), route.shapeStartM, GuidanceFromShape(shape!!)) { stops }
        else TripBrain(PresetStore.fromScenario(scenario.setup), shape, route.shapeStartM) { stops }
        var mode = ModeState()
        val sug = mutableListOf<Pair<Int, PlanDecision.Suggest>>()
        val fc = mutableListOf<Pair<Int, MotionForecast>>()
        val motion = mutableMapOf<Int, Int>()
        for (t in 0..scenario.durationSec) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            val now = t * 1000L
            mode = AppModeReducer.reduce(mode, ModeInput(now, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            val eta = if (f.remainDistanceM != null && f.remainTimeSec != null) EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!) else null
            val s = brain.tick(BrainInput(now, 540 + t / 60, mode.mode, f.driveState, f.sample, scenario.capabilities, eta, route.totalDistanceM))
            s.speakForecast?.let { fc += t to it; brain.onForecastSpoken(now) }
            s.pending?.let { d ->
                brain.onShown(now)
                if (d is PlanDecision.Suggest) {
                    sug += t to d
                    brain.onAccepted(d.stop)
                }
            }
            if (t % 60 == 0) motion[t] = s.motion.index
        }
        return Run(sug, fc, motion)
    }

    @Test
    fun `F2 첫 쉼표 제안은 쉼표 추천 장면 무렵, 반려견 놀이터가 있는 휴게소`() {
        val run = replay()
        val (t, d) = run.suggestions.first()
        val jump = DemoScenes.JUMPS.first { it.label == "쉼표 추천" }.tSec
        assertTrue(t in jump - 120..jump + 300, "첫 제안 t=$t, 장면 $jump, ${d.stop.name} ${d.reasons} ${d.minutesAway}분")
        assertTrue(Facility.PET_ZONE in d.stop.facilities, d.stop.name)
        assertEquals("횡성휴게소", d.stop.name, "시나리오가 서는 곳과 같아야 시연 흐름이 맞는다")
    }

    @Test
    fun `F1-15 회전 안내만 켠 모드에서도 pet-trip 고갯길 예보가 나온다 (GPX 없이)`() {
        val gpx = replay()
        val guided = replay(guidanceOnly = true)
        val mountainAt = DemoScenes.JUMPS.first { it.label == "고갯길 예보" }.tSec
        val (tg, fg) = guided.forecasts.first { it.first >= mountainAt - 300 }
        val (tx, _) = gpx.forecasts.first { it.first >= mountainAt - 300 }
        // 안내는 앞 3km만 보므로 GPX보다 늦게 말한다(2026-10-30: GPX t=8692, 회전 안내 t=8872 — shape_compare.md).
        // 지켜야 할 것: 굽이에 들어서기 전에(남은 거리 ≥ 알림 최소 300m) 말한다.
        assertTrue(tg > tx, "회전 안내 t=$tg, GPX t=$tx")
        assertTrue(fg.startInM in TripBrain.ANNOUNCE_MIN_AHEAD_M..GuidanceFromShape.HORIZON_M, "시작 ${fg.startInM}m (t=$tg) — 굽이 앞에서, 앞 3km 안")
        println("F1-15 예보 시각: GPX t=$tx, 회전 안내 t=$tg (차이 ${tg - tx}초), 회전 안내 시작 ${fg.startInM.toInt()}m")
        // 고갯길에서 지수가 오르는 것은 같다. 다만 pet-trip 시나리오에는 방위 신호가 없어 "지금" 횡가속도 형상에서 구하므로,
        // 회전 안내 모드의 가정 원호(R10~15m)가 실제 굽이보다 급해 지수가 더 높게 나온다(2026-10-30: GPX 54, 회전 안내 77).
        // 실차는 방위로 지금 값을 재므로 이 차이가 없다 — shape_compare.md.
        val g = guided.motionAt.getValue(9_840)
        val x = gpx.motionAt.getValue(9_840)
        assertTrue(g >= guided.motionAt.getValue(8_820) + 15, "회전 안내 모드에서도 고갯길에서 지수가 오른다")
        assertTrue(g >= x, "가정 원호가 더 급하므로 같거나 높다: GPX $x, 회전 안내 $g")
        println("F1-15 고갯길 뒤 지수: GPX $x, 회전 안내 $g")
    }

    @Test
    fun `F1 고갯길 3km 앞에서 예보를 말하고 고갯길에서 멀미 지수가 오른다`() {
        val run = replay()
        val mountainAt = DemoScenes.JUMPS.first { it.label == "고갯길 예보" }.tSec
        val (t, f) = run.forecasts.first { it.first >= mountainAt - 300 }
        assertTrue(t <= mountainAt + 20, "예보 t=$t")
        assertTrue(f.startInM in 300.0..5000.0, "시작 ${f.startInM}m")
        val before = run.motionAt.getValue(8_820)
        val after = run.motionAt.getValue(9_840)
        assertTrue(after >= before + 15, "고갯길 전 $before → 후 $after")
    }

    @Test
    fun `F2 출발 체크 미리보기의 첫 쉼표 = 주행 중 첫 추천 (같은 플래너)`() {
        val route = scenario.route
        val stops = bundled.alongRoute(route.points.map { it.toModel() }, route.totalDistanceM.toDouble())
        val setup = PresetStore.fromScenario(scenario.setup)
        val kmPerMin = route.totalDistanceM / 1000.0 / (scenario.durationSec / 60.0)
        val preview = PetTripPreview.plan(stops, route.totalDistanceM / 1000.0, kmPerMin, setup.passengers, setup.sleepHours, minuteOfDay = 540)
        val live = replay().suggestions.first().second
        assertEquals(live.stop.name, preview.stops.first().stop.name, "미리보기 ${preview.stops.map { it.stop.name }}")
    }
}
