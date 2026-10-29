package com.emop.pause.engine.planner

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.SignalSample
import com.emop.pause.engine.fatigue.FatigueEngine
import com.emop.pause.engine.fatigue.FatigueInput
import com.emop.pause.engine.needs.NeedsEngine
import com.emop.pause.engine.needs.NeedsInput
import com.emop.pause.engine.needs.Passenger
import kotlin.math.roundToInt

/**
 * 출발 체크의 "오늘의 쉼표" — 주행 중 추천과 같은 [PetRestPlanner]를 앞으로 돌려 본다 (F2, 미리보기 = 실제 추천).
 *
 * 일정한 속도로 달린다고 보고 [STEP_SEC]마다 피로·배변 니즈 엔진을 진행시키고, 주행 중과 같은 간격으로 플래너를 부른다.
 * 플래너가 제안하면 받아들였다고 보고 그 휴게소까지 달린 뒤 [REST_MIN]분 쉬고(니즈 리셋·피로 감소) 이어 간다.
 * - 멀미 지수는 출발 전에는 모르므로 0으로 둔다 → 미리보기는 배변 간격·운전자 피로로만 정해진다.
 *   실제 주행에서 멀미가 주의를 넘으면 더 가까운 휴게소가 먼저 나올 수 있다.
 * - 신호(속도·실내 온도)는 없다고 보고 시간 기반 요인만 쓴다 (수면 시간은 반영).
 * - 주행 중 다시 계획할 때는 지금까지의 피로·니즈 누적값([fatigue]·[needsElapsedSec])과 가고 있는 휴게소부터 시작한다.
 */
object PetTripPreview {
    const val STEP_SEC = 10
    const val REST_MIN = 15

    fun plan(
        stops: List<RestStop>,
        totalKm: Double,
        kmPerMin: Double,
        passengers: List<Passenger>,
        sleepHours: Float?,
        preferred: Set<Facility> = setOf(Facility.PET_ZONE),
        minuteOfDay: Int = DEFAULT_START_MINUTE,
        fromKm: Double = 0.0,
        fatigue: FatigueEngine.Saved? = null,
        needsElapsedSec: List<Double>? = null,
        /** 이미 받아들여 가고 있는 휴게소 (그곳에서 쉰 뒤부터 이어 계획). */
        heading: RestStop? = null,
    ): TripPlan {
        val speed = kmPerMin.takeIf { it > 0 } ?: DEFAULT_KM_PER_MIN
        val fatigueEngine = FatigueEngine(sleepHours).also { e -> fatigue?.let(e::restore) }
        val needsEngine = NeedsEngine(passengers).also { e -> needsElapsedSec?.let(e::restore) }
        val interval = passengers.filterIsInstance<Passenger.Dog>().minOfOrNull { it.walkIntervalMin } ?: NeedsEngine.PET_WALK_MIN
        val wanted = preferred + Facility.PET_ZONE

        var sec = 0L
        var km = fromKm
        var goingTo = heading
        val picked = mutableListOf<PlannedStop>()
        fun tick(driving: Boolean) = (sec * 1000).let { ms ->
            val minute = Math.floorMod(minuteOfDay + (sec / 60).toInt(), MINUTES_PER_DAY)
            val f = fatigueEngine.update(FatigueInput(ms, minute, if (driving) DriveState.DRIVING else DriveState.PARKED, SignalSample(ms), emptySet()))
            val n = needsEngine.update(NeedsInput(ms, paused = !driving, capabilities = emptySet()))
            f to n
        }

        while (km < totalKm && sec < MAX_SEC) {
            val (f, n) = tick(driving = true)
            val target = goingTo
            if (target != null && km >= target.distanceAlongRouteKm) {
                picked += PlannedStop(target, (sec / 60.0).roundToInt(), wanted.filter { it in target.facilities })
                goingTo = null
                repeat(REST_MIN * 60 / STEP_SEC) { sec += STEP_SEC; tick(driving = false) }
                continue
            }
            if (target == null) {
                val d = PetRestPlanner.plan(PetPlannerInput(stops, km, speed, f, n.needs, motionIndex = 0, preferred = preferred))
                if (d is PlanDecision.Suggest) goingTo = d.stop
            }
            sec += STEP_SEC
            km += speed * STEP_SEC / 60.0
        }
        return TripPlan(picked, firstAdvancedMin = 0, intervalMin = interval)
    }

    private const val DEFAULT_KM_PER_MIN = 100.0 / 60
    private const val DEFAULT_START_MINUTE = 9 * 60
    private const val MINUTES_PER_DAY = 24 * 60
    private const val MAX_SEC = 24 * 3600L
}
