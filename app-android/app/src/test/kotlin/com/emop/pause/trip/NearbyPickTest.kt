package com.emop.pause.trip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan F4-10~15: 주행 중 주변 추천 (UI v9, 일반 도로에서만 — 사용자 결정 11/03). */
class NearbyPickTest {
    private fun input(now: Long = 0, highway: Boolean? = false, busy: Boolean = false, reports: Int = 0, emergency: Boolean = false, remainKm: Double? = 30.0) =
        NearbyInput(now, highway, busy, reports, emergency, remainKm)

    private fun place(id: String, kind: NearbyKind, ahead: Double = 2.0, off: Double = 0.3, dest: Double? = null, sample: Boolean = false) =
        NearbyPlace(id, "장소 $id", kind, 37.7, 128.9, ahead, off, dest, sample)

    @Test
    fun `F4-10 고속도로이거나 아직 모르면 주변 추천을 띄우지 않는다`() {
        val p = NearbyPick()
        assertNull(p.due(input(highway = true, remainKm = 5.0)))
        assertNull(p.due(input(highway = true, reports = 1)))
        assertNull(p.due(input(highway = null, remainKm = 5.0)))
        // 일반 도로에서는 기존 조건대로
        assertEquals(NearbyWhy.DESTINATION, p.due(input(highway = false, remainKm = 9.9)))
        assertEquals(NearbyWhy.REPORT_VET, p.due(input(highway = false, reports = 1)))
        assertNull(p.due(input(highway = false, remainKm = 10.1)))
    }

    @Test
    fun `F4-10 고속도로 판단 = 최근 5분 평균 속도 80km·h 이상, 표본 60초 미만은 모름`() {
        val h = HighwayGuess()
        repeat(59) { h.add(it * 1000L, 100f) }
        assertNull(h.highway())
        h.add(59_000, 100f)
        assertEquals(true, h.highway())
        // 5분 동안 45km/h로 달리면 일반 도로 (앞의 100km/h는 창 밖으로 빠진다)
        for (t in 60..400) h.add(t * 1000L, 45f)
        assertEquals(false, h.highway())
        // 속도 신호가 없으면 표본을 넣지 않는다
        val none = HighwayGuess()
        repeat(120) { none.add(it * 1000L, null) }
        assertNull(none.highway())
    }

    @Test
    fun `F4-11 목적지 10km 안 - 산책로가 먼저, 없으면 반려견 동반 쇼핑, 목적지 3km 밖은 버린다`() {
        val p = NearbyPick()
        val walk = place("w", NearbyKind.WALK, dest = 2.5)
        val shop = place("s", NearbyKind.SHOP, dest = 1.4)
        val far = place("f", NearbyKind.WALK, dest = 3.5)
        assertEquals("w", p.pick(NearbyWhy.DESTINATION, listOf(shop, walk, far))!!.place.id)
        assertEquals("s", p.pick(NearbyWhy.DESTINATION, listOf(shop, far))!!.place.id)
        val o = p.pick(NearbyWhy.DESTINATION, listOf(shop))!!
        assertEquals("목적지에서 1.4km", o.reason)
        assertEquals("장소 s", o.title)
        assertEquals("2.0km 앞 · 반려견 동반 쇼핑", o.line)
        // 한 여행에 한 번
        p.onSearched(NearbyWhy.DESTINATION)
        assertNull(p.due(input(remainKm = 5.0)))
    }

    @Test
    fun `F4-12 데이터가 없는 종류는 안 뜨고, 샘플 이름은 쓰지 않는다 (지어내지 않음)`() {
        val p = NearbyPick()
        assertNull(p.pick(NearbyWhy.DESTINATION, emptyList()))
        assertNull(p.pick(NearbyWhy.DESTINATION, listOf(place("x", NearbyKind.WALK, dest = 1.0, sample = true))))
        // 마켓이 데이터에 없으면 산책로만, 산책로도 없으면 아무것도
        assertNull(p.pick(NearbyWhy.DESTINATION, listOf(place("v", NearbyKind.VET, dest = 1.0))))
        assertNull(p.pick(NearbyWhy.REPORT_VET, listOf(place("v", NearbyKind.VET, sample = true))))
    }

    @Test
    fun `F4-13 관찰 신고 + 앞 5km·경로 1km 안 병원 - 응급이면 응급 흐름이 먼저라 안 뜬다`() {
        val p = NearbyPick()
        val ok = place("a", NearbyKind.VET, ahead = 3.0, off = 0.5)
        val behind = place("b", NearbyKind.VET, ahead = -1.0, off = 0.1)
        val tooFar = place("c", NearbyKind.VET, ahead = 6.0, off = 0.1)
        val offRoute = place("d", NearbyKind.VET, ahead = 2.0, off = 1.5)
        assertEquals("a", p.pick(NearbyWhy.REPORT_VET, listOf(behind, tooFar, offRoute, ok))!!.place.id)
        assertNull(p.pick(NearbyWhy.REPORT_VET, listOf(behind, tooFar, offRoute)))
        assertNull(p.due(input(reports = 1, emergency = true)))
        // 신고가 없으면 병원 추천도 없다
        assertNull(p.due(input(reports = 0)))
    }

    @Test
    fun `F4-14 같은 장소는 다시 안 뜨고 추천 사이 최소 15분, 다른 카드가 떠 있으면 미룬다`() {
        val p = NearbyPick()
        val a = place("a", NearbyKind.VET)
        val offer = p.pick(NearbyWhy.REPORT_VET, listOf(a))!!
        p.onShown(offer, 0)
        assertNull(p.due(input(now = 14 * 60_000L, reports = 1)))
        assertEquals(NearbyWhy.REPORT_VET, p.due(input(now = 15 * 60_000L, reports = 1)))
        assertNull(p.pick(NearbyWhy.REPORT_VET, listOf(a)), "같은 곳은 다시 안 뜸")
        assertNull(p.due(input(now = 20 * 60_000L, reports = 1, busy = true)), "예보·쉼표 추천·응급 중이면 미룸")
    }

    @Test
    fun `F4-15 병원 후보가 없으면 1분 뒤 다시 찾고, 그 사이 목적지 추천은 볼 수 있다`() {
        val p = NearbyPick()
        assertEquals(NearbyWhy.REPORT_VET, p.due(input(now = 0, reports = 1, remainKm = 9.0)))
        p.onEmpty(0)
        assertEquals(NearbyWhy.DESTINATION, p.due(input(now = 1_000, reports = 1, remainKm = 9.0)))
        p.onSearched(NearbyWhy.DESTINATION)
        assertNull(p.due(input(now = 30_000, reports = 1, remainKm = 9.0)))
        assertEquals(NearbyWhy.REPORT_VET, p.due(input(now = 60_000, reports = 1, remainKm = 9.0)))
        p.reset()
        assertFalse(p.destinationDone)
    }

    @Test
    fun `F4-11 음성 한 줄은 48자 이하, 길면 이름을 뺀다`() {
        val o = NearbyOffer(place("s", NearbyKind.SHOP, dest = 1.4).copy(name = "야옹아멍멍해봐 강릉내곡점"), NearbyWhy.DESTINATION)
        assertEquals("곧 도착해요. 근처 야옹아멍멍해봐 강릉내곡점에 콩이와 들를까요?", o.phrase("콩이"))
        val long = NearbyOffer(place("w", NearbyKind.WALK, dest = 1.0).copy(name = "아주 아주 긴 이름을 가진 반려견 동반 산책로 공원 입구 주차장"), NearbyWhy.DESTINATION)
        assertTrue(long.phrase("콩이").length <= 48, long.phrase("콩이"))
        assertFalse("주차장" in long.phrase("콩이"))
    }
}
