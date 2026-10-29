package com.emop.pause.voice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KoreanNumberTest {
    @Test
    fun `T1-2 다섯 시간·5시간·오 시간은 모두 5`() {
        for (s in listOf("5시간", "다섯 시간", "다섯시간", "오 시간", "오시간이요", "음 다섯 시간 정도")) {
            assertEquals(5f, KoreanNumber.sleepHours(s), s)
        }
    }

    @Test
    fun `반은 0점5, 두 자리 고유어·한자어`() {
        assertEquals(6.5f, KoreanNumber.sleepHours("여섯 시간 반"))
        assertEquals(4f, KoreanNumber.sleepHours("네 시간"))
        assertEquals(11, KoreanNumber.first("열한 시"))
        assertEquals(24, KoreanNumber.first("스물네 시간"))
        assertEquals(24, KoreanNumber.first("이십사 시간"))
        assertEquals(10, KoreanNumber.first("십 분"))
        assertEquals(7, KoreanNumber.first("7시간"))
    }

    @Test
    fun `T1-5 범위 밖이나 숫자 없음은 null - 재질문`() {
        assertNull(KoreanNumber.sleepHours("0시간"))
        assertNull(KoreanNumber.sleepHours("25시간"))
        assertNull(KoreanNumber.sleepHours("오늘 잘 잤어"))
        assertNull(KoreanNumber.sleepHours("세상에"))
        assertNull(KoreanNumber.sleepHours(""))
    }
}
