package com.emop.pause.trip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** UPGRADE 3 (test_plan F5-06): 증상 메모 → 도착 예정 공유. 기기에만, 체크했을 때만 공유. */
class VetMemoTest {
    @Test
    fun `F5-06 체크하면 고른 증상·메모·오늘 요약을 공유, 안 하면 아무것도`() {
        val memo = VetMemo(chips = setOf("구토", "헐떡임"), text = "  아침에 사료를 반만 먹었어요  ", include = true)
        val out = VetMemo.share(memo, tripLine = "이동 2시간 10분 · 주행 자극 최고 46 · 관찰 신고 없음")
        assertEquals(listOf("구토", "헐떡임"), out.symptoms)
        assertEquals("아침에 사료를 반만 먹었어요", out.memo)
        assertEquals("이동 2시간 10분 · 주행 자극 최고 46 · 관찰 신고 없음", out.tripLine)
        val off = VetMemo.share(memo.copy(include = false), tripLine = "x")
        assertTrue(off.symptoms.isEmpty())
        assertNull(off.memo)
        assertNull(off.tripLine)
    }

    @Test
    fun `F5-06 버튼 목록과 받아쓰기 덧붙이기, 길이 제한`() {
        assertEquals(listOf("구토", "설사", "헐떡임", "축 처짐", "떨림", "기타"), VetMemo.CHIPS)
        val m = VetMemo().append("구토 두 번").append("물은 마셨어요")
        assertEquals("구토 두 번 / 물은 마셨어요", m.text)
        assertEquals(VetMemo.MAX_TEXT, VetMemo().withText("가".repeat(500)).text.length)
        assertEquals(VetMemo(), VetMemo().append("   "))
    }

    @Test
    fun `F5-06 오늘 요약 한 줄 - 이동 시간·멀미 주의 횟수·최고·증상 이벤트`() {
        assertEquals("이동 2시간 10분 · 주행 자극 최고 46 · 관찰 신고 없음", VetMemo.tripLine(130, cautionCount = 1, peak = 46, events = emptyList()))
        assertEquals("이동 45분 · 주행 자극 최고 20 · 관찰 신고 헐떡임 1회", VetMemo.tripLine(45, cautionCount = 0, peak = 20, events = listOf("헐떡임")))
        assertNull(VetMemo.tripLine(0, 0, 0, emptyList()))
    }
}
