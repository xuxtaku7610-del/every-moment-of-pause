package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import com.emop.pause.engine.fatigue.FatigueEngine
import com.emop.pause.engine.fatigue.FatigueInput
import com.emop.pause.engine.fatigue.FatigueResult
import com.emop.pause.engine.needs.EvProfile
import com.emop.pause.engine.needs.NeedsEngine
import com.emop.pause.engine.needs.NeedsInput
import com.emop.pause.engine.needs.NeedsResult
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.motion.MotionComfortEngine
import com.emop.pause.engine.motion.MotionConstants
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.MotionInput
import com.emop.pause.engine.motion.MotionState
import com.emop.pause.engine.motion.RouteShapeProvider
import com.emop.pause.engine.motion.TbtRouteShape
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.PetPlannerInput
import com.emop.pause.engine.planner.PetRestPlanner
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.SuggestionPolicy

/** 출발 체크에서 정한 여행 설정 (반려견 프로필·전기차·수면 한마디). */
data class TripSetup(
    val passengers: List<Passenger> = emptyList(),
    val ev: EvProfile? = null,
    val sleepHours: Float? = null,
    val pet: PetProfile = PetProfile.DEFAULT,
)

data class BrainInput(
    val nowMs: Long,
    val minuteOfDay: Int,
    val mode: AppMode,
    val driveState: DriveState,
    val sample: SignalSample,
    val capabilities: Set<Signal>,
    val eta: EtaInfo?,
    /** 경로 총 거리(m). 지금까지 온 거리 = 총 거리 - 남은 거리. */
    val totalDistanceM: Int?,
    /** NaviHelper 회전 안내 (실차). 형상 소스가 [TbtRouteShape]일 때만 쓴다. */
    val turns: List<RouteTurn> = emptyList(),
)

/** 엔진 결과 한 장. 화면과 음성 흐름이 이것만 본다. */
data class BrainState(
    val fatigue: FatigueResult? = null,
    val needs: NeedsResult? = null,
    /** 새로 낼 제안. 음성 흐름이 [TripBrain.onShown]으로 받아 간다. */
    val pending: PlanDecision? = null,
    /** 수락해서 가고 있는 휴게소. */
    val heading: RestStop? = null,
    val traveledKm: Double = 0.0,
    /** 반려견 멀미 지수 (F1). */
    val motion: MotionState = MotionState(),
    /** 앞 구간 예보 (경로 형상이 있을 때만). */
    val forecast: MotionForecast? = null,
    /** 새로 말할 예보. 음성 흐름이 [TripBrain.onForecastSpoken]으로 받아 간다 (쿨다운 10분, F1-10). */
    val speakForecast: MotionForecast? = null,
    /** 앞 1km 곡률(1/m, [ROAD_STEP_M] 간격) — 주행 화면 도로 그림. 형상이 없으면 빈 목록. */
    val roadAhead: List<Float> = emptyList(),
)

/**
 * 멀미 예보·피로·배변 니즈 엔진과 반려견 맞춤 쉼표 플래너를 묶는다 (flowchart §2·§3 "10초마다").
 * 시뮬레이터·실차 모두 같은 입력을 1초마다 넣고, 플래너·예보는 [EVAL_INTERVAL_MS]마다 돈다.
 * 경로 형상([shape])은 경로 진행 거리 [shapeStartM]부터 적용한다: 시연 GPX([RouteShape]) 또는 실차 회전 안내·방위 추정([TbtRouteShape]).
 * 없으면 방위만, 그것도 없으면 종방향만.
 * 순수 로직이라 시나리오를 그대로 재생해 단위 테스트한다.
 */
class TripBrain(
    private val setup: TripSetup,
    shape: RouteShapeProvider? = null,
    private val shapeStartM: Int = 0,
    private val stops: () -> List<RestStop>,
) {
    private val fatigue = FatigueEngine(setup.sleepHours)
    private val needs = NeedsEngine(setup.passengers, setup.ev)
    private val motion = MotionComfortEngine(setup.pet, shape)
    private val tbt = shape as? TbtRouteShape
    private var lastTurns: List<RouteTurn>? = null
    private val preferred = setup.pet.facilityPrefs.mapNotNull(Facility::fromCode).toSet()
    private var lastForecastMs: Long? = null
    /** 마지막으로 말한 예보: (시각, 구간 시작, 구간 끝) — 경로 진행 거리(m) 기준. */
    private var lastSpokenForecast: Triple<Long, Double, Double>? = null
    val policy = SuggestionPolicy()
    private var lastEvalMs: Long? = null
    private var awaitingReply = false
    /** 직전에 보여 준 제안. "나중에" 뒤 재평가 때 히스테리시스 기준 (ScoringPolicy). */
    private var lastSuggestedId: String? = null
    var state = BrainState()
        private set

    fun tick(i: BrainInput): BrainState {
        val f = fatigue.update(FatigueInput(i.nowMs, i.minuteOfDay, i.driveState, i.sample, i.capabilities))
        val n = needs.update(
            NeedsInput(i.nowMs, i.mode == AppMode.PAUSE, i.capabilities, i.sample.batterySocPercent, i.eta?.remainDistanceM, i.eta?.remainTimeSec),
        )
        val traveledKm = traveledKm(i)
        val routeM = (traveledKm * 1000 - shapeStartM).takeIf { motion.shape != null }
        if (tbt != null && routeM != null) {
            i.sample.headingDeg?.let { tbt.observe(routeM, it) }
            if (i.turns != lastTurns) {
                lastTurns = i.turns
                tbt.onTurns(routeM, i.turns)
            }
        }
        val m = motion.update(MotionInput(i.nowMs, i.sample.speedKph, i.sample.headingDeg, routeM))
        var forecast = state.forecast
        var speak = state.speakForecast
        if (i.mode == AppMode.DRIVING && routeM != null && lastForecastMs.let { it == null || i.nowMs - it >= EVAL_INTERVAL_MS }) {
            lastForecastMs = i.nowMs
            forecast = i.sample.speedKph?.let { motion.forecast(routeM, it) }
            if (forecast != null && speak == null && shouldSpeak(forecast, routeM, i.nowMs)) speak = forecast
        } else if (i.mode != AppMode.DRIVING) {
            forecast = null
        }
        var heading = state.heading
        // 수락한 휴게소에 들렀거나(멈춤) 지나쳤으면 다음 제안을 받을 수 있다.
        if (heading != null && (i.mode == AppMode.PAUSE || traveledKm > heading.distanceAlongRouteKm + 1)) heading = null
        var pending = state.pending
        val due = lastEvalMs.let { it == null || i.nowMs - it >= EVAL_INTERVAL_MS }
        // 휴게소 목록이 아직 없으면(출발 직후 로딩 중) 계획하지 않는다 — "앞쪽 휴게소 없음"을 잘못 말하지 않게.
        // 내장 기본 데이터가 항상 있으므로 빈 목록은 로딩 중이라는 뜻이다.
        val loaded = stops().isNotEmpty()
        if (due && loaded && i.mode == AppMode.DRIVING && heading == null && pending == null && !awaitingReply && policy.canSuggest(i.nowMs, f.level)) {
            lastEvalMs = i.nowMs
            val kmPerMin = i.eta?.takeIf { it.remainTimeSec > 0 }?.let { it.remainDistanceM / 1000.0 / (it.remainTimeSec / 60.0) } ?: 0.0
            val d = PetRestPlanner.plan(PetPlannerInput(stops(), traveledKm, kmPerMin, f, n.needs, m.index, preferred, lastSuggestedId))
            if (d !is PlanDecision.NoNeed) pending = d
        }
        val road = routeM?.let { at -> motion.shape?.let { sh -> List(ROAD_SAMPLES) { k -> sh.curvatureAt(at + k * ROAD_STEP_M).toFloat() } } }.orEmpty()
        state = BrainState(f, n, pending, heading, traveledKm, m, forecast, speak, road)
        return state
    }

    /**
     * 5km 안에 다가온 구간만, 이미 들어선 구간(앞 [ANNOUNCE_MIN_AHEAD_M] 미만)은 말하지 않는다.
     * 같은 구간(직전에 말한 구간 끝 + 병합 간격 안에서 시작)은 다시 말하지 않고, 다른 구간이어도 10분 쿨다운 (F1-10).
     */
    private fun shouldSpeak(f: MotionForecast, routeM: Double, nowMs: Long): Boolean {
        if (f.startInM > ANNOUNCE_WITHIN_M || f.startInM < ANNOUNCE_MIN_AHEAD_M) return false
        val startAbs = routeM + f.startInM
        val last = lastSpokenForecast ?: return true
        val sameSegment = startAbs <= last.third + MotionConstants.MERGE_GAP_M
        val cooled = nowMs - last.first >= MotionConstants.FORECAST_COOLDOWN_MIN * 60_000L
        return !sameSegment && cooled
    }

    /** 발표 모드 점프로 건너뛴 구간의 예보: 말한 것으로 치지 않고 버린다 (점프한 뒤 앞 구간을 다시 말할 수 있게). */
    fun dropForecast() {
        state = state.copy(speakForecast = null)
    }

    /** 음성 흐름이 예보를 읽기 시작함. */
    fun onForecastSpoken(nowMs: Long) {
        val f = state.speakForecast ?: return
        val routeM = state.traveledKm * 1000 - shapeStartM
        lastSpokenForecast = Triple(nowMs, routeM + f.startInM, routeM + f.startInM + f.lengthM)
        state = state.copy(speakForecast = null)
    }

    /** 음성 흐름이 제안을 꺼내 읽기 시작함. 일반 안내(NoCandidate)는 "나중에"와 같은 간격으로 다시 알린다. */
    fun onShown(nowMs: Long) {
        val d = state.pending ?: return
        state = state.copy(pending = null)
        when (d) {
            is PlanDecision.NoCandidate -> policy.onDeclined(nowMs, state.fatigue?.level)
            is PlanDecision.Suggest -> {
                lastSuggestedId = d.stop.id
                awaitingReply = true
            }
            PlanDecision.NoNeed -> Unit
        }
    }

    fun onAccepted(stop: RestStop) {
        awaitingReply = false
        policy.onAccepted()
        lastSuggestedId = null
        state = state.copy(heading = stop)
    }

    fun onDeclined(nowMs: Long) {
        awaitingReply = false
        policy.onDeclined(nowMs, state.fatigue?.level)
    }

    /** 여행 상태 저장 (프로세스 재시작 복원, [TripResume]). */
    fun save(nowMs: Long, routeKey: String?) = TripResume(nowMs, TripResume.hash(routeKey), fatigue.save(), needs.save(), lastSuggestedId, state.heading?.id)

    /** 저장된 누적값을 되살린다. 동승자 구성이 달라 니즈를 못 살려도 피로는 살린다. */
    fun restore(r: TripResume) {
        fatigue.restore(r.fatigue)
        needs.restore(r.needsElapsedSec)
        lastSuggestedId = r.lastSuggestedId
        r.headingId?.let { id -> stops().firstOrNull { it.id == id } }?.let { state = state.copy(heading = it) }
    }

    private fun traveledKm(i: BrainInput): Double {
        val total = i.totalDistanceM ?: return state.traveledKm
        val remain = i.eta?.remainDistanceM ?: return state.traveledKm
        return ((total - remain) / 1000.0).coerceAtLeast(0.0)
    }

    companion object {
        const val EVAL_INTERVAL_MS = 10_000L
        const val ANNOUNCE_WITHIN_M = 5_000.0
        const val ANNOUNCE_MIN_AHEAD_M = 300.0
        const val ROAD_STEP_M = 50.0
        const val ROAD_SAMPLES = 21
    }
}
