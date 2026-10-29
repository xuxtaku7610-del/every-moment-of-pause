package com.emop.pause.uistate

/**
 * 화면 계약의 바깥 틀 (docs/ui_contract.md).
 * ViewModel은 [AppUiState] 하나만 내보내고, 화면은 그 안의 [Screen]만 그린 뒤 사용자 동작을 [UiEvent]로 돌려보낸다.
 * 새 디자인(docs/design/v5)으로 바꿀 때는 이 파일과 [ScreenStates]를 그대로 두고 화면(Compose)만 갈아 끼운다.
 */

/** 지금 그릴 화면 하나와 그 상태. */
sealed interface Screen {
    data class Depart(val ui: DepartUi) : Screen
    /** 주행 글랜스. 멀미 예보·쉼표 추천·음성·RESTRICTED는 [DriveUi] 안의 값으로 갈린다. */
    data class Drive(val ui: DriveUi) : Screen
    data class Rest(val ui: RestUi) : Screen
    data class Wait(val ui: WaitUi) : Screen
    data class Places(val ui: PlacesUi) : Screen
    data class Vets(val ui: VetsUi) : Screen
    data class Arrival(val ui: ArrivalUi) : Screen
    data class TripLog(val ui: TripLogUi) : Screen
    data class PetProfile(val ui: PetEditUi) : Screen
    data class Settings(val ui: SettingsUi) : Screen
    /** 첫 실행 3단계 (반려견 프로필 → 차량 연동 → 멀미 민감도 진단). */
    data class Onboarding(val ui: OnboardingUi) : Screen
}

data class AppUiState(
    val screen: Screen,
    /** DriveState가 DRIVING/RESTRICTED: [screen]은 항상 [Screen.Drive]이고, 입력·애니메이션·설정이 막힌다 (DS-01). */
    val drivingLocked: Boolean = false,
    /** 애니메이션 허용 = AppMode PAUSE && PARKED && 움직임 줄이기 꺼짐 (architecture §5). */
    val motionAllowed: Boolean = false,
    /** 발표 모드 패널이 열려 있음. 실차 주행 중에는 항상 false. */
    val demoOpen: Boolean = false,
    /** 발표 모드 "장면 고정" 중인 장면. */
    val scene: Scene? = null,
)

/** 발표 모드에서 고정 장면을 보여 줄 때 (화면 검증·PT 예비용). [Fixtures.screen]이 상태를 만든다. */
enum class Scene { DEPART, DRIVE, FORECAST, SUGGEST, URGENT, RESTRICTED, VET_DRIVE, REST, WAIT, WAIT_OVERDUE, PLACES, VETS, ARRIVAL, ARRIVAL_HOT, TRIPLOG, PET, SETTINGS, ONBOARD_1, ONBOARD_2, ONBOARD_3 }

/** 화면 → ViewModel. 화면은 이 이벤트만 보내고, 무엇이 일어나는지는 ViewModel이 정한다. */
sealed interface UiEvent {
    /** 로고 탭. 3초 안 5번이면 발표 모드 패널. 모든 화면 공통. */
    data object LogoTap : UiEvent

    // 주행 (쉼표 추천 카드·음성 실패 시트)
    data object AcceptSuggestion : UiEvent
    data object DeclineSuggestion : UiEvent

    // 출발 체크
    data class ToggleCheck(val kind: CheckKind) : UiEvent
    /** "이대로 출발". 체크를 다 해야 보낸다. 처음이면 마이크 권한을 먼저 묻는다(화면 밖 Activity 처리). */
    data object StartTrip : UiEvent

    // 화면 열고 닫기 (정차 중에만)
    data object OpenPet : UiEvent
    data object OpenSettings : UiEvent
    data object OpenPlaces : UiEvent
    data object OpenVets : UiEvent
    data object Close : UiEvent

    // 쉼표 (정차)
    data class StartActivity(val kind: ActivityKind) : UiEvent
    data class SetRestMinutes(val minutes: Int) : UiEvent

    // 잠깐 대기 (F3)
    data object StartWait : UiEvent
    data object StopWait : UiEvent
    data class SetWaitLimit(val minutes: Int) : UiEvent

    // 장소·병원 (F4·F5): 경유지로 넣거나 길 안내
    data class GoPlace(val id: String) : UiEvent
    data class GoVet(val id: String) : UiEvent
    /** 동물병원 도착 예정 공유 링크 만들기 (정차 중). 위치는 보내지 않는다. */
    data class ShareVetEta(val id: String) : UiEvent

    // 도착·기록 (F6·F7)
    data object ConfirmLeft : UiEvent
    data class ToggleSymptom(val label: String) : UiEvent
    data object FinishTrip : UiEvent

    // 반려견 프로필
    data class SetPetName(val name: String) : UiEvent
    data class SetPetSize(val size: SizeUi) : UiEvent
    data class SetPetAge(val years: Int) : UiEvent
    data class SetPetTendency(val tendency: TendencyUi) : UiEvent
    data class SetPotty(val minutes: Int) : UiEvent
    data object TogglePetZone : UiEvent
    data object SavePet : UiEvent

    // 온보딩 (첫 실행)
    data object OnboardNext : UiEvent
    data object OnboardBack : UiEvent
    data class AnswerQuiz(val question: Int, val option: Int) : UiEvent

    // 설정
    /** 0 빠르게 · 1 보통 · 2 여유. */
    data class SetTiming(val value: Int) : UiEvent
    data object ToggleVoice : UiEvent
    data object ToggleReduceMotion : UiEvent
    data object DeleteData : UiEvent
}
