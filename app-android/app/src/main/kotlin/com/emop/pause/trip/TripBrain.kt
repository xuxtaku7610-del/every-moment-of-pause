package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
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
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.PlannerInput
import com.emop.pause.engine.planner.RestPlanner
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.SuggestionPolicy

/** 출발 준비에서 정한 여행 설정 (동승자 프리셋·수면 한마디). */
data class TripSetup(
    val passengers: List<Passenger> = emptyList(),
    val ev: EvProfile? = null,
    val sleepHours: Float? = null,
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
)

/**
 * 피로·니즈 엔진과 휴게소 플래너를 묶는다 (flowchart §2 "10초마다").
 * 시뮬레이터·실차 모두 같은 입력을 1초마다 넣고, 플래너는 [EVAL_INTERVAL_MS]마다 돈다.
 * 순수 로직이라 시나리오를 그대로 재생해 단위 테스트한다.
 */
class TripBrain(setup: TripSetup, private val stops: () -> List<RestStop>) {
    private val fatigue = FatigueEngine(setup.sleepHours)
    private val needs = NeedsEngine(setup.passengers, setup.ev)
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
            val d = RestPlanner.plan(PlannerInput(stops(), traveledKm, kmPerMin, f, n.needs, previousStopId = lastSuggestedId))
            if (d !is PlanDecision.NoNeed) pending = d
        }
        state = BrainState(f, n, pending, heading, traveledKm)
        return state
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
    }
}
