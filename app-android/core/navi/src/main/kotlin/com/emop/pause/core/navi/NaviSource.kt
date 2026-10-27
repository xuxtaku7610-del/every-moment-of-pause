package com.emop.pause.core.navi

import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.RouteInfo
import kotlinx.coroutines.flow.Flow

/** 경로·ETA·경유지 공급원 (api_spec §3). */
interface NaviSource {
    fun route(): Flow<RouteInfo?>
    fun eta(): Flow<EtaInfo?>

    /** 위도, 경도 순서 (NaviHelper 3.0.5 `RequestWaypointInfo`와 같은 순서). */
    suspend fun addWaypoint(lat: Double, lng: Double, name: String): Result<Unit>
}
