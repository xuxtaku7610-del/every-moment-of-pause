package com.emop.pause.data

import com.emop.pause.core.model.LatLng
import com.emop.pause.data.net.PauseApi
import com.emop.pause.data.net.PlaceDto
import com.emop.pause.data.net.PlacesReq
import com.emop.pause.data.net.PointDto
import com.emop.pause.data.net.VetDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.hypot

/** 화면이 쓰는 장소 한 곳 (서버·내장 공통). */
data class PetPlace(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val petPolicy: String?,
    val sample: Boolean,
    /** 경로 진행 거리(km, 경로 검색일 때). */
    val alongKm: Double? = null,
    /** 경로·지점에서 떨어진 거리(km). */
    val offKm: Double? = null,
    /** TourAPI 대표 이미지(https, 사진 한국관광공사). */
    val imageUrl: String? = null,
    /** 실내·실외를 말하는 동반 조건 원문 문장. 없으면 null — 실내 여부를 추측하지 않는다(결정 10/30). */
    val spaceNote: String? = null,
    /** 동반 가능 동물 원문 ("소형견(10kg이하) 한마리 동반 가능"). 대형견 가능 여부를 판정하지 않는다. */
    val sizeNote: String? = null,
    val companion: String? = null,
    /** 필터 (UPGRADE 6): 글에 명시됐을 때만, 모르면 null. */
    val largeDogOk: Boolean? = null,
    val indoorOk: Boolean? = null,
)

/** [partner] = 사전 접수함 제휴 병원 (지금은 시연용 가상 병원 한 곳만, trip.DemoPartner). 실제 병원은 false. */
data class Vet(val id: String, val name: String, val lat: Double, val lng: Double, val distanceKm: Double, val address: String?, val phone: String?, val sample: Boolean, val partner: Boolean = false)

/** 어디서 받았는지: 서버면 SERVER, 서버가 없거나 실패하면 앱 내장 샘플. */
enum class PlaceSource { SERVER, BUNDLED }

data class Found<T>(val items: List<T>, val source: PlaceSource)

/**
 * 함께 가는 장소(F4)·동물병원(F5). 서버(api_spec §2.5~2.7)가 없거나 실패하면 앱에 묶은 내장 데이터로 같은 계산을 기기에서 한다.
 * 내장 데이터 = 서버 DB의 실제 데이터를 내보낸 파일(data/embedded, scripts/export_embedded_places.py — TourAPI 반려동물 동반 장소·
 * 영업 중 동물병원, 11/03). 파일에 `sample`이 없으면(옛 샘플 형식) 샘플로 본다.
 * 현재 위치·경로 좌표는 계산에만 쓰고 저장하지 않는다.
 */
class PetPlaceRepository(
    private val api: PauseApi?,
    private val bundledPlaces: () -> String,
    private val bundledVets: () -> String,
) {
    private val places by lazy { parsePlaces(bundledPlaces()) }
    private val vets by lazy { parseVets(bundledVets()) }

    suspend fun alongRoute(points: List<LatLng>, routeLengthM: Double?, bufferM: Int = BUFFER_M): Found<PetPlace> {
        if (points.size < 2) return Found(emptyList(), PlaceSource.BUNDLED)
        api?.let { a ->
            runCatching { a.placesAlongRoute(PlacesReq(points.map { PointDto(it.lat, it.lng) }, bufferM, routeLengthM?.toInt())) }
                .getOrNull()?.let { r -> return Found(r.items.map(::fromDto), PlaceSource.SERVER) }
        }
        val geo = RouteGeo.length(points)
        val scale = if (routeLengthM != null && geo > 0) routeLengthM / geo else 1.0
        val hits = places.mapNotNull { p ->
            val proj = RouteGeo.project(points, p.lat, p.lng)
            if (proj.offsetM > bufferM || proj.behind) null else p.copy(alongKm = proj.alongM * scale / 1000, offKm = proj.offsetM / 1000)
        }.sortedBy { it.alongKm }
        return Found(hits, PlaceSource.BUNDLED)
    }

    suspend fun near(at: LatLng, radiusM: Int = NEAR_M, categories: Set<String> = setOf("walk", "cafe")): Found<PetPlace> {
        api?.let { a ->
            runCatching { a.placesNear(at.lat, at.lng, radiusM, categories.joinToString(",")) }
                .getOrNull()?.let { r -> return Found(r.items.map(::fromDto), PlaceSource.SERVER) }
        }
        val hits = places.filter { it.category in categories }
            .map { it.copy(offKm = distanceM(at, LatLng(it.lat, it.lng)) / 1000) }
            .filter { (it.offKm ?: 0.0) * 1000 <= radiusM }
            .sortedBy { it.offKm }
        return Found(hits, PlaceSource.BUNDLED)
    }

    suspend fun vetsNear(at: LatLng, limit: Int = 5): Found<Vet> {
        api?.let { a ->
            runCatching { a.vetsNearby(at.lat, at.lng, limit) }.getOrNull()?.let { r -> return Found(r.items.map(::fromDto), PlaceSource.SERVER) }
        }
        val hits = vets.map { it.copy(distanceKm = distanceM(at, LatLng(it.lat, it.lng)) / 1000) }.sortedBy { it.distanceKm }.take(limit)
        return Found(hits, PlaceSource.BUNDLED)
    }

    @Serializable private data class PlaceJson(
        val sourceId: String, val name: String, val category: String, val lat: Double, val lng: Double,
        val address: String? = null, val petPolicy: String? = null, val spaceNote: String? = null, val sizeNote: String? = null,
        val companion: String? = null, val largeDogOk: Boolean? = null, val indoorOk: Boolean? = null, val sample: Boolean = true,
    )
    @Serializable private data class PlacesDoc(val items: List<PlaceJson>)
    @Serializable private data class VetJson(
        val licenseNo: String, val name: String, val lat: Double, val lng: Double, val address: String? = null, val phone: String? = null,
        val open: Boolean = true, val sample: Boolean = true,
    )
    @Serializable private data class VetsDoc(val items: List<VetJson>)

    companion object {
        const val BUFFER_M = 5_000
        const val NEAR_M = 3_000
        const val PLACES_ASSET = "pet_places_default.json"
        const val VETS_ASSET = "vets_default.json"
        private val json = Json { ignoreUnknownKeys = true }

        fun parsePlaces(text: String): List<PetPlace> = json.decodeFromString(PlacesDoc.serializer(), text).items
            .map {
                PetPlace(
                    it.sourceId, it.name, it.category, it.lat, it.lng, it.address, it.petPolicy, sample = it.sample,
                    spaceNote = it.spaceNote, sizeNote = it.sizeNote, companion = it.companion, largeDogOk = it.largeDogOk, indoorOk = it.indoorOk,
                )
            }

        fun parseVets(text: String): List<Vet> = json.decodeFromString(VetsDoc.serializer(), text).items.filter { it.open }
            .map { Vet(it.licenseNo, it.name, it.lat, it.lng, 0.0, it.address, it.phone, sample = it.sample) }

        private fun fromDto(d: PlaceDto) = PetPlace(
            d.placeId, d.name, d.category, d.lat, d.lng, d.address, d.petPolicy, d.sample, d.distanceAlongRouteKm, d.offRouteKm ?: d.distanceKm,
            d.imageUrl, d.spaceNote, d.sizeNote, d.companion, d.largeDogOk, d.indoorOk,
        )
        private fun fromDto(d: VetDto) = Vet(d.vetId, d.name, d.lat, d.lng, d.distanceKm, d.address, d.phone, d.sample)

        /** 등장방형 근사 거리(m). */
        fun distanceM(a: LatLng, b: LatLng): Double {
            val lat0 = Math.toRadians((a.lat + b.lat) / 2)
            val dx = Math.toRadians(b.lng - a.lng) * cos(lat0) * 6_371_000.0
            val dy = Math.toRadians(b.lat - a.lat) * 6_371_000.0
            return hypot(dx, dy)
        }

        /** 경로 좌표열에서 진행 비율 [fraction](0~1) 지점. 실차는 NaviHelper 현재 위치를 쓰고, 시뮬레이터는 이걸로 근사한다. */
        fun pointAlong(points: List<LatLng>, fraction: Double): LatLng? {
            if (points.isEmpty()) return null
            if (points.size == 1) return points.first()
            val segs = points.zipWithNext { a, b -> distanceM(a, b) }
            var target = segs.sum() * fraction.coerceIn(0.0, 1.0)
            for ((i, d) in segs.withIndex()) {
                if (target <= d) {
                    val t = if (d > 0) target / d else 0.0
                    val a = points[i]
                    val b = points[i + 1]
                    return LatLng(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
                }
                target -= d
            }
            return points.last()
        }
    }
}
