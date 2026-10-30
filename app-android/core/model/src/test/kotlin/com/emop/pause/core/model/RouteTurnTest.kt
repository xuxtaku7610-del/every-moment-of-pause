package com.emop.pause.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** test_plan F1-14: NaviHelper TBT `distance`는 구간 거리 → 현재 위치 기준 누적 (에뮬레이터 확인 2026-10-30). */
class RouteTurnTest {
    @Test
    fun `F1-14 구간 거리를 누적하고, 굽이가 아닌 안내(톨게이트·휴게소)도 거리에는 넣는다`() {
        // 에뮬레이터 실제 앞부분: 좌 80, 좌 390, 차로 유지 우 1000, … 톨게이트 610(종류 없음), 휴게소 9700(종류 없음), 우 70
        val got = RouteTurn.fromSegments(
            listOf(TurnKind.LEFT to 80.0, TurnKind.LEFT to 390.0, TurnKind.KEEP_RIGHT to 1000.0, null to 610.0, null to 9700.0, TurnKind.RIGHT to 70.0),
        )
        assertEquals(listOf(80.0, 470.0, 1470.0, 11850.0), got.map { it.distanceM })
        assertEquals(listOf(TurnKind.LEFT, TurnKind.LEFT, TurnKind.KEEP_RIGHT, TurnKind.RIGHT), got.map { it.kind })
        assertEquals(emptyList<RouteTurn>(), RouteTurn.fromSegments(emptyList()))
        assertEquals(listOf(5.0), RouteTurn.fromSegments(listOf(TurnKind.LEFT to -3.0, TurnKind.RIGHT to 5.0)).drop(1).map { it.distanceM })
    }
}
