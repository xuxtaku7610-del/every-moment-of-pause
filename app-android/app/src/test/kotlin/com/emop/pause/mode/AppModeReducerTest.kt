package com.emop.pause.mode

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.DriveState.DRIVING
import com.emop.pause.core.model.DriveState.PARKED
import com.emop.pause.core.model.DriveState.RESTRICTED
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.TripPhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** test_plan §6: AppModeReducer 전이와 경계 조건 (architecture §5.2). */
class AppModeReducerTest {
    private class Driver(var state: ModeState = ModeState()) {
        var now = 0L
        fun step(
            drive: DriveState,
            trip: TripPhase = TripPhase.ACTIVE,
            gear: Gear? = Gear.DRIVE,
            speed: Float? = 80f,
            charging: Boolean? = false,
            advanceMs: Long = 1_000,
        ): AppMode {
            now += advanceMs
            state = AppModeReducer.reduce(state, ModeInput(now, drive, trip, gear, speed, charging))
            return state.mode
        }
    }

    @Test
    fun `전체 여행 PREPARE → DRIVING → PAUSE(charging) → DRIVING → ARRIVED`() {
        val d = Driver()
        assertEquals(AppMode.PREPARE, d.step(PARKED, trip = TripPhase.NOT_STARTED, gear = Gear.PARK, speed = 0f))
        assertEquals(AppMode.PREPARE, d.step(PARKED, gear = Gear.PARK, speed = 0f)) // 여행 시작, 아직 정차
        assertEquals(AppMode.DRIVING, d.step(DRIVING))
        assertEquals(AppMode.PAUSE, d.step(PARKED, gear = Gear.PARK, speed = 0f, charging = true))
        assertEquals(PauseCause.CHARGING, d.state.pauseCause)
        assertEquals(AppMode.DRIVING, d.step(DRIVING))
        assertEquals(AppMode.ARRIVED, d.step(PARKED, trip = TripPhase.ENDED, gear = Gear.PARK, speed = 0f))
        assertEquals(AppMode.PREPARE, d.step(PARKED, trip = TripPhase.NOT_STARTED, gear = Gear.PARK, speed = 0f))
    }

    @Test
    fun `충전 아니면 PAUSE 원인은 rest`() {
        val d = Driver()
        d.step(DRIVING)
        assertEquals(AppMode.PAUSE, d.step(PARKED, gear = Gear.PARK, speed = 0f, charging = false))
        assertEquals(PauseCause.REST, d.state.pauseCause)
    }

    @Test
    fun `P0 PARKED인데 기어 N이나 D면 시간이 지나도 DRIVING 유지`() {
        val d = Driver()
        d.step(DRIVING)
        repeat(300) { assertEquals(AppMode.DRIVING, d.step(PARKED, gear = Gear.NEUTRAL, speed = 0f)) }
        repeat(300) { assertEquals(AppMode.DRIVING, d.step(PARKED, gear = Gear.DRIVE, speed = 0f)) }
    }

    @Test
    fun `P0 기어 미지원 차종은 속도 0이 59초면 DRIVING, 60초면 PAUSE`() {
        val d = Driver()
        d.step(DRIVING, gear = null)
        assertEquals(AppMode.DRIVING, d.step(PARKED, gear = null, speed = 0f)) // t0
        assertEquals(AppMode.DRIVING, d.step(PARKED, gear = null, speed = 0f, advanceMs = 59_000))
        assertEquals(AppMode.PAUSE, d.step(PARKED, gear = null, speed = 0f, advanceMs = 1_000))
    }

    @Test
    fun `P0 기어 미지원 차종은 30초에 움직이면 타이머 리셋 후 다시 60초 필요`() {
        val d = Driver()
        d.step(DRIVING, gear = null)
        d.step(PARKED, gear = null, speed = 0f)
        d.step(PARKED, gear = null, speed = 0f, advanceMs = 30_000)
        assertEquals(AppMode.DRIVING, d.step(PARKED, gear = null, speed = 3f))
        d.step(PARKED, gear = null, speed = 0f) // 새 t0
        assertEquals(AppMode.DRIVING, d.step(PARKED, gear = null, speed = 0f, advanceMs = 59_000))
        assertEquals(AppMode.PAUSE, d.step(PARKED, gear = null, speed = 0f, advanceMs = 1_000))
    }

    @Test
    fun `P0 기어 미지원 차종 PAUSE 중 속도가 0이 아니면 DRIVING`() {
        val d = Driver()
        d.step(DRIVING, gear = null)
        d.step(PARKED, gear = null, speed = 0f)
        d.step(PARKED, gear = null, speed = 0f, advanceMs = 60_000)
        assertEquals(AppMode.PAUSE, d.state.mode)
        assertEquals(AppMode.DRIVING, d.step(PARKED, gear = null, speed = 2f))
    }

    @Test
    fun `P0 PAUSE 중 기어 P 해제면 DriveState가 PARKED여도 DRIVING`() {
        val d = Driver()
        d.step(DRIVING)
        d.step(PARKED, gear = Gear.PARK, speed = 0f)
        assertEquals(AppMode.DRIVING, d.step(PARKED, gear = Gear.DRIVE, speed = 0f))
    }

    @Test
    fun `P0 PAUSE 중 DriveState가 DRIVING이나 RESTRICTED면 즉시 DRIVING`() {
        for (restriction in listOf(DRIVING, RESTRICTED)) {
            val d = Driver()
            d.step(DRIVING)
            d.step(PARKED, gear = Gear.PARK, speed = 0f)
            assertEquals(AppMode.DRIVING, d.step(restriction, gear = Gear.PARK, speed = 0f), "$restriction")
        }
    }

    @Test
    fun `속도 신호도 없으면 PAUSE로 가지 않는다`() {
        val d = Driver()
        d.step(DRIVING, gear = null, speed = null)
        assertEquals(AppMode.DRIVING, d.step(PARKED, gear = null, speed = null, advanceMs = 600_000))
    }
}
