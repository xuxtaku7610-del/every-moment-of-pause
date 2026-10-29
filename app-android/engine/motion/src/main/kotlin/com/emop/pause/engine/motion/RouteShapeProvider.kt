package com.emop.pause.engine.motion

/**
 * 앞 구간 곡률 소스 (F1 예보, docs/plan/sdk_check.md "형상 소스 우선순위").
 * - 시연: [RouteShape] — 경로가 정해진 시연 GPX(대관령 옛길, OSM).
 * - 실차: [TbtRouteShape] — NaviHelper 회전 안내 목록(TBT) + 진행 방위로 추정. NaviHelper는 폴리라인을 주지 않는다.
 * 위치는 모두 "경로 진행 거리(m)"로 쓴다.
 */
interface RouteShapeProvider {
    /** 경로 진행 거리 [atM]의 부호 있는 곡률(1/m, 왼쪽 굽음 +). 모르는 곳은 0. */
    fun curvatureAt(atM: Double): Double

    /** 형상을 아는 마지막 경로 거리(m). 예보는 이 앞까지만 본다. */
    val lengthM: Double
}
