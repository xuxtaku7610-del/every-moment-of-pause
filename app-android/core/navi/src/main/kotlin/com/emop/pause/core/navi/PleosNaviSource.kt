package com.emop.pause.core.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.RouteInfo
import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.TurnKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * NaviHelper 3.0.5 어댑터. 시그니처는 aar(classes.jar, navi-data 3.0.4)를 javap로 확인한 것만 쓴다 (M1, 2026-10-28).
 *
 * - 남은 거리·시간: DestinationInfo/WaypointInfo의 remainDistance(m)·remainTime(초). getDestinationInfo()/
 *   getWaypointInfo() 요청 → onDestinationInfo/onWayPointInfo 콜백으로 받는다.
 * - 경유지 추가: addWaypoint(RequestWaypointInfo(위도, 경도, 이름, WaypointIndex, ...)) → onWaypointChanged / onError.
 * - 경로 좌표열(폴리라인)을 주는 API는 없다. RouteInfo.points에는 현재 위치와 목적지만 담는다.
 */
class PleosNaviSource(context: Context) : NaviSource {
    private val navi = NaviHelper(context)
    private val routeState = MutableStateFlow<RouteInfo?>(null)
    private val etaState = MutableStateFlow<EtaInfo?>(null)
    private val headingState = MutableStateFlow<Double?>(null)
    private val turnsState = MutableStateFlow<List<RouteTurn>>(emptyList())
    private var currentLocation: LatLng? = null
    private var routeDistanceM: Int = 0
    private var pendingWaypoint: CompletableDeferred<Result<Unit>>? = null

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            routeDistanceM = routeStartInfo.distance
            refresh()
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onRouteEnded() {
            routeState.value = null
            etaState.value = null
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onRouteCancelled() = onRouteEnded()

        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            currentLocation = LatLng(currentLocationInfo.latitude, currentLocationInfo.longitude)
            // 방위: CurrentLocationInfo.getHeading(): Double (javap, docs/plan/sdk_check.md). 멀미 예보의 횡가속에 쓴다.
            headingState.value = currentLocationInfo.heading
        }

        // 회전 안내: onTBTInfo(List<TBTInfo>), TBTInfo.getType(): TurnType · getDistance(): Double (javap navi-data 3.0.4).
        // 멀미 예보의 앞 구간 형상 추정에만 쓴다 (TbtRouteShape). 위치는 서버로 보내지 않는다.
        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            // distance는 앞 안내 지점부터의 구간 거리 → 누적해서 현재 위치 기준으로 (에뮬레이터 확인 10/30)
            turnsState.value = RouteTurn.fromSegments(tbtInfoList.map { t -> turnKind(t.type) to t.distance })
            // 실차·에뮬레이터 확인용: 건수와 앞 3개의 종류·거리만 (위치는 남기지 않는다)
            Log.i(TAG, "onTBTInfo n=${tbtInfoList.size} " + tbtInfoList.take(3).joinToString { "${it.type}@${it.distance.toInt()}m" })
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            val dest = LatLng(destinationInfo.latitude, destinationInfo.longitude)
            routeState.value = RouteInfo(
                destinationName = destinationInfo.name,
                destination = dest,
                totalDistanceM = maxOf(routeDistanceM, destinationInfo.remainDistance),
                points = listOfNotNull(currentLocation, dest),
            )
            etaState.value = (etaState.value ?: EtaInfo(0, 0)).copy(
                remainDistanceM = destinationInfo.remainDistance,
                remainTimeSec = destinationInfo.remainTime,
            )
        }

        override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
            val next = waypointInfoList.firstOrNull()
            etaState.value = etaState.value?.copy(
                nextWaypointName = next?.name,
                nextWaypointRemainDistanceM = next?.remainDistance,
                nextWaypointRemainTimeSec = next?.remainTime,
            )
        }

        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) {
            pendingWaypoint?.complete(Result.success(Unit))
            refresh()
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "NaviHelper 오류: $errorCode")
            pendingWaypoint?.complete(Result.failure(IllegalStateException("NaviHelper $errorCode")))
        }
    }

    fun start() {
        navi.initialize(
            {
                navi.addListener(listener)
                refresh()
            },
            { e -> Log.w(TAG, "NaviHelper 초기화 실패: ${e.javaClass.simpleName} ${e.message}") },
        )
    }

    fun stop() {
        navi.removeListener(listener)
        navi.release()
    }

    /** 남은 거리·시간은 요청-응답이므로 주기적으로 호출한다. */
    fun refresh() {
        // SDK는 권한·연결 문제를 콜백 대신 예외로 던지므로 여기서 막는다.
        try {
            navi.getCurrentLocationInfo()
            navi.getDestinationInfo()
            navi.getWaypointInfo()
            navi.getTBTInfo()
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper 조회 실패: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    /** 방위용 현재 위치만 요청 (1초). */
    fun refreshLocation() {
        try {
            navi.getCurrentLocationInfo()
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper 위치 조회 실패: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    override fun route(): Flow<RouteInfo?> = routeState
    override fun eta(): Flow<EtaInfo?> = etaState
    override fun heading(): Flow<Double?> = headingState
    override fun turns(): Flow<List<RouteTurn>> = turnsState

    override suspend fun addWaypoint(lat: Double, lng: Double, name: String): Result<Unit> {
        val done = CompletableDeferred<Result<Unit>>()
        pendingWaypoint = done
        // TODO(pleos-sdk): poiId·address·poiSubId·poiProvider가 빈 문자열이어도 되는지 에뮬레이터에서 확인.
        try {
            navi.addWaypoint(RequestWaypointInfo(lat, lng, name, WaypointIndex.FIRST, "", "", "", ""))
        } catch (e: Exception) {
            pendingWaypoint = null
            return Result.failure(e)
        }
        return withTimeoutOrNull(WAYPOINT_TIMEOUT_MS) { done.await() }
            ?: Result.failure(IllegalStateException("경유지 추가 응답 없음"))
    }

    private companion object {
        const val TAG = "PleosNavi"
        const val WAYPOINT_TIMEOUT_MS = 5_000L
    }
}

/** SDK 회전 종류 → 멀미 예보용 종류. 곡률과 상관없는 안내(톨게이트·터널·휴게소·도착·알 수 없음)는 null. */
internal fun turnKind(t: TurnType): TurnKind? = when (t) {
    TurnType.TURN_LEFT -> TurnKind.LEFT
    TurnType.TURN_RIGHT -> TurnKind.RIGHT
    TurnType.TURN_SHARP_L -> TurnKind.SHARP_LEFT
    TurnType.TURN_SHARP_R -> TurnKind.SHARP_RIGHT
    TurnType.TURN_KEEP_L -> TurnKind.KEEP_LEFT
    TurnType.TURN_KEEP_R -> TurnKind.KEEP_RIGHT
    TurnType.TURN_UTURN -> TurnKind.UTURN
    TurnType.TURN_ENTER_IC_L, TurnType.TURN_EXIT_IC_L -> TurnKind.RAMP_LEFT
    TurnType.TURN_ENTER_IC_R, TurnType.TURN_EXIT_IC_R -> TurnKind.RAMP_RIGHT
    TurnType.TURN_ENTER_ROUNDABOUT_02, TurnType.TURN_ENTER_ROUNDABOUT_03, TurnType.TURN_ENTER_ROUNDABOUT_04,
    TurnType.TURN_ENTER_ROUNDABOUT_06, TurnType.TURN_ENTER_ROUNDABOUT_08, TurnType.TURN_ENTER_ROUNDABOUT_09,
    TurnType.TURN_ENTER_ROUNDABOUT_10, TurnType.TURN_ENTER_ROUNDABOUT_12 -> TurnKind.ROUNDABOUT
    TurnType.TURN_STRAIGHT, TurnType.TURN_ENTER_IC_C, TurnType.TURN_EXIT_IC_C,
    TurnType.TURN_ENTER_UNDER_R, TurnType.TURN_ENTER_UNDER_L, TurnType.TURN_ENTER_UNDER_C,
    TurnType.TURN_AVOID_UNDER_R, TurnType.TURN_AVOID_UNDER_L, TurnType.TURN_AVOID_UNDER_C,
    TurnType.TURN_ENTER_OVER_R, TurnType.TURN_ENTER_OVER_L, TurnType.TURN_ENTER_OVER_C,
    TurnType.TURN_AVOID_OVER_R, TurnType.TURN_AVOID_OVER_L, TurnType.TURN_AVOID_OVER_C,
    TurnType.TURN_EXIT_ROUNDABOUT, TurnType.TURN_WAY, TurnType.TURN_DEVIDE_MERGE -> TurnKind.STRAIGHT
    // TURN_TOLL·TURN_ENTER_SERVICEAREA·TURN_SERVICE_AREA·TURN_RESTAREA·TURN_REST_AREA·TURN_ARRIVE 등(에뮬레이터에서 실제로 옴)은
    // 굽이가 아니라 null — 거리는 RouteTurn.fromSegments에서 누적에만 쓴다.
    else -> null
}
