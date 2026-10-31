package com.emop.pause.engine.motion

import kotlin.math.exp
import kotlin.math.ln

/**
 * 보호자 음성 신고에서 자동으로 고른 답 (2026-10-31 재정비 — 도착 뒤 질문은 없앴다):
 * 구토 신고 = [YES], 침 흘림·헐떡임 신고 = [LITTLE], 주행 자극 높음 구간 뒤 30분 동안 신고 없음 = [NO](약하게, [MotionCalibration.QUIET_WEIGHT]).
 */
enum class MotionAnswer { YES, LITTLE, NO }

/** 그때까지 주행 자극 최고값과 답 한 쌍. [weight]는 학습률에 곱한다(신고 1.0, 신고 없음 [MotionCalibration.QUIET_WEIGHT]). */
data class CalibrationSample(val peak: Int, val answer: MotionAnswer, val weight: Double = 1.0)

/**
 * 개인 맞춤 보정 (UPGRADE 4, docs/plan/motion_model.md "개인 보정").
 * 민감도 S에 곱하는 계수 c를 로그 스케일에서 작은 학습률로 갱신한다 — 베이지안 갱신을 단순화한 형태:
 *   log c ← log c + η · w · (관측 − 예측),  관측 = 답의 기대 지수(예 0.7 · 조금 0.45 · 아니요 0.15),  예측 = 그때까지 주행 자극 최고/100,
 *   w = 표본 가중치(신고 1, 신고 없음 [QUIET_WEIGHT])
 * 지수가 낮았는데 "예"가 반복되면 c가 오르고, 높았는데 "아니요"가 반복되면 내린다. 답과 지수가 맞으면 거의 그대로.
 * [MIN_SAMPLES]회 미만이면 1.0(한두 번의 답으로 흔들리지 않게), 결과는 [MIN_SCALE]~[MAX_SCALE]로 묶는다.
 * 모든 값은 가정값이고 반려견 대상 검증은 없다 — 사용자의 답에 맞춰 예보를 조금씩 옮기는 장치다.
 */
object MotionCalibration {
    const val MIN_SAMPLES = 3
    const val LEARNING_RATE = 0.6
    const val MIN_SCALE = 0.6
    const val MAX_SCALE = 1.6
    /** "높음 구간 뒤 30분 신고 없음"을 괜찮았음으로 반영할 때의 가중치 — 신고(1.0)의 절반 이하 (사용자 결정 10/31). */
    const val QUIET_WEIGHT = 0.4
    /** 높음 구간을 지난 뒤 이 시간 동안 신고가 없으면 괜찮았음 표본 하나. */
    const val QUIET_WINDOW_SEC = 30 * 60

    fun observed(a: MotionAnswer): Double = when (a) {
        MotionAnswer.YES -> 0.7
        MotionAnswer.LITTLE -> 0.45
        MotionAnswer.NO -> 0.15
    }

    fun scale(samples: List<CalibrationSample>): Double {
        if (samples.size < MIN_SAMPLES) return 1.0
        var logC = 0.0
        val lo = ln(MIN_SCALE)
        val hi = ln(MAX_SCALE)
        for (s in samples) {
            logC = (logC + LEARNING_RATE * s.weight.coerceIn(0.0, 1.0) * (observed(s.answer) - s.peak.coerceIn(0, 100) / 100.0)).coerceIn(lo, hi)
        }
        return exp(logC).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    /** 프로필 화면 문구: 보통에서 얼마나 옮겨졌는지. */
    fun label(scale: Double): String = when {
        scale < 0.8 -> "낮음"
        scale < 0.93 -> "조금 낮음"
        scale <= 1.07 -> "보통"
        scale <= 1.25 -> "조금 높음"
        else -> "높음"
    }
}
