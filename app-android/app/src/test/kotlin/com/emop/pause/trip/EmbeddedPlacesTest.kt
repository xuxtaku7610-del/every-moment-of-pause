package com.emop.pause.trip

import com.emop.pause.core.model.LatLng
import com.emop.pause.data.PetPlaceRepository
import com.emop.pause.data.PlaceSource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * test_plan F4-16·F5-11 (UI v9 마무리): 제출 APK(릴리스 = 서버 주소 없음)는 앱 내장 데이터만 쓴다.
 * 내장 데이터 = 서버 DB의 실제 데이터 내보내기(data/embedded) — 서버 없이도 도착 10km 전 주변 추천(실제 TourAPI 쇼핑 장소)과
 * 긴급 도움의 가장 가까운 실제 병원이 나와야 한다. 샘플 이름은 없어야 한다.
 */
class EmbeddedPlacesTest {
    private val repo = PetPlaceRepository(
        api = null,
        bundledPlaces = { File("../data/embedded/pet_places.json").readText() },
        bundledVets = { File("../data/embedded/vets.json").readText() },
    )
    private val gangneung = LatLng(37.7519, 128.8761)

    @Test
    fun `F4-16 서버 없이 목적지 10km 전 주변 추천 = 실제 TourAPI 쇼핑 장소`() = runBlocking {
        val found = repo.near(gangneung, (NearbyPick.DEST_RADIUS_KM * 1000).toInt(), NearbyPick.PLACE_CATEGORIES)
        assertEquals(PlaceSource.BUNDLED, found.source)
        assertTrue(found.items.none { it.sample || it.name.startsWith("[샘플]") }, found.items.map { it.name }.toString())
        // TripCoordinator.nearbyCandidates와 같은 매핑 (walk = 산책로·공원, shop = 반려견 동반 쇼핑)
        val candidates = found.items.mapNotNull { p ->
            val kind = when (p.category) {
                "walk" -> NearbyKind.WALK
                "shop" -> NearbyKind.SHOP
                else -> null
            } ?: return@mapNotNull null
            NearbyPlace(p.id, p.name, kind, p.lat, p.lng, aheadKm = 9.7, offKm = 1.0, fromDestKm = p.offKm, sample = p.sample)
        }
        val pick = NearbyPick()
        assertEquals(NearbyWhy.DESTINATION, pick.due(NearbyInput(0, highway = false, busy = false, reports = 0, emergency = false, remainKm = 9.9)))
        val offer = pick.pick(NearbyWhy.DESTINATION, candidates)!!
        assertEquals("야옹아멍멍해봐 강릉내곡점", offer.place.name)
        assertEquals(NearbyKind.SHOP, offer.place.kind)
        assertEquals("목적지에서 1.4km", offer.reason)
        assertEquals("9.7km 앞 · 반려견 동반 쇼핑", offer.line)
    }

    @Test
    fun `F5-11 서버 없이 긴급 도움 가장 가까운 병원 = 실제 인허가 병원 (시뮬레이터는 시연용 제휴 병원이 앞)`() = runBlocking {
        val hoengseong = LatLng(37.495, 128.07)
        val found = repo.vetsNear(hoengseong)
        assertEquals(PlaceSource.BUNDLED, found.source)
        val first = found.items.first()
        assertFalse(first.sample, first.name)
        assertFalse(first.name.startsWith("[샘플]"))
        assertTrue(first.distanceKm < 15, "${first.name} ${first.distanceKm}km")
        assertTrue(found.items.zipWithNext().all { (a, b) -> a.distanceKm <= b.distanceKm })
        // 시뮬레이터 시연은 가상 제휴 병원을 맨 앞에(사용자 결정 10/31), 그 다음이 실제 병원
        val sim = DemoPartner.prepend(found.items, hoengseong, simulation = true)
        assertEquals(DemoPartner.ID, sim[0].id)
        assertEquals(first.id, sim[1].id)
        assertEquals(found.items, DemoPartner.prepend(found.items, hoengseong, simulation = false))
    }

    @Test
    fun `F4-16 내장 데이터는 실제 데이터만, 개수와 출처`() {
        val places = PetPlaceRepository.parsePlaces(File("../data/embedded/pet_places.json").readText())
        val vets = PetPlaceRepository.parseVets(File("../data/embedded/vets.json").readText())
        assertEquals(327, places.size)
        assertEquals(5_063, vets.size)
        assertTrue(places.none { it.sample } && vets.none { it.sample })
        assertTrue(places.count { it.category == "shop" } >= 1)
        val raw = File("../data/embedded/pet_places.json").readText() + File("../data/embedded/vets.json").readText()
        assertFalse("serviceKey" in raw || "imageUrl" in raw)
        assertTrue("한국관광공사" in raw && "인허가" in raw)
    }
}
