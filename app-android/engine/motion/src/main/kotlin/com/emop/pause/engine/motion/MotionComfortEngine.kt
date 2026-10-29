package com.emop.pause.engine.motion

import com.emop.pause.core.model.PetProfile
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class MotionLevel { LOW, CAUTION, HIGH }

fun motionLevelOf(index: Int): MotionLevel = when {
    index >= MotionConstants.HIGH -> MotionLevel.HIGH
    index >= MotionConstants.CAUTION -> MotionLevel.CAUTION
    else -> MotionLevel.LOW
}

/** 1초(또는 [MotionComfortEngine.sampleHz]) 입력 한 개. 없는 신호는 null. */
data class MotionInput(
    val nowMs: Long,
    /** 잡음 처리한 속도 (SignalSmoother). */
    val speedKph: Float?,
    /** 진행 방위(도, 0=북, 시계 방향). NaviHelper `CurrentLocationInfo.heading` 또는 시뮬레이터. */
    val headingDeg: Double? = null,
    /** 경로 형상 위의 현재 위치(m). 방위가 없을 때 곡률로 횡가속을 구한다. */
    val routeM: Double? = null,
)

/** 지금 상태. [index]는 0~100 "주의 예보" 지수 (의학적 진단 아님). */
data class MotionState(
    val index: Int = 0,
    val level: MotionLevel = MotionLevel.LOW,
    /** ISO 2631-1 멀미 누적량(MSDV, m/s^1.5)을 단순화한 값 (누설 포함). */
    val msdv: Double = 0.0,
    val peak: Int = 0,
    /** 이번 여행 주행 시간(초, 정차 제외). */
    val drivingSec: Long = 0,
    /** 지수가 처음 주의(40)에 닿은 주행 시간(초). 기록 패턴(F7)에 쓴다. */
    val firstCautionDrivingSec: Long? = null,
    /** 횡가속을 무엇으로 구했는지: 방위 · 경로 형상 · 없음(종방향만). */
    val lateralSource: LateralSource = LateralSource.NONE,
)

enum class LateralSource { HEADING, SHAPE, NONE }

/** 앞 구간 예보 (CONCEPT 4장). 거리는 현재 위치 기준. */
data class MotionForecast(
    val startInM: Double,
    val lengthM: Double,
    val durationSec: Int,
    /** 지금 속도 그대로 구간 끝까지 갔을 때 예상 지수. */
    val predictedIndex: Int,
    /** 구간 최대 곡률에서 편안한 횡가속([MotionConstants.A_COMFORT])이 되는 속도. */
    val recommendedKph: Int,
)

/**
 * 멀미 예보 엔진 (F1, docs/plan/motion_model.md).
 * 1) 가속도 추정: a_x = Δv/Δt, a_y = v·Δψ/Δt (방위) 또는 v²·κ(s) (경로 형상)
 * 2) 축마다 0.1~0.5Hz 대역 필터 → 제곱 합
 * 3) 누설 적분 D ← D·e^(−Δt/τ) + a_w²·Δt (주행 τ 120분, 정차 τ 15분), MSDV = √D
 * 4) 지수 = 100·(1 − e^(−S·MSDV/M₀)), S = 프로필 민감도
 * 순수 로직이라 시뮬레이터·실차 모두 같은 입력으로 돌고, [forecast]는 현재 상태를 복사해 앞 구간을 미리 돌린다.
 */
class MotionComfortEngine private constructor(
    private val sensitivity: Double,
    val shape: RouteShape?,
    val sampleHz: Double,
) {
    constructor(profile: PetProfile = PetProfile.DEFAULT, shape: RouteShape? = null, sampleHz: Double = 1.0) :
        this(MotionConstants.sensitivity(profile), shape, sampleHz)

    private var bandX = BandPass(sampleHz)
    private var bandY = BandPass(sampleHz)
    private var dose = 0.0
    private var lastMs: Long? = null
    private var lastV: Double? = null
    private var lastHeading: Double? = null
    var state = MotionState()
        private set

    fun update(i: MotionInput): MotionState {
        val kph = i.speedKph ?: return state
        val v = max(0.0, kph / 3.6)
        val prevMs = lastMs
        lastMs = i.nowMs
        val dt = prevMs?.let { (i.nowMs - it) / 1000.0 }
        if (dt == null || dt <= 0 || dt > MotionConstants.MAX_GAP_SEC) {
            // 처음이거나 신호가 끊겼다: 필터를 새로 시작하고 이전 값만 기억한다.
            bandX = BandPass(sampleHz)
            bandY = BandPass(sampleHz)
            lastV = v
            lastHeading = i.headingDeg
            return state
        }
        val ax = (v - (lastV ?: v)) / dt
        val (ay, src) = lateral(v, dt, i)
        lastV = v
        lastHeading = i.headingDeg
        return integrate(ax, ay, v, dt, src)
    }

    private fun lateral(v: Double, dt: Double, i: MotionInput): Pair<Double, LateralSource> {
        val h0 = lastHeading
        val h1 = i.headingDeg
        if (h0 != null && h1 != null) return v * Math.toRadians(wrap180(h1 - h0)) / dt to LateralSource.HEADING
        val s = i.routeM
        if (shape != null && s != null) return v * v * shape.curvatureAt(s) to LateralSource.SHAPE
        return 0.0 to LateralSource.NONE
    }

    private fun integrate(ax: Double, ay: Double, v: Double, dt: Double, src: LateralSource): MotionState {
        val fx = bandX.step(ax) * MotionConstants.K_LONGITUDINAL
        val fy = bandY.step(ay) * MotionConstants.K_LATERAL
        val moving = v >= MotionConstants.STOP_SPEED_MPS
        val tau = if (moving) MotionConstants.TAU_DRIVE_SEC else MotionConstants.TAU_REST_SEC
        dose = dose * exp(-dt / tau) + (fx * fx + fy * fy) * dt
        val msdv = sqrt(dose)
        val index = (100 * (1 - exp(-sensitivity * msdv / MotionConstants.INDEX_SCALE_MSDV))).roundToInt().coerceIn(0, 100)
        val driving = state.drivingSec + if (moving) dt.roundToInt() else 0
        val firstCaution = state.firstCautionDrivingSec ?: driving.takeIf { index >= MotionConstants.CAUTION }
        state = MotionState(index, motionLevelOf(index), msdv, max(state.peak, index), driving, firstCaution, src)
        return state
    }

    /**
     * 앞 구간 예보 (docs/plan/motion_model.md §6). 경로 형상이 없거나 거의 멈춰 있으면 null(= "곡선 정보 없음").
     * 현재 속도 그대로 [MotionConstants.FORECAST_MIN]분 앞까지 보고, v²·|κ| ≥ A_RISK인 첫 구간을 돌려준다.
     */
    fun forecast(atM: Double, speedKph: Float): MotionForecast? {
        val sh = shape ?: return null
        val v = speedKph / 3.6
        if (v < MIN_FORECAST_SPEED_MPS) return null
        val end = minOf(sh.lengthM, atM + v * MotionConstants.FORECAST_MIN * 60)
        var segStart: Double? = null
        var segEnd = 0.0
        var kMax = 0.0
        var d = atM
        while (d <= end) {
            val k = abs(sh.curvatureAt(d))
            if (v * v * k >= MotionConstants.A_RISK) {
                if (segStart == null) segStart = d
                else if (d - segEnd > MotionConstants.MERGE_GAP_M) break
                segEnd = d
                kMax = max(kMax, k)
            }
            d += RouteShape.STEP_M
        }
        val start = segStart ?: return null
        val length = max(RouteShape.STEP_M, segEnd - start)
        return MotionForecast(
            startInM = start - atM,
            lengthM = length,
            durationSec = travelSec(sh, start, segEnd, v).roundToInt(),
            predictedIndex = simulate(atM, segEnd, v),
            recommendedKph = (sqrt(MotionConstants.A_COMFORT / kMax) * 3.6).roundToInt(),
        )
    }

    /** 위치 [s]에서 "안내 없이" 운전자의 속도: 지금 속도, 단 굽이에서는 [MotionConstants.A_DRIVER_MAX] 이하. */
    private fun naturalSpeed(sh: RouteShape, s: Double, v: Double): Double {
        val k = abs(sh.curvatureAt(s))
        return if (k < 1e-6) v else min(v, sqrt(MotionConstants.A_DRIVER_MAX / k))
    }

    private fun travelSec(sh: RouteShape, from: Double, to: Double, v: Double): Double {
        var t = 0.0
        var s = from
        while (s < to) {
            t += RouteShape.STEP_M / max(1.0, naturalSpeed(sh, s, v))
            s += RouteShape.STEP_M
        }
        return t
    }

    /** 지금 상태를 복사해 [fromM]→[toM]을 "안내 없이" 달린 뒤의 지수. 원래 상태는 건드리지 않는다. */
    private fun simulate(fromM: Double, toM: Double, v: Double): Int {
        val sh = shape ?: return state.index
        val copy = fork()
        val dt = 1.0 / sampleHz
        var s = fromM
        var t = (lastMs ?: 0L)
        while (s <= toM) {
            val vs = naturalSpeed(sh, s, v)
            s += vs * dt
            t += (dt * 1000).toLong()
            copy.update(MotionInput(t, (vs * 3.6).toFloat(), routeM = s))
        }
        return copy.state.index
    }

    /** 예보용 복사본: 방위 대신 경로 형상으로 이어서 계산한다. */
    private fun fork(): MotionComfortEngine = MotionComfortEngine(sensitivity, shape, sampleHz).also {
        it.bandX = bandX.copy()
        it.bandY = bandY.copy()
        it.dose = dose
        it.lastMs = lastMs
        it.lastV = lastV
        it.lastHeading = null
        it.state = state
    }

    companion object {
        /** 이보다 느리면(정차·서행) 예보하지 않는다 (m/s, 18km/h). */
        const val MIN_FORECAST_SPEED_MPS = 5.0

        /** 방위 차이를 −180~180도로 접는다. */
        fun wrap180(d: Double): Double {
            var x = d % 360
            if (x > 180) x -= 360
            if (x < -180) x += 360
            return x
        }
    }
}
