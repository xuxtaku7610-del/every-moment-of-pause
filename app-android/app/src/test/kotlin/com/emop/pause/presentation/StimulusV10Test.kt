package com.emop.pause.presentation

import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.trip.TripUi
import com.emop.pause.uistate.CompareChart
import com.emop.pause.uistate.ComparePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan V10-01·V10-02 (v10-2 흔들림 그래프): 그래프는 엔진이 준 값만 — 예측 21점·굽은 길 시작·길이·시각. */
class StimulusV10Test {
    private val trajectory = listOf(22, 22, 23, 25, 31, 40, 48, 55, 58, 57, 55, 52, 50, 48, 46, 45, 44, 43, 42, 41, 40)

    @Test
    fun `V10-01 주행 자극 보기 - 예측 점·굽은 길 시작(거리÷속도)·길이·시각을 엔진 값 그대로`() {
        val f = MotionForecast(startInM = 4_000.0, lengthM = 9_000.0, durationSec = 720, predictedIndex = 55, recommendedKph = 39, trajectory = trajectory)
        val ui = UiMapper.stimulus(TripUi(forecast = f, kmPerMin = 1.0), peak = 46, clockMinute = 11 * 60 + 53)
        assertEquals(trajectory, ui.ahead)
        assertEquals(4, ui.curveStartMin)
        assertEquals(12, ui.curveLenMin)
        assertEquals("4.0km 앞", ui.curveKm)
        assertEquals(11 * 60 + 53, ui.clockMinute)
    }

    @Test
    fun `V10-01 정차 중(예보 없음)에는 예측·구간을 지어내지 않는다`() {
        val ui = UiMapper.stimulus(TripUi(motionRecent = listOf(20, 22, 25)), peak = 46, clockMinute = 600)
        assertTrue(ui.ahead.isEmpty())
        assertNull(ui.curveStartMin)
        assertNull(ui.curveLenMin)
        assertEquals(listOf(20, 22, 25), ui.recent)
        // 속도를 모르면 시작 분을 계산하지 않는다
        val f = MotionForecast(4_000.0, 9_000.0, 720, 55, 39, trajectory)
        assertNull(UiMapper.stimulus(TripUi(forecast = f, kmPerMin = 0.0), null).curveStartMin)
    }

    @Test
    fun `V10-02 고갯길 비교 수치 - 최고값과 거리 가중 평균`() {
        val line = listOf(ComparePoint(0f, 0), ComparePoint(1f, 100), ComparePoint(3f, 100))
        assertEquals(100, CompareChart.peak(line))
        // (0~1km 평균 50) + (1~3km 100) → (50·1 + 100·2) / 3 = 83
        assertEquals(83, CompareChart.mean(line))
        assertEquals(0, CompareChart.peak(emptyList()))
        assertEquals(7, CompareChart.mean(listOf(ComparePoint(0f, 7))))
    }
}
