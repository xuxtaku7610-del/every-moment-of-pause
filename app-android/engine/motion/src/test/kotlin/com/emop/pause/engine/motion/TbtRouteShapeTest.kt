package com.emop.pause.engine.motion

import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.TurnKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class TbtRouteShapeTest {
    @Test
    fun `F1-11 회전 안내는 회전 지점 가운데 가정 반경의 원호가 된다 (왼쪽 +)`() {
        val s = TbtRouteShape()
        s.onTurns(1_000.0, listOf(RouteTurn(TurnKind.SHARP_LEFT, 500.0), RouteTurn(TurnKind.KEEP_RIGHT, 2_000.0), RouteTurn(TurnKind.STRAIGHT, 2_500.0)))
        assertEquals(1 / 10.0, s.curvatureAt(1_500.0), 1e-9)
        assertEquals(0.0, s.curvatureAt(1_540.0), 1e-9) // 135°·R10 → 호 23.6m, 반쪽 11.8m 밖
        assertEquals(-1 / 150.0, s.curvatureAt(3_000.0), 1e-9)
        assertEquals(0.0, s.curvatureAt(3_500.0), 1e-9)
    }

    @Test
    fun `F1-11 직선 기록은 앞 구간을 굽은 길로 보지 않는다`() {
        val s = TbtRouteShape()
        for (m in 0..1_500 step 12) s.observe(m.toDouble(), 90.0)
        assertEquals(0.0, s.curvatureAt(1_600.0), 1e-12)
    }

    @Test
    fun `F1-11 좌우로 굽이진 기록은 앞 1_5km에 같은 무늬로 이어진다 — 예보가 나온다`() {
        val s = TbtRouteShape()
        val engine = MotionComfortEngine(shape = s)
        // 반경 60m 굽이를 좌우로 번갈아 (호 90m), 12m마다 방위 기록
        var heading = 0.0
        var m = 0.0
        var t = 0L
        repeat(120) {
            val dir = if ((m / 90).toInt() % 2 == 0) 1 else -1
            heading = (heading + dir * Math.toDegrees(12.0 / 60.0) + 360) % 360
            s.observe(m, heading)
            engine.update(MotionInput(t, 43f, heading, m))
            m += 12.0
            t += 1_000
        }
        assertTrue(abs(s.curvatureAt(m + 100)) > 1 / 100.0)
        val f = engine.forecast(m, 43f)
        assertNotNull(f)
        f!!
        assertTrue(f.startInM < 200, "시작 ${f.startInM}")
        assertTrue(s.curvatureAt(m + TbtRouteShape.PERSIST_M + 50) == 0.0, "1.5km 넘어서는 모른다")
    }
}
