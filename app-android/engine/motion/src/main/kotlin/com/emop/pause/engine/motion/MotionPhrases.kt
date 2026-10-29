package com.emop.pause.engine.motion

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 멀미 예보 문구 (F1). 안전·건강과 닿은 문구라 LLM을 쓰지 않고 템플릿만 쓴다 (architecture §3.3).
 * 주행 중 규칙: 한 줄, 숫자·장소 먼저. "진단"처럼 들리는 말은 쓰지 않는다.
 */
object MotionPhrases {
    /** 주행 화면 한 덩어리 최대 글자 수 (uistate DrivingRules.MAX_BLOCK_CHARS와 같다, 2줄). */
    const val MAX_CHARS = 48

    /**
     * "3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속해 주세요."
     * 이름이 길어 [MAX_CHARS]를 넘으면 이름을 뺀 문장을 쓴다 (주행 중 2줄 규칙, DS-02).
     */
    fun forecast(f: MotionForecast, petName: String): String {
        val where = "${distance(f.startInM)} 뒤 굽은 길 ${max(1, (f.durationSec / 60.0).roundToInt())}분"
        fun line(who: String) = when (motionLevelOf(f.predictedIndex)) {
            MotionLevel.LOW -> "$where, 천천히 가면 ${who}편해요."
            MotionLevel.CAUTION -> "$where, ${who}멀미 주의 — 조금 일찍 감속해 주세요."
            MotionLevel.HIGH -> "$where, ${who}멀미 걱정 — ${f.recommendedKph}km/h 아래로 천천히요."
        }
        return line("$petName ").takeIf { it.length <= MAX_CHARS } ?: line("")
    }

    /** 주행 화면 한 줄 (지금 상태). */
    fun now(s: MotionState, petName: String): String = when (s.level) {
        MotionLevel.LOW -> "$petName 컨디션 편안 ${s.index}"
        MotionLevel.CAUTION -> "$petName 멀미 주의 ${s.index}"
        MotionLevel.HIGH -> "$petName 멀미 높음 ${s.index}"
    }

    fun levelLabel(l: MotionLevel) = when (l) {
        MotionLevel.LOW -> "편안"
        MotionLevel.CAUTION -> "주의"
        MotionLevel.HIGH -> "높음"
    }

    /** 1km 미만은 100m 단위, 10km 미만은 소수 한 자리(.0은 뺌). */
    fun distance(m: Double): String = when {
        m < 1000 -> "${max(100, (m / 100).roundToInt() * 100)}m"
        m < 10_000 -> String.format(Locale.US, "%.1f", m / 1000).removeSuffix(".0") + "km"
        else -> "${(m / 1000).roundToInt()}km"
    }
}
