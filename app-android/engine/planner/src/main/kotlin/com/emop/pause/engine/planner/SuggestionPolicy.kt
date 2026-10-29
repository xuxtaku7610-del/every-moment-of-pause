package com.emop.pause.engine.planner

import com.emop.pause.engine.fatigue.FatigueLevel

/**
 * 제안 간격 정책 (test_plan 4장).
 * - "나중에" → [RETRY_MS] 뒤 재평가.
 * - 연속 거절 [BACKOFF_AFTER]회부터 간격 2배(20분). 단, 피로 HIGH면 기본 간격.
 * - 거절한 뒤 피로가 HIGH로 올라가면 기다리지 않고 바로 제안한다 (가장 가까운 쉼터, COMMERCIAL_READINESS B P0).
 * - 수락하면 거절 횟수를 초기화한다.
 */
class SuggestionPolicy {
    private var lastDeclinedMs: Long? = null
    private var declinedAtLevel: FatigueLevel? = null
    var consecutiveDeclines = 0
        private set

    fun canSuggest(nowMs: Long, level: FatigueLevel): Boolean {
        val last = lastDeclinedMs ?: return true
        if (level == FatigueLevel.HIGH && declinedAtLevel != FatigueLevel.HIGH) return true
        val interval = if (consecutiveDeclines >= BACKOFF_AFTER && level != FatigueLevel.HIGH) RETRY_MS * 2 else RETRY_MS
        return nowMs - last >= interval
    }

    fun onDeclined(nowMs: Long, level: FatigueLevel? = null) {
        consecutiveDeclines++
        lastDeclinedMs = nowMs
        declinedAtLevel = level
    }

    fun onAccepted() {
        consecutiveDeclines = 0
        lastDeclinedMs = null
        declinedAtLevel = null
    }

    companion object {
        const val RETRY_MS = 10 * 60 * 1000L
        const val BACKOFF_AFTER = 3
    }
}
