package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.StopType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TripRecorderTest {
    @Test
    fun `F7-01 이동 시간·멀미 지수 추이(5분 간격)·휴식을 남긴다`() {
        val r = TripRecorder()
        for (min in 0..60) {
            val mode = if (min in 30..40) AppMode.PAUSE else AppMode.DRIVING
            val motion = if (mode == AppMode.PAUSE) 20 else min
            r.onMotion(motion, drivingSec = (min * 60).toLong(), firstCautionSec = if (min >= 40) 40 * 60L else null)
            r.tick(9 * 60 + min, mode, null, 30, min, min * 1.5, emptyList())
        }
        val log = r.log()
        // 점마다 그 5분 안의 최고값 (30분 점 = 26~29분 주행 29, 정차 20) → 그래프 최고 = 최고 지수
        assertEquals(listOf(0, 5, 10, 15, 20, 25, 29, 20, 20, 45, 50, 55, 60), log.motionSeries)
        assertEquals(60, log.motionPeak)
        assertEquals(log.motionPeak, log.motionSeries.max())
        assertEquals(1, log.cautionCount, "40에 41분에 처음 들어가고 내려오지 않았다")
        assertEquals(40, log.firstCautionMin)
        assertEquals(1, log.visits.size)
        assertEquals(60, log.drivingMin)
    }

    @Test
    fun `장면 점프 직후 휴게소 목록이 비어 있어도 목록이 오면 휴게소 이름을 쓴다`() {
        val r = TripRecorder()
        val stops = listOf(RestStop("Y03", "횡성휴게소", StopType.SERVICE_AREA, 95.0))
        r.tick(600, AppMode.DRIVING, null, 10, 50, 94.0, emptyList())
        r.tick(601, AppMode.PAUSE, null, 10, 0, 95.2, emptyList())
        assertEquals(TripRecorder.ROADSIDE, r.currentStop)
        r.tick(602, AppMode.PAUSE, null, 10, 0, 95.2, stops)
        assertEquals("횡성휴게소", r.currentStop)
    }

    @Test
    fun `휴게소에서 먼 곳에 멈추면 길가`() {
        val r = TripRecorder()
        r.tick(600, AppMode.PAUSE, null, 10, 0, 40.0, listOf(RestStop("Y03", "횡성휴게소", StopType.SERVICE_AREA, 95.0)))
        assertEquals(TripRecorder.ROADSIDE, r.currentStop)
    }

    @Test
    fun `주의 횟수는 1초 값 기준이고 39·40을 오가도 한 번만 센다`() {
        val r = TripRecorder()
        listOf(10, 39, 40, 39, 41, 30, 45, 20).forEachIndexed { i, v ->
            r.onMotion(v, i * 60L, null)
            r.tick(i, AppMode.DRIVING, null, 10, i, i * 1.0, emptyList())
        }
        assertEquals(2, r.log().cautionCount, "40 진입 → 30까지 내려감 → 45 다시 진입")
    }
}
