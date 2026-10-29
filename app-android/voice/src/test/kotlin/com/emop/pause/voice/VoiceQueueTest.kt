package com.emop.pause.voice

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VoiceQueueTest {
    // ───────── 의도 ─────────

    @Test
    fun `F3-01 잠깐 다녀올게 계열은 대기 모드 시작`() {
        for (t in listOf("잠깐 다녀올게", "콩이야 금방 올게", "화장실 갔다 올게", "대기 모드 켜 줘")) {
            assertEquals(Utterance.StartWait, VoiceIntent.parse(t), t)
        }
    }

    @Test
    fun `F2-05 응답 단어는 그대로 응·나중에, 여기서 쉴게요는 응`() {
        assertEquals(Utterance.Answer(Reply.YES), VoiceIntent.parse("응 들를게"))
        assertEquals(Utterance.Answer(Reply.YES), VoiceIntent.parse("여기서 쉴게요"))
        assertEquals(Utterance.Answer(Reply.LATER), VoiceIntent.parse("아니 다음에"))
        assertEquals(Utterance.Answer(Reply.UNKNOWN), VoiceIntent.parse(""))
        assertEquals(Utterance.Answer(Reply.UNKNOWN), VoiceIntent.parse(null))
    }

    // ───────── 큐 ─────────

    private class FakeOut : SpeechOutput {
        val spoken = mutableListOf<String>()
        val stopped = mutableListOf<String>()
        var speaking: String? = null
        override suspend fun speak(text: String) {
            speaking = text
            try {
                delay(1_000)
                spoken += text
            } finally {
                speaking = null
            }
        }
        override fun stop() {
            speaking?.let { stopped += it }
        }
    }

    private class FakeFocus(var grant: Boolean = true) : AudioFocus {
        val requests = mutableListOf<FocusKind>()
        override fun request(kind: FocusKind): Boolean {
            requests += kind
            return grant
        }
        override fun abandon() = Unit
        override var onLoss: (() -> Unit)? = null
    }

    @Test
    fun `T5-9 우선순위 - 기다리는 문장은 안전 경고, 제안, 안내, 배경음 순`() = runTest {
        val out = FakeOut()
        val q = SpeechQueue(out, FakeFocus(), this, clock = { testScheduler.currentTime })
        val first = async { q.say("배경 1", SpeechPriority.BACKGROUND) }
        runCurrent()
        // 배경음이 말하는 동안 세 문장이 들어온다 (낮은 것부터)
        val kid = async { q.say("안내", SpeechPriority.INFO) }
        val sug = async { q.say("제안", SpeechPriority.SUGGESTION) }
        val safe = async { q.say("안전", SpeechPriority.SAFETY) }
        advanceUntilIdle()
        assertFalse(first.await(), "배경음은 더 급한 말에 끊긴다")
        assertEquals(listOf("배경 1"), out.stopped)
        assertEquals(listOf("안전", "제안", "안내"), out.spoken)
        assertTrue(kid.await() && sug.await() && safe.await())
    }

    @Test
    fun `T5-9 같은 우선순위는 먼저 온 순서, 낮은 순위는 지금 말을 끊지 않는다`() = runTest {
        val out = FakeOut()
        val q = SpeechQueue(out, FakeFocus(), this, clock = { testScheduler.currentTime })
        val a = async { q.say("제안 A", SpeechPriority.SUGGESTION) }
        runCurrent()
        val b = async { q.say("제안 B", SpeechPriority.SUGGESTION) }
        val c = async { q.say("안내", SpeechPriority.INFO) }
        advanceUntilIdle()
        assertEquals(listOf("제안 A", "제안 B", "안내"), out.spoken)
        assertTrue(a.await() && b.await() && c.await())
        assertTrue(out.stopped.isEmpty())
    }

    @Test
    fun `C P0 제안·안내는 잠깐 덕킹 포커스, 배경음은 미디어 포커스`() = runTest {
        val focus = FakeFocus()
        val q = SpeechQueue(FakeOut(), focus, this, clock = { testScheduler.currentTime })
        q.say("제안", SpeechPriority.SUGGESTION)
        q.say("배경", SpeechPriority.BACKGROUND)
        assertEquals(listOf(FocusKind.TRANSIENT_DUCK, FocusKind.MEDIA), focus.requests)
    }

    @Test
    fun `T5-9 내비 안내가 포커스를 잡고 있으면 기다렸다가 말하고, 너무 오래면 버린다`() = runTest {
        val out = FakeOut()
        val focus = FakeFocus(grant = false)
        val q = SpeechQueue(out, focus, this, focusRetryMs = 500, maxWaitMs = 10_000, clock = { testScheduler.currentTime })
        val r = async { q.say("제안", SpeechPriority.SUGGESTION) }
        advanceTimeBy(3_000)
        assertTrue(out.spoken.isEmpty())
        focus.grant = true // 내비 안내 끝
        advanceUntilIdle()
        assertTrue(r.await())
        assertEquals(listOf("제안"), out.spoken)

        focus.grant = false
        val dropped = async { q.say("늦은 제안", SpeechPriority.SUGGESTION) }
        advanceUntilIdle()
        assertFalse(dropped.await())
        // 안전 경고는 포커스를 못 받아도 말한다
        val safety = async { q.say("안전", SpeechPriority.SAFETY) }
        advanceUntilIdle()
        assertTrue(safety.await())
        assertEquals(listOf("제안", "안전"), out.spoken)
    }

    @Test
    fun `T5-9 말하는 중 포커스를 잃으면 멈췄다가 한 번 다시 말한다`() = runTest {
        val out = FakeOut()
        val focus = FakeFocus()
        val q = SpeechQueue(out, focus, this, clock = { testScheduler.currentTime })
        val r = async { q.say("제안", SpeechPriority.SUGGESTION) }
        runCurrent()
        advanceTimeBy(300)
        focus.onLoss?.invoke() // 내비 안내 시작
        advanceUntilIdle()
        assertTrue(r.await())
        assertEquals(listOf("제안"), out.stopped)
        assertEquals(listOf("제안"), out.spoken)
    }

    @Test
    fun `clear는 기다리는 문장을 모두 버린다`() = runTest {
        val out = FakeOut()
        val q = SpeechQueue(out, FakeFocus(), this, clock = { testScheduler.currentTime })
        val a = async { q.say("하나", SpeechPriority.SUGGESTION) }
        val b = async { q.say("둘", SpeechPriority.SUGGESTION) }
        runCurrent()
        q.clear()
        advanceUntilIdle()
        assertFalse(a.await())
        assertFalse(b.await())
        assertTrue(out.spoken.isEmpty())
    }
}
