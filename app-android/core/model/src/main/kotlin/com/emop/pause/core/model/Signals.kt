package com.emop.pause.core.model

/** 앱이 읽는 차량 신호. 차종마다 지원 여부가 다르므로 값은 모두 nullable (architecture §3.1 신호 수급 전략). */
enum class Signal { SPEED, GEAR, BATTERY_SOC, CHARGING, HVAC_TEMP, HVAC_AC, HVAC_RECIRCULATION }

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/** 1초 단위 차량 신호 묶음. 지원하지 않거나 아직 값이 없는 항목은 null. */
data class SignalSample(
    val timestampMs: Long,
    val speedKph: Float? = null,
    val gear: Gear? = null,
    val batterySocPercent: Int? = null,
    val charging: Boolean? = null,
    val hvacTempC: Float? = null,
    val hvacAcOn: Boolean? = null,
    val recirculationOn: Boolean? = null,
)

data class LatLng(val lat: Double, val lng: Double)

/** 경로 정보. NaviHelper는 경로 좌표열을 주지 않으므로 points는 현재 위치·목적지 등 알 수 있는 점만 담는다. */
data class RouteInfo(
    val destinationName: String,
    val destination: LatLng,
    val totalDistanceM: Int,
    val points: List<LatLng> = emptyList(),
)

/** 목적지·다음 경유지까지 남은 거리(m)와 시간(초). NaviHelper `remainDistance/remainTime`. */
data class EtaInfo(
    val remainDistanceM: Int,
    val remainTimeSec: Int,
    val nextWaypointName: String? = null,
    val nextWaypointRemainDistanceM: Int? = null,
    val nextWaypointRemainTimeSec: Int? = null,
)

enum class TripPhase { NOT_STARTED, ACTIVE, ENDED }

enum class PauseCause { REST, CHARGING }
