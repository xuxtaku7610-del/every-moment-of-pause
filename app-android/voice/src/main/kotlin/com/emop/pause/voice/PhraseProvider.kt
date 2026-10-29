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
    /** [petName]이 있으면 반려견 맞춤 문장(같이가개 F2), 없으면 쉼표 때 문장. */
    suspend fun restSuggestion(d: PlanDecision.Suggest, level: FatigueLevel, passengers: List<Passenger>, petName: String? = null): Phrase {
        val template = Phrase(petName?.let { RestPhrases.petSuggestion(d, it) } ?: RestPhrases.suggestion(d), PhraseSource.TEMPLATE)
        // 피로 HIGH 문장은 안전 문구라 다듬지 않고 템플릿 그대로 쓴다.
        val call = remote?.takeUnless { d.fatigueFirst } ?: return template
        val text = withTimeoutOrNull(timeoutMs) { runCatching { call(facts(d, level, passengers)) }.getOrNull() }
        return if (text.isNullOrBlank()) template else Phrase(text, PhraseSource.OLLAMA)
    }

    companion object {
        fun facts(d: PlanDecision.Suggest, level: FatigueLevel, passengers: List<Passenger>) = PhraseFacts(
            restStopName = d.stop.name,
            minutesAway = d.minutesAway,
            // 시설 코드 + 반려견 이유(motion·potty). 반려견 이름은 보내지 않는다 (architecture §6).
            reasons = d.reasons.mapNotNull {
                when (it) {
                    is Reason.HasFacility -> it.facility.code
                    Reason.Motion -> "motion"
                    Reason.PottyDue -> "potty"
                    Reason.Fatigue -> null
                }
            },
            fatigueLevel = level.name.lowercase(),
            // 서버에는 나이·종류 요약만 보낸다 (architecture §6).
            passengers = passengers.map { p ->
                when (p) {
                    is Passenger.Child -> "child_${p.age}"
                    is Passenger.Dog -> "dog"
                }
            },
        )
    }
}
