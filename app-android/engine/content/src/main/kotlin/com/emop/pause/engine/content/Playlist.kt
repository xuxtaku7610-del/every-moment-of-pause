package com.emop.pause.engine.content

/** 편성 결과. [totalSec] ≤ 목표. */
data class Playlist(val items: List<Episode>, val totalSec: Int, val targetSec: Int) {
    val episodeCount get() = items.size
}

/**
 * 도착 맞춤 라디오 편성 (COMMERCIAL_READINESS F P0, test_plan T5-2·T9-7).
 * 목표 시간(도착까지 남은 시간) 이하에서 가장 가까운 조합을 0/1 배낭 DP로 고른다.
 * - 같은 여행에서 이미 들은 에피소드([played])는 빼고, 나이에 맞는 것만.
 * - 합이 같으면 편수가 적은 쪽(긴 이야기 우선), 그다음 입력 순서.
 * - 5초 단위로 계산해 표 크기를 줄인다(에피소드 길이도 5초 단위).
 */
object PlaylistPlanner {
    private const val STEP = 5

    fun plan(episodes: List<Episode>, targetSec: Int, age: Int?, played: Set<String> = emptySet(), topics: Set<String>? = null): Playlist {
        if (targetSec <= 0) return Playlist(emptyList(), 0, targetSec)
        val pool = episodes.filter { it.id !in played && (age == null || it.fits(age)) && (topics == null || it.topic in topics) && it.durationSec > 0 }
        val cap = targetSec / STEP
        val w = pool.map { (it.durationSec + STEP - 1) / STEP }
        // best[c] = 용량 c에서 만들 수 있는 최대 합, count[c] = 그때 편수(적을수록 좋음), pick = 선택 기록
        val best = IntArray(cap + 1)
        val count = IntArray(cap + 1)
        val take = Array(pool.size) { BooleanArray(cap + 1) }
        for (i in pool.indices) {
            for (c in cap downTo w[i]) {
                val sum = best[c - w[i]] + w[i]
                val n = count[c - w[i]] + 1
                if (sum > best[c] || (sum == best[c] && n < count[c])) {
                    best[c] = sum
                    count[c] = n
                    take[i][c] = true
                }
            }
        }
        val chosen = ArrayList<Episode>()
        var c = cap
        for (i in pool.indices.reversed()) {
            if (take[i][c]) {
                chosen += pool[i]
                c -= w[i]
            }
        }
        chosen.reverse()
        return Playlist(chosen, chosen.sumOf { it.durationSec }, targetSec)
    }
}
