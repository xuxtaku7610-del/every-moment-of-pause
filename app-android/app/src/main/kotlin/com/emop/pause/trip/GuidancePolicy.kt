package com.emop.pause.trip

import com.emop.pause.core.model.Gear
import com.emop.pause.engine.motion.MotionConstants
import java.time.LocalDate

/** 안내 빈도 (설정, v10-1). 보통 = 지금까지의 동작. */
enum class GuidanceLevel(val label: String) {
    /** 낮음 단계 예보까지 말한다. */
    MORE("많이"),
    NORMAL("보통"),
    /** 예보는 높음 구간만, 주변 추천은 끈다. */
    LESS("적게"),
}

/** 같이가개가 먼저 꺼내는 안내의 종류. */
enum class AnnounceKind { FORECAST, SUGGEST, NEARBY, EMERGENCY, OTHER }

/**
 * 주행 중 안내를 낼지 (v10-1 4번, 사용자 결정 11/04 — "주행 중 주인공은 기본 내비"):
 * - **오늘은 안 타요**: 그날 운행 동안 아무 안내도 하지 않는다(응급 포함 — 반려견이 타지 않았다고 답했다).
 * - **조용히 모드**: 응급과 쉼표 추천만. 1시간 뒤 또는 정차 때 풀린다.
 * - **빈도**: 많이(낮음 예보까지) · 보통(예보 보통 이상, 주변 추천) · 적게(예보 높음만, 주변 추천 끔).
 * 엔진 스레드에서만 만진다. 상태는 기기에 저장하지 않는다(오늘은 안 타요 날짜·빈도는 [SettingsStore]).
 */
class GuidancePolicy {
    var level: GuidanceLevel = GuidanceLevel.NORMAL
    /** "오늘은 안 타요"를 고른 날. 다른 날이면 효과 없음. */
    var dayOff: LocalDate? = null
    var quietUntilMs: Long? = null
        private set

    fun isDayOff(today: LocalDate) = dayOff == today
    fun isQuiet(nowMs: Long) = quietUntilMs?.let { nowMs < it } == true

    fun startQuiet(nowMs: Long) {
        quietUntilMs = nowMs + QUIET_MS
    }

    /** 정차(쉼표·도착)하거나 사용자가 끄면 풀린다. */
    fun endQuiet() {
        quietUntilMs = null
    }

    /** [forecastPeak] = 예보 구간 최고(개인 보정 반영, 예보 단계를 정하는 값). FORECAST에서만 본다. */
    fun allows(kind: AnnounceKind, nowMs: Long, today: LocalDate, forecastPeak: Int? = null): Boolean {
        if (isDayOff(today)) return false
        if (isQuiet(nowMs) && kind != AnnounceKind.EMERGENCY && kind != AnnounceKind.SUGGEST) return false
        return when (kind) {
            AnnounceKind.FORECAST -> {
                val peak = forecastPeak ?: return level != GuidanceLevel.LESS
                when (level) {
                    GuidanceLevel.MORE -> true
                    GuidanceLevel.NORMAL -> peak >= MotionConstants.CAUTION
                    GuidanceLevel.LESS -> peak >= MotionConstants.HIGH
                }
            }
            AnnounceKind.NEARBY -> level != GuidanceLevel.LESS
            AnnounceKind.SUGGEST, AnnounceKind.EMERGENCY, AnnounceKind.OTHER -> true
        }
    }

    companion object {
        const val QUIET_MS = 60 * 60_000L
    }
}

/**
 * 자동 시작 (v10-1 4번): 실차 모드에서 출발 체크를 안 했어도, 기본 내비에 목적지가 잡혀 있고 D단으로 출발(속도 [MIN_KPH] 이상)하면
 * 여행을 시작한다. "오늘은 안 타요"인 날은 시작하지 않는다. 체크는 첫 정차 때 한 줄로 다시 묻는다.
 */
object AutoStart {
    const val MIN_KPH = 5f

    fun due(notStarted: Boolean, hasDestination: Boolean, gear: Gear?, speedKph: Float?, dayOff: Boolean): Boolean =
        notStarted && hasDestination && !dayOff && gear == Gear.DRIVE && (speedKph ?: 0f) >= MIN_KPH
}
