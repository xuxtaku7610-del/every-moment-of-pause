package com.emop.pause.data

import com.emop.pause.core.model.LatLng
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.StopType

/** 경로와 무관한 휴게소 원본(캐시·내장 데이터 공통). 경로가 정해지면 [alongRoute]로 경로 거리를 붙인다. */
data class StopRecord(
    val id: String,
    val name: String,
    val type: StopType,
    val lat: Double,
    val lng: Double,
    val facilities: Set<Facility>,
    /** 노선 이름 ("경부선"). */
    val routeName: String? = null,
    /** 진행 방향 종점 이름 ("부산" = 부산방향). 도로공사 응답의 방향 표기. */
    val direction: String? = null,
) {
    /** 화면 부제 "경부선 부산방향". */
    val routeLabel: String? get() = listOfNotNull(routeName, direction?.let { "${it}방향" }).joinToString(" ").ifBlank { null }
}

/**
 * 경로 앞쪽, 반경 안, 진행 방향이 맞는 휴게소만 경로 거리 순으로 (api_spec §2.3과 같은 규칙, COMMERCIAL_READINESS B P0).
 *
 * 경로 좌표는 띄엄띄엄한 점이라 직선 거리 합이 실제 도로 거리보다 짧다. [routeLengthM](내비 총거리)을 주면
 * 투영 거리를 그 비율로 늘려 여행 진행 거리(총거리 - 남은 거리)와 같은 척도로 맞춘다.
 * 방향 종점을 알 수 없는 휴게소는 남긴다(판단 불가 → 제외하지 않음).
 */
fun List<StopRecord>.alongRoute(points: List<LatLng>, routeLengthM: Double? = null, corridorM: Double = 1500.0): List<RestStop> {
    if (points.size < 2) return emptyList()
    val geo = RouteGeo.length(points)
    val scale = if (routeLengthM != null && routeLengthM > 0 && geo > 0) routeLengthM / geo else 1.0
    return mapNotNull { r ->
        val p = RouteGeo.project(points, r.lat, r.lng)
        when {
            p.offsetM > corridorM || p.behind -> null
            RouteGeo.headingToward(points, r.direction) == false -> null
            else -> RestStop(r.id, r.name, r.type, p.alongM * scale / 1000, r.facilities, r.lat, r.lng, r.routeLabel)
        }
    }.sortedBy { it.distanceAlongRouteKm }
}
