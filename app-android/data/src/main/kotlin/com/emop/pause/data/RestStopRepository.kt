package com.emop.pause.data

import com.emop.pause.core.model.LatLng
import com.emop.pause.data.net.AlongRouteReq
import com.emop.pause.data.net.PauseApi
import com.emop.pause.data.net.PointDto
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.StopType

interface RestStopCache {
    suspend fun load(): List<StopRecord>
    suspend fun replace(records: List<StopRecord>)
}

enum class StopSource { SERVER, CACHE, BUNDLED }

data class RouteStops(val stops: List<RestStop>, val source: StopSource)

/**
 * 출발 시 경로상 휴게소를 받아 캐시한다 (flowchart §2 "출발 시 1회").
 * 서버 실패 → Room 캐시 → 앱 내장 데이터 순서로 대체한다 (test_plan 4장 [E][P0]).
 */
class RestStopRepository(
    private val api: PauseApi?,
    private val cache: RestStopCache,
    private val bundled: () -> List<StopRecord>,
) {
    /** [routeLengthM]은 내비 총거리 — 경로 거리 척도 맞춤용 ([alongRoute]). */
    suspend fun loadForRoute(points: List<LatLng>, routeLengthM: Double? = null): RouteStops {
        val api = api
        if (api != null && points.size >= 2) {
            val remote = runCatching { api.alongRoute(AlongRouteReq(points.map { PointDto(it.lat, it.lng) }, routeLengthMeters = routeLengthM?.toInt())) }.getOrNull()
            if (remote != null) {
                val records = remote.items.mapNotNull { d ->
                    StopType.fromCode(d.type)?.let {
                        StopRecord(d.restStopId, d.name, it, d.lat, d.lng, d.facilities.mapNotNull(Facility::fromCode).toSet(), d.routeName, d.direction)
                    }
                }
                runCatching { cache.replace(records) }
                return RouteStops(records.alongRoute(points, routeLengthM), StopSource.SERVER)
            }
        }
        val cached = runCatching { cache.load() }.getOrDefault(emptyList()).alongRoute(points, routeLengthM)
        if (cached.isNotEmpty()) return RouteStops(cached, StopSource.CACHE)
        return RouteStops(bundled().alongRoute(points, routeLengthM), StopSource.BUNDLED)
    }
}
