package com.emop.pause.engine.motion

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class ForecastSegmentTest {
    /** 직선 [straights]m와 반경 [r]m·각도 [deg]° 굽이를 번갈아 이은 평면 경로 (1m 간격). */
    private fun road(vararg parts: Pair<String, Double>, r: Double = 80.0): RouteShape {
        val pts = mutableListOf(0.0 to 0.0)
        var h = 0.0
        var left = true
        for ((kind, v) in parts) {
            val n = if (kind == "s") v.toInt() else (Math.toRadians(v) * r).toInt()
            repeat(n) {
                if (kind == "c") h += (if (left) 1 else -1) / r
                val (x, y) = pts.last()
                pts += (x + cos(h)) to (y + sin(h))
            }
            if (kind == "c") left = !left
        }
        return RouteShape.fromXY(pts)!!
    }

    @Test
    fun `F1-12 굽이 하나뿐인 짧은 구간은 예보하지 않는다`() {
        val shape = road("s" to 500.0, "c" to 60.0, "s" to 3000.0)
        assertNull(MotionComfortEngine(shape = shape).forecast(0.0, 60f))
    }

    @Test
    fun `F1-12 고속으로 다가갈 때 30초 안 간격의 굽이들은 한 구간 — 첫 굽이만 짧게 말하지 않는다`() {
        // 굽이 사이 직선 500m: 300m 병합 간격으로는 갈라지지만 100km/h(30초 = 833m)로는 한 구간
        val shape = road("s" to 300.0, "c" to 90.0, "s" to 500.0, "c" to 90.0, "s" to 500.0, "c" to 90.0, "s" to 500.0, "c" to 90.0, "s" to 2000.0)
        val f = MotionComfortEngine(shape = shape).forecast(0.0, 100f)
        assertNotNull(f)
        assertTrue(f!!.lengthM > 1_500, "구간 길이 ${f.lengthM}")
    }
}
