package com.emop.pause.engine.planner

import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.fatigue.FatigueResult
import com.emop.pause.engine.needs.Need
import com.emop.pause.engine.needs.NeedKind
import kotlin.math.max
import kotlin.math.roundToInt

data class PetPlannerInput(
    val stops: List<RestStop>,
    /** 출발점부터 지금까지 경로 거리(km). */
    val traveledKm: Double,
    /** 평균 진행 속도(km/분). */
    val kmPerMin: Double,
    val fatigue: FatigueResult,
    /** 니즈 타이머 결과: 반려견 배변·산책(프로필 간격), 충전. */
    val needs: List<Need>,
    /** 반려견 멀미 지수 0~100 (MotionComfortEngine). */
    val motionIndex: Int,
    /** 프로필이 선호하는 시설 (기본 반려견 놀이터). */
    val preferred: Set<Facility> = setOf(Facility.PET_ZONE),
    val previousStopId: String? = null,
    val policy: ScoringPolicy = ScoringPolicy.PET,
)

/**
 * 반려견 맞춤 쉼표 (CONCEPT F2, test_plan F2-01~F2-04). 쉼표의 [RestPlanner]를 확장했다.
 * - 쉬어야 하나: 운전자 피로 MEDIUM 이상, 배변·충전 니즈 긴급도 0.7 이상, 또는 멀미 지수 주의(40) 이상.
 * - 운전자 피로 HIGH: 안전이 먼저라 반려견 시설과 무관하게 가장 가까운 곳(같으면 졸음쉼터) — [RestPlanner]와 같다.
 * - 그 외: 휴게소만, 점수 = 시각 맞춤 + 멀미(가까울수록) + 운전자 피로 + 반려견 시설 충족 − 휴식 간격 페널티 ([ScoringPolicy.PET]).
 *   멀미가 주의 이상이면 15분 안(높음이면 5분 안)을 목표 시각으로 잡고, 반려견 놀이터를 원하는 시설에 더한다.
 * - 이유: 실제 점수 기여 상위 1~2개만 (멀미 · 배변 시각 · 시설 · 피로). 직전 제안은 15% 이상 나은 곳이 있어야 바꾼다.
 */
object PetRestPlanner {
    const val MOTION_CAUTION = 40
    const val MOTION_HIGH = 70
    const val MOTION_CAUTION_TARGET_MIN = 15.0
    const val MOTION_HIGH_TARGET_MIN = 5.0
    /** 프로필 선호 시설 하나의 기본 가중치 (니즈가 없어도 조금은 반영). */
    const val PREFERENCE_WEIGHT = 0.2

    fun plan(input: PetPlannerInput): PlanDecision {
        val kmPerMin = input.kmPerMin.takeIf { it > 0 } ?: DEFAULT_KM_PER_MIN
        val ahead = input.stops
            .filter { it.distanceAlongRouteKm - input.traveledKm >= RestPlanner.MIN_LEAD_KM }
            .sortedBy { it.distanceAlongRouteKm }
        fun minutesTo(s: RestStop) = (s.distanceAlongRouteKm - input.traveledKm) / kmPerMin
        fun nextKm() = ahead.firstOrNull()?.let { it.distanceAlongRouteKm - input.traveledKm }
        val window = ahead.filter { minutesTo(it) <= RestPlanner.HORIZON_MIN }

        if (input.fatigue.level == FatigueLevel.HIGH) {
            val nearest = window.minWithOrNull(compareBy<RestStop> { it.distanceAlongRouteKm }.thenBy { it.type != StopType.DROWSY_SHELTER })
                ?: return PlanDecision.NoCandidate(nextKm())
            return suggest(nearest, input.traveledKm, kmPerMin, listOf(Reason.Fatigue), 1.0, fatigueFirst = true)
        }

        val charge = input.needs.firstOrNull { it.kind == NeedKind.CHARGE }
        val reachLimitKm = charge?.reachKm?.let { input.traveledKm + it }
        val chargers = ahead.filter { Facility.EV_FAST in it.facilities && (reachLimitKm == null || it.distanceAlongRouteKm <= reachLimitKm) }
        val lastChargerSoon = charge != null && chargers.lastOrNull()?.let { minutesTo(it) <= RestPlanner.LAST_CHARGER_WARN_MIN } == true
        val motion = input.motionIndex.coerceIn(0, 100)
        val needsRest = input.fatigue.level == FatigueLevel.MEDIUM ||
            input.needs.any { it.urgency >= RestPlanner.NEED_URGENT } ||
            motion >= MOTION_CAUTION ||
            lastChargerSoon
        if (!needsRest) return PlanDecision.NoNeed

        val candidates = window.filter { it.type == StopType.SERVICE_AREA }.filter { charge == null || it in chargers }
        if (candidates.isEmpty()) return PlanDecision.NoCandidate(nextKm())

        val targetMin = listOfNotNull(
            input.needs.minOfOrNull { it.dueInMin.toDouble() },
            RestPlanner.MEDIUM_FATIGUE_TARGET_MIN.takeIf { input.fatigue.level == FatigueLevel.MEDIUM },
            when {
                motion >= MOTION_HIGH -> MOTION_HIGH_TARGET_MIN
                motion >= MOTION_CAUTION -> MOTION_CAUTION_TARGET_MIN
                else -> null
            },
        ).minOrNull() ?: RestPlanner.MEDIUM_FATIGUE_TARGET_MIN

        val wanted = desiredFacilities(input, motion)
        val wantedSum = wanted.values.sum()
        val policy = input.policy
        val pottyDue = input.needs.any { it.kind == NeedKind.PET_WALK && it.urgency >= RestPlanner.NEED_URGENT }

        data class Scored(val stop: RestStop, val b: ScoreBreakdown, val reasons: List<Reason>)
        // 같은 점수면 더 먼 곳(더 오래 달린 뒤 쉬기)이 앞에 오도록 먼 순서로 둔다.
        val scored = candidates.sortedByDescending { it.distanceAlongRouteKm }.map { s ->
            val present = wanted.filterKeys { it in s.facilities }
            val match = if (wantedSum > 0) present.values.sum() / wantedSum else 0.0
            val b = policy.score(minutesTo(s), targetMin, input.fatigue.score, match, input.fatigue.continuousDriveMinutes, motion)
            Scored(s, b, reasonsOf(b, present, wantedSum, pottyDue, policy))
        }
        val chosenId = policy.stabilize(scored.map { it.stop.id to it.b.total }, input.previousStopId)
        val best = scored.first { it.stop.id == chosenId }
        return suggest(best.stop, input.traveledKm, kmPerMin, best.reasons, best.b.total, fatigueFirst = false)
    }

    /** 원하는 시설과 가중치: 배변·산책 니즈 → 놀이터, 충전 니즈 → 급속충전, 멀미 → 놀이터(안정·산책), 프로필 선호. */
    internal fun desiredFacilities(input: PetPlannerInput, motion: Int): Map<Facility, Double> {
        val w = mutableMapOf<Facility, Double>()
        fun add(f: Facility, v: Double) { w[f] = (w[f] ?: 0.0) + v }
        input.needs.forEach { n ->
            when (n.kind) {
                NeedKind.PET_WALK -> add(Facility.PET_ZONE, max(n.urgency, 0.2))
                NeedKind.CHARGE -> add(Facility.EV_FAST, max(n.urgency, 0.2))
                NeedKind.TOILET, NeedKind.NURSING -> Unit
            }
        }
        if (motion >= MOTION_CAUTION) add(Facility.PET_ZONE, motion / 100.0)
        input.preferred.forEach { add(it, PREFERENCE_WEIGHT) }
        return w
    }

    /** 실제 점수 기여 상위 1~2개 (거짓 이유 금지). 시각 맞춤은 배변 시각이 됐을 때만 이유가 된다. */
    internal fun reasonsOf(b: ScoreBreakdown, present: Map<Facility, Double>, wantedSum: Double, pottyDue: Boolean, policy: ScoringPolicy): List<Reason> {
        val facility = present.map { (f, w) -> Reason.HasFacility(f, needOf(f)) as Reason to policy.facilityWeight * w / wantedSum }
        val others = listOfNotNull(
            (Reason.Motion to b.motion * policy.motionWeight),
            (Reason.Fatigue to b.fatigue * policy.fatigueWeight),
            (Reason.PottyDue to b.timing * policy.timingWeight).takeIf { pottyDue },
        )
        return (facility + others)
            .filter { it.second > RestPlanner.MIN_REASON_CONTRIBUTION }
            .sortedByDescending { it.second }
            .take(RestPlanner.MAX_REASONS)
            .map { it.first }
            .ifEmpty { listOf(Reason.Fatigue) }
    }

    private fun needOf(f: Facility) = when (f) {
        Facility.EV_FAST, Facility.EV_SLOW -> NeedKind.CHARGE
        else -> NeedKind.PET_WALK
    }

    private fun suggest(s: RestStop, traveledKm: Double, kmPerMin: Double, reasons: List<Reason>, score: Double, fatigueFirst: Boolean): PlanDecision.Suggest {
        val km = s.distanceAlongRouteKm - traveledKm
        return PlanDecision.Suggest(s, max(1, (km / kmPerMin).roundToInt()), km, reasons, score, fatigueFirst)
    }

    private const val DEFAULT_KM_PER_MIN = 100.0 / 60
}
