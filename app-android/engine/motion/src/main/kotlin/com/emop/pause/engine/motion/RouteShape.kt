package com.emop.pause.engine.motion

import com.emop.pause.core.model.LatLng
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * 경로 형상: 경로 진행 거리 s(m)마다 부호 있는 곡률 κ(1/m, 왼쪽 굽음 +).
 * NaviHelper가 폴리라인을 주지 않으므로 시연 GPX·시뮬레이터에서 만든다 (docs/plan/sdk_check.md). 실차는 [TbtRouteShape].
 * 곡률은 [STEP_M] 간격으로 다시 뽑은 선에서 앞뒤 [WINDOW_M] 떨어진 세 점의 외접원(멩거 곡률)으로 구한다 —
 * 듬성듬성한 GPX 꼭짓점의 각도가 창 길이에 고르게 퍼진다.
 */
class RouteShape private constructor(private val s: DoubleArray, private val kappa: DoubleArray) : RouteShapeProvider {
    override val lengthM: Double get() = s.last()

    /** 경로 진행 거리 [atM]의 곡률. 범위 밖은 0. */
    override fun curvatureAt(atM: Double): Double {
        if (atM < 0 || atM > lengthM) return 0.0
        var lo = 0
        var hi = s.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (s[mid] <= atM) lo = mid else hi = mid
        }
        val t = if (s[hi] > s[lo]) (atM - s[lo]) / (s[hi] - s[lo]) else 0.0
        return kappa[lo] + (kappa[hi] - kappa[lo]) * t
    }

    companion object {
        const val STEP_M = 10.0
        const val WINDOW_M = 30.0
        private const val EARTH_M = 6_371_000.0

        /** 위경도 → 지역 평면(m, 등장방형 근사)으로 바꿔 만든다. 수십 km 범위의 시연 경로에 충분하다. */
        fun fromLatLng(points: List<LatLng>): RouteShape? {
            if (points.size < 2) return null
            val lat0 = Math.toRadians(points.first().lat)
            val xy = points.map { p ->
                val x = Math.toRadians(p.lng - points.first().lng) * cos(lat0) * EARTH_M
                val y = Math.toRadians(p.lat - points.first().lat) * EARTH_M
                x to y
            }
            return fromXY(xy)
        }

        /** 평면 좌표(m) 점열에서 만든다. 테스트·시뮬레이터용. */
        fun fromXY(points: List<Pair<Double, Double>>): RouteShape? {
            val pts = points.zipWithNext().filter { (a, b) -> hypot(b.first - a.first, b.second - a.second) > 0.01 }.map { it.first } + points.last()
            if (pts.size < 2) return null
            val cum = DoubleArray(pts.size)
            for (i in 1 until pts.size) cum[i] = cum[i - 1] + hypot(pts[i].first - pts[i - 1].first, pts[i].second - pts[i - 1].second)
            val total = cum.last()
            val n = max(2, (total / STEP_M).toInt() + 1)
            val ss = DoubleArray(n) { min(total, it * STEP_M) }.also { it[n - 1] = total }
            val at = { d: Double -> pointAt(pts, cum, d) }
            val ks = DoubleArray(n) { i ->
                val d = ss[i]
                if (d - WINDOW_M < 0 || d + WINDOW_M > total) 0.0 else menger(at(d - WINDOW_M), at(d), at(d + WINDOW_M))
            }
            return RouteShape(ss, ks)
        }

        private fun pointAt(pts: List<Pair<Double, Double>>, cum: DoubleArray, d: Double): Pair<Double, Double> {
            var i = cum.indexOfFirst { it >= d }.let { if (it <= 0) 1 else it }
            if (i >= pts.size) i = pts.size - 1
            val seg = cum[i] - cum[i - 1]
            val t = if (seg > 0) (d - cum[i - 1]) / seg else 0.0
            val a = pts[i - 1]
            val b = pts[i]
            return (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
        }

        /** 부호 있는 멩거 곡률: 4·넓이/(세 변 길이 곱). 왼쪽으로 굽으면 +. */
        private fun menger(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>): Double {
            val cross = (b.first - a.first) * (c.second - a.second) - (b.second - a.second) * (c.first - a.first)
            val ab = hypot(b.first - a.first, b.second - a.second)
            val bc = hypot(c.first - b.first, c.second - b.second)
            val ca = hypot(a.first - c.first, a.second - c.second)
            val den = ab * bc * ca
            return if (den < 1e-9 || abs(cross) < 1e-9) 0.0 else 2 * cross / den
        }
    }
}
