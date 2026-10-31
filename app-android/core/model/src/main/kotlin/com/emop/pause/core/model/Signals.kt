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

/**
 * 앞쪽 회전 안내 한 건 — NaviHelper `onTBTInfo(List<TBTInfo>)`, `TBTInfo(type: TurnType, distance: Double, description)` (javap 3.0.4).
 * [distanceM] = 받은 시점 현재 위치에서 이 회전 지점까지 남은 거리(m). NaviHelper가 주는 `distance`는 **앞 안내 지점에서 이 지점까지의
 * 구간 거리**라서(에뮬레이터 확인 2026-10-30: 성남→강릉 안내 32건 합 188.5km ≈ 경로 194km) [fromSegments]로 누적해 만든다.
 */
data class RouteTurn(val kind: TurnKind, val distanceM: Double) {
    companion object {
        /**
         * NaviHelper 안내 목록(종류, 구간 거리 m) → 현재 위치 기준 누적 거리의 회전 목록.
         * 종류가 null(톨게이트·휴게소·졸음쉼터·도착 등 굽이가 아닌 안내)이어도 거리는 누적에 넣고 목록에서만 뺀다.
         */
        fun fromSegments(items: List<Pair<TurnKind?, Double>>): List<RouteTurn> {
            var at = 0.0
            return items.mapNotNull { (kind, seg) ->
                at += seg.coerceAtLeast(0.0)
                kind?.let { RouteTurn(it, at) }
            }
        }
    }
}

/** SDK `TurnType`을 멀미 예보에 필요한 만큼만 줄인 종류 (매핑은 core:navi `PleosNaviSource`). */
enum class TurnKind { LEFT, RIGHT, SHARP_LEFT, SHARP_RIGHT, KEEP_LEFT, KEEP_RIGHT, UTURN, RAMP_LEFT, RAMP_RIGHT, ROUNDABOUT, STRAIGHT }

/**
 * 바로 다음 길 안내 한 건 (주행 화면 "다음 회전" 카드). NaviHelper `onTBTInfo` 목록의 첫 항목 그대로 —
 * [distanceM]은 지금 위치에서 그 지점까지(첫 항목의 구간 거리), [description]은 내비가 준 방면 글("기업지원허브 방면", 없으면 빈 글).
 * 굽이가 아닌 안내(톨게이트·휴게소·도착)도 들어온다. 직접 만들지 않는다 — 실제 안내가 없으면 null.
 */
data class NextGuide(val icon: GuideIcon, val distanceM: Double, val description: String = "")

enum class GuideIcon { LEFT, RIGHT, KEEP_LEFT, KEEP_RIGHT, UTURN, STRAIGHT, ROUNDABOUT, TOLL, REST_AREA, ARRIVE, OTHER }

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
