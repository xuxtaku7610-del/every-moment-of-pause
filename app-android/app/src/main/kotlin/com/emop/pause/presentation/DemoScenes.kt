package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.PauseCause

/**
 * 발표 모드 점프 장면 (COMMERCIAL_READINESS I P0). seoul-busan-family 기준 시각(초).
 * 시나리오를 고치면 `DemoScenesTest`가 각 장면이 기대한 AppMode에 떨어지는지 확인한다.
 * [say]가 있으면 점프한 뒤 그 말을 넣는다("아직 멀었어?" 장면).
 */
data class DemoJump(
    val label: String,
    val tSec: Int,
    val nap: Boolean = false,
    val expectMode: AppMode,
    val expectCause: PauseCause? = null,
    val say: String? = null,
)

object DemoScenes {
    const val SCENARIO = "seoul-busan-family"

    val JUMPS = listOf(
        DemoJump("출발 준비", 0, expectMode = AppMode.PREPARE),
        DemoJump("아직 멀었어?", 2_400, expectMode = AppMode.DRIVING, say = "아직 멀었어?"),
        DemoJump("피로 상승 제안", 4_440, expectMode = AppMode.DRIVING),
        DemoJump("충전 정차", 5_760, expectMode = AppMode.PAUSE, expectCause = PauseCause.CHARGING),
        DemoJump("파워냅", 5_790, nap = true, expectMode = AppMode.PAUSE, expectCause = PauseCause.CHARGING),
        DemoJump("다시 출발", 7_500, expectMode = AppMode.DRIVING),
        DemoJump("도착", 16_520, expectMode = AppMode.ARRIVED),
    )
}
