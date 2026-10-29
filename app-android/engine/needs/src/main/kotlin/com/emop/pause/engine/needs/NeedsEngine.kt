package com.emop.pause.engine.needs

import com.emop.pause.core.model.Signal
import kotlin.math.max

/** 동승자 프리셋 한 명. 서버로 보내지 않는다 (architecture §6). */
sealed interface Passenger {
    data class Child(val age: Int) : Passenger
    data object Dog : Passenger
}

enum class NeedKind { TOILET, NURSING, PET_WALK, CHARGE }

/**
 * 니즈 하나. urgency는 0~1(1이면 지금 쉬어야 함), dueInMin은 그때까지 남은 분.
 * 충전 니즈의 reachKm는 예비 잔량까지 갈 수 있는 거리로, 휴게소 후보를 이 안에서만 고르게 한다.
 */
data class Need(val kind: NeedKind, val urgency: Double, val dueInMin: Int, val reachKm: Double? = null)

data class NeedsResult(
    /** 긴급한 순서. */
    val needs: List<Need>,
    /** 배터리 신호를 못 읽거나 전기차가 아니면 false → 정차 화면에 "충전 정보 없음". */
    val chargeInfoAvailable: Boolean,
)

/** 전기차 소비 모델. 기본값: 100% → 500km, 예비 10%. */
data class EvProfile(val socPercentPerKm: Double = 0.2, val reservePercent: Int = 10)

data class NeedsInput(
    val nowMs: Long,
    /** AppMode = PAUSE (휴게소·충전 정차). 신호 대기·정체는 해당 없음. */
    val paused: Boolean,
    val capabilities: Set<Signal>,
    val socPercent: Int? = null,
    val remainDistanceM: Int? = null,
    val remainTimeSec: Int? = null,
)

/**
 * 가족 니즈 타이머 (architecture §3.1 `engine:needs`).
 * - 아이: 나이별 화장실(또는 수유) 간격, 반려견: 산책 간격. 여행 시간으로 흐르고, [RESET_PAUSE_SEC] 이상 멈춤이면 리셋.
 * - 충전: 남은 거리보다 예비 잔량까지 갈 수 있는 거리가 짧으면 생성.
 */
class NeedsEngine(
    private val passengers: List<Passenger>,
    private val ev: EvProfile? = null,
) {
    private val timers: List<Pair<NeedKind, Int>> = passengers.mapNotNull { p ->
        when (p) {
            is Passenger.Child -> intervalFor(p.age)
            Passenger.Dog -> NeedKind.PET_WALK to PET_WALK_MIN
        }
    }
    private val elapsedSec = DoubleArray(timers.size)
    private var lastMs: Long? = null
    private var pausedSec = 0.0

    /** 여행 상태 복원용: 니즈 타이머별 경과 초 (동승자 순서). */
    fun save(): List<Double> = elapsedSec.toList()

    /** 동승자 구성이 같을 때(타이머 수가 같을 때)만 복원한다. */
    fun restore(saved: List<Double>): Boolean {
        if (saved.size != elapsedSec.size) return false
        saved.forEachIndexed { i, v -> elapsedSec[i] = v.coerceAtLeast(0.0) }
        lastMs = null
        pausedSec = 0.0
        return true
    }

    fun update(input: NeedsInput): NeedsResult {
        val dt = lastMs?.let { ((input.nowMs - it) / 1000.0).coerceIn(0.0, MAX_STEP_SEC) } ?: 0.0
        lastMs = input.nowMs
        if (input.paused) {
            pausedSec += dt
            if (pausedSec >= RESET_PAUSE_SEC) elapsedSec.fill(0.0)
        } else {
            pausedSec = 0.0
            for (i in elapsedSec.indices) elapsedSec[i] += dt
        }

        val needs = timers.mapIndexed { i, (kind, intervalMin) ->
            val elapsedMin = elapsedSec[i] / 60
            Need(kind, (elapsedMin / intervalMin).coerceIn(0.0, 1.0), max(0, (intervalMin - elapsedMin).toInt()))
        }.toMutableList()

        val chargeInfo = ev != null && Signal.BATTERY_SOC in input.capabilities && input.socPercent != null
        if (chargeInfo) chargeNeed(ev!!, input)?.let { needs += it }
        return NeedsResult(needs.sortedByDescending { it.urgency }, chargeInfo)
    }

    private fun chargeNeed(ev: EvProfile, input: NeedsInput): Need? {
        val soc = input.socPercent ?: return null
        val remainKm = (input.remainDistanceM ?: return null) / 1000.0
        val reachKm = max(0.0, (soc - ev.reservePercent) / ev.socPercentPerKm)
        if (reachKm >= remainKm) return null
        val kmPerMin = input.remainTimeSec?.takeIf { it > 0 }?.let { remainKm / (it / 60.0) } ?: DEFAULT_KM_PER_MIN
        val dueMin = (reachKm / kmPerMin).toInt()
        return Need(NeedKind.CHARGE, (1.0 - dueMin / CHARGE_HORIZON_MIN).coerceIn(0.0, 1.0), dueMin, reachKm)
    }

    companion object {
        const val PET_WALK_MIN = 120
        const val RESET_PAUSE_SEC = 300.0
        const val CHARGE_HORIZON_MIN = 90.0
        const val DEFAULT_KM_PER_MIN = 100.0 / 60
        const val MAX_STEP_SEC = 60.0

        /** 나이별 간격(분): 2세 이하 수유·기저귀 150, 3~6세 화장실 90, 7세 이상 120. */
        fun intervalFor(age: Int): Pair<NeedKind, Int> = when {
            age <= 2 -> NeedKind.NURSING to 150
            age <= 6 -> NeedKind.TOILET to 90
            else -> NeedKind.TOILET to 120
        }
    }
}
