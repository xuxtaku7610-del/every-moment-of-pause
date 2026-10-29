package com.emop.pause.voice

import com.emop.pause.data.net.PhraseFacts
import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.Reason
import com.emop.pause.engine.planner.RestPhrases
import kotlinx.coroutines.withTimeoutOrNull

enum class PhraseSource { OLLAMA, TEMPLATE }

data class Phrase(val text: String, val source: PhraseSource)

/**
 * 제안 문장 공급자 (architecture §3.3). 서버(Ollama)가 [timeoutMs] 안에 답하면 그 문장,
 * 아니면 같은 내용의 템플릿 문장을 즉시 쓴다. 실패해도 예외를 내지 않는다.
 */
class PhraseProvider(
    private val remote: (suspend (PhraseFacts) -> String)?,
    private val timeoutMs: Long = 2_500,
) {
    suspend fun restSuggestion(d: PlanDecision.Suggest, level: FatigueLevel, passengers: List<Passenger>): Phrase {
        val template = Phrase(RestPhrases.suggestion(d), PhraseSource.TEMPLATE)
        // 피로 HIGH 문장은 안전 문구라 다듬지 않고 템플릿 그대로 쓴다.
        val call = remote?.takeUnless { d.fatigueFirst } ?: return template
        val text = withTimeoutOrNull(timeoutMs) { runCatching { call(facts(d, level, passengers)) }.getOrNull() }
        return if (text.isNullOrBlank()) template else Phrase(text, PhraseSource.OLLAMA)
    }

    companion object {
        fun facts(d: PlanDecision.Suggest, level: FatigueLevel, passengers: List<Passenger>) = PhraseFacts(
            restStopName = d.stop.name,
            minutesAway = d.minutesAway,
            reasons = d.reasons.filterIsInstance<Reason.HasFacility>().map { it.facility.code },
            fatigueLevel = level.name.lowercase(),
            // 서버에는 나이·종류 요약만 보낸다 (architecture §6).
            passengers = passengers.map { p ->
                when (p) {
                    is Passenger.Child -> "child_${p.age}"
                    Passenger.Dog -> "dog"
                }
            },
        )
    }
}
