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

/**
 * 지금 상태. [index]는 0~100 주행 자극(차량 흔들림 기준, 반려견 측정 아님) — 화면·기록·비교에 쓰는 보정 전 값.
 * [personal]은 같은 누적량에 개인 보정·"콩이가 이상해요" 가중을 곱한 값으로, 쉼표 시각(PetRestPlanner)에만 쓴다.
 */
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
    /** 개인 보정·가중 반영 지수 (화면에 보이지 않음). */
    val personal: Int = 0,
)

enum class LateralSource { HEADING, SHAPE, NONE }

/** 앞 구간 예보 (CONCEPT 4장). 거리는 현재 위치 기준. */
data class MotionForecast(
    val startInM: Double,
    val lengthM: Double,
    val durationSec: Int,
    /** 지금 속도 그대로 구간 끝까지 갔을 때 예상 지수 (보정 전). */
    val predictedIndex: Int,
    /** 구간 최대 곡률에서 편안한 횡가속([MotionConstants.A_COMFORT])이 되는 속도. */
    val recommendedKph: Int,
    /** 지금부터 1분 간격 예상 지수([MotionConstants.FORECAST_MIN]+1개, 0번 = 지금, 보정 전). 화면 그래프는 이 점을 그대로 잇는다. */
    val trajectory: List<Int> = emptyList(),
    /** 개인 보정 반영 — 구간 끝 예상(운전 안내 문구)·1분 간격 최고(예보 단계). 화면 숫자에는 쓰지 않는다. 보정이 없으면 위 값과 같다. */
    val personalPredicted: Int = predictedIndex,
    val personalPeak: Int = trajectory.maxOrNull() ?: predictedIndex,
)

/**
 * 멀미 예보 엔진 (F1, docs/plan/motion_model.md).
 * 1) 가속도 추정: a_x = Δv/Δt, a_y = v·Δψ/Δt (방위) 또는 v²·κ(s) (경로 형상)
 * 2) 축마다 0.02~0.5Hz 대역 필터 → 제곱 합 (결정 10/29, motion_model.md §3)
 * 3) 누설 적분 D ← D·e^(−Δt/τ) + a_w²·Δt (주행 τ 120분, 정차 τ 15분), MSDV = √D
 * 4) 지수 = 100·(1 − e^(−S·MSDV/M₀)), S = 프로필 민감도
 * 순수 로직이라 시뮬레이터·실차 모두 같은 입력으로 돌고, [forecast]는 현재 상태를 복사해 앞 구간을 미리 돌린다.
 */
class MotionComfortEngine private constructor(
    private val sensitivity: Double,
    /** 개인 보정 계수 — [MotionState.personal]에만 곱한다. */
    private val personalScale: Double,
    val shape: RouteShapeProvider?,
    val sampleHz: Double,
) {
    constructor(profile: PetProfile = PetProfile.DEFAULT, shape: RouteShapeProvider? = null, sampleHz: Double = 1.0) :
        this(MotionConstants.sensitivity(profile), MotionConstants.personalScale(profile), shape, sampleHz)

    /**
     * 잠시 민감도를 올린다 (UPGRADE 7 "콩이가 이상해요" — 불안 신호 뒤 부드러운 주행 안내). 1.0 = 그대로.
     * 개인 지수([MotionState.personal])·예보 단계에만 곱해진다 — 화면의 주행 자극 숫자는 그대로. TripBrain이 시간이 지나면 1.0으로 되돌린다.
     */
    var boost: Double = 1.0
        set(v) { field = v.coerceIn(1.0, 2.0) }

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
        val index = indexOf(sensitivity, msdv)
        val personal = indexOf(sensitivity * personalScale * boost, msdv)
        val driving = state.drivingSec + if (moving) dt.roundToInt() else 0
        val firstCaution = state.firstCautionDrivingSec ?: driving.takeIf { index >= MotionConstants.CAUTION }
        state = MotionState(index, motionLevelOf(index), msdv, max(state.peak, index), driving, firstCaution, src, personal)
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
        val (start, segEnd, kMax) = segment(sh, atM, end, v) ?: return null
        val length = max(RouteShape.STEP_M, segEnd - start)
        // 계획은 20분 범위와 구간 끝 중 먼 쪽까지 (그 뒤는 지금 속도로 직선)
        val plan = naturalPlan(sh, atM, max(end, segEnd), start, segEnd, v)
        val sim = simulate(plan, atM, segEnd)
        return MotionForecast(
            startInM = start - atM,
            lengthM = length,
            durationSec = travelSec(plan, atM, start, segEnd).roundToInt(),
            predictedIndex = sim.end,
            recommendedKph = (sqrt(MotionConstants.A_COMFORT / kMax) * 3.6).roundToInt(),
            trajectory = sim.traj,
            personalPredicted = sim.personalEnd,
            personalPeak = sim.personalPeak,
        )
    }

    /**
     * 20분 범위와 상관없이 형상이 아는 데까지 본 다음 굽은 길(예보와 같은 기준)까지 거리(m). 없으면 null.
     * 주행 화면 상태 패널 "다음 굽은 길까지"에 쓴다.
     */
    fun nextCurveInM(atM: Double, speedKph: Float): Double? {
        val sh = shape ?: return null
        val v = speedKph / 3.6
        if (v < MIN_FORECAST_SPEED_MPS) return null
        val from = max(atM, 0.0)
        return segment(sh, from, sh.lengthM, v)?.let { it.first - atM }
    }

    /** [from]~[end]에서 v²·|κ| ≥ A_RISK인 첫 구간 (시작, 끝, 최대 곡률). 굽이 한두 개짜리(짧은) 구간은 건너뛴다. */
    private fun segment(sh: RouteShapeProvider, from: Double, end: Double, v: Double): Triple<Double, Double, Double>? {
        // 구간 사이 간격이 지금 속도로 MERGE_GAP_SEC초 안(최소 MERGE_GAP_M)이면 한 구간 — 고속으로 다가가면 굽이 사이 직선도 금방 지난다.
        val mergeGap = max(MotionConstants.MERGE_GAP_M, v * MotionConstants.MERGE_GAP_SEC)
        var segStart: Double? = null
        var segEnd = 0.0
        var kMax = 0.0
        var d = from
        while (d <= end) {
            val k = abs(sh.curvatureAt(d))
            if (v * v * k >= MotionConstants.A_RISK) {
                if (segStart != null && d - segEnd > mergeGap) {
                    // 앞 구간이 굽이 하나뿐(짧음)이면 버리고 다음 구간을 본다 — 멀미는 반복되는 흔들림에서 온다.
                    if (segEnd - segStart >= MotionConstants.MIN_SEGMENT_M) break
                    segStart = null
                    kMax = 0.0
                }
                if (segStart == null) segStart = d
                segEnd = d
                kMax = max(kMax, k)
            }
            d += RouteShape.STEP_M
        }
        val start = segStart ?: return null
        if (segEnd - start < MotionConstants.MIN_SEGMENT_M) return null
        return Triple(start, segEnd, kMax)
    }

    /**
     * "안내 없이" 운전자의 속도 계획(10m 격자, [fromM]부터). 굽은 길 구간([segStart]~[segEnd])에서는 그 길의 보통 굽이(곡률 중앙값)가
     * [MotionConstants.A_DRIVER_MAX] 이하가 되는 속도로 달리고(고속도로 속도 그대로 들어가지 않는다), 더 급한 굽이에서는 더 줄인다.
     * 속도 변화는 [MotionConstants.DRIVER_DECEL_MPS2]·[MotionConstants.DRIVER_ACCEL_MPS2] 안에서만 — 순간 감속 없음.
     */
    private fun naturalPlan(sh: RouteShapeProvider, fromM: Double, toM: Double, segStart: Double, segEnd: Double, v: Double): DoubleArray {
        val step = RouteShape.STEP_M
        val n = ((toM - fromM) / step).toInt() + 2
        val ks = generateSequence(segStart) { it + step }.takeWhile { it <= segEnd }.map { abs(sh.curvatureAt(it)) }.sorted().toList()
        val typical = ks.getOrNull(ks.size / 2) ?: 0.0
        val cruise = if (typical < 1e-6) v else min(v, sqrt(MotionConstants.A_DRIVER_MAX / typical))
        val plan = DoubleArray(n) { i ->
            val s = fromM + i * step
            val k = abs(sh.curvatureAt(s))
            val base = if (s >= segStart && s <= segEnd) cruise else v
            if (k < 1e-6) base else min(base, sqrt(MotionConstants.A_DRIVER_MAX / k))
        }
        for (i in n - 2 downTo 0) plan[i] = min(plan[i], sqrt(plan[i + 1] * plan[i + 1] + 2 * MotionConstants.DRIVER_DECEL_MPS2 * step))
        for (i in 1 until n) plan[i] = min(plan[i], sqrt(plan[i - 1] * plan[i - 1] + 2 * MotionConstants.DRIVER_ACCEL_MPS2 * step))
        return plan
    }

    private fun speedAt(plan: DoubleArray, fromM: Double, s: Double): Double {
        val x = ((s - fromM) / RouteShape.STEP_M).coerceIn(0.0, (plan.size - 1).toDouble())
        val i = x.toInt().coerceAtMost(plan.size - 2)
        return max(1.0, plan[i] + (plan[i + 1] - plan[i]) * (x - i))
    }

    private fun travelSec(plan: DoubleArray, fromM: Double, a: Double, b: Double): Double {
        var t = 0.0
        var s = a
        while (s < b) {
            t += RouteShape.STEP_M / speedAt(plan, fromM, s)
            s += RouteShape.STEP_M
        }
        return t
    }

    /**
     * 지금 상태를 복사해 "안내 없이" 계획대로 달린다. 1분마다 지수를 찍고(앞으로 [MotionConstants.FORECAST_MIN]분),
     * 구간 끝([segEnd])에 닿았을 때 지수와 그때까지 개인 지수 최고를 함께 돌려준다. 원래 상태는 건드리지 않는다.
     */
    private class Sim(val end: Int, val traj: List<Int>, val personalEnd: Int, val personalPeak: Int)

    private fun simulate(plan: DoubleArray, fromM: Double, segEnd: Double): Sim {
        val copy = fork()
        val dt = 1.0 / sampleHz
        val perMin = (60 * sampleHz).roundToInt()
        val horizon = MotionConstants.FORECAST_MIN * perMin
        val lastM = fromM + (plan.size - 1) * RouteShape.STEP_M
        val traj = mutableListOf(state.index)
        var atEnd: Int? = null
        var personalEnd: Int? = null
        var personalPeak = state.personal
        var s = fromM
        var t = (lastMs ?: 0L)
        var n = 0
        while ((n < horizon || atEnd == null) && n < MAX_SIM_STEPS) {
            val vs = if (s < lastM) speedAt(plan, fromM, s) else plan.last()
            s += vs * dt
            t += (dt * 1000).toLong()
            n++
            copy.update(MotionInput(t, (vs * 3.6).toFloat(), routeM = s))
            if (atEnd == null && s >= segEnd) {
                atEnd = copy.state.index
                personalEnd = copy.state.personal
            }
            if (n <= horizon && n % perMin == 0) {
                traj += copy.state.index
                personalPeak = max(personalPeak, copy.state.personal)
            }
        }
        return Sim(atEnd ?: copy.state.index, traj, personalEnd ?: copy.state.personal, personalPeak)
    }

    private fun indexOf(s: Double, msdv: Double): Int =
        (100 * (1 - exp(-s * msdv / MotionConstants.INDEX_SCALE_MSDV))).roundToInt().coerceIn(0, 100)

    /** 예보용 복사본: 방위 대신 경로 형상으로 이어서 계산한다. */
    private fun fork(): MotionComfortEngine = MotionComfortEngine(sensitivity, personalScale, shape, sampleHz).also {
        it.boost = boost
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

        /** 예보 미리 돌리기 상한(단계, 1Hz면 2시간). */
        private const val MAX_SIM_STEPS = 7_200

        /** 방위 차이를 −180~180도로 접는다. */
        fun wrap180(d: Double): Double {
            var x = d % 360
            if (x > 180) x -= 360
            if (x < -180) x += 360
            return x
        }
    }
}
