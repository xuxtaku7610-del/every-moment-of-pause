package com.emop.pause.engine.planner

import com.emop.pause.engine.needs.NeedKind
import com.emop.pause.engine.needs.NeedsEngine
import com.emop.pause.engine.needs.Passenger
import kotlin.math.roundToInt

/** 출발 전에 잡아 두는 쉼표 하나. 주행 중 실제 제안은 [RestPlanner]가 그때그때 다시 정한다. */
data class PlannedStop(val stop: RestStop, val etaMin: Int, val matched: List<Facility>)

data class TripPlan(
    val stops: List<PlannedStop>,
    /** 수면 부족으로 첫 휴식을 앞당긴 분 (0이면 기본 간격). */
    val firstAdvancedMin: Int,
    /** 계획에 쓴 간격(분). */
    val intervalMin: Int,
)

/**
 * 출발 준비 화면의 "오늘의 쉼표 N번"과 경로선 쉼표 (UI_SPEC §4.2, §4.7).
 *
 * - 간격 = min(운전 2시간, 동승자 니즈 간격). 제안 민감도가 빠르면 90분.
 * - 수면 6시간 미만이면 첫 휴식만 20분(5시간 미만 30분) 앞당긴다.
 * - 목표 시각보다 늦지 않은 휴게소 중 가장 늦은 곳(목표 근처)을 고르고, 같은 거리대면 니즈 편의시설이 많은 곳.
 *   전기차면 급속충전 휴게소를 우선한다. 졸음쉼터는 계획에 넣지 않는다.
 * - 남은 거리가 간격 안이면 끝낸다.
 */
object TripPlanPreview {
    const val DRIVE_LIMIT_MIN = 120
    const val EAGER_LIMIT_MIN = 90
    const val MIN_GAP_MIN = 30
    const val REPLAN_MIN_GAP_MIN = 10

    fun plan(
        stops: List<RestStop>,
        totalKm: Double,
        kmPerMin: Double,
        passengers: List<Passenger>,
        ev: Boolean,
        sleepHours: Float?,
        eager: Boolean = false,
        /** 주행 중 다시 계획할 때: 지금까지 온 거리(km). 이 앞쪽 휴게소만 본다. */
        fromKm: Double = 0.0,
        /** 주행 중 다시 계획할 때: 첫 휴식까지 남은 분(마지막 휴식 뒤 운전 시간·가장 급한 니즈로 계산). null이면 출발 기준. */
        firstGapMin: Int? = null,
    ): TripPlan {
        val speed = kmPerMin.takeIf { it > 0 } ?: (100.0 / 60)
        val needIntervals = passengers.map {
            when (it) {
                is Passenger.Child -> NeedsEngine.intervalFor(it.age).second
                is Passenger.Dog -> it.walkIntervalMin
            }
        }
        val interval = (listOf(if (eager) EAGER_LIMIT_MIN else DRIVE_LIMIT_MIN) + needIntervals).min()
        val advance = when {
            sleepHours == null -> 0
            sleepHours < 5f -> 30
            sleepHours < 6f -> 20
            else -> 0
        }
        val wanted = buildSet {
            passengers.forEach {
                when (it) {
                    is Passenger.Child -> add(if (NeedsEngine.intervalFor(it.age).first == NeedKind.NURSING) Facility.NURSING_ROOM else Facility.KIDS_ZONE)
                    is Passenger.Dog -> add(Facility.PET_ZONE)
                }
            }
            if (ev) add(Facility.EV_FAST)
        }
        val areas = stops.filter { it.type == StopType.SERVICE_AREA }.sortedBy { it.distanceAlongRouteKm }
        val picked = mutableListOf<PlannedStop>()
        var lastKm = fromKm
        var first = true
        while (true) {
            val replanFirst = first && firstGapMin != null
            val gap = if (replanFirst) firstGapMin!!.coerceAtLeast(REPLAN_MIN_GAP_MIN) else if (first) interval - advance else interval
            if (totalKm - lastKm <= gap * speed) break
            val targetKm = lastKm + gap * speed
            // 다시 계획할 때 첫 쉼표는 2km 앞부터 (니즈가 급하면 가까운 곳도 후보), 그 밖에는 직전 쉼표에서 30분 이상.
            val minAhead = if (replanFirst) RestPlanner.MIN_LEAD_KM else MIN_GAP_MIN * speed
            val window = areas.filter { it.distanceAlongRouteKm > lastKm + minAhead && it.distanceAlongRouteKm <= targetKm }
            // 다시 계획할 때 남은 시간 안에 휴게소가 없으면 앞쪽에서 가장 가까운 휴게소 (계획이 비지 않게).
            val reach = if (replanFirst && window.isEmpty()) listOfNotNull(areas.firstOrNull { it.distanceAlongRouteKm > lastKm + minAhead }) else window
            val pool = if (ev) reach.filter { Facility.EV_FAST in it.facilities }.ifEmpty { reach } else reach
            // 목표에 가까운 30분 구간 안에서 편의시설이 가장 잘 맞는 곳, 같으면 목표에 가까운 곳.
            val near = pool.filter { targetKm - it.distanceAlongRouteKm <= MIN_GAP_MIN * speed }.ifEmpty { pool }
            val best = near.maxWithOrNull(compareBy<RestStop> { (it.facilities intersect wanted).size }.thenBy { it.distanceAlongRouteKm }) ?: break
            picked += PlannedStop(best, (best.distanceAlongRouteKm / speed).roundToInt(), wanted.filter { it in best.facilities })
            lastKm = best.distanceAlongRouteKm
            first = false
        }
        return TripPlan(picked, if (picked.isNotEmpty()) advance else 0, interval)
    }
}
