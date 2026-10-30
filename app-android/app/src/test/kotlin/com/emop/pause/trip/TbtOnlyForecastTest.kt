package com.emop.pause.trip

import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import com.emop.pause.engine.motion.GuidanceFromShape
import com.emop.pause.engine.motion.MotionComfortEngine
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.MotionInput
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.motion.TbtRouteShape
import com.emop.pause.sim.Gpx
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
 * UPGRADE 5 (test_plan F1-15): GPX 없이 회전 안내만으로 예보. 가짜 NaviHelper([GuidanceFromShape], OSM 고갯길에서 만든
 * 회전 종류·남은 거리)를 10초마다 [TbtRouteShape.onTurns]에 넣고, 같은 길을 GPX([RouteShape])로 본 예보와 1km마다 견준다.
 *
 * 허용 오차 (결과를 보기 전에 정함): GPX가 예보한 지점의 70% 이상에서 예보 · 시작 거리 차이 중앙값 600m 이하 ·
 * 지속 시간 비율 중앙값 0.5~2배. 결과 표는 build/shape_compare/tbt_only.txt → docs/plan/shape_compare.md.
 */
class TbtOnlyForecastTest {
    private val points = Gpx.parse(File("../../scenarios/geo/mountain_road.gpx").readText())
    private val gpx = RouteShape.fromLatLng(points)!!
    private val pet = PetProfile(name = "콩이", size = PetSize.SMALL, ageYears = 9, motionTendency = MotionTendency.HIGH, pottyIntervalMin = 90)

    private data class Row(val atM: Int, val gpx: MotionForecast?, val tbt: MotionForecast?)

    private fun run(): Pair<List<Row>, Int> {
        val path = Path(points)
        val guidance = GuidanceFromShape(gpx)
        val tbt = TbtRouteShape()
        val a = MotionComfortEngine(pet, gpx)
        val b = MotionComfortEngine(pet, tbt)
        val rows = mutableListOf<Row>()
        var s = 0.0
        var t = 0L
        var nextCheck = 1_000.0
        var nextGuide = 0L
        while (s < gpx.lengthM) {
            val k = abs(gpx.curvatureAt(s))
            val v = if (k < 1e-6) CRUISE else min(CRUISE, sqrt(3.0 / k))
            val heading = path.headingAt(s)
            // 가짜 NaviHelper: 10초마다 앞 3km 안내 목록 (실제 onTBTInfo 주기와 비슷하게)
            if (t >= nextGuide) {
                tbt.onTurns(s, guidance.upcoming(s))
                nextGuide = t + 10_000
            }
            tbt.observe(s, heading)
            a.update(MotionInput(t, (v * 3.6).toFloat(), heading, s))
            b.update(MotionInput(t, (v * 3.6).toFloat(), heading, s))
            if (s >= nextCheck) {
                rows += Row(nextCheck.toInt(), a.forecast(s, (v * 3.6).toFloat()), b.forecast(s, (v * 3.6).toFloat()))
                nextCheck += 1_000.0
            }
            s += v
            t += 1_000
        }
        return rows to guidance.points.size
    }

    @Test
    fun `F1-15 회전 안내만으로도 GPX 예보와 허용 오차 안`() {
        val (rows, guides) = run()
        val both = rows.filter { it.gpx != null }
        val caught = both.filter { it.tbt != null }
        val startDiff = caught.map { abs(it.gpx!!.startInM - it.tbt!!.startInM) }.sorted()
        val ratio = caught.map { it.tbt!!.durationSec.toDouble() / it.gpx!!.durationSec.coerceAtLeast(1) }.sorted()
        fun median(xs: List<Double>) = if (xs.isEmpty()) Double.NaN else xs[xs.size / 2]
        val catchRate = caught.size.toDouble() / both.size.coerceAtLeast(1)
        val text = buildString {
            appendLine("guides=$guides  gpx_forecasts=${both.size}  caught=${caught.size}  catch_rate=${"%.2f".format(catchRate)}")
            appendLine("start_diff_median_m=${"%.0f".format(median(startDiff))}  duration_ratio_median=${"%.2f".format(median(ratio))}")
            appendLine("at_m | gpx start len dur pred | tbt start len dur pred")
            rows.forEach { r ->
                fun f(x: MotionForecast?) = x?.let { "${it.startInM.toInt()} ${it.lengthM.toInt()} ${it.durationSec} ${it.predictedIndex}" } ?: "- - - -"
                appendLine("${r.atM} | ${f(r.gpx)} | ${f(r.tbt)}")
            }
        }
        File("build/shape_compare").apply { mkdirs() }.resolve("tbt_only.txt").writeText(text)
        println(text)
        assertTrue(guides > 10, "고갯길에서 안내가 만들어져야 한다\n$text")
        assertTrue(catchRate >= 0.7, "예보 비율 $catchRate\n$text")
        assertTrue(median(startDiff) <= 600, "시작 거리 차이 ${median(startDiff)}\n$text")
        assertTrue(median(ratio) in 0.5..2.0, "지속 시간 비율 ${median(ratio)}\n$text")
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
