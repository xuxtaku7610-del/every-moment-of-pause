package com.emop.pause.trip

import com.emop.pause.engine.pet.CheckItem

/**
 * 출발 체크 기억 (v10-3 3번): 출발할 때의 체크를 최근 [KEEP]번까지 기기에 남긴다(SettingsStore 문자열 한 줄, 위치·시각 없음).
 * - [지난번과 같아요] = 가장 최근 출발의 체크를 한 번에.
 * - 최근 3번 중 한 번이라도 빠뜨린 항목은 출발 전 화면에서 다시 강조.
 */
data class CheckMemory(val recent: List<Set<CheckItem>> = emptyList()) {
    val last: Set<CheckItem>? get() = recent.firstOrNull()

    /** 최근 3번 중 빠뜨린 적이 있는 항목 (기록이 없으면 없음). */
    val missed: Set<CheckItem> get() = if (recent.isEmpty()) emptySet() else CheckItem.entries.filter { item -> recent.any { item !in it } }.toSet()

    /** 출발할 때 부른다 — 맨 앞에 넣고 [KEEP]번만 남긴다. */
    fun onDeparted(checked: Set<CheckItem>): CheckMemory = CheckMemory((listOf(checked) + recent).take(KEEP))

    fun encode(): String = recent.joinToString("|") { s -> s.sortedBy { it.ordinal }.joinToString(",") { it.name } }

    companion object {
        const val KEEP = 3

        fun decode(raw: String?): CheckMemory {
            if (raw.isNullOrBlank()) return CheckMemory()
            val sets = raw.split('|').map { part -> part.split(',').mapNotNull { n -> CheckItem.entries.firstOrNull { it.name == n.trim() } }.toSet() }
            return CheckMemory(sets.take(KEEP))
        }
    }
}
