package com.emop.pause.presentation

import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/** test_plan DM-01: 발표 모드 점프 장면이 시나리오에서 기대한 화면(AppMode)에 떨어진다. */
class DemoScenesTest {
    @Test
    fun `DM-01 점프 장면마다 AppMode와 정차 원인이 맞다`() {
        val sc = Scenario.parse(File("../../scenarios/${DemoScenes.SCENARIO}.json").readText())
        for (j in DemoScenes.JUMPS + DemoScenes.HELPER_JUMPS) {
            // SignalRuntime.seek과 같이 처음부터 1초씩 다시 계산한다
            var m = ModeState()
            for (t in 0..j.tSec) {
                val f = ScenarioPlayer.frameAt(sc, t.toDouble())
                m = AppModeReducer.reduce(m, ModeInput(t * 1000L, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging, f.sample.parkingBrakeOn))
            }
            assertEquals(j.expectMode, m.mode, j.label)
            if (j.expectCause != null) assertEquals(j.expectCause, m.pauseCause, j.label)
        }
    }
}
