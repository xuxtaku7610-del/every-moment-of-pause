package com.emop.pause.core.navi

/**
 * 경로·ETA·경유지 공급원 (api_spec §3).
 *
 * TODO(pleos-sdk): M1에서 NaviHelper 3.0.5 aar로 실제 시그니처(인자 타입, 위도·경도 순서,
 * remainDistance/remainTime)를 확인한 뒤 route()/eta()와 PleosNaviSource를 추가한다.
 */
interface NaviSource {
    suspend fun addWaypoint(lat: Double, lng: Double, name: String): Result<Unit>
}
