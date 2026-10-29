package com.emop.pause.data.net

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

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
@Serializable data class PhraseRes(val text: String, val source: String)
