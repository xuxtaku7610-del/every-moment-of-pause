package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.RouteInfo
import com.emop.pause.core.model.UxLimits
import com.emop.pause.data.BundledRestStops
import com.emop.pause.data.alongRoute
import com.emop.pause.engine.needs.EvProfile
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.TripPlanPreview
import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.runtime.Snapshot
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import com.emop.pause.trip.BrainInput
import com.emop.pause.trip.TripBrain
import com.emop.pause.trip.TripSetup
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.VoicePhase
import com.emop.pause.uistate.DrivingRules
import com.emop.pause.uistate.UxPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * COMMERCIAL_READINESS D P0 "주행 중 화면: 텍스트 2줄, 버튼 ≤ 2개 — 테스트로 검증", test_plan T4-12·T10-2.
 * 시나리오를 엔진에 흘려 실제로 매핑된 주행 화면 상태가 모든 시점에서 [DrivingRules]를 지키는지 본다.
 */
class DrivingUiScenarioTest {
    private val bundled = BundledRestStops.parse(File("../../server/app/data/sample_reststops.json").readText())

    private fun check(name: String, setup: TripSetup) {
        val sc = Scenario.parse(File("../../scenarios/$name.json").readText())
        val route = RouteInfo(sc.route.destinationName, sc.route.destination.toModel(), sc.route.totalDistanceM, sc.route.points.map { it.toModel() })
        val stops = bundled.alongRoute(route.points, route.totalDistanceM.toDouble())
        val plan = TripPlanPreview.plan(stops, route.totalDistanceM / 1000.0, 100.0 / 60, setup.passengers, setup.ev != null, setup.sleepHours)
        val brain = TripBrain(setup) { stops }
        var mode = ModeState()
        var checked = 0
        for (t in 0..sc.durationSec step 1) {
            val f = ScenarioPlayer.frameAt(sc, t.toDouble())
            mode = AppModeReducer.reduce(mode, ModeInput(t * 1000L, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            val eta = if (f.remainDistanceM != null && f.remainTimeSec != null) EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!) else null
            val st = brain.tick(BrainInput(t * 1000L, sc.startMinute + t / 60, mode.mode, f.driveState, f.sample, sc.capabilities, eta, route.totalDistanceM))
            val asking = st.pending as? PlanDecision.Suggest
            if (st.pending != null) {
                brain.onShown(t * 1000L)
                if (asking != null) brain.onDeclined(t * 1000L)
            }
            if (t % 30 != 0 && asking == null) continue
            if (mode.mode != AppMode.DRIVING && !f.driveState.forcesDrivingUi) continue
            val snap = Snapshot(driveState = f.driveState, eta = eta, mode = mode.mode, route = route, sample = f.sample, uxLimits = UxLimits.approximate(f.driveState))
            val trip = TripUi(
                fatigue = st.fatigue, needs = st.needs, asking = asking, heading = st.heading,
                voice = if (asking != null && t % 60 == 0) VoicePhase.FAILED else VoicePhase.IDLE,
                toast = if (t % 600 == 0) "경로에 죽암휴게소를 넣었어요" else null,
                stops = stops, plan = plan, traveledKm = st.traveledKm, kmPerMin = 100.0 / 60, passengers = setup.passengers,
                sleepHours = setup.sleepHours,
            )
            val l = snap.uxLimits
            val ui = UxPolicy.apply(UiMapper.drive(snap, trip), UxPolicy.Flags(l.noVideo, l.limitStringLength, l.maxStringLength, l.noKeyboard, l.noSetup))
            assertEquals(emptyList<String>(), DrivingRules.violations(ui), "$name t=$t")
            if (f.driveState == DriveState.RESTRICTED) assertTrue(ui.restricted)
            checked++
        }
        assertTrue(checked > 100, "$name 점검 $checked 회")
    }

    @Test
    fun `T4-12 T10-2 seoul-busan-family 주행 화면 전 구간이 제한을 지킨다`() =
        check("seoul-busan-family", TripSetup(listOf(Passenger.Child(5), Passenger.Dog), EvProfile(), sleepHours = 6f))

    @Test
    fun `T4-12 T10-2 night-solo 주행 화면 전 구간이 제한을 지킨다`() = check("night-solo", TripSetup())

    @Test
    fun `T4-12 T10-2 no-signal-car 주행 화면 전 구간이 제한을 지킨다`() = check("no-signal-car", TripSetup(listOf(Passenger.Child(2))))
}
