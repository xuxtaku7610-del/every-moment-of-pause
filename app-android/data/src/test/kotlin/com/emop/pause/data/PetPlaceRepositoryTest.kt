package com.emop.pause.data

import com.emop.pause.core.model.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** F4-02·F4-03·F5-01 (서버 없이 앱 내장 샘플로 같은 계산). */
class PetPlaceRepositoryTest {
    private val repo = PetPlaceRepository(
        api = null,
        bundledPlaces = { File("../../server/app/data/sample_pet_places.json").readText() },
        bundledVets = { File("../../server/app/data/sample_vets.json").readText() },
    )
    private val route = listOf(LatLng(37.5665, 126.9780), LatLng(37.3300, 127.8600), LatLng(37.5700, 128.4000), LatLng(37.7519, 128.8761))

    @Test
    fun `F4-02 서버 없이 경로 버퍼 안 장소를 진행 순으로`() = runTest {
        val r = repo.alongRoute(route, 230_000.0)
        assertEquals(PlaceSource.BUNDLED, r.source)
        assertTrue(r.items.isNotEmpty() && r.items.all { it.sample && (it.offKm ?: 99.0) <= 5.0 })
        val along = r.items.map { it.alongKm!! }
        assertEquals(along.sorted(), along)
    }

    @Test
    fun `F4-03 목적지 3km 안 산책로·카페`() = runTest {
        val r = repo.near(LatLng(37.7519, 128.8761))
        assertTrue(r.items.size >= 2)
        assertTrue(r.items.all { it.category in setOf("walk", "cafe") && it.offKm!! <= 3.0 })
    }

    @Test
    fun `F5-01 서버 없이 가까운 동물병원, 전화번호 없음`() = runTest {
        val r = repo.vetsNear(LatLng(37.7519, 128.8761), 3)
        assertEquals(3, r.items.size)
        assertEquals(r.items.map { it.distanceKm }.sorted(), r.items.map { it.distanceKm })
        assertTrue(r.items.all { it.phone == null && it.sample })
    }

    @Test
    fun `경로 진행 비율 지점`() {
        assertEquals(route.first(), PetPlaceRepository.pointAlong(route, 0.0))
        assertEquals(route.last(), PetPlaceRepository.pointAlong(route, 1.0))
    }
}
