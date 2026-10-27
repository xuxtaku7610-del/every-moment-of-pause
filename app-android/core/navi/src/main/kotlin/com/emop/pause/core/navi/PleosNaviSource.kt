package com.emop.pause.core.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.RouteInfo
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
        } catch (e: Exception) {
            Log.w(TAG, "NaviHelper 조회 실패: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    override fun route(): Flow<RouteInfo?> = routeState
    override fun eta(): Flow<EtaInfo?> = etaState

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
