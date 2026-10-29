package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** COMMERCIAL_READINESS D P0: CarUxRestrictions 플래그별 처리, test_plan T10-2. */
class UxPolicyTest {
    @Test
    fun `T10-2 NO_VIDEO면 정차 중 허용 조건이어도 움직임 정지`() {
        assertTrue(UxPolicy.motionAllowed(true, UxPolicy.Flags()))
        assertFalse(UxPolicy.motionAllowed(true, UxPolicy.Flags(noVideo = true)))
        assertFalse(UxPolicy.motionAllowed(false, UxPolicy.Flags()))
    }

    @Test
    fun `T1-6 NO_SETUP이나 NO_KEYBOARD면 설정·온보딩 막음`() {
        assertTrue(UxPolicy.setupAllowed(UxPolicy.Flags()))
        assertFalse(UxPolicy.setupAllowed(UxPolicy.Flags(noSetup = true)))
        assertFalse(UxPolicy.setupAllowed(UxPolicy.Flags(noKeyboard = true)))
    }

    @Test
    fun `LIMIT_STRING_LENGTH - 첫 문장만, 그래도 길면 말줄임`() {
        assertEquals("1시간 40분째 쉬지 않고 운전 중이에요.", UxPolicy.shorten("1시간 40분째 쉬지 않고 운전 중이에요. 어젯밤 5시간 주무셨어요.", 30))
        assertEquals("가나다라…", UxPolicy.shorten("가나다라마바사아자차", 5))
        assertEquals("짧아요.", UxPolicy.shorten("짧아요.", 30))
        assertEquals(null, UxPolicy.shorten(null, 30))
    }

    @Test
    fun `LIMIT_STRING_LENGTH면 주행 화면 문장을 줄이고, 아니면 그대로`() {
        val f = UxPolicy.Flags(limitStringLength = true, maxStringLength = 25)
        val ui = UxPolicy.apply(Fixtures.drive, f)
        assertEquals("1시간 40분째 쉬지 않고 운전 중이에요.", ui.fatigue!!.detail)
        assertTrue(ui.family.all { it.text.length <= 25 })
        assertEquals(Fixtures.drive, UxPolicy.apply(Fixtures.drive, UxPolicy.Flags()))
        assertEquals(emptyList<String>(), DrivingRules.violations(ui))
    }
}
