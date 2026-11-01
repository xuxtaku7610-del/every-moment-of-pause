package com.emop.pause.engine.motion

import com.emop.pause.core.model.PetProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** UPGRADE 4 (test_plan F7-05): 쓸수록 맞춰지는 멀미 예보 — 개인 보정 계수. */
class MotionCalibrationTest {
    private fun many(n: Int, peak: Int, a: MotionAnswer) = List(n) { CalibrationSample(peak, a) }

    @Test
    fun `F7-05 3회 미만이면 그대로 1_0`() {
        assertEquals(1.0, MotionCalibration.scale(emptyList()))
        assertEquals(1.0, MotionCalibration.scale(many(2, 20, MotionAnswer.YES)))
    }

    @Test
    fun `F7-05 지수는 낮았는데 멀미했다가 반복되면 계수 상승, 높았는데 아니요가 반복되면 하락`() {
        val up = MotionCalibration.scale(many(5, 25, MotionAnswer.YES))
        val down = MotionCalibration.scale(many(5, 65, MotionAnswer.NO))
        assertTrue(up > 1.0, "$up")
        assertTrue(down < 1.0, "$down")
        // 더 많이 반복할수록 더 움직인다(상한 안에서)
        assertTrue(MotionCalibration.scale(many(8, 25, MotionAnswer.YES)) >= up)
        // 예측과 맞으면(지수 높고 멀미함) 거의 그대로
        val agree = MotionCalibration.scale(many(5, 70, MotionAnswer.YES))
        assertTrue(agree in 0.95..1.05, "$agree")
    }

    @Test
    fun `F7-05 상한·하한 유지`() {
        assertEquals(MotionCalibration.MAX_SCALE, MotionCalibration.scale(many(40, 0, MotionAnswer.YES)), 1e-9)
        assertEquals(MotionCalibration.MIN_SCALE, MotionCalibration.scale(many(40, 100, MotionAnswer.NO)), 1e-9)
    }

    @Test
    fun `F7-05 계수는 주행 자극 민감도에 곱하지 않고 개인 계수로만, 화면 문구는 보통에서 바뀐 정도`() {
        val p = PetProfile("콩이")
        // 결정 11/01: 화면의 주행 자극 숫자는 보정 전 값 — 계수는 예보 단계·쉼표 시각(personal)에만
        assertEquals(MotionConstants.sensitivity(p), MotionConstants.sensitivity(p.copy(motionScale = 1.3)), 1e-9)
        assertEquals(1.3, MotionConstants.personalScale(p.copy(motionScale = 1.3)), 1e-9)
        assertEquals(MotionCalibration.MAX_SCALE, MotionConstants.personalScale(p.copy(motionScale = 9.0)), 1e-9)
        assertEquals("보통", MotionCalibration.label(1.0))
        assertEquals("조금 높음", MotionCalibration.label(1.15))
        assertEquals("낮음", MotionCalibration.label(0.7))
    }

    @Test
    fun `F7-05b 신고 없음(괜찮았음)은 신고의 절반 이하 가중치로만 반영`() {
        assertTrue(MotionCalibration.QUIET_WEIGHT <= 0.5, "가중치 ${MotionCalibration.QUIET_WEIGHT}")
        val base = many(3, 70, MotionAnswer.YES)
        val full = MotionCalibration.scale(base + CalibrationSample(70, MotionAnswer.NO))
        val quiet = MotionCalibration.scale(base + CalibrationSample(70, MotionAnswer.NO, MotionCalibration.QUIET_WEIGHT))
        val before = MotionCalibration.scale(base)
        // 같은 "아니요"라도 신고 없음은 덜 내린다 — 내린 폭이 절반 이하
        assertTrue(quiet < before && quiet > full, "before=$before quiet=$quiet full=$full")
        assertTrue(kotlin.math.ln(before / quiet) <= 0.5 * kotlin.math.ln(before / full) + 1e-9)
    }
}
