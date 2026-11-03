package com.emop.pause.presentation

import com.emop.pause.engine.pet.WaitStatus
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.WaitShareState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 잠깐 대기 "한눈에 크게" (2026-11-03, test_plan F3-10~F3-13): 온도 신호 있음/없음, 에어컨 신호, 제한 시간 넘김, 창 폭별 큰 글 한 줄.
 * 기존 대기 테스트(EmptyStatesTest·V5MapperTest)는 그대로.
 */
class WaitGlanceTest {
    private val w = WaitStatus(active = true, elapsedSec = 240, limitSec = 600)

    @Test
    fun `F3-10 실내 온도는 측정값이 있을 때만 큰 숫자, 없으면 줄을 숨긴다`() {
        val with = UiMapper.wait(w.copy(cabinTempC = 24.4f), TripUi(), month = 11)
        assertEquals("24℃", with.temp)
        assertEquals("실내 24℃", with.cabin)
        val without = UiMapper.wait(w, TripUi(), month = 11)
        assertNull(without.temp)
        assertNull(without.cabin)
        assertEquals("4분째", without.elapsed)
        assertEquals("My owner will be back soon", without.english)
        assertEquals("My owner is on the way back", UiMapper.wait(w, TripUi(share = WaitShareState("u", true, returning = true))).english)
    }

    @Test
    fun `F3-11 에어컨 켜짐은 공조 신호가 켜짐일 때만 (앱은 공조를 제어하지 않는다)`() {
        assertTrue(UiMapper.wait(w, TripUi(), acOn = true).acOn)
        assertFalse(UiMapper.wait(w, TripUi(), acOn = false).acOn)
        assertFalse(UiMapper.wait(w, TripUi(), acOn = null).acOn)
        assertFalse(UiMapper.wait(w, TripUi()).acOn)
    }

    @Test
    fun `F3-12 제한 시간을 넘기면 큰 글은 그대로, 보호자 줄에만 한 줄`() {
        val over = UiMapper.wait(w.copy(elapsedSec = 660, overdue = true), TripUi())
        assertEquals("주인이 곧 돌아와요", over.headline)
        assertEquals("설정한 10분이 지났어요", over.overdueLine)
        assertNull(UiMapper.wait(w, TripUi()).overdueLine)
    }

    @Test
    fun `F3-13 큰 글은 창 폭에 맞춰 한 줄 — 전체 창은 기준 72, 1·3 창은 줄여서 한 줄`() {
        val lines = listOf("주인이 돌아오는 중이에요", "24℃")
        // 가장 긴 문구(돌아오는 중) 기준
        assertEquals(WaitLayout.BIG * 2f, WaitLayout.bigSp(2000f, 2f, lines)) // 충분히 넓으면 기준 크기 그대로
        // 1/2·1/3 창: 가장 긴 줄의 어림 폭이 쓸 수 있는 폭을 넘지 않는다
        for (avail in listOf(1200f, 800f, 520f, 380f)) {
            val sp = WaitLayout.bigSp(avail, 2f, lines)
            assertTrue(sp * WaitLayout.em(lines[0]) <= avail + 0.01f, "$avail → $sp")
            assertTrue(sp <= WaitLayout.BIG * 2f)
        }
        // 1/3 창(약 800dp)에서도 기존 metric 36×배율보다 작아지지 않는다 — "약 2배"의 뜻을 지킨다
        assertTrue(WaitLayout.bigSp(800f, 1.3f, listOf("주인이 곧 돌아와요", "24℃")) >= 36f * 1.3f)
        assertEquals(WaitLayout.BIG, WaitLayout.bigSp(100f, 1f, emptyList()))
    }
}
