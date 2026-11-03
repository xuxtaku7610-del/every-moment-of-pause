package com.emop.pause.trip

import com.emop.pause.voice.Reply
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 쉼표 제안 응답 받기 (BLOCKED 11/01 "듣고 있어요" 멈춤, test_plan F2-13·F2-14·F2-15). */
class ReplyGateTest {
    private fun TestScope.gate(listen: suspend (Long) -> String?, watchMs: Long = ReplyGate.VOICE_MS) =
        ReplyGate(listen, backgroundScope, backgroundScope, watchMs = watchMs)

    /** "듣는 중"이 이어진 가장 긴 시간(가상 시간 ms). 상태가 바뀌는 즉시 기록한다. */
    private fun TestScope.longestListening(g: ReplyGate): () -> Long {
        var since = -1L
        var longest = 0L
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            g.state.collect { s ->
                val now = testScheduler.currentTime
                if (since >= 0) longest = maxOf(longest, now - since)
                since = if (s.phase == VoicePhase.LISTENING) now else -1L
            }
        }
        return { if (since >= 0) maxOf(longest, testScheduler.currentTime - since) else longest }
    }

    private val never: suspend (Long) -> String? = { awaitCancellation() }

    @Test
    fun `F2-13 음성 응답이 8초 안에 없으면 버튼으로 넘어가고 버튼 응답을 쓴다`() = runTest {
        val g = gate(never)
        val t = g.begin()
        val answer = async { g.await(t, voice = true) { delay(1_000) } }
        advanceTimeBy(1_001)
        assertEquals(VoicePhase.LISTENING, g.state.value.phase)
        advanceTimeBy(8_000)
        assertEquals(VoicePhase.FAILED, g.state.value.phase)
        g.offer("응")
        assertEquals(Reply.YES, answer.await())
        assertEquals(VoicePhase.IDLE, g.state.value.phase)
    }

    @Test
    fun `F2-13 인식기가 없으면 기다리지 않고 바로 버튼`() = runTest {
        val g = gate({ null })
        val t = g.begin()
        val answer = async { g.await(t, voice = true) {} }
        runCurrent()
        assertEquals(VoicePhase.FAILED, g.state.value.phase)
        g.offer("나중에")
        assertEquals(Reply.LATER, answer.await())
    }

    @Test
    fun `F2-13 버튼도 15초 안에 없으면 UNKNOWN(나중에로 처리)`() = runTest {
        val g = gate(never)
        val t = g.begin()
        val answer = async { g.await(t, voice = false) {} }
        runCurrent()
        assertEquals(VoicePhase.IDLE, g.state.value.phase)
        advanceTimeBy(ReplyGate.BUTTON_MS + 1)
        assertEquals(Reply.UNKNOWN, answer.await())
    }

    @Test
    fun `F2-13 듣는 중 상태가 감시 시간을 넘으면 버튼으로 바꾸고 버튼 응답이 그대로 동작`() = runTest {
        // 감시를 듣기 창보다 짧게 둬서 "듣기 창이 끝나지 않는"(엔진 스레드가 막힌) 경우를 흉내 낸다.
        val g = gate(never, watchMs = 4_000)
        val t = g.begin()
        val answer = async { g.await(t, voice = true) {} }
        advanceTimeBy(4_001)
        assertEquals(VoicePhase.FAILED, g.state.value.phase)
        g.offer("응")
        assertEquals(Reply.YES, answer.await())
    }

    /**
     * 장면 점프 20회 연속: 매번 다른 시점(말하는 중·듣는 중·버튼 대기 중)에 취소하고 초기화한다.
     * - 취소된 차례의 받아쓰기는 늦게(20초 뒤) "응"을 돌려주고, 일부 차례는 취소를 삼킨다(취소가 늦게 닿는 경우).
     * - 점프 뒤에는 늘 IDLE이고 30초를 더 흘려도 지난 차례가 "듣고 있어요"로 되돌리지 못한다.
     * - 20번째 뒤 새 제안은 8초 뒤 버튼으로 넘어가고, 지난 차례의 "응"이 응답으로 새지 않는다.
     */
    @Test
    fun `F2-14 장면 점프 20회 연속 뒤에도 쉼표 추천은 8초 뒤 버튼으로 넘어간다`() = runTest {
        val late: suspend (Long) -> String? = { withContext(NonCancellable) { delay(20_000) }; "응" }
        var listen = late
        val g = gate({ ms -> listen(ms) })
        val longest = longestListening(g)
        repeat(20) { i ->
            val t = g.begin()
            val swallow = i % 3 == 0
            val job = launch {
                g.await(t, voice = true) {
                    if (swallow) try { delay(1_000) } catch (_: CancellationException) {} else delay(1_000)
                }
            }
            // 0.3초(말하는 중) ~ 12.5초(버튼 대기 중) 사이 여러 시점
            advanceTimeBy(300L + (i * 1_637L) % 12_200L)
            job.cancel()
            g.invalidate()
            runCurrent()
            assertEquals(VoicePhase.IDLE, g.state.value.phase, "점프 ${i + 1}회 직후")
            advanceTimeBy(30_000)
            assertEquals(VoicePhase.IDLE, g.state.value.phase, "점프 ${i + 1}회 뒤 30초")
        }
        listen = never
        val t = g.begin()
        val answer = async { g.await(t, voice = true) { delay(1_000) } }
        advanceTimeBy(1_001)
        assertEquals(VoicePhase.LISTENING, g.state.value.phase)
        advanceTimeBy(8_000)
        assertEquals(VoicePhase.FAILED, g.state.value.phase)
        g.offer("나중에") // 화면 "다음 휴게소로" 버튼
        assertEquals(Reply.LATER, answer.await())
        assertEquals(VoicePhase.IDLE, g.state.value.phase)
        assertTrue(longest() <= ReplyGate.VOICE_MS, "듣는 중 최장 ${longest()}ms")
    }

    @Test
    fun `F2-15 지난 차례의 늦은 받아쓰기 콜백은 새 차례 응답·상태를 바꾸지 못한다`() = runTest {
        // 차례 1의 받아쓰기는 취소를 무시하고 5초 뒤 "응"을 돌려준다(늦게 온 인식 콜백).
        var lateFired = false
        val g = gate({ withContext(NonCancellable) { delay(5_000) }; lateFired = true; "응" })
        val t1 = g.begin()
        val first = launch { g.await(t1, voice = true) {} }
        advanceTimeBy(1_000)
        assertEquals(VoicePhase.LISTENING, g.state.value.phase)
        // 장면 점프: 차례 1을 끝내고 바로 다음 쉼표 추천(차례 2, 버튼만).
        first.cancel()
        g.invalidate()
        val t2 = g.begin()
        val second = async { g.await(t2, voice = false) {} }
        advanceTimeBy(6_000)
        assertTrue(lateFired, "늦은 콜백이 실제로 도착해야 시험이 의미 있다")
        assertEquals(VoiceTurn(t2, VoicePhase.IDLE), g.state.value) // 차례 2 상태 그대로
        assertTrue(second.isActive, "늦은 \"응\"이 차례 2 응답으로 새면 안 된다")
        g.offer("나중에")
        assertEquals(Reply.LATER, second.await())
    }
}
