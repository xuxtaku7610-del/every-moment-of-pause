package com.emop.pause.engine.planner

import java.util.Locale

/**
 * 템플릿 문장. LLM이 없거나 2.5초를 넘기면 이 문장을 그대로 읽는다 (architecture §3.3).
 * 운전 중 문장 규칙: 최대 2문장, 숫자·장소 먼저.
 */
object RestPhrases {
    fun suggestion(d: PlanDecision.Suggest): String {
        val where = "${d.minutesAway}분 뒤 ${d.stop.name}"
        if (d.fatigueFirst) return "$where, 잠깐 쉬어 가요. 졸음은 참을수록 위험해요."
        val facilities = d.reasons.filterIsInstance<Reason.HasFacility>().map { it.facility.labelKo }
        return if (facilities.isEmpty()) {
            "$where, 잠깐 쉬어 갈까요?"
        } else {
            "$where, ${joinWithAnd(facilities)}${subjectParticle(facilities.last())} 있어요. 들렀다 갈까요?"
        }
    }

    /**
     * 반려견 맞춤 쉼표 제안 (F2). 2문장 이내, 숫자·장소 먼저. 반려견 이유(멀미·배변)가 있으면 앞 문장에 둔다.
     * 예: "흔들리는 길을 오래 달렸어요. 18분 뒤 반려견 놀이터가 있는 안성휴게소에서 쉬어 갈까요?" (주행 자극은 차량 흔들림 — 반려견 상태로 말하지 않는다)
     */
    fun petSuggestion(d: PlanDecision.Suggest, petName: String): String {
        if (d.fatigueFirst) return suggestion(d)
        val pet = when {
            Reason.Motion in d.reasons -> "흔들리는 길을 오래 달렸어요."
            Reason.PottyDue in d.reasons -> "$petName 산책 시간이에요."
            else -> null
        }
        val facilities = d.reasons.filterIsInstance<Reason.HasFacility>().map { it.facility.labelKo }
        val place = if (facilities.isEmpty()) d.stop.name else "${joinWithAnd(facilities)}${subjectParticle(facilities.last())} 있는 ${d.stop.name}"
        val ask = "${d.minutesAway}분 뒤 ${place}에서 쉬어 갈까요?"
        return if (pet != null) "$pet $ask" else ask
    }

    fun noCandidate(d: PlanDecision.NoCandidate): String =
        if (d.nextStopKm != null) {
            "다음 휴게소까지 ${km(d.nextStopKm)} km, 졸음이 오면 갓길 대신 졸음쉼터를 이용하세요."
        } else {
            "앞쪽 휴게소 정보가 없어요. 졸음이 오면 갓길 대신 졸음쉼터를 이용하세요."
        }

    fun accepted(stop: RestStop) = "${stop.name}, 경로에 추가했어요."

    /** 경유지 추가 실패 → 이름·거리 안내로 대체 (test_plan 4장 P1). */
    fun waypointFailed(d: PlanDecision.Suggest) = "${d.stop.name}까지 ${km(d.kmAway)} km 남았어요. 표지판을 따라가 주세요."

    fun later() = "알겠어요. 10분 뒤에 다시 알려 드릴게요."

    /** "키즈존과 급속충전기": 앞말 받침에 따라 과/와. */
    fun joinWithAnd(words: List<String>): String = words.reduce { acc, w -> acc + (if (hasBatchim(acc)) "과 " else "와 ") + w }

    private fun subjectParticle(word: String) = if (hasBatchim(word)) "이" else "가"

    private fun hasBatchim(word: String): Boolean {
        val c = word.last()
        if (c !in '가'..'힣') return false
        return (c - '가') % 28 != 0
    }

    private fun km(v: Double) = if (v >= 10) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}
