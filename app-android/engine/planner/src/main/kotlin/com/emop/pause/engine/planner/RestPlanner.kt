package com.emop.pause.engine.planner

import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.fatigue.FatigueResult
import com.emop.pause.engine.needs.Need
import com.emop.pause.engine.needs.NeedKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** 추천 이유. 문장의 "키즈존과 급속충전기" 부분이 된다. */
sealed interface Reason {
    data class HasFacility(val facility: Facility, val need: NeedKind) : Reason
    /** 피로 때문에 쉬어야 함 (편의시설과 무관). */
    data object Fatigue : Reason
}

sealed interface PlanDecision {
    /** 아직 쉴 필요 없음. */
    data object NoNeed : PlanDecision

    data class Suggest(
        val stop: RestStop,
        val minutesAway: Int,
        val kmAway: Double,
        val reasons: List<Reason>,
        val score: Double,
        /** 피로 높음 → 가장 가까운 곳 우선. */
        val fatigueFirst: Boolean,
    ) : PlanDecision

    /** 쉬어야 하는데 앞쪽 후보가 없음 → 일반 안내. nextStopKm는 범위 밖이라도 가장 가까운 다음 휴게소까지 거리. */
    data class NoCandidate(val nextStopKm: Double?) : PlanDecision
}

data class PlannerInput(
    val stops: List<RestStop>,
    /** 출발점부터 지금까지 경로 거리(km). */
    val traveledKm: Double,
    /** 평균 진행 속도(km/분). ETA에서 구한다. */
    val kmPerMin: Double,
    val fatigue: FatigueResult,
    val needs: List<Need>,
    /** 직전에 제안한 휴게소 ID. 아직 앞쪽에 있으면 히스테리시스로 유지한다 ("나중에" 뒤 같은 곳 다시 제안). */
    val previousStopId: String? = null,
    val policy: ScoringPolicy = ScoringPolicy.DEFAULT,
)

/**
 * 휴게소 플래너 (architecture §3.1 `engine:planner`, flowchart §1).
 * - 쉬어야 하나: 피로 MEDIUM 이상, 니즈 긴급도 [NEED_URGENT] 이상, 또는 충전 가능한 마지막 휴게소가 곧 나옴.
 * - 피로 HIGH: 편의시설과 무관하게 가장 가까운 곳(같으면 졸음쉼터).
 * - 그 외: 휴게소만 대상으로 [ScoringPolicy] 점수(도착 시각 적합도 + 피로 긴급도 + 편의시설 충족 − 휴식 간격 페널티), 추천 1개 + 이유.
 *   직전 제안은 새 후보가 15% 이상 나을 때만 바꾼다(히스테리시스).
 * - 충전 니즈가 있으면 급속충전기가 있고 배터리로 갈 수 있는 곳만 후보.
 */
object RestPlanner {
    const val NEED_URGENT = 0.7
    const val MIN_LEAD_KM = 2.0
    const val HORIZON_MIN = 60.0
    const val MEDIUM_FATIGUE_TARGET_MIN = 20.0
    const val LAST_CHARGER_WARN_MIN = 30.0
    const val MAX_REASONS = 2
    /** 이보다 작게 기여한 요인은 이유로 말하지 않는다 (점수 0~1 기준). */
    const val MIN_REASON_CONTRIBUTION = 0.02

    private val facilityFor = mapOf(
        NeedKind.TOILET to Facility.KIDS_ZONE,
        NeedKind.NURSING to Facility.NURSING_ROOM,
        NeedKind.PET_WALK to Facility.PET_ZONE,
        NeedKind.CHARGE to Facility.EV_FAST,
    )

    fun plan(input: PlannerInput): PlanDecision {
        val kmPerMin = input.kmPerMin.takeIf { it > 0 } ?: DEFAULT_KM_PER_MIN
        val ahead = input.stops
            .filter { it.distanceAlongRouteKm - input.traveledKm >= MIN_LEAD_KM }
            .sortedBy { it.distanceAlongRouteKm }
        val charge = input.needs.firstOrNull { it.kind == NeedKind.CHARGE }
        val reachLimitKm = charge?.reachKm?.let { input.traveledKm + it }
        fun minutesTo(s: RestStop) = (s.distanceAlongRouteKm - input.traveledKm) / kmPerMin
        val window = ahead.filter { minutesTo(it) <= HORIZON_MIN }

        val high = input.fatigue.level == FatigueLevel.HIGH
        if (high) {
            val nearest = window.minWithOrNull(compareBy<RestStop> { it.distanceAlongRouteKm }.thenBy { it.type != StopType.DROWSY_SHELTER })
                ?: return PlanDecision.NoCandidate(ahead.firstOrNull()?.let { it.distanceAlongRouteKm - input.traveledKm })
            return suggest(nearest, input.traveledKm, kmPerMin, listOf(Reason.Fatigue), 1.0, fatigueFirst = true)
        }

        val chargers = ahead.filter { Facility.EV_FAST in it.facilities && (reachLimitKm == null || it.distanceAlongRouteKm <= reachLimitKm) }
        val lastChargerSoon = charge != null && chargers.lastOrNull()?.let { minutesTo(it) <= LAST_CHARGER_WARN_MIN } == true
        val needsRest = input.fatigue.level == FatigueLevel.MEDIUM ||
            input.needs.any { it.urgency >= NEED_URGENT } ||
            lastChargerSoon
        if (!needsRest) return PlanDecision.NoNeed

        val candidates = window.filter { it.type == StopType.SERVICE_AREA }
            .filter { charge == null || it in chargers }
        if (candidates.isEmpty()) {
            return PlanDecision.NoCandidate(ahead.firstOrNull()?.let { it.distanceAlongRouteKm - input.traveledKm })
        }

        // 언제 쉬면 좋은가: 니즈 중 가장 이른 시각, 피로 MEDIUM이면 20분 안.
        val dueTimes = input.needs.map { it.dueInMin.toDouble() } +
            listOfNotNull(MEDIUM_FATIGUE_TARGET_MIN.takeIf { input.fatigue.level == FatigueLevel.MEDIUM })
        val targetMin = dueTimes.minOrNull() ?: MEDIUM_FATIGUE_TARGET_MIN
        val weights = input.needs.associate { it.kind to max(it.urgency, 0.2) }
        val weightSum = weights.values.sum()
        val policy = input.policy

        data class Scored(val stop: RestStop, val breakdown: ScoreBreakdown, val reasons: List<Reason>)
        // 같은 점수면 더 먼 곳(더 오래 달린 뒤 쉬기)이 앞에 오도록 먼 순서로 둔다.
        val scored = candidates.sortedByDescending { it.distanceAlongRouteKm }.map { s ->
            val matched = input.needs.filter { facilityFor[it.kind] in s.facilities }
            val facilityMatch = if (weightSum > 0) matched.sumOf { weights.getValue(it.kind) } / weightSum else 0.0
            val b = policy.score(minutesTo(s), targetMin, input.fatigue.score, facilityMatch, input.fatigue.continuousDriveMinutes)
            Scored(s, b, reasonsOf(b, matched, weights, weightSum, policy))
        }
        val chosenId = policy.stabilize(scored.map { it.stop.id to it.breakdown.total }, input.previousStopId)
        val best = scored.first { it.stop.id == chosenId }

        return suggest(best.stop, input.traveledKm, kmPerMin, best.reasons, best.breakdown.total, fatigueFirst = false)
    }

    /**
     * 이유 = 실제 점수에 가장 크게 기여한 요인 1~[MAX_REASONS]개 (COMMERCIAL_READINESS B P1, 거짓 이유 금지, test_plan T4-13).
     * 요인 기여도: 니즈별 편의시설 = facility 가중치 × 그 니즈 몫, 피로 = fatigue 항 × 가중치.
     * 시각 맞춤(timing)은 모든 후보에 비슷하게 붙어 "이 곳"의 이유가 못 되므로 문장에 쓰지 않는다.
     * 기여가 0인 요인은 쓰지 않고, 아무것도 없으면 피로(쉬어 갈 때)로 둔다.
     */
    internal fun reasonsOf(b: ScoreBreakdown, matched: List<Need>, weights: Map<NeedKind, Double>, weightSum: Double, policy: ScoringPolicy): List<Reason> {
        val facility = matched.map { n ->
            Reason.HasFacility(facilityFor.getValue(n.kind), n.kind) as Reason to policy.facilityWeight * weights.getValue(n.kind) / weightSum
        }
        val fatigue = Reason.Fatigue to b.fatigue * policy.fatigueWeight
        return (facility + fatigue)
            .filter { it.second > MIN_REASON_CONTRIBUTION }
            .sortedByDescending { it.second }
            .take(MAX_REASONS)
            .map { it.first }
            .ifEmpty { listOf(Reason.Fatigue) }
    }

    private fun suggest(s: RestStop, traveledKm: Double, kmPerMin: Double, reasons: List<Reason>, score: Double, fatigueFirst: Boolean): PlanDecision.Suggest {
        val km = s.distanceAlongRouteKm - traveledKm
        return PlanDecision.Suggest(s, max(1, (km / kmPerMin).roundToInt()), km, reasons, score, fatigueFirst)
    }

    private const val DEFAULT_KM_PER_MIN = 100.0 / 60
}
