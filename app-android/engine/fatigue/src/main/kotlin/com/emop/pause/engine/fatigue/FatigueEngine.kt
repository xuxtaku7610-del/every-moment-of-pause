package com.emop.pause.engine.fatigue

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import kotlin.math.min
import kotlin.math.roundToInt

enum class FatigueLevel { LOW, MEDIUM, HIGH }

/** 피로 점수에 기여하는 요인. 화면에는 상위 3개를 보여준다 (test_plan 2장). */
enum class FatigueFactor { CONTINUOUS_DRIVE, TIME_OF_DAY, MONOTONY, CABIN_WARMTH, SLEEP_DEBT }

/** 요인 하나의 기여. points는 0~100 점수 중 이 요인이 차지한 몫, detailMinutes는 화면 문구용(연속 주행 1시간 50분 등). */
data class Contribution(val factor: FatigueFactor, val points: Int, val detailMinutes: Int? = null)

data class FatigueResult(
    val score: Int,
    val level: FatigueLevel,
    /** 점수가 0보다 큰 요인 중 상위 3개 (큰 순서). */
    val topContributors: List<Contribution>,
    /** 이 차에서 실제로 쓰는 요인. 미지원 신호 요인은 빠지고 나머지로 재정규화된다. */
    val activeFactors: Set<FatigueFactor>,
    /** 지원하는 신호가 30초 넘게 들어오지 않음 → 시간 기반 요인만 사용 중. */
    val signalLost: Boolean,
    val continuousDriveMinutes: Int,
)

data class FatigueInput(
    val nowMs: Long,
    /** 현지 시각(0~1439분). 시간대 요인에 쓴다. */
    val minuteOfDay: Int,
    val driveState: DriveState,
    val sample: SignalSample,
    val capabilities: Set<Signal>,
)

/**
 * 피로 위험 지수 v1 (규칙 기반, architecture §3.1 `engine:fatigue`).
 *
 * 점수 = 연속 주행 점수(0~[DRIVE_MAX_POINTS]) + 보조 요인 점수(0~[OTHER_POINTS]).
 * - 연속 주행: 2시간에 [HIGH_THRESHOLD]점이 되도록 선형 증가. 시간만 있으면 계산되므로 항상 쓴다.
 * - 보조 요인(시간대·단조로움·실내 온열·수면 부족)은 가중 평균. 신호가 없거나 입력이 없는 요인은
 *   가중치를 0으로 두고 나머지로 재정규화한다 (신호 수급 전략).
 * - 정차가 끝날 때 정차 시간에 따라 누적값을 줄인다: 5분 미만 그대로, 15분 이상이면 [RESET_KEEP]만 남김.
 */
class FatigueEngine(sleepHours: Float? = null) {
    var sleepHours: Float? = sleepHours?.takeIf { it in 0f..24f }
        set(value) {
            field = value?.takeIf { it in 0f..24f }
        }

    private var lastMs: Long? = null
    private var driveSec = 0.0
    private var monotonySec = 0.0
    private var cabinSec = 0.0
    private var stopSec = 0.0
    private var parked = true
    private var lastSpeed: Float? = null
    private var lastSpeedMs = Long.MIN_VALUE
    private var lastWarm: Boolean? = null
    private var lastWarmMs = Long.MIN_VALUE

    /** 여행 상태 복원용 누적값 (COMMERCIAL_READINESS A P1). 시각 기준값은 저장하지 않는다 — 복원 직후 첫 입력은 dt 0. */
    data class Saved(val driveSec: Double, val monotonySec: Double, val cabinSec: Double)

    fun save() = Saved(driveSec, monotonySec, cabinSec)

    /** 프로세스가 죽어 있던 동안은 알 수 없으므로 더하지 않는다. 정차 리셋은 복원 뒤 정차부터 다시 센다. */
    fun restore(s: Saved) {
        driveSec = s.driveSec.coerceAtLeast(0.0)
        monotonySec = s.monotonySec.coerceAtLeast(0.0)
        cabinSec = s.cabinSec.coerceAtLeast(0.0)
        stopSec = 0.0
        lastMs = null
    }

    fun update(input: FatigueInput): FatigueResult {
        // 시계가 거꾸로 가거나(시간대 변경) 오래 멈췄다 돌아와도 누적값이 튀지 않게 묶는다 (test_plan 2장 P2).
        val dtSec = lastMs?.let { ((input.nowMs - it) / 1000.0).coerceIn(0.0, MAX_STEP_SEC) } ?: 0.0
        lastMs = input.nowMs

        val s = input.sample
        val caps = input.capabilities
        if (s.speedKph != null) { lastSpeed = s.speedKph; lastSpeedMs = input.nowMs }
        val warmNow = warm(s, caps)
        if (warmNow != null) { lastWarm = warmNow; lastWarmMs = input.nowMs }
        val speedAvailable = Signal.SPEED in caps && fresh(lastSpeedMs, input.nowMs)
        val cabinCaps = Signal.HVAC_TEMP in caps || Signal.HVAC_RECIRCULATION in caps
        val cabinAvailable = cabinCaps && fresh(lastWarmMs, input.nowMs)
        val signalLost = (Signal.SPEED in caps && !speedAvailable) || (cabinCaps && !cabinAvailable)

        val moving = input.driveState != DriveState.PARKED
        if (moving) {
            if (parked) applyStopReset()
            parked = false
            driveSec += dtSec
            if (speedAvailable && (lastSpeed ?: 0f) >= MONOTONY_SPEED_KPH) monotonySec += dtSec
            if (cabinAvailable && lastWarm == true) cabinSec += dtSec
        } else {
            if (!parked) stopSec = 0.0
            parked = true
            stopSec += dtSec
        }
        return evaluate(input.minuteOfDay, speedAvailable, cabinAvailable, signalLost)
    }

    private fun applyStopReset() {
        val keep = when {
            stopSec < SHORT_STOP_SEC -> 1.0
            stopSec >= FULL_RESET_STOP_SEC -> RESET_KEEP
            else -> 1.0 - (1.0 - RESET_KEEP) * (stopSec - SHORT_STOP_SEC) / (FULL_RESET_STOP_SEC - SHORT_STOP_SEC)
        }
        driveSec *= keep
        monotonySec *= keep
        cabinSec *= keep
        stopSec = 0.0
    }

    private fun evaluate(minuteOfDay: Int, speedAvailable: Boolean, cabinAvailable: Boolean, signalLost: Boolean): FatigueResult {
        val driveMin = driveSec / 60.0
        val drivePoints = min(DRIVE_MAX_POINTS, driveMin * HIGH_THRESHOLD / DRIVE_HIGH_MIN)

        // 보조 요인: (가중치, 0~1 값, 화면용 분)
        val others = buildList {
            add(Triple(FatigueFactor.TIME_OF_DAY, W_TIME_OF_DAY, timeOfDay(minuteOfDay)))
            if (speedAvailable) add(Triple(FatigueFactor.MONOTONY, W_MONOTONY, min(1.0, monotonySec / ACCUM_FULL_SEC)))
            if (cabinAvailable) add(Triple(FatigueFactor.CABIN_WARMTH, W_CABIN, min(1.0, cabinSec / ACCUM_FULL_SEC)))
            sleepHours?.let { add(Triple(FatigueFactor.SLEEP_DEBT, W_SLEEP, ((GOOD_SLEEP_H - it) / SLEEP_DEBT_SPAN_H).coerceIn(0.0, 1.0))) }
        }
        val weightSum = others.sumOf { it.second }
        val contributions = mutableListOf(
            Contribution(FatigueFactor.CONTINUOUS_DRIVE, drivePoints.roundToInt(), driveMin.toInt()),
        )
        var total = drivePoints
        for ((factor, w, v) in others) {
            val points = OTHER_POINTS * w * v / weightSum
            total += points
            val detail = when (factor) {
                FatigueFactor.MONOTONY -> (monotonySec / 60).toInt()
                FatigueFactor.CABIN_WARMTH -> (cabinSec / 60).toInt()
                else -> null
            }
            contributions += Contribution(factor, points.roundToInt(), detail)
        }
        val score = min(100, total.roundToInt())
        return FatigueResult(
            score = score,
            level = levelOf(score),
            topContributors = contributions.filter { it.points > 0 }.sortedByDescending { it.points }.take(3),
            activeFactors = setOf(FatigueFactor.CONTINUOUS_DRIVE) + others.map { it.first },
            signalLost = signalLost,
            continuousDriveMinutes = driveMin.toInt(),
        )
    }

    private fun fresh(lastSeenMs: Long, nowMs: Long) = lastSeenMs != Long.MIN_VALUE && nowMs - lastSeenMs <= SIGNAL_HOLD_MS

    private fun warm(s: SignalSample, caps: Set<Signal>): Boolean? {
        val temp = s.hvacTempC.takeIf { Signal.HVAC_TEMP in caps }
        val recirc = s.recirculationOn.takeIf { Signal.HVAC_RECIRCULATION in caps }
        if (temp == null && recirc == null) return null
        return (temp != null && temp >= WARM_CABIN_C) || recirc == true
    }

    companion object {
        const val HIGH_THRESHOLD = 70
        const val MEDIUM_THRESHOLD = 40
        const val DRIVE_HIGH_MIN = 120.0
        const val DRIVE_MAX_POINTS = 85.0
        const val OTHER_POINTS = 30.0
        const val W_TIME_OF_DAY = 0.30
        const val W_MONOTONY = 0.25
        const val W_CABIN = 0.20
        const val W_SLEEP = 0.25
        const val MONOTONY_SPEED_KPH = 80f
        const val WARM_CABIN_C = 25f
        const val ACCUM_FULL_SEC = 3600.0
        const val GOOD_SLEEP_H = 7.0
        const val SLEEP_DEBT_SPAN_H = 3.0
        const val SHORT_STOP_SEC = 300.0
        const val FULL_RESET_STOP_SEC = 900.0
        const val RESET_KEEP = 0.2
        const val SIGNAL_HOLD_MS = 30_000L
        const val MAX_STEP_SEC = 60.0

        fun levelOf(score: Int) = when {
            score >= HIGH_THRESHOLD -> FatigueLevel.HIGH
            score >= MEDIUM_THRESHOLD -> FatigueLevel.MEDIUM
            else -> FatigueLevel.LOW
        }

        /** 시간대 위험(0~1): 새벽 2~6시 최고, 자정~2시·밤 10시 이후·오후 2~4시(식곤증) 가중. */
        fun timeOfDay(minuteOfDay: Int): Double {
            val h = Math.floorMod(minuteOfDay, 1440) / 60
            return when (h) {
                in 2..5 -> 1.0
                0, 1 -> 0.8
                14, 15 -> 0.8
                22, 23 -> 0.6
                else -> 0.0
            }
        }
    }
}
