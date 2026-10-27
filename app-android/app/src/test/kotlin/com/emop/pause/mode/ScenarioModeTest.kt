package com.emop.pause.mode

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.PauseCause
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

/** M1 완료 기준: 시나리오를 1초 간격으로 재생하며 AppModeReducer가 지나가는 모드를 확인한다. */
class ScenarioModeTest {
    private fun replay(name: String): List<Pair<AppMode, PauseCause?>> {
        val scenario = Scenario.parse(File("../../scenarios/$name.json").readText())
        var state = ModeState()
        val visited = mutableListOf<Pair<AppMode, PauseCause?>>()
        for (t in 0..scenario.durationSec) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            state = AppModeReducer.reduce(
                state,
                ModeInput(t * 1000L, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging),
            )
            val entry = state.mode to state.pauseCause
            if (visited.lastOrNull() != entry) visited += entry
        }
        return visited
    }

    @Test
    fun `seoul-busan-family는 PREPARE DRIVING PAUSE(charging) DRIVING ARRIVED 순서`() {
        val modes = replay("seoul-busan-family")
        assertEquals(
            listOf(
                AppMode.PREPARE to null,
                AppMode.DRIVING to null,
                AppMode.PAUSE to PauseCause.CHARGING,
                AppMode.PAUSE to PauseCause.REST, // 충전 완료 후 출발 전까지는 휴식
                AppMode.DRIVING to null,
                AppMode.ARRIVED to null,
            ),
            modes,
        )
    }

    @Test
    fun `P0 정체 구간(PARKED·기어 D)에서는 PAUSE로 가지 않는다`() {
        val scenario = Scenario.parse(File("../../scenarios/seoul-busan-family.json").readText())
        var state = ModeState()
        for (t in 0..9119) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            state = AppModeReducer.reduce(state, ModeInput(t * 1000L, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            if (t in 9000..9119) assertEquals(AppMode.DRIVING, state.mode, "t=$t")
        }
    }

    @Test
    fun `no-signal-car는 신호 대기 30초에는 멈춤으로 가지 않고 졸음쉼터 정차에서만 PAUSE`() {
        val modes = replay("no-signal-car").map { it.first }
        assertEquals(listOf(AppMode.PREPARE, AppMode.DRIVING, AppMode.PAUSE, AppMode.DRIVING, AppMode.ARRIVED), modes)
    }

    @Test
    fun `night-solo는 정차 없이 도착`() {
        val modes = replay("night-solo").map { it.first }
        assertFalse(AppMode.PAUSE in modes)
        assertEquals(listOf(AppMode.PREPARE, AppMode.DRIVING, AppMode.ARRIVED), modes)
    }
}
