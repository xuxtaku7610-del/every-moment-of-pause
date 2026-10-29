package com.emop.pause.sim

import com.emop.pause.core.model.LatLng

/**
 * 아주 작은 GPX 읽기: `trkpt`·`rtept`·`wpt`의 lat/lon 속성만 순서대로 읽는다 (시연 경로 형상용).
 * 시연 GPX는 공개 도로 형상을 단순화해 직접 만든 좌표다 (scenarios/geo/README).
 */
object Gpx {
    private val point = Regex("""<(?:trkpt|rtept)\s+([^>]*?)/?>""")
    private val lat = Regex("""lat\s*=\s*"([-0-9.]+)"""")
    private val lon = Regex("""lon\s*=\s*"([-0-9.]+)"""")

    fun parse(text: String): List<LatLng> = point.findAll(text).mapNotNull { m ->
        val attrs = m.groupValues[1]
        val la = lat.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull() ?: return@mapNotNull null
        val lo = lon.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull() ?: return@mapNotNull null
        LatLng(la, lo)
    }.toList()
}
