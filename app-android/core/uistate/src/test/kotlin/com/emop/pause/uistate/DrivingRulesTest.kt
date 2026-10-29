package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan T4-12·T10-2 주행 화면: 텍스트 2줄·버튼 ≤ 2개. */
class DrivingRulesTest {
    private val driveFixtures = Scene.entries.map(Fixtures::screen).filterIsInstance<Screen.Drive>().map { it.ui }

    @Test
    fun `T4-12 T10-2 모든 주행 시안 장면이 주행 제한을 지킨다`() {
        assertTrue(driveFixtures.size >= 7)
        driveFixtures.forEach { assertEquals(emptyList<String>(), DrivingRules.violations(it)) }
    }

    @Test
    fun `T4-12 버튼 수 - 제안 2, 긴급 1, 제안 없음 0, 음성 실패 시트 2`() {
        assertEquals(2, DrivingRules.buttons(Fixtures.drive))
        assertEquals(1, DrivingRules.buttons(Fixtures.urgent))
        assertEquals(0, DrivingRules.buttons(Fixtures.calm))
        assertEquals(2, DrivingRules.buttons(Fixtures.voiceFail))
    }

    @Test
    fun `T4-12 긴 문장과 많은 칩은 위반으로 잡는다`() {
        val long = Fixtures.calm.copy(
            content = DriveContent.Calm("가".repeat(60), List(4) { ChipUi("칩$it") }),
        )
        val v = DrivingRules.violations(long)
        assertEquals(2, v.size, v.toString())
    }

    @Test
    fun `장면마다 화면 종류가 맞다`() {
        assertTrue(Fixtures.screen(Scene.NAP) is Screen.Nap)
        assertTrue(Fixtures.screen(Scene.ONBOARD3).let { it is Screen.Onboard && it.ui.step == OnboardStep.MIC })
        assertTrue(Fixtures.screen(Scene.SETTINGS) is Screen.Settings)
    }
}
