package com.emop.pause.data.net

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 서버 API (api_spec §2). 인증 헤더는 [ApiClient]의 인터셉터가 붙인다. */
interface PauseApi {
    @POST("devices")
    suspend fun register(@Body body: DeviceReq): DeviceRes

    @POST("reststops/along-route")
    suspend fun alongRoute(@Body body: AlongRouteReq): AlongRouteRes

    @POST("llm/phrases")
    suspend fun phrase(@Body body: PhraseReq): PhraseRes

    /** 내 데이터 삭제 (api_spec §2.14). */
    @DELETE("devices/me")
    suspend fun deleteMe()

    /** 이동 기록 요약 문장 (api_spec §2.9 trip_summary, F7). */
    @POST("llm/phrases")
    suspend fun summary(@Body body: SummaryReq): PhraseRes

    /** 경로 버퍼 안 반려동물 동반 장소 (api_spec §2.5, F4). */
    @POST("places/along-route")
    suspend fun placesAlongRoute(@Body body: PlacesReq): PlacesRes

    /** 지점 주변 동반 장소 (api_spec §2.6, F4 도착 10분 전). */
    @GET("places/near")
    suspend fun placesNear(@Query("lat") lat: Double, @Query("lng") lng: Double, @Query("radiusMeters") radiusMeters: Int = 3000, @Query("categories") categories: String = "walk,cafe"): PlacesRes

    /** 가까운 동물병원 (api_spec §2.7, F5). */
    @GET("vets/nearby")
    suspend fun vetsNearby(@Query("lat") lat: Double, @Query("lng") lng: Double, @Query("limit") limit: Int = 5): VetsRes

    /** 대기 공유 (api_spec §2.8, F3). 위치는 보내지 않는다. */
    @POST("wait")
    suspend fun waitStart(@Body body: WaitStartReq): WaitStartRes

    @PUT("wait/{token}")
    suspend fun waitUpdate(@Path("token") token: String, @Body body: WaitUpdateReq)

    @DELETE("wait/{token}")
    suspend fun waitEnd(@Path("token") token: String)
}

@Serializable data class DeviceReq(val platform: String, val appVersion: String)
@Serializable data class DeviceRes(val deviceId: String, val token: String)

@Serializable data class PointDto(val lat: Double, val lng: Double)
@Serializable data class AlongRouteReq(val points: List<PointDto>, val corridorMeters: Int = 1500, val routeLengthMeters: Int? = null)

@Serializable
data class RestStopDto(
    val restStopId: String,
    val name: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val routeName: String? = null,
    val direction: String? = null,
    val distanceAlongRouteKm: Double = 0.0,
    val facilities: List<String> = emptyList(),
)

@Serializable data class AlongRouteRes(val items: List<RestStopDto>, val count: Int)

@Serializable
data class PhraseFacts(
    val restStopName: String,
    val minutesAway: Int,
    val reasons: List<String>,
    val fatigueLevel: String,
    val passengers: List<String>,
)

@Serializable data class PhraseReq(val kind: String, val facts: PhraseFacts, val maxSentences: Int = 2)

/** 이동 기록 요약 사실. 반려견 이름·증상 메모는 보내지 않는다 (architecture §6). */
@Serializable data class SummaryFacts(val durationMin: Int, val stops: Int, val motionPeak: Int, val limitMin: Int? = null)
@Serializable data class SummaryReq(val facts: SummaryFacts, val kind: String = "trip_summary", val maxSentences: Int = 2)

@Serializable
data class PlacesReq(val points: List<PointDto>, val bufferMeters: Int = 5000, val routeLengthMeters: Int? = null, val limit: Int = 30)

@Serializable
data class PlaceDto(
    val placeId: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val petPolicy: String? = null,
    val sample: Boolean = false,
    val distanceAlongRouteKm: Double? = null,
    val offRouteKm: Double? = null,
    val distanceKm: Double? = null,
)

@Serializable data class PlacesRes(val items: List<PlaceDto>, val count: Int)

@Serializable
data class VetDto(
    val vetId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
    val address: String? = null,
    val phone: String? = null,
    val sample: Boolean = false,
)

@Serializable data class VetsRes(val items: List<VetDto>, val count: Int)

@Serializable data class WaitStartReq(val limitMin: Int)
@Serializable data class WaitStartRes(val token: String, val pageUrl: String, val expiresAt: String)

/** 대기 상태. 위치 필드는 없다 — 서버도 정의되지 않은 필드를 422로 거절한다 (test_plan F3-08). */
@Serializable
data class WaitUpdateReq(val state: String, val elapsedSec: Int, val limitMin: Int, val batteryPercent: Int? = null, val cabinTempC: Float? = null)
@Serializable data class PhraseRes(val text: String, val source: String)
