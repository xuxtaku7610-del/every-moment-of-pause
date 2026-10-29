package com.emop.pause.engine.planner

import kotlin.math.abs

/** 점수 구성 (각 0~1, [total]은 가중합). 이유 문장은 실제로 크게 기여한 항목에서만 만든다. */
data class ScoreBreakdown(
    /** 니즈가 몰리는 시각(또는 피로 목표 시각)에 맞게 도착하는가. */
    val timing: Double,
    /** 피로가 높을수록 가까운 곳이 낫다. */
    val fatigue: Double,
    /** 니즈에 맞는 편의시설이 있는가 (니즈 긴급도 가중). */
    val facility: Double,
    /** 직전 휴식 직후라 너무 이른 정차 (0 = 문제없음, 1 = 방금 쉼). */
    val gapPenalty: Double,
    val total: Double,
)

/**
 * 휴게소 점수 가중치 — 한 곳에 모은다 (COMMERCIAL_READINESS B P0, `ScoringPolicyTest`로 고정).
 * 점수 = timing·[timingWeight] + fatigue·[fatigueWeight] + facility·[facilityWeight] − gapPenalty·[gapWeight].
 */
data class ScoringPolicy(
    val timingWeight: Double = 0.35,
    val fatigueWeight: Double = 0.2,
    val facilityWeight: Double = 0.35,
    val gapWeight: Double = 0.1,
    /** 목표 시각에서 이만큼 벗어나면 timing 0. */
    val timingToleranceMin: Double = 45.0,
    /** 후보를 보는 범위(분). fatigue 항은 이 범위 안에서 가까울수록 1. */
    val horizonMin: Double = 60.0,
    /** 마지막 휴식(연속 운전 시작) 뒤 이 시간 안에 서면 간격 페널티. */
    val minRestGapMin: Double = 45.0,
    /** 한 번 제안한 곳은 새 후보가 이 비율 이상 높을 때만 바꾼다 (제안 깜빡임 방지). */
    val hysteresis: Double = 0.15,
) {
    /**
     * @param minutesAway 지금부터 그 휴게소까지(분)
     * @param targetMin 쉬기 좋은 시각(분 뒤)
     * @param fatigueScore 0~100
     * @param facilityMatch 니즈 가중 편의시설 충족률 0~1
     * @param minutesSinceRest 연속 운전 시간(분)
     */
    fun score(minutesAway: Double, targetMin: Double, fatigueScore: Int, facilityMatch: Double, minutesSinceRest: Int): ScoreBreakdown {
        val timing = (1.0 - abs(minutesAway - targetMin) / timingToleranceMin).coerceIn(0.0, 1.0)
        val closeness = (1.0 - minutesAway / horizonMin).coerceIn(0.0, 1.0)
        val fatigue = closeness * (fatigueScore.coerceIn(0, 100) / 100.0)
        val sinceAtArrival = minutesSinceRest + minutesAway
        val gap = if (sinceAtArrival >= minRestGapMin) 0.0 else 1.0 - sinceAtArrival / minRestGapMin
        val facility = facilityMatch.coerceIn(0.0, 1.0)
        val total = timing * timingWeight + fatigue * fatigueWeight + facility * facilityWeight - gap * gapWeight
        return ScoreBreakdown(timing, fatigue, facility, gap, total)
    }

    /**
     * 히스테리시스: [previous]가 아직 후보에 있으면, 최고 후보가 그보다 [hysteresis] 이상 높을 때만 바꾼다.
     * 점수가 0 이하일 수 있어 차이는 절댓값 기준 |prev|·hysteresis(최소 0.01)로 본다.
     */
    fun <T> stabilize(ranked: List<Pair<T, Double>>, previous: T?): T? {
        val best = ranked.maxByOrNull { it.second } ?: return null
        val prev = ranked.firstOrNull { it.first == previous } ?: return best.first
        val margin = maxOf(abs(prev.second) * hysteresis, 0.01)
        return if (best.second >= prev.second + margin) best.first else prev.first
    }

    companion object {
        val DEFAULT = ScoringPolicy()
    }
}
