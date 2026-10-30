package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** UPGRADE 6 (test_plan F4-08): 장소 필터 — 대형견·실내는 명시된 곳(true)만, 모름은 따로 센다. */
class PlaceFilterTest {
    private fun p(id: String, cat: String, large: Boolean?, indoor: Boolean?) =
        PlaceUi(id, id, cat, "1km", null, false, largeDogOk = large, indoorOk = indoor)

    private val items = listOf(
        p("a", "카페", true, true), p("b", "카페", null, false), p("c", "식당", false, null),
        p("d", "산책로", true, null), p("e", "관광지", null, null), p("f", "숙소", true, null),
    )

    @Test
    fun `F4-08 분류 묶음과 개수`() {
        assertEquals(listOf("전체" to 6, "카페" to 2, "식당" to 1, "숙소" to 1, "산책·관광" to 2), PlaceFilter.groups(items))
        assertEquals(listOf("d", "e"), PlaceFilter.apply(items, "산책·관광", false, false).shown.map { it.id })
    }

    @Test
    fun `F4-08 대형견·실내 필터는 명시된 곳만, 정보 없음은 따로`() {
        val large = PlaceFilter.apply(items, null, largeDog = true, indoor = false)
        assertEquals(listOf("a", "d", "f"), large.shown.map { it.id })
        assertEquals(2, large.unknown) // b, e: 대형견 정보 없음 (c는 명시적으로 불가)
        val both = PlaceFilter.apply(items, null, largeDog = true, indoor = true)
        assertEquals(listOf("a"), both.shown.map { it.id })
        assertEquals("대형견 정보 없음 2곳·실내 정보 없음 4곳은 빼고 보여요", PlaceFilter.unknownNote(items, null, true, true))
        assertEquals(null, PlaceFilter.unknownNote(items, null, false, false))
    }
}
