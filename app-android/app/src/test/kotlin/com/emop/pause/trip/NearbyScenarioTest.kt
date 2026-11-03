package com.emop.pause.trip

import com.emop.pause.core.model.EtaInfo
import com.emop.pause.data.BundledRestStops
import com.emop.pause.data.alongRoute
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.Reason
import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.core.model.AppMode
import com.emop.pause.presentation.DemoScenes
import com.emop.pause.sim.Gpx
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * UI v9 시연 두 장면을 pet-trip 재생으로 고정한다 (사용자 결정 11/03, test_plan F2-16·F4-10·DM-01).
 * (1) 점프 따라잡기처럼 제안을 모두 "나중에"로 넘기면 약 4,660초 횡성휴게소 쉼표 추천의 이유 첫 줄이 배변
 * (2) 고속도로(최근 5분 평균 80km/h 이상)에서는 주변 추천이 안 뜨고, 목적지 10km 안(일반 도로)에서 처음 뜬다
 */
class NearbyScenarioTest {
    private val scenario = Scenario.parse(File("../../scenarios/pet-trip.json").readText())
    private val bundled = BundledRestStops.parse(File("../../server/app/data/sample_reststops.json").readText())

    @Test
    fun `F2-16 배변 임박 쉼표 - 장면 시각 뒤 횡성휴게소, 이유 첫 줄 배변`() {
        val route = scenario.route
        val stops = bundled.alongRoute(route.points.map { it.toModel() }, route.totalDistanceM.toDouble())
        val shape = RouteShape.fromLatLng(Gpx.parse(File("../../scenarios/${route.shapeGpx}").readText()))
        val brain = TripBrain(PresetStore.fromScenario(scenario.setup), shape, route.shapeStartM) { stops }
        var mode = ModeState()
        val jump = DemoScenes.JUMPS.first { it.label == "배변 임박 쉼표" }.tSec
        var potty: Pair<Int, PlanDecision.Suggest>? = null
        for (t in 0..jump + 120) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            val now = t * 1000L
            mode = AppModeReducer.reduce(mode, ModeInput(now, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            val eta = if (f.remainDistanceM != null && f.remainTimeSec != null) EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!) else null
            val s = brain.tick(BrainInput(now, 540 + t / 60, mode.mode, f.driveState, f.sample, scenario.capabilities, eta, route.totalDistanceM))
            s.speakForecast?.let { brain.onForecastSpoken(now) }
            s.pending?.let { d ->
                brain.onShown(now)
                // 장면 시각 전은 점프 따라잡기(= 나중에), 그 뒤 첫 제안이 화면에 뜨는 것
                if (t > jump && d is PlanDecision.Suggest && potty == null) potty = t to d
                brain.onDeclined(now)
            }
        }
        val (t, d) = potty ?: error("장면 $jump 뒤 120초 안에 쉼표 추천이 없음")
        println("배변 임박 쉼표: t=$t ${d.stop.name} ${d.minutesAway}분 ${d.reasons}")
        assertEquals("횡성휴게소", d.stop.name)
        assertEquals(Reason.PottyDue, d.reasons.first())
        assertTrue(d.minutesAway <= 30)
    }

    @Test
    fun `F4-10 pet-trip 고속도로 구간에는 주변 추천이 없고, 목적지 10km 안 일반 도로에서 처음 뜬다`() {
        val pick = NearbyPick()
        val hw = HighwayGuess()
        var mode = ModeState()
        var first: Int? = null
        for (t in 0..scenario.durationSec) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            mode = AppModeReducer.reduce(mode, ModeInput(t * 1000L, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            hw.add(t * 1000L, f.sample.speedKph)
            if (mode.mode != AppMode.DRIVING) continue
            // 고속도로 구간(1,800~5,000초): 관찰 신고가 있어도 병원 추천을 찾지 않는다
            val reports = if (t >= 2_400) 1 else 0
            val why = pick.due(NearbyInput(t * 1000L, hw.highway(), busy = false, reports = 0, emergency = false, remainKm = f.remainDistanceM?.div(1000.0)))
            if (t in 1_800..5_000) {
                assertEquals(true, hw.highway(), "t=$t 고속도로로 봐야 한다")
                assertEquals(null, pick.due(NearbyInput(t * 1000L, hw.highway(), false, reports, false, f.remainDistanceM?.div(1000.0))), "t=$t")
            }
            if (why != null && first == null) {
                first = t
                assertEquals(NearbyWhy.DESTINATION, why)
            }
        }
        val jump = DemoScenes.JUMPS.first { it.label == "주변 추천" }.tSec
        println("주변 추천 첫 시각: t=$first (장면 $jump)")
        assertTrue(first != null && first in jump..jump + 60, "주변 추천 t=$first, 장면 $jump")
    }
}
