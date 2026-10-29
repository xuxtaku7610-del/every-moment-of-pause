package com.emop.pause.core.navi

import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.RouteInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 경로·ETA·경유지 공급원 (api_spec §3). */
interface NaviSource {
    fun route(): Flow<RouteInfo?>
    fun eta(): Flow<EtaInfo?>

    /** 진행 방위(도). 모르면 null (시뮬레이터는 경로 형상으로 대신한다). */
    fun heading(): Flow<Double?> = flowOf(null)

    /** 위도, 경도 순서 (NaviHelper 3.0.5 `RequestWaypointInfo`와 같은 순서). */
    suspend fun addWaypoint(lat: Double, lng: Double, name: String): Result<Unit>
}
