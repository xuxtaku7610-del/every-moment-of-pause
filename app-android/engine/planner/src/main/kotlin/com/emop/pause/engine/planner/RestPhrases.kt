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

    /** 인식 실패 후 한 번만 다시 묻는다 (COMMERCIAL_READINESS C P0). */
    fun askAgain() = "\"응\" 또는 \"나중에\"로 답해 주세요."

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
