package com.emop.pause.voice

import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.needs.NeedKind
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.Reason
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.StopType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VoiceTest {
    @ParameterizedTest
    @CsvSource(
        "응, YES", "어, YES", "네 좋아요, YES", "그래 들르자, YES", "가자, YES", "오케이, YES",
        "나중에, LATER", "아니, LATER", "안 갈래, LATER", "괜찮아 그냥 가, LATER", "이따가, LATER",
        "음악 틀어줘, UNKNOWN", "'', UNKNOWN",
    )
    fun `응·나중에 의도 인식`(text: String, expected: Reply) {
        assertEquals(expected, ReplyIntent.parse(text))
    }

    @Test
    fun `인식 결과 없음은 UNKNOWN`() = assertEquals(Reply.UNKNOWN, ReplyIntent.parse(null))

    private val suggest = PlanDecision.Suggest(
        RestStop("rs_c", "다온휴게소", StopType.SERVICE_AREA, 30.0, setOf(Facility.KIDS_ZONE, Facility.EV_FAST)),
        18, 30.0,
        listOf(Reason.HasFacility(Facility.KIDS_ZONE, NeedKind.TOILET), Reason.HasFacility(Facility.EV_FAST, NeedKind.CHARGE)),
        1.0, fatigueFirst = false,
    )
    private val passengers = listOf(Passenger.Child(5), Passenger.Dog)

    @Test
    fun `서버 문장이 오면 그 문장, 서버에는 나이·종류 요약만`() = runTest {
        var sent: com.emop.pause.data.net.PhraseFacts? = null
        val p = PhraseProvider({ f -> sent = f; "18분 뒤 다온휴게소에 키즈존이 있어요. 들를까요?" })
        val phrase = p.restSuggestion(suggest, FatigueLevel.MEDIUM, passengers)
        assertEquals(PhraseSource.OLLAMA, phrase.source)
        assertEquals(listOf("child_5", "dog"), sent!!.passengers)
        assertEquals(listOf("kids_zone", "ev_fast"), sent!!.reasons)
        assertEquals("medium", sent!!.fatigueLevel)
    }

    @Test
    fun `P0 LLM 2,5초 초과 - 템플릿 문장으로 즉시 출력`() = runTest {
        val p = PhraseProvider({ delay(10_000); "늦은 문장" })
        val phrase = p.restSuggestion(suggest, FatigueLevel.MEDIUM, passengers)
        assertEquals(PhraseSource.TEMPLATE, phrase.source)
        assertEquals("18분 뒤 다온휴게소, 키즈존과 급속충전기가 있어요. 들렀다 갈까요?", phrase.text)
        assertTrue(currentTime <= 2_500)
    }

    @Test
    fun `서버 오류·서버 없음 - 템플릿`() = runTest {
        assertEquals(PhraseSource.TEMPLATE, PhraseProvider({ error("503") }).restSuggestion(suggest, FatigueLevel.LOW, passengers).source)
        assertEquals(PhraseSource.TEMPLATE, PhraseProvider(null).restSuggestion(suggest, FatigueLevel.LOW, passengers).source)
    }

    @Test
    fun `피로 HIGH 안전 문구는 LLM을 부르지 않는다`() = runTest {
        var called = false
        val p = PhraseProvider({ called = true; "x" })
        p.restSuggestion(suggest.copy(fatigueFirst = true), FatigueLevel.HIGH, passengers)
        assertEquals(false, called)
    }
}
