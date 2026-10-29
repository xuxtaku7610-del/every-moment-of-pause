package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.PauseCause

/**
 * 발표 모드 점프 장면 (CONCEPT 7장 시연 흐름, test_plan DM-01). pet-trip 기준 시각(초) — scripts/gen_demo_data.py가 만든 시나리오.
 * 시나리오를 다시 만들면 `DemoScenesTest`가 각 장면이 기대한 AppMode에 떨어지는지 확인한다.
 * [say]가 있으면 점프한 뒤 그 말을 넣는다(대기 모드 "잠깐 다녀올게").
 */
data class DemoJump(
    val label: String,
    val tSec: Int,
    val expectMode: AppMode,
    val expectCause: PauseCause? = null,
    val say: String? = null,
)

object DemoScenes {
    const val SCENARIO = "pet-trip"

    val JUMPS = listOf(
        DemoJump("출발 체크", 0, expectMode = AppMode.PREPARE),
        DemoJump("고속도로", 210, expectMode = AppMode.DRIVING),
        // 약 3,460초에 반려견 맞춤 쉼표(횡성휴게소, 놀이터·운전 피로)를 제안한다 — PetTripScenarioTest가 확인
        DemoJump("쉼표 추천", 3_420, expectMode = AppMode.DRIVING),
        DemoJump("휴게소 쉼표", 5_240, expectMode = AppMode.PAUSE, expectCause = PauseCause.REST),
        DemoJump("잠깐 대기", 5_540, expectMode = AppMode.PAUSE, expectCause = PauseCause.REST, say = "잠깐 다녀올게"),
        // 고갯길 3km 앞: 예보 "3km 뒤 굽은 길" (5km 안·300m 밖 구간만 말한다)
        DemoJump("고갯길 예보", 8_748, expectMode = AppMode.DRIVING),
        // 도착 후 P단 60초가 지나면 "콩이 내렸나요?"
        DemoJump("도착 체크", 10_820, expectMode = AppMode.ARRIVED),
    )
}
