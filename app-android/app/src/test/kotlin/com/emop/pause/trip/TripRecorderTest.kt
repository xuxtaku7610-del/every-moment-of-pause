package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
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
        assertEquals(listOf(0, 5, 10, 15, 20, 25, 20, 20, 20, 45, 50, 55, 60), log.motionSeries)
        assertEquals(60, log.motionPeak)
        assertEquals(40, log.firstCautionMin)
        assertEquals(1, log.visits.size)
        assertEquals(60, log.drivingMin)
    }
}
