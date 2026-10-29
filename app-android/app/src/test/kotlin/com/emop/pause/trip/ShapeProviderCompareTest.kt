package com.emop.pause.trip

import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import com.emop.pause.core.model.MotionTendency
import com.emop.pause.engine.motion.MotionComfortEngine
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.MotionInput
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.motion.TbtRouteShape
import com.emop.pause.sim.Gpx
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

/**
 * F1-11 앞 구간 형상 두 구현 비교: 시연 GPX([RouteShape]) vs 실차 추정([TbtRouteShape], 회전 안내 없이 진행 방위만).
 * 같은 고갯길(대관령 옛길, OSM)을 같은 방위·속도로 달리며 1km마다 두 엔진의 예보를 견준다.
 * 결과는 app/build/shape_compare/summary.txt → docs/plan/shape_compare.md.
 */
class ShapeProviderCompareTest {
    private val points = Gpx.parse(File("../../scenarios/geo/mountain_road.gpx").readText())
    private val gpx = RouteShape.fromLatLng(points)!!
    private val pet = PetProfile(name = "콩이", size = PetSize.SMALL, ageYears = 9, motionTendency = MotionTendency.HIGH, pottyIntervalMin = 90)

    private data class Row(val atM: Int, val gpx: MotionForecast?, val tbt: MotionForecast?, val indexGpx: Int, val indexTbt: Int)

    private fun run(): List<Row> {
        val path = Path(points)
        val tbt = TbtRouteShape()
        val a = MotionComfortEngine(pet, gpx)
        val b = MotionComfortEngine(pet, tbt)
        val rows = mutableListOf<Row>()
        var s = 0.0
        var t = 0L
        var nextCheck = 1_000.0
        while (s < gpx.lengthM) {
            // "안내 없이" 운전: 45km/h, 굽이에서는 횡가속 3m/s² 이하 (motion_model.md §6과 같은 가정)
            val k = abs(gpx.curvatureAt(s))
            val v = if (k < 1e-6) CRUISE else min(CRUISE, sqrt(3.0 / k))
            val heading = path.headingAt(s)
            tbt.observe(s, heading)
            a.update(MotionInput(t, (v * 3.6).toFloat(), heading, s))
            b.update(MotionInput(t, (v * 3.6).toFloat(), heading, s))
            if (s >= nextCheck) {
                rows += Row(nextCheck.toInt(), a.forecast(s, (v * 3.6).toFloat()), b.forecast(s, (v * 3.6).toFloat()), a.state.index, b.state.index)
                nextCheck += 1_000.0
            }
            s += v
            t += 1_000
        }
        return rows
    }

    @Test
    fun `F1-11 GPX와 방위 추정 예보 비교 — 굽이 구간에 들어선 뒤에는 방위 추정도 앞 굽이를 잡는다`() {
        val rows = run()
        val out = File("build/shape_compare").apply { mkdirs() }
        val text = buildString {
            appendLine("at_m | gpx_start_m gpx_len_m gpx_pred gpx_rec | tbt_start_m tbt_len_m tbt_pred tbt_rec | index_now")
            rows.forEach { r ->
                fun f(x: MotionForecast?) = x?.let { "${it.startInM.toInt()} ${it.lengthM.toInt()} ${it.predictedIndex} ${it.recommendedKph}" } ?: "- - - -"
                appendLine("${r.atM} | ${f(r.gpx)} | ${f(r.tbt)} | ${r.indexGpx}")
            }
        }
        File(out, "summary.txt").writeText(text)
        // 지수(현재 측정)는 같은 방위를 쓰므로 같다.
        rows.forEach { assertTrue(it.indexGpx == it.indexTbt, "현재 지수는 같아야 한다 $it") }
        // 굽이 구간 안: GPX가 예보하는 지점의 대부분에서 방위 추정도 예보한다.
        val both = rows.filter { it.gpx != null }
        val caught = both.count { it.tbt != null }
        assertTrue(caught >= both.size * 0.7, "방위 추정이 잡은 예보 $caught / ${both.size}\n$text")
        // 예상 지수 차이는 크지 않다 (같은 무늬를 되풀이한다고 보므로 과대·과소 모두 가능).
        val diffs = both.filter { it.tbt != null }.map { abs(it.gpx!!.predictedIndex - it.tbt!!.predictedIndex) }
        assertTrue(diffs.average() <= 10, "예상 지수 평균 차이 ${diffs.average()}\n$text")
    }

    @Test
    fun `F1-11 방위 기록이 없으면(고갯길 들어서기 전) 회전 안내 없이는 예보하지 않는다 — GPX만 미리 안다`() {
        val tbt = TbtRouteShape()
        val engine = MotionComfortEngine(pet, tbt)
        assertNull(engine.forecast(0.0, 45f))
        assertTrue(MotionComfortEngine(pet, gpx).forecast(0.0, 45f) != null)
    }

    /** GPX 점열 위 경로 거리 s의 진행 방위(도, 0=북, 시계 방향). */
    private class Path(points: List<LatLng>) {
        private val xy: List<Pair<Double, Double>>
        private val cum: DoubleArray

        init {
            val lat0 = Math.toRadians(points.first().lat)
            xy = points.map { p -> Math.toRadians(p.lng - points.first().lng) * cos(lat0) * EARTH to Math.toRadians(p.lat - points.first().lat) * EARTH }
            cum = DoubleArray(xy.size)
            for (i in 1 until xy.size) cum[i] = cum[i - 1] + hypot(xy[i].first - xy[i - 1].first, xy[i].second - xy[i - 1].second)
        }

        fun headingAt(s: Double): Double {
            var i = cum.indexOfFirst { it > s }
            if (i <= 0) i = if (i == 0) 1 else xy.size - 1
            val (ax, ay) = xy[i - 1]
            val (bx, by) = xy[i]
            return (Math.toDegrees(atan2(bx - ax, by - ay)) + 360) % 360
        }
    }

    private companion object {
        const val CRUISE = 45 / 3.6
        const val EARTH = 6_371_000.0
    }
}
