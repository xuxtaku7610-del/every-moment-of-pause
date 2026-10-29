package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan DS-02·DS-04: CarUxRestrictions 플래그별 처리. */
class UxPolicyTest {
    @Test
    fun `DS-02 NO_VIDEO면 정차 중 허용 조건이어도 움직임 정지`() {
        assertTrue(UxPolicy.motionAllowed(true, UxPolicy.Flags()))
        assertFalse(UxPolicy.motionAllowed(true, UxPolicy.Flags(noVideo = true)))
        assertFalse(UxPolicy.motionAllowed(false, UxPolicy.Flags()))
    }

    @Test
    fun `DS-04 NO_SETUP이나 NO_KEYBOARD면 프로필 편집·설정 막음`() {
        assertTrue(UxPolicy.setupAllowed(UxPolicy.Flags()))
        assertFalse(UxPolicy.setupAllowed(UxPolicy.Flags(noSetup = true)))
        assertFalse(UxPolicy.setupAllowed(UxPolicy.Flags(noKeyboard = true)))
    }

    @Test
    fun `LIMIT_STRING_LENGTH - 첫 문장만, 그래도 길면 말줄임`() {
        assertEquals("다음 쉼표는 32분 뒤예요.", UxPolicy.shorten("다음 쉼표는 32분 뒤예요. 횡성휴게소에 놀이터가 있어요.", 30))
        assertEquals("가나다라…", UxPolicy.shorten("가나다라마바사아자차", 5))
        assertEquals(null, UxPolicy.shorten(null, 30))
    }

    @Test
    fun `LIMIT_STRING_LENGTH면 예보 문장을 줄이고 이유는 하나만, 아니면 그대로`() {
        val f = UxPolicy.Flags(limitStringLength = true, maxStringLength = 20)
        val fc = UxPolicy.apply(Fixtures.forecast, f).content as DriveContent.Forecast
        assertTrue(fc.line.length <= 20, fc.line)
        val sg = UxPolicy.apply(Fixtures.suggest, f).content as DriveContent.Suggest
        assertEquals(1, sg.reasons.size)
        assertEquals(Fixtures.forecast, UxPolicy.apply(Fixtures.forecast, UxPolicy.Flags()))
    }
}
