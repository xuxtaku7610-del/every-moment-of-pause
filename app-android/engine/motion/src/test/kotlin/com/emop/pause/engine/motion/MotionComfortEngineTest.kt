package com.emop.pause.engine.motion

import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import com.emop.pause.core.model.SignalSample
import com.emop.pause.core.model.SignalSmoother
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MotionComfortEngineTest {
    private val normal = PetProfile("콩이")
    private val sensitive = PetProfile("콩이", PetSize.SMALL, ageYears = 0, motionTendency = MotionTendency.HIGH)

    /** 1초마다 (속도, 방위)를 넣고 분마다 지수를 모은다. */
    private fun run(engine: MotionComfortEngine, seconds: Int, startSec: Int = 0, frame: (Int) -> Pair<Float, Double?>): List<Int> {
        val perMinute = mutableListOf<Int>()
        for (t in startSec until startSec + seconds) {
            val (kph, heading) = frame(t)
            engine.update(MotionInput(t * 1000L, kph, heading))
            if ((t - startSec + 1) % 60 == 0) perMinute += engine.state.index
        }
        return perMinute
    }

    /** 반경 [radiusM], 속도 [kph]로 [periodSec]마다 좌우가 바뀌는 S자 커브의 방위(도). */
    private fun winding(kph: Float, radiusM: Double, periodSec: Int): (Int) -> Pair<Float, Double?> {
        val yawDegPerSec = Math.toDegrees(kph / 3.6 / radiusM)
        var heading = 90.0
        return { t ->
            val left = (t / (periodSec / 2)) % 2 == 0
            heading += if (left) -yawDegPerSec else yawDegPerSec
            kph to heading
        }
    }

    @Test
    fun `F1-01 직선 정속 10분은 낮음`() {
        val rnd = Random(7)
        val e = MotionComfortEngine(normal)
        val minutes = run(e, 600) { (100f + rnd.nextDouble(-1.0, 1.0).toFloat()) to 45.0 }
        assertTrue(minutes.last() < 15, minutes.toString())
        assertEquals(MotionLevel.LOW, e.state.level)
    }

    @Test
    fun `F1-02 연속 커브 10분은 분마다 오르고 직선보다 확실히 높다`() {
        val e = MotionComfortEngine(normal)
        val minutes = run(e, 600, frame = winding(50f, 80.0, 8))
        assertTrue(minutes.zipWithNext().all { (a, b) -> b >= a }, "분마다 단조 증가: $minutes")
        assertTrue(minutes.last() >= 20, minutes.toString())
        assertEquals(LateralSource.HEADING, e.state.lateralSource)

        val straight = MotionComfortEngine(normal)
        run(straight, 600) { 50f to 90.0 }
        assertTrue(minutes.last() >= straight.state.index + 15)
    }

    @Test
    fun `F1-03 정차하면 멀미 지수가 줄어든다`() {
        val e = MotionComfortEngine(normal)
        run(e, 600, frame = winding(50f, 80.0, 8))
        val before = e.state.index
        val resting = run(e, 15 * 60, startSec = 600) { 0f to 90.0 }
        assertTrue(resting.zipWithNext().all { (a, b) -> b <= a }, resting.toString())
        assertTrue(e.state.index < before * 0.75, "before=$before after=${e.state.index}")
        assertTrue(e.state.peak >= before, "최고점은 남는다")
    }

    @Test
    fun `F1-04 민감 프로필은 보통 프로필보다 높다`() {
        val a = MotionComfortEngine(normal)
        val b = MotionComfortEngine(sensitive)
        run(a, 600, frame = winding(50f, 80.0, 8))
        run(b, 600, frame = winding(50f, 80.0, 8))
        assertTrue(b.state.index > a.state.index + 10, "normal=${a.state.index} sensitive=${b.state.index}")
        assertTrue(MotionConstants.sensitivity(sensitive) > MotionConstants.sensitivity(normal))
    }

    /** 직선 [straightM] → 반경 [r]·호 [arcM]인 좌우 교대 커브 [curvyM] → 직선 [tailM]. 10m 간격 점열. */
    private fun testRoad(straightM: Double, curvyM: Double, r: Double, arcM: Double, tailM: Double): RouteShape {
        val pts = mutableListOf(0.0 to 0.0)
        var x = 0.0
        var y = 0.0
        var heading = 0.0
        var d = 0.0
        val step = 10.0
        while (d < straightM + curvyM + tailM) {
            if (d >= straightM && d < straightM + curvyM) {
                val left = ((d - straightM) / arcM).toInt() % 2 == 0
                heading += (if (left) 1 else -1) * step / r
            }
            x += step * cos(heading)
            y += step * sin(heading)
            d += step
            pts += x to y
        }
        return RouteShape.fromXY(pts)!!
    }

    @Test
    fun `F1-05 예보 - 3km 앞 굽은 길의 시작 거리와 지속 시간`() {
        val shape = testRoad(3000.0, 2400.0, 120.0, 150.0, 2000.0)
        val e = MotionComfortEngine(normal, shape)
        val f = e.forecast(atM = 0.0, speedKph = 60f)!!
        assertTrue(abs(f.startInM - 3000) <= 300, "시작 ${f.startInM}m")
        val expectedSec = 2400 / (60 / 3.6)
        assertTrue(abs(f.durationSec - expectedSec) <= expectedSec * 0.15, "지속 ${f.durationSec}s, 기대 ${expectedSec.toInt()}s")
        assertTrue(f.predictedIndex > e.state.index, "예상 지수 ${f.predictedIndex}")
        assertTrue(f.recommendedKph in 35..45, "권장 ${f.recommendedKph}km/h")
        // 1km 지나서 다시 보면 시작 거리가 그만큼 줄어든다
        val later = e.forecast(atM = 1000.0, speedKph = 60f)!!
        assertTrue(abs(later.startInM - (f.startInM - 1000)) <= 20)
        // 굽은 길을 다 지나면 예보 없음
        assertNull(e.forecast(atM = 5500.0, speedKph = 60f))
    }

    @Test
    fun `F1-05 예보는 엔진 상태를 바꾸지 않는다`() {
        val shape = testRoad(3000.0, 2400.0, 120.0, 150.0, 2000.0)
        val e = MotionComfortEngine(normal, shape)
        run(e, 120) { 60f to null }
        val before = e.state
        e.forecast(0.0, 60f)
        assertEquals(before, e.state)
    }

    private fun gain(freqHz: Double, sampleHz: Double): Double {
        val f = BandPass(sampleHz)
        val n = (120 * sampleHz).toInt()
        var peak = 0.0
        for (i in 0 until n) {
            val y = f.step(sin(2 * PI * freqHz * i / sampleHz))
            if (i > n / 2) peak = maxOf(peak, abs(y))
        }
        return peak
    }

    @Test
    fun `F1-06 대역 필터 - 0점2Hz 통과, 2Hz와 직류는 걸러진다`() {
        assertTrue(gain(0.2, 10.0) > 0.6, "0.2Hz ${gain(0.2, 10.0)}")
        assertTrue(gain(2.0, 10.0) < 0.2, "2Hz ${gain(2.0, 10.0)}")
        val f = BandPass(10.0)
        var last = 0.0
        repeat(1200) { last = f.step(1.0) }
        assertTrue(abs(last) < 0.05, "직류 $last")
        // 앱의 1Hz 신호에서도 0.2Hz는 통과한다
        assertTrue(gain(0.2, 1.0) > 0.6, "1Hz 샘플 ${gain(0.2, 1.0)}")
    }

    @Test
    fun `F1-08 방위·경로 형상이 없으면 종방향만, 예보는 없음`() {
        val e = MotionComfortEngine(normal)
        // 가다 서다: 20초 주기로 30↔10km/h
        run(e, 600) { t -> (if ((t / 10) % 2 == 0) 30f else 10f) to null }
        assertEquals(LateralSource.NONE, e.state.lateralSource)
        assertTrue(e.state.index > 0, "종방향 흔들림은 누적된다")
        assertNull(e.forecast(0.0, 30f))
    }

    @Test
    fun `F1-09 튀는 속도는 잡음 처리 후 지수를 크게 올리지 않는다`() {
        val raw = MotionComfortEngine(normal)
        val smoothed = MotionComfortEngine(normal)
        val smoother = SignalSmoother()
        for (t in 0 until 600) {
            val kph = if (t % 37 == 0) 140f else 100f
            raw.update(MotionInput(t * 1000L, kph))
            val s = smoother.apply(SignalSample(t * 1000L, speedKph = kph))
            smoothed.update(MotionInput(t * 1000L, s.speedKph))
        }
        assertTrue(smoothed.state.index < raw.state.index, "raw=${raw.state.index} smoothed=${smoothed.state.index}")
    }

    @Test
    fun `신호가 3초 넘게 끊기면 필터를 새로 시작하고 지수는 유지`() {
        val e = MotionComfortEngine(normal)
        run(e, 300, frame = winding(50f, 80.0, 8))
        val kept = e.state.index
        e.update(MotionInput(400_000L, 50f, 0.0)) // 100초 뒤 첫 값
        assertEquals(kept, e.state.index)
    }

    @Test
    fun `방위 차이는 0-360 경계를 넘어도 짧은 쪽으로`() {
        assertEquals(0.0, MotionComfortEngine.wrap180(360.0), 1e-9)
        assertEquals(20.0, MotionComfortEngine.wrap180(10.0 - 350.0), 1e-9)
        assertEquals(-20.0, MotionComfortEngine.wrap180(350.0 - 10.0), 1e-9)
    }

    @Test
    fun `주의에 처음 닿은 주행 시간을 기록한다 (F7 입력)`() {
        val e = MotionComfortEngine(sensitive)
        run(e, 1800, frame = winding(50f, 80.0, 8))
        val first = e.state.firstCautionDrivingSec
        assertTrue(first != null && first in 60..1800, "first=$first")
    }
}

class MotionPhrasesTest {
    @Test
    fun `F1-10 예보 문구 - 숫자 먼저, 단계별, 한 줄`() {
        val caution = MotionForecast(3000.0, 2400.0, 480, 45, 39)
        assertEquals("3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속해 주세요.", MotionPhrases.forecast(caution, "콩이"))
        assertEquals("800m 뒤 굽은 길 2분, 천천히 가면 콩이 편해요.", MotionPhrases.forecast(MotionForecast(780.0, 500.0, 100, 12, 50), "콩이"))
        assertEquals("2.5km 뒤 굽은 길 3분, 콩이 멀미 걱정 — 39km/h 아래로 천천히요.", MotionPhrases.forecast(MotionForecast(2500.0, 2000.0, 170, 80, 39), "콩이"))
        // 이름이 길면 이름을 빼서 2줄(48자) 안에
        val long = MotionPhrases.forecast(caution, "아주아주긴이름을가진우리집강아지")
        assertTrue(long.length <= MotionPhrases.MAX_CHARS, long)
        assertEquals("3km 뒤 굽은 길 8분, 멀미 주의 — 조금 일찍 감속해 주세요.", long)
    }
}
