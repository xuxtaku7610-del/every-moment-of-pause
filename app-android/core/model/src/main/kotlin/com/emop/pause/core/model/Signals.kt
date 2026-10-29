package com.emop.pause.core.model

/** 앱이 읽는 차량 신호. 차종마다 지원 여부가 다르므로 값은 모두 nullable (architecture §3.1 신호 수급 전략). */
enum class Signal { SPEED, GEAR, PARKING_BRAKE, BATTERY_SOC, CHARGING, HVAC_TEMP, HVAC_AC, HVAC_RECIRCULATION, HEADING }

/**
 * 신호를 어디서 받았는지 (COMMERCIAL_READINESS A P0). 발표 모드·설정 "이 차에서 쓰는 신호"에 표시한다.
 * 우선순위: Pleos Vehicle SDK → AAOS CarPropertyManager → (시연 보충을 켰을 때만) 시뮬레이터 → 없음.
 */
enum class SignalSource { PLEOS_SDK, AAOS_PROPERTY, SIM, NONE }

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/** 1초 단위 차량 신호 묶음. 지원하지 않거나 아직 값이 없는 항목은 null. */
data class SignalSample(
    val timestampMs: Long,
    val speedKph: Float? = null,
    val gear: Gear? = null,
    val parkingBrakeOn: Boolean? = null,
    val batterySocPercent: Int? = null,
    val charging: Boolean? = null,
    val hvacTempC: Float? = null,
    val hvacAcOn: Boolean? = null,
    val recirculationOn: Boolean? = null,
    /** 진행 방위(도, 0=북). NaviHelper `CurrentLocationInfo.heading` — 멀미 예보의 횡가속 (같이가개 F1). */
    val headingDeg: Double? = null,
)

data class LatLng(val lat: Double, val lng: Double)

/**
 * 경로 정보. NaviHelper는 경로 좌표열을 주지 않으므로 points는 현재 위치·목적지 등 알 수 있는 점만 담는다.
 * [shape]는 멀미 예보용 경로 형상(시연 GPX·시뮬레이터). 경로 진행 거리 [shapeStartM]부터 적용한다 (docs/plan/sdk_check.md).
 */
data class RouteInfo(
    val destinationName: String,
    val destination: LatLng,
    val totalDistanceM: Int,
    val points: List<LatLng> = emptyList(),
    val shape: List<LatLng> = emptyList(),
    val shapeStartM: Int = 0,
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
