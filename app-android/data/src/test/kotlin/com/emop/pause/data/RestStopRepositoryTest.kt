package com.emop.pause.data

import com.emop.pause.core.model.LatLng
import com.emop.pause.data.net.ApiClient
import com.emop.pause.data.net.TokenStore
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.StopType
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class RestStopRepositoryTest {
    // scenarios/seoul-busan-family.json 경로
    private val route = listOf(
        LatLng(37.4602, 127.0417), LatLng(37.2411, 127.1776), LatLng(36.8065, 127.1522), LatLng(36.3504, 127.3845),
        LatLng(36.1398, 128.1136), LatLng(35.8714, 128.6014), LatLng(35.1796, 129.0756),
    )
    private val bundled = BundledRestStops.parse(File("../../server/app/data/sample_reststops.json").readText())

    private class MemoryCache(var records: List<StopRecord> = emptyList()) : RestStopCache {
        override suspend fun load() = records
        override suspend fun replace(records: List<StopRecord>) { this.records = records }
    }

    private class Tokens(override var token: String? = "t0") : TokenStore

    private lateinit var server: MockWebServer

    @BeforeEach fun start() { server = MockWebServer().also { it.start() } }
    @AfterEach fun stop() = server.shutdown()

    private fun api() = ApiClient.create(server.url("/api/v1/").toString(), Tokens(), timeoutMs = 1_000)

    private val routeM = 395_000.0

    @Test
    fun `T4-1 내장 데이터는 경로 앞쪽·반경 안·진행 방향만 거리순`() {
        val stops = bundled.alongRoute(route, routeM)
        assertEquals(19, stops.size)
        assertEquals("서울만남의광장휴게소", stops.first().name)
        assertEquals("통도사휴게소", stops.last().name)
        assertEquals(setOf(Facility.NURSING_ROOM, Facility.KIDS_ZONE, Facility.EV_FAST, Facility.EV_SLOW), stops.first().facilities)
        assertEquals(stops.map { it.distanceAlongRouteKm }.sorted(), stops.map { it.distanceAlongRouteKm })
        assertEquals("경부선 부산방향", stops.first().routeLabel)
    }

    @Test
    fun `T4-1 반대 방향(서울방향) 휴게소는 빼고, 거꾸로 가면 서울방향만`() {
        val south = bundled.alongRoute(route, routeM).map { it.name }
        assertFalse("죽전휴게소" in south)
        assertFalse("천안호두휴게소" in south)
        val north = bundled.alongRoute(route.reversed(), routeM).map { it.name }
        assertEquals(listOf("신탄진휴게소", "천안호두휴게소", "입장거봉포도휴게소", "죽전휴게소"), north)
    }

    @Test
    fun `경로 거리는 내비 총거리 척도로 맞춘다 - 죽암휴게소는 서울에서 약 150km`() {
        val juk = bundled.alongRoute(route, routeM).first { it.name == "죽암휴게소" }
        assertEquals(150.0, juk.distanceAlongRouteKm, 1.0)
        // 총거리를 모르면 직선 거리(약 341km 척도) 그대로
        val raw = bundled.alongRoute(route).first { it.name == "죽암휴게소" }
        assertTrue(raw.distanceAlongRouteKm < 135)
    }

    @Test
    fun `지나온 구간의 휴게소는 빠진다`() {
        val fromDaejeon = bundled.alongRoute(route.drop(3))
        assertFalse(fromDaejeon.any { it.name == "안성휴게소" })
        assertTrue(fromDaejeon.first().distanceAlongRouteKm < 20)
    }

    @Test
    fun `방향 종점 판단`() {
        assertEquals(true, RouteGeo.headingToward(route, "부산"))
        assertEquals(false, RouteGeo.headingToward(route, "서울방향"))
        assertEquals(null, RouteGeo.headingToward(route, "어딘가"))
        assertEquals(null, RouteGeo.headingToward(route, null))
    }

    @Test
    fun `T4-1 출발 시 서버 결과를 받아 캐시에 저장하고 토큰 헤더를 붙인다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"items":[{"restStopId":"rs_g04","name":"안성휴게소","type":"service_area","routeName":"경부선","direction":"부산","lat":37.0,"lng":127.17,"distanceAlongRouteKm":13.1,"facilities":["kids_zone","ev_fast","new_code"],"evChargers":{"fast":4,"slow":0}}],"count":1}"""))
        val cache = MemoryCache()
        val r = RestStopRepository(api(), cache) { bundled }.loadForRoute(route)
        assertEquals(StopSource.SERVER, r.source)
        assertEquals(listOf("rs_g04"), r.stops.map { it.id })
        assertEquals("경부선 부산방향", r.stops.single().routeLabel)
        assertEquals(setOf(Facility.KIDS_ZONE, Facility.EV_FAST), r.stops.single().facilities)
        assertEquals(1, cache.records.size)
        val req = server.takeRequest()
        assertEquals("/api/v1/reststops/along-route", req.path)
        assertEquals("t0", req.getHeader("X-Device-Token"))
        assertTrue(req.body.readUtf8().contains("\"points\""))
    }

    @Test
    fun `T4-7 서버가 꺼져 있으면 캐시로 추천`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        val cached = listOf(StopRecord("rs_x", "캐시휴게소", StopType.SERVICE_AREA, 37.30, 127.13, emptySet()))
        val r = RestStopRepository(api(), MemoryCache(cached)) { bundled }.loadForRoute(route)
        assertEquals(StopSource.CACHE, r.source)
        assertEquals("캐시휴게소", r.stops.single().name)
    }

    @Test
    fun `T4-7 서버도 캐시도 없으면 내장 기본 데이터`() = runTest {
        server.shutdown()
        val r = RestStopRepository(api(), MemoryCache()) { bundled }.loadForRoute(route, routeM)
        assertEquals(StopSource.BUNDLED, r.source)
        assertEquals(19, r.stops.size)
    }

    @Test
    fun `서버 주소가 없으면(오프라인 빌드) 바로 캐시·내장 데이터`() = runTest {
        val r = RestStopRepository(null, MemoryCache()) { bundled }.loadForRoute(route)
        assertEquals(StopSource.BUNDLED, r.source)
    }

    @Test
    fun `G P1 내 데이터 삭제 - 서버 기기 삭제 후 토큰을 버리고, 서버에 못 닿으면 토큰을 남긴다`() = runTest {
        val tokens = Tokens("t0")
        val session = com.emop.pause.data.net.ServerSession(ApiClient.create(server.url("/api/v1/").toString(), tokens, timeoutMs = 1_000), tokens, "0.1.0")
        server.enqueue(MockResponse().setResponseCode(204))
        assertTrue(session.forget())
        assertEquals(null, tokens.token)
        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/api/v1/devices/me", req.path)
        assertEquals("t0", req.getHeader("X-Device-Token"))

        val t2 = Tokens("t1")
        server.shutdown()
        val offline = com.emop.pause.data.net.ServerSession(ApiClient.create(server.url("/api/v1/").toString(), t2, timeoutMs = 500), t2, "0.1.0")
        assertFalse(offline.forget())
        assertEquals("t1", t2.token)
    }
}
