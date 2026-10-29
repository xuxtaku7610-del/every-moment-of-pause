package com.emop.pause.presentation

import com.emop.pause.data.Found
import com.emop.pause.data.PetPlace
import com.emop.pause.data.PlaceSource
import com.emop.pause.data.Vet
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.MotionPhrases
import com.emop.pause.engine.pet.WaitStatus
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.WaitShareState
import com.emop.pause.uistate.DrivingRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v5 화면 계약에 새로 넣은 값 (docs/ui_contract.md, STYLE_V5 §1 "시안과 다르게 하는 것"). */
class V5MapperTest {
    private fun place(indoor: String?, large: Boolean?, companion: String?, image: String? = null) =
        PetPlace("pp_1", "카페", "cafe", 37.7, 128.9, null, null, false, offKm = 1.2, imageUrl = image, indoor = indoor, largeDog = large, companion = companion)

    @Test
    fun `F4-05 장소 태그는 데이터에 있는 것만, 사진이 있으면 관광공사 표기`() {
        assertEquals(listOf("실외", "대형견 불가", "일부구역 동반"), UiMapper.placeTags(place("outdoor", false, "일부구역 동반가능")))
        assertEquals(emptyList<String>(), UiMapper.placeTags(place(null, null, null)))
        val ui = UiMapper.places(null, Found(listOf(place(null, true, null, "https://tong.visitkorea.or.kr/a.jpg")), PlaceSource.SERVER), false)
        assertEquals("사진 한국관광공사", ui.credit)
        assertEquals("https://tong.visitkorea.or.kr/a.jpg", ui.items.single().imageUrl)
        assertNull(UiMapper.places(null, Found(listOf(place(null, null, null)), PlaceSource.SERVER), false).credit)
    }

    @Test
    fun `F5-03 병원은 영업 상태까지만 — 24시·영업시간을 쓰지 않는다`() {
        val real = Vet("vt_1", "강릉24시동물의료센터", 37.7, 128.9, 2.3, "강릉시", null, sample = false)
        val sample = real.copy(id = "vs_1", sample = true)
        val ui = UiMapper.vets(Found(listOf(real, sample), PlaceSource.SERVER), false)
        assertEquals("영업 중", ui.items[0].status)
        assertNull(ui.items[1].status)
        assertTrue(ui.items.all { it.minutes != null && it.minutes!! >= 1 })
        val all = ui.items.joinToString { listOfNotNull(it.distance, it.status).joinToString() } + ui.note
        assertFalse("24시" in all || "24시간" in all || "진료 중" in all, all)
        // 주행 중 한 줄: 이름이 길면 14자로 줄인다 (DS-02)
        val drive = UiMapper.vetDrive("강원양돈축산업협동조합동물병원", 3.1)
        assertTrue(drive.name.length <= DrivingRules.MAX_PLACE_CHARS, drive.name)
    }

    @Test
    fun `F3-11 대기 모드 안내는 처음 20초, 배터리는 신호가 있을 때만`() {
        val t = TripUi()
        val first = UiMapper.wait(WaitStatus(active = true, elapsedSec = 3), t)
        assertEquals(UiMapper.WAIT_NOTICE, first.notice)
        assertNull(first.batteryUsed)
        val later = UiMapper.wait(WaitStatus(active = true, elapsedSec = 300, batteryStart = 58, batteryNow = 57), t)
        assertNull(later.notice)
        assertEquals("1%", later.batteryUsed)
        assertEquals("실내 온도 정보 없음", later.cabin)
        val back = UiMapper.wait(WaitStatus(active = true, elapsedSec = 300), t.copy(share = WaitShareState("u", true, returning = true)))
        assertEquals("주인이 돌아오는 중이에요", back.headline)
    }

    @Test
    fun `F1-13 v5 예보 제목과 코칭은 각각 한 줄(24자) 이하`() {
        listOf(20, 45, 80).forEach { idx ->
            val f = MotionForecast(3_200.0, 2_000.0, 720, idx, 17)
            assertTrue(MotionPhrases.title(f).length <= DrivingRules.MAX_LINE_CHARS, MotionPhrases.title(f))
            assertTrue(MotionPhrases.coach(f, "아주긴이름의반려견").length <= DrivingRules.MAX_LINE_CHARS, MotionPhrases.coach(f, "아주긴이름의반려견"))
        }
        assertEquals("3.2km 뒤 굽은 길 · 12분", MotionPhrases.title(MotionForecast(3_200.0, 2_000.0, 720, 45, 17)))
    }
}
