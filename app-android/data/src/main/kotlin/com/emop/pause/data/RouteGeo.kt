package com.emop.pause.data

import com.emop.pause.core.model.LatLng
import kotlin.math.cos
import kotlin.math.hypot

/**
 * 경로 좌표열 기준 거리 계산. 서버 server/app/geo.py와 같은 알고리즘이라
 * 서버가 꺼져 있을 때 캐시·내장 데이터로도 같은 결과가 나온다.
 */
object RouteGeo {
    private const val EARTH_M = 6_371_000.0

    data class Projection(val alongM: Double, val offsetM: Double, val behind: Boolean)

    fun project(points: List<LatLng>, lat: Double, lng: Double): Projection {
        require(points.size >= 2) { "경로 좌표 2개 이상 필요" }
        var best: Projection? = null
        var along = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val lat0 = (a.lat + b.lat) / 2
            val (ax, ay) = xy(a.lat, a.lng, lat0)
            val (bx, by) = xy(b.lat, b.lng, lat0)
            val (px, py) = xy(lat, lng, lat0)
            val dx = bx - ax
            val dy = by - ay
            val seg = hypot(dx, dy)
            val tRaw = if (seg > 0) ((px - ax) * dx + (py - ay) * dy) / (seg * seg) else 0.0
            val t = tRaw.coerceIn(0.0, 1.0)
            val offset = hypot(px - (ax + t * dx), py - (ay + t * dy))
            if (best == null || offset < best.offsetM) best = Projection(along + t * seg, offset, i == 0 && tRaw < 0)
            along += seg
        }
        return best!!
    }

    /** 경로 좌표열의 길이(m). */
    fun length(points: List<LatLng>): Double = points.zipWithNext { a, b -> distanceM(a, b) }.sum()

    /**
     * 방향 표기(종점 도시 이름)가 이 경로의 진행 방향과 맞는지. 경로 끝이 시작보다 그 종점에 가까우면 그쪽으로 가는 중이다.
     * 종점을 모르면 null(판단 불가). 반대 방향 휴게소 제외에 쓴다 (COMMERCIAL_READINESS B P0).
     */
    fun headingToward(points: List<LatLng>, direction: String?): Boolean? {
        val t = direction?.let { TERMINALS[it.removeSuffix("방향").trim()] } ?: return null
        if (points.size < 2) return null
        return distanceM(points.last(), t) < distanceM(points.first(), t)
    }

    private fun distanceM(a: LatLng, b: LatLng): Double {
        val lat0 = (a.lat + b.lat) / 2
        val (ax, ay) = xy(a.lat, a.lng, lat0)
        val (bx, by) = xy(b.lat, b.lng, lat0)
        return hypot(bx - ax, by - ay)
    }

    /** 고속도로 방향 표기에 쓰이는 종점 도시 (시청 부근 좌표. 방향 판단용이라 정밀도는 필요 없다). */
    val TERMINALS: Map<String, LatLng> = mapOf(
        "서울" to LatLng(37.5665, 126.9780),
        "부산" to LatLng(35.1796, 129.0756),
        "인천" to LatLng(37.4563, 126.7052),
        "대전" to LatLng(36.3504, 127.3845),
        "대구" to LatLng(35.8714, 128.6014),
        "광주" to LatLng(35.1595, 126.8526),
        "목포" to LatLng(34.8118, 126.3922),
        "강릉" to LatLng(37.7519, 128.8761),
        "춘천" to LatLng(37.8813, 127.7298),
        "순천" to LatLng(34.9507, 127.4872),
    )

    private fun xy(lat: Double, lng: Double, lat0: Double): Pair<Double, Double> {
        val k = Math.PI / 180 * EARTH_M
        return lng * k * cos(Math.toRadians(lat0)) to lat * k
    }
}
