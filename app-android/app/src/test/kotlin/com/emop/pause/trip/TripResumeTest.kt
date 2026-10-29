package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import com.emop.pause.engine.needs.Passenger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** COMMERCIAL_READINESS A P1: 앱 프로세스가 죽었다 살아나도 여행 상태 복원. */
class TripResumeTest {
    private val setup = TripSetup(listOf(Passenger.Child(5)))

    private fun drive(brain: TripBrain, fromSec: Int, toSec: Int) {
        for (t in fromSec..toSec) {
            brain.tick(
                BrainInput(t * 1000L, 600, AppMode.DRIVING, DriveState.DRIVING, SignalSample(t * 1000L, speedKph = 100f), setOf(Signal.SPEED), null, null),
            )
        }
    }

    @Test
    fun `T6-18 저장한 피로·니즈를 새 엔진에 되살리면 같은 값에서 이어 간다`() {
        val before = TripBrain(setup) { emptyList() }
        drive(before, 0, 3600)
        val saved = TripResume.decode(before.save(3_600_000, "route-a").encode())!!

        val after = TripBrain(setup) { emptyList() }
        after.restore(saved)
        drive(after, 10_000, 10_000) // 복원 직후 첫 입력은 dt 0
        assertEquals(before.state.fatigue!!.continuousDriveMinutes, after.state.fatigue!!.continuousDriveMinutes)
        assertEquals(before.state.needs!!.needs.single().dueInMin, after.state.needs!!.needs.single().dueInMin)
    }

    @Test
    fun `3시간 안·같은 경로일 때만 이어서 안내`() {
        val r = TripBrain(setup) { emptyList() }.save(0, "route-a")
        assertTrue(r.usable(60 * 60_000L, "route-a"))
        assertTrue(r.usable(60 * 60_000L, null)) // 내비 경로를 아직 못 받음
        assertFalse(r.usable(60 * 60_000L, "route-b"))
        assertFalse(r.usable(TripResume.MAX_AGE_MS + 1, "route-a"))
        assertFalse(r.usable(-1, "route-a")) // 시계가 거꾸로
    }

    @Test
    fun `경로 키에 구분자와 좌표가 있어도 해시만 저장하고 되읽힌다`() {
        val key = "LatLng(lat=0.0, lng=0.0)|37.4127,127.0947;0.0000,0.0000"
        val r = TripBrain(setup) { emptyList() }.save(0, key)
        val raw = r.encode()
        assertFalse("37.4127" in raw)
        val back = TripResume.decode(raw)!!
        assertEquals(r, back)
        assertTrue(back.usable(1000, key))
        assertFalse(back.usable(1000, key + "x"))
    }

    @Test
    fun `깨진 저장값은 복원하지 않는다`() {
        assertNull(TripResume.decode(null))
        assertNull(TripResume.decode("v0|1|x"))
        assertNull(TripResume.decode("v1|abc|||||||"))
        val ok = TripBrain(TripSetup()) { emptyList() }.save(5, null)
        assertEquals(ok, TripResume.decode(ok.encode()))
    }
}
