package com.emop.pause.trip

import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.RouteInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** COMMERCIAL_READINESS B P0: 경로 이탈·목적지 변경이면 휴게소 후보를 다시 받는다. */
class RouteKeyTest {
    private val pts = listOf(LatLng(37.46, 127.04), LatLng(35.18, 129.08))
    private val r = RouteInfo("부산", LatLng(35.18, 129.08), 395_000, pts)

    @Test
    fun `남은 거리만 바뀌면 같은 경로, 좌표나 목적지가 바뀌면 다른 경로`() {
        assertEquals(routeKey(r), routeKey(r.copy(totalDistanceM = 390_000)))
        assertNotEquals(routeKey(r), routeKey(r.copy(points = pts + LatLng(35.0, 129.0))))
        assertNotEquals(routeKey(r), routeKey(r.copy(destinationName = "대구")))
        assertNull(routeKey(r.copy(points = pts.take(1))))
        assertNull(routeKey(null))
    }

    @Test
    fun `T6-18 이어서 안내 키는 목적지만 - 현재 위치가 바뀌어도 같다`() {
        assertEquals(resumeKey(r), resumeKey(r.copy(points = listOf(LatLng(36.0, 127.5), LatLng(35.18, 129.08)))))
        assertNotEquals(resumeKey(r), resumeKey(r.copy(destinationName = "대구")))
        assertNull(resumeKey(null))
    }
}
