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
    fun `T5-1 아직 멀었어 계열은 남은 시간 질문, 반말이면 아이 말투`() {
        for (t in listOf("아직 멀었어?", "얼마나 남았어", "언제 도착해?", "엄마 다 왔어?")) {
            assertEquals(Utterance.AskEta(Tone.KID), VoiceIntent.parse(t), t)
        }
    }

    @Test
    fun `T5-6 존댓말·시간 명사면 어른 말투`() {
        for (t in listOf("얼마나 남았어요?", "도착 시간 알려 줘", "남은 거리 몇 km야", "언제 도착합니까")) {
            assertEquals(Utterance.AskEta(Tone.ADULT), VoiceIntent.parse(t), t)
        }
    }

    @Test
    fun `T4-3 응답 단어는 그대로 응·나중에`() {
        assertEquals(Utterance.Answer(Reply.YES), VoiceIntent.parse("응 들를게"))
        assertEquals(Utterance.Answer(Reply.LATER), VoiceIntent.parse("아니 다음에"))
        assertEquals(Utterance.Answer(Reply.UNKNOWN), VoiceIntent.parse(""))
        assertEquals(Utterance.Answer(Reply.UNKNOWN), VoiceIntent.parse(null))
    }

    // ───────── 답변 ─────────

    private val facts = EtaFacts("부산", remainMin = 130, remainKm = 182.4, nextStopName = "죽암휴게소", nextStopMin = 40)

    @Test
    fun `T5-1 아이 답 - 라디오 편수가 있으면 편수로`() {
        assertEquals("라디오 2편만 들으면 부산 도착!", EtaAnswer.answer(facts.copy(episodesLeft = 2), Tone.KID))
    }

    @Test
    fun `T5-1 아이 답 - 앞에 쉼표가 있으면 쉬는 곳, 없으면 남은 시간`() {
        assertEquals("40분 뒤에 죽암휴게소에서 한 번 쉬고 가자. 조금만 기다려 줘!", EtaAnswer.answer(facts, Tone.KID))
        assertEquals("부산까지 2시간 10분 남았어. 조금만 더 힘내자!", EtaAnswer.answer(facts.copy(nextStopName = null, nextStopMin = null), Tone.KID))
        assertEquals("거의 다 왔어! 8분만 기다리면 부산이야.", EtaAnswer.answer(facts.copy(remainMin = 8), Tone.KID))
        assertEquals("거의 다 왔어! 5분만 기다리면 서울이야.", EtaAnswer.answer(facts.copy(destination = "서울", remainMin = 5), Tone.KID))
    }

    @Test
    fun `T5-6 어른 답 - 시간·거리 중심, 다음 쉼표`() {
        assertEquals("부산까지 2시간 10분, 182km 남았어요. 다음 쉼표는 40분 뒤 죽암휴게소예요.", EtaAnswer.answer(facts, Tone.ADULT))
        assertEquals(
            "부산까지 2시간 10분, 182km 남았어요. 다음 쉼표는 40분 뒤 오산 부근 졸음쉼터예요.",
            EtaAnswer.answer(facts.copy(nextStopName = "오산 부근 졸음쉼터"), Tone.ADULT),
        )
        // 쉼표가 도착보다 뒤면 말하지 않는다
        assertEquals("부산까지 30분, 42km 남았어요.", EtaAnswer.answer(facts.copy(remainMin = 30, remainKm = 42.0), Tone.ADULT))
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
    fun `T5-9 우선순위 - 기다리는 문장은 안전 경고, 제안, 아이 답변, 라디오 순`() = runTest {
        val out = FakeOut()
        val q = SpeechQueue(out, FakeFocus(), this, clock = { testScheduler.currentTime })
        val first = async { q.say("라디오 1", SpeechPriority.RADIO) }
        runCurrent()
        // 라디오가 말하는 동안 세 문장이 들어온다 (낮은 것부터)
        val kid = async { q.say("아이 답", SpeechPriority.KID_ANSWER) }
        val sug = async { q.say("제안", SpeechPriority.SUGGESTION) }
        val safe = async { q.say("안전", SpeechPriority.SAFETY) }
        advanceUntilIdle()
        assertFalse(first.await(), "라디오는 더 급한 말에 끊긴다")
        assertEquals(listOf("라디오 1"), out.stopped)
        assertEquals(listOf("안전", "제안", "아이 답"), out.spoken)
        assertTrue(kid.await() && sug.await() && safe.await())
    }

    @Test
    fun `T5-9 같은 우선순위는 먼저 온 순서, 낮은 순위는 지금 말을 끊지 않는다`() = runTest {
        val out = FakeOut()
        val q = SpeechQueue(out, FakeFocus(), this, clock = { testScheduler.currentTime })
        val a = async { q.say("제안 A", SpeechPriority.SUGGESTION) }
        runCurrent()
        val b = async { q.say("제안 B", SpeechPriority.SUGGESTION) }
        val c = async { q.say("아이 답", SpeechPriority.KID_ANSWER) }
        advanceUntilIdle()
        assertEquals(listOf("제안 A", "제안 B", "아이 답"), out.spoken)
        assertTrue(a.await() && b.await() && c.await())
        assertTrue(out.stopped.isEmpty())
    }

    @Test
    fun `C P0 제안·답변은 잠깐 덕킹 포커스, 라디오는 미디어 포커스`() = runTest {
        val focus = FakeFocus()
        val q = SpeechQueue(FakeOut(), focus, this, clock = { testScheduler.currentTime })
        q.say("제안", SpeechPriority.SUGGESTION)
        q.say("라디오", SpeechPriority.RADIO)
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
