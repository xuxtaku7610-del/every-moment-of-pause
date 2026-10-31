package com.emop.pause.engine.motion

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 주행 자극 예보 문구 (F1). 안전·건강과 닿은 문구라 LLM을 쓰지 않고 템플릿만 쓴다 (architecture §3.3).
 * 엔진 값은 차량 흔들림 누적량이지 반려견 몸을 잰 값이 아니다(2026-10-31 재정비) → 반려견 상태("멀미 주의")로 말하지 않고
 * 길과 운전("굽은 길", "천천히 가면 덜 흔들려요")으로만 말한다. 주행 중 규칙: 한 줄, 거리·시간 먼저. 숫자 지수는 말하지 않는다.
 */
object MotionPhrases {
    /** 주행 화면 한 덩어리 최대 글자 수 (uistate DrivingRules.MAX_BLOCK_CHARS와 같다, 2줄). */
    const val MAX_CHARS = 48

    /**
     * 음성·제한 화면 한 줄: "3km 뒤 굽은 길 8분, 천천히 가면 덜 흔들려요." ([MAX_CHARS] 이하).
     * [petName]은 예전 호출과 맞추려고 남겨 둔다 — 반려견 이름을 넣으면 반려견 상태처럼 들려서 쓰지 않는다.
     */
    @Suppress("UNUSED_PARAMETER")
    fun forecast(f: MotionForecast, petName: String = ""): String = "${title(f).replace(" · ", " ")}, ${coach(f)}."

    /** 예보 띠 제목 1줄: "3km 뒤 굽은 길 · 8분". */
    fun title(f: MotionForecast): String = "${distance(f.startInM)} 뒤 굽은 길 · ${max(1, (f.durationSec / 60.0).roundToInt())}분"

    /** 예보 운전 안내 1줄 ([LINE_CHARS] 이하): 단계별로 "천천히 가면 덜 흔들려요" / "39km/h 아래로 천천히 가요". */
    @Suppress("UNUSED_PARAMETER")
    fun coach(f: MotionForecast, petName: String = ""): String = when (motionLevelOf(f.predictedIndex)) {
        MotionLevel.LOW -> "지금처럼 부드럽게 가요"
        MotionLevel.CAUTION -> "천천히 가면 덜 흔들려요"
        MotionLevel.HIGH -> "${f.recommendedKph}km/h 아래로 천천히 가요"
    }

    const val LINE_CHARS = 24

    /** 정차 중 "주행 자극 보기" 한 줄: "주행 자극 보통 45". 주행 화면에는 쓰지 않는다. */
    @Suppress("UNUSED_PARAMETER")
    fun now(s: MotionState, petName: String = ""): String = "주행 자극 ${levelLabel(s.level)} ${s.index}"

    /** 주행 자극 단계: 낮음(40 미만) · 보통(40~69) · 높음(70 이상) — 사용자 결정 10/31. */
    fun levelLabel(l: MotionLevel) = when (l) {
        MotionLevel.LOW -> "낮음"
        MotionLevel.CAUTION -> "보통"
        MotionLevel.HIGH -> "높음"
    }

    /** 1km 미만은 100m 단위, 10km 미만은 소수 한 자리(.0은 뺌). */
    fun distance(m: Double): String = when {
        m < 1000 -> "${max(100, (m / 100).roundToInt() * 100)}m"
        m < 10_000 -> String.format(Locale.US, "%.1f", m / 1000).removeSuffix(".0") + "km"
        else -> "${(m / 1000).roundToInt()}km"
    }
}
