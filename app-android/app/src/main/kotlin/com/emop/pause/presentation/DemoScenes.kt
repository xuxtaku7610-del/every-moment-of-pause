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
    /** 시뮬레이터 장면용 실내 온도(여름 장면). 실차는 측정값만 쓴다. */
    val cabinTempC: Float? = null,
    /** 콩이 도우미 질문 (점프 뒤 `MainViewModel.ask`: 주행 중이면 음성 답, 정차 중이면 도우미 화면). */
    val ask: String? = null,
)

object DemoScenes {
    const val SCENARIO = "pet-trip"

    val JUMPS = listOf(
        DemoJump("출발 체크", 0, expectMode = AppMode.PREPARE),
        // 출발 30분 뒤: 최근 30분 그래프가 차고 고속도로 교통 흐름으로 지수가 0이 아니다(2026-10-31)
        DemoJump("고속도로", 1_800, expectMode = AppMode.DRIVING),
        // UPGRADE 7: 고속도로 중 "콩이가 헥헥거려" + 여름 실내 31℃ → 1줄 안내 + 가장 가까운 정차 장소
        DemoJump("콩이가 이상해요", 2_400, expectMode = AppMode.DRIVING, say = "콩이가 헥헥거려", cabinTempC = 31f),
        // 약 3,460초에 반려견 맞춤 쉼표(횡성휴게소, 놀이터·운전 피로)를 제안한다 — PetTripScenarioTest가 확인
        DemoJump("쉼표 추천", 3_420, expectMode = AppMode.DRIVING),
        // UI v9 (사용자 결정 11/03): 배변 15분 전 + 횡성휴게소 30분 안 → 쉼표 추천 이유 첫 줄 "배변 시간이 다가와요".
        // 점프 따라잡기가 3460·4060초 제안을 "나중에"로 지나가므로 약 4,660초에 다시 묻는다 — NearbyScenarioTest가 확인
        DemoJump("배변 임박 쉼표", 4_620, expectMode = AppMode.DRIVING),
        DemoJump("휴게소 쉼표", 5_240, expectMode = AppMode.PAUSE, expectCause = PauseCause.REST),
        DemoJump("잠깐 대기", 5_540, expectMode = AppMode.PAUSE, expectCause = PauseCause.REST, say = "잠깐 다녀올게"),
        // 고갯길 3km 앞: 예보 "3.2km 뒤 굽은 길 12분" (5km 안·300m 밖 구간만, 굽이 한두 개짜리 구간은 건너뜀)
        DemoJump("고갯길 예보", 8_748, expectMode = AppMode.DRIVING),
        // UI v9 주변 추천: 일반 도로(최근 5분 평균 80km/h 미만) + 목적지 10km 안(약 10,071초) → 목적지 3km 산책로, 없으면 반려견 동반 쇼핑
        // (실제 TourAPI: 야옹아멍멍해봐 강릉내곡점). 기존 "도착 전 산책"(F4-03)을 이 카드로 합쳤다.
        DemoJump("주변 추천", 10_040, expectMode = AppMode.DRIVING),
        // 도착 후 P단 60초가 지나면 "콩이 내렸나요?"
        DemoJump("도착 체크", 10_843, expectMode = AppMode.ARRIVED),
    )

    /**
     * 콩이 도우미 장면 (HELPER_PROMPT 5번) — [JUMPS]와 따로 둔다(장면 숫자 대조 DemoNumbersTest는 기존 장면만).
     * ① 주행 중 "콩이가 초콜릿 먹은 것 같아" → 응급 흐름(1줄 안내 + 병원 제안 음성), ② 휴게소 정차 "쉬할 데 있어?" → 놀이터 휴게소 답 → [여기서 쉴게요] 경유지.
     */
    val HELPER_JUMPS = listOf(
        DemoJump("도우미 · 주행 중 응급", 2_460, expectMode = AppMode.DRIVING, ask = "콩이가 초콜릿 먹은 것 같아"),
        DemoJump("도우미 · 쉬할 곳", 5_300, expectMode = AppMode.PAUSE, expectCause = PauseCause.REST, ask = "쉬할 데 있어?"),
    )
}
