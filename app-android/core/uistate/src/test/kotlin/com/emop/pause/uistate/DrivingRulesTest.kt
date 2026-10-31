package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan DS-02·DS-03: 주행 화면은 텍스트 2줄 이하, 버튼 2개 이하. */
class DrivingRulesTest {
    private val driveFixtures = Scene.entries.map(Fixtures::screen).filterIsInstance<Screen.Drive>().map { it.ui }

    @Test
    fun `DS-02 모든 주행 장면이 주행 제한을 지킨다`() {
        assertTrue(driveFixtures.size >= 5)
        (driveFixtures + Fixtures.voiceFail).forEach { assertEquals(emptyList<String>(), DrivingRules.violations(it)) }
    }

    @Test
    fun `DS-02 버튼 수 - 쉼표 추천 2, 피로 위험 1, 예보·안내 0, 음성 실패 시트 2`() {
        assertEquals(2, DrivingRules.buttons(Fixtures.suggest))
        assertEquals(1, DrivingRules.buttons(Fixtures.urgent))
        assertEquals(0, DrivingRules.buttons(Fixtures.drive))
        assertEquals(0, DrivingRules.buttons(Fixtures.forecast))
        assertEquals(2, DrivingRules.buttons(Fixtures.voiceFail))
    }

    @Test
    fun `DS-02 긴 문장·긴 장소 이름·이유 3개는 위반으로 잡는다`() {
        val long = Fixtures.drive.copy(content = DriveContent.Forecast("가".repeat(30), "천천히 가요"))
        assertEquals(1, DrivingRules.violations(long).size)
        val many = Fixtures.drive.copy(content = DriveContent.Suggest(10, "아주아주긴휴게소이름입니다정말로", listOf("a", "b", "c")))
        assertEquals(2, DrivingRules.violations(many).size, DrivingRules.violations(many).toString())
    }

    @Test
    fun `장면마다 화면 종류가 맞다`() {
        assertTrue(Fixtures.screen(Scene.WAIT) is Screen.Wait)
        assertTrue(Fixtures.screen(Scene.ARRIVAL) is Screen.Arrival)
        assertTrue(Fixtures.screen(Scene.PET) is Screen.PetProfile)
        assertTrue(Fixtures.screen(Scene.SETTINGS) is Screen.Settings)
    }
}
