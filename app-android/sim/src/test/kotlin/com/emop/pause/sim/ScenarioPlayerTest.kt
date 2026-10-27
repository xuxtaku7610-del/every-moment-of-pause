package com.emop.pause.sim

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.TripPhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

/** test_plan §8 F8 시뮬레이터. */
class ScenarioPlayerTest {
    private fun load(name: String) = Scenario.parse(File("../../scenarios/$name.json").readText())

    @Test
    fun `시나리오 3종을 읽는다`() {
        for (name in listOf("seoul-busan-family", "night-solo", "no-signal-car")) {
            val s = load(name)
            assertEquals(name, s.name)
            assertTrue(s.durationSec > 0)
        }
    }

    @Test
    fun `숫자 신호는 키프레임 사이를 선형 보간하고 나머지는 이전 값을 유지한다`() {
        val s = load("seoul-busan-family")
        val f = ScenarioPlayer.frameAt(s, 210.0) // t120 30km/h → t300 100km/h 중간
        assertEquals(65f, f.sample.speedKph!!, 0.01f)
        assertEquals(Gear.DRIVE, f.sample.gear)
        assertEquals(DriveState.DRIVING, f.driveState)
        assertEquals(TripPhase.ACTIVE, f.trip)
    }

    @Test
    fun `E 지원하지 않는 신호는 null`() {
        // test_plan §2 [E][P0] no-signal-car: 공조·배터리·기어 신호 없음
        val f = ScenarioPlayer.frameAt(load("no-signal-car"), 600.0)
        assertNotNull(f.sample.speedKph)
        assertNull(f.sample.gear)
        assertNull(f.sample.hvacTempC)
        assertNull(f.sample.batterySocPercent)
        assertNull(f.sample.charging)
    }

    @Test
    fun `E 잘못된 JSON은 예외`() {
        assertThrows<Exception> { Scenario.parse("{\"name\": 1}") }
        assertThrows<IllegalArgumentException> {
            Scenario.parse(
                """{"name":"x","capabilities":[],"route":{"destinationName":"a","destination":{"lat":0,"lng":0},"totalDistanceM":1},
                "timeline":[{"t":10},{"t":5}]}""",
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `N 배속 1x 10x 100x로 시간이 진행되고 일시정지·점프가 된다`() = runTest {
        val scope = TestScope(testScheduler)
        val player = ScenarioPlayer(load("seoul-busan-family"), scope, tickMs = 200)
        fun advance(ms: Long) {
            scope.advanceTimeBy(ms)
            scope.runCurrent()
        }

        player.play()
        advance(1_000)
        assertEquals(1.0, player.frame.value.tSec, 1e-6)

        player.setSpeed(10)
        advance(1_000)
        assertEquals(11.0, player.frame.value.tSec, 1e-6)

        player.setSpeed(100)
        advance(1_000)
        assertEquals(111.0, player.frame.value.tSec, 1e-6)

        player.pause()
        advance(5_000)
        assertEquals(111.0, player.frame.value.tSec, 1e-6)
        assertFalse(player.playing.value)

        player.seek(5_700.0)
        assertEquals(true, player.frame.value.sample.charging)
        assertThrows<IllegalArgumentException> { player.setSpeed(3) }
    }
}
