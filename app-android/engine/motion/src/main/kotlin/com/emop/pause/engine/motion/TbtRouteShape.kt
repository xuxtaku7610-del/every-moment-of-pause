package com.emop.pause.engine.motion

import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.TurnKind
import kotlin.math.abs
import kotlin.math.max

/**
 * 실차용 앞 구간 형상: NaviHelper 회전 안내(TBT)와 진행 방위로 곡률을 추정한다 (F1, docs/plan/motion_model.md §6).
 *
 * 1) 회전 안내: 회전 지점마다 종류별 가정 각도·반경의 원호(곡률 ±1/R, 호 길이 = 각도·R)를 회전 지점 가운데에 놓는다.
 * 2) 진행 방위: 지나온 [HISTORY_M]의 방위 변화로 잰 곡률 Δψ/Δs를 기억하고, 최근 길이 굽이진 길이면
 *    (좌우가 바뀌는 굽이가 [MIN_WINDING_FLIPS]번 이상) 앞 [PERSIST_M]도 같은 무늬가 이어진다고 본다.
 *    고갯길의 연속 커브는 TBT에 나오지 않는 경우가 많아 이것이 주 근거가 된다.
 * 회전 안내 원호가 있는 곳은 그 값을, 없는 곳은 방위 무늬를 쓴다. 둘 다 없으면 0(직선).
 * 시연 GPX([RouteShape])보다 거칠다 — 비교는 `TbtRouteShapeTest`.
 */
class TbtRouteShape : RouteShapeProvider {
    private data class Arc(val centerM: Double, val halfM: Double, val kappa: Double)

    private var arcs: List<Arc> = emptyList()
    private val history = ArrayDeque<Pair<Double, Double>>() // (경로 거리, 방위 도)
    private var bins: DoubleArray = DoubleArray(0)
    private var lastM: Double? = null

    /** 회전 안내 목록이 새로 오면 바꾼다. [atM] = 받은 시점의 경로 진행 거리. */
    fun onTurns(atM: Double, turns: List<RouteTurn>) {
        arcs = turns.mapNotNull { t ->
            val (deg, radius) = geometry(t.kind) ?: return@mapNotNull null
            val half = Math.toRadians(abs(deg)) * radius / 2
            Arc(atM + t.distanceM, half, (if (deg > 0) 1 else -1) / radius)
        }
    }

    /** 1초마다 지금 위치·방위. 뒤로 가거나 같은 자리면 무시한다. */
    fun observe(atM: Double, headingDeg: Double) {
        val prev = history.lastOrNull()
        if (prev != null && atM - prev.first < MIN_SAMPLE_M) return
        history.addLast(atM to headingDeg)
        while (history.size > 2 && atM - history.first().first > HISTORY_M) history.removeFirst()
        lastM = atM
        bins = measuredBins()
    }

    override fun curvatureAt(atM: Double): Double {
        arcs.firstOrNull { abs(atM - it.centerM) <= it.halfM }?.let { return it.kappa }
        val last = lastM ?: return 0.0
        val ahead = atM - last
        if (ahead <= 0 || ahead > PERSIST_M || bins.isEmpty() || !winding()) return 0.0
        // 지나온 무늬를 앞으로 되풀이: 앞 d m = 기록 구간의 (d mod 길이) 번째 칸.
        val i = ((ahead / RouteShape.STEP_M).toInt()) % bins.size
        return bins[i]
    }

    override val lengthM: Double
        get() = max((lastM ?: 0.0) + PERSIST_M, arcs.maxOfOrNull { it.centerM + it.halfM } ?: 0.0)

    /** 기록을 [RouteShape.STEP_M] 칸으로 나눈 곡률(1/m), 오래된 칸부터. */
    private fun measuredBins(): DoubleArray {
        if (history.size < 2) return DoubleArray(0)
        val start = history.first().first
        val n = max(1, ((history.last().first - start) / RouteShape.STEP_M).toInt())
        val turn = DoubleArray(n) { Double.NaN }
        history.zipWithNext().forEach { (a, b) ->
            val ds = b.first - a.first
            if (ds <= 0) return@forEach
            // 방위는 시계 방향(오른쪽 +)이라 부호를 바꿔 왼쪽 굽음 +로 맞춘다. 표본 사이 칸은 같은 곡률로 채운다.
            val k = -Math.toRadians(MotionComfortEngine.wrap180(b.second - a.second)) / ds
            val i0 = ((a.first - start) / RouteShape.STEP_M).toInt().coerceIn(0, n - 1)
            val i1 = ((b.first - start) / RouteShape.STEP_M).toInt().coerceIn(i0, n - 1)
            for (i in i0..i1) turn[i] = k
        }
        for (i in turn.indices) if (turn[i].isNaN()) turn[i] = 0.0
        return turn
    }

    /** 최근 길에서 |κ| ≥ [WINDING_KAPPA]인 굽이의 좌우가 몇 번 바뀌었나. */
    private fun winding(): Boolean {
        var flips = 0
        var sign = 0
        for (k in bins) {
            if (abs(k) < WINDING_KAPPA) continue
            val s = if (k > 0) 1 else -1
            if (sign != 0 && s != sign) flips++
            sign = s
        }
        return flips >= MIN_WINDING_FLIPS
    }

    companion object {
        const val HISTORY_M = 1_500.0
        const val PERSIST_M = 1_500.0
        const val MIN_SAMPLE_M = 1.0
        /** 반경 200m보다 급한 굽이만 "굽이"로 센다. */
        const val WINDING_KAPPA = 1 / 200.0
        const val MIN_WINDING_FLIPS = 2

        /**
         * 회전 종류 → (방향 있는 각도°, 반경 m). 모두 가정값: 교차로 회전 90°·R15, 급회전 135°·R10, 차로 유지 30°·R150,
         * 유턴 180°·R8, 나들목 진출입 90°·R60, 회전교차로 180°·R15(반시계 = 왼쪽). 직진류는 없음.
         */
        fun geometry(kind: TurnKind): Pair<Double, Double>? = when (kind) {
            TurnKind.LEFT -> 90.0 to 15.0
            TurnKind.RIGHT -> -90.0 to 15.0
            TurnKind.SHARP_LEFT -> 135.0 to 10.0
            TurnKind.SHARP_RIGHT -> -135.0 to 10.0
            TurnKind.KEEP_LEFT -> 30.0 to 150.0
            TurnKind.KEEP_RIGHT -> -30.0 to 150.0
            TurnKind.UTURN -> 180.0 to 8.0
            TurnKind.RAMP_LEFT -> 90.0 to 60.0
            TurnKind.RAMP_RIGHT -> -90.0 to 60.0
            TurnKind.ROUNDABOUT -> 180.0 to 15.0
            TurnKind.STRAIGHT -> null
        }
    }
}
