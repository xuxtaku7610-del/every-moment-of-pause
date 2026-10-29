package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.data.PetPlace
import com.emop.pause.uistate.DrivingRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArrivalWalkTest {
    private fun place(name: String, cat: String, km: Double) = PetPlace(name, name, cat, 0.0, 0.0, null, null, true, offKm = km)

    @Test
    fun `F4-03 주행 중 남은 시간 10분 안에서 한 번만`() {
        assertTrue(ArrivalWalk.due(AppMode.DRIVING, 590, done = false))
        assertFalse(ArrivalWalk.due(AppMode.DRIVING, 700, done = false))
        assertFalse(ArrivalWalk.due(AppMode.DRIVING, 590, done = true))
        assertFalse(ArrivalWalk.due(AppMode.PAUSE, 590, done = false))
        assertFalse(ArrivalWalk.due(AppMode.DRIVING, 0, done = false))
    }

    @Test
    fun `F4-03 산책로를 카페보다 먼저, 가까운 곳 — 없으면 제안하지 않는다`() {
        val picked = ArrivalWalk.pick(listOf(place("카페", "cafe", 0.5), place("먼 산책로", "walk", 2.5), place("가까운 산책로", "walk", 1.0)))
        assertEquals("가까운 산책로", picked?.name)
        assertEquals("카페", ArrivalWalk.pick(listOf(place("카페", "cafe", 0.5)))?.name)
        assertNull(ArrivalWalk.pick(emptyList()))
    }

    @Test
    fun `F4-03 문장은 주행 중 한 줄(48자) 안, 샘플 표시는 읽지 않는다`() {
        val line = ArrivalWalk.phrase("콩이", place("[샘플] 강릉 반려견 동반 산책로 1", "walk", 2.3))
        assertEquals("곧 도착해요. 근처 강릉 반려견 동반 산책로 1에서 콩이와 산책 어때요?", line)
        assertTrue(line.length <= DrivingRules.MAX_BLOCK_CHARS)
        val long = ArrivalWalk.phrase("아주아주긴이름을가진강아지", place("아주 아주 긴 이름의 반려견 동반 산책로 공원", "walk", 1.0))
        assertTrue(long.length <= DrivingRules.MAX_BLOCK_CHARS, long)
    }
}
