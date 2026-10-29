package com.emop.pause.trip

import android.content.Context
import com.emop.pause.engine.fatigue.FatigueEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진행 중 여행 상태 (COMMERCIAL_READINESS A P1 "프로세스가 죽었다 살아나도 여행 상태 복원").
 * 실차 모드에서만 저장한다 — 시뮬레이션은 시나리오를 처음부터 다시 재생하면 된다.
 * 기기에만 저장하고, 위치·경로 좌표는 담지 않는다(경로는 [routeHash]만).
 */
data class TripResume(
    val savedAtMs: Long,
    /** 경로 키의 해시 ([hash]). 좌표가 든 경로 키 원문은 저장하지 않는다. */
    val routeHash: String?,
    val fatigue: FatigueEngine.Saved,
    val needsElapsedSec: List<Double>,
    val lastSuggestedId: String? = null,
    val headingId: String? = null,
) {
    /** 이어서 안내할 수 있나: [MAX_AGE_MS] 안이고, 경로를 알면 같은 경로여야 한다. */
    fun usable(nowMs: Long, currentRouteKey: String?): Boolean =
        nowMs - savedAtMs in 0..MAX_AGE_MS && (routeHash == null || currentRouteKey == null || routeHash == hash(currentRouteKey))

    fun encode(): String = listOf(
        VERSION, savedAtMs, routeHash.orEmpty(),
        fatigue.driveSec, fatigue.monotonySec, fatigue.cabinSec,
        needsElapsedSec.joinToString(","), lastSuggestedId.orEmpty(), headingId.orEmpty(),
    ).joinToString(SEP)

    companion object {
        /** 이보다 오래 지났으면 새 여행으로 본다 (다음 날 같은 경로 출발 등). */
        const val MAX_AGE_MS = 3 * 60 * 60 * 1000L
        /** 실차 모드 저장 간격. */
        const val SAVE_INTERVAL_MS = 30_000L
        private const val VERSION = "v1"
        private const val SEP = "|"

        /** 경로 키 → 16자리 16진 해시 (구분자 `|`가 들어갈 일이 없다). */
        fun hash(routeKey: String?): String? = routeKey?.let { k ->
            java.security.MessageDigest.getInstance("SHA-256").digest(k.toByteArray()).take(8).joinToString("") { "%02x".format(it) }
        }

        /** 형식이 다르거나 깨졌으면 null (복원하지 않고 새로 시작). */
        fun decode(raw: String?): TripResume? = runCatching {
            val p = raw!!.split(SEP)
            require(p.size == 9 && p[0] == VERSION)
            TripResume(
                savedAtMs = p[1].toLong(),
                routeHash = p[2].ifEmpty { null },
                fatigue = FatigueEngine.Saved(p[3].toDouble(), p[4].toDouble(), p[5].toDouble()),
                needsElapsedSec = if (p[6].isEmpty()) emptyList() else p[6].split(",").map { it.toDouble() },
                lastSuggestedId = p[7].ifEmpty { null },
                headingId = p[8].ifEmpty { null },
            )
        }.getOrNull()
    }
}

/** [TripResume] 한 개를 기기에 둔다. 여행이 끝나거나 "내 데이터 삭제"면 지운다. */
@Singleton
class TripResumeStore @Inject constructor(@param:ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("trip_resume", Context.MODE_PRIVATE)

    fun load(): TripResume? = TripResume.decode(prefs.getString(KEY, null))

    fun save(r: TripResume) = prefs.edit().putString(KEY, r.encode()).apply()

    fun clear() = prefs.edit().remove(KEY).apply()

    private companion object {
        const val KEY = "active"
    }
}
