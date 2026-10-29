package com.emop.pause.uistate

/**
 * 화면 계약의 바깥 틀 (docs/ui_contract.md).
 * ViewModel은 [AppUiState] 하나만 내보내고, 화면은 그 안의 [Screen]만 그린 뒤 사용자 동작을 [UiEvent]로 돌려보낸다.
 * 새 디자인으로 바꿀 때는 이 파일과 [ScreenStates]를 그대로 두고 화면(Compose)만 갈아 끼운다.
 */

/** 지금 그릴 화면 하나와 그 상태. */
sealed interface Screen {
    /** 주행 글랜스. 제안·제안 없음·긴급·듣는 중·음성 실패·오프라인은 [DriveUi] 안의 값으로 갈린다. */
    data class Drive(val ui: DriveUi) : Screen
    data class Prepare(val ui: PrepareScreenUi) : Screen
    data class Pause(val ui: PauseScreenUi) : Screen
    data class Nap(val ui: NapUi) : Screen
    data class Report(val ui: ReportUi) : Screen
    data class Onboard(val ui: OnboardUi) : Screen
    data class Settings(val ui: SettingsUi) : Screen
}

data class OnboardUi(val step: OnboardStep, val members: List<MemberUi>)

data class AppUiState(
    val screen: Screen,
    /** DriveState가 DRIVING/RESTRICTED: [screen]은 항상 [Screen.Drive]이고, 입력·애니메이션·설정이 막힌다. */
    val drivingLocked: Boolean = false,
    /** 애니메이션 허용 = AppMode PAUSE && PARKED && 움직임 줄이기 꺼짐 (architecture §5). */
    val motionAllowed: Boolean = false,
    /** 파워냅 중: 창 밝기를 낮춘다 (UI_SPEC §4.4). */
    val dimmed: Boolean = false,
    /** 발표 모드 패널이 열려 있음. 실차 주행 중에는 항상 false. */
    val demoOpen: Boolean = false,
    /** 발표 모드 "시안 장면 고정" 중인 장면. */
    val scene: Scene? = null,
)

/** 발표 모드에서 시안 그대로의 장면을 고정해 보여 줄 때 (UI 검증·PT 예비용). [Fixtures.screen]이 상태를 만든다. */
enum class Scene { DRIVE, HALF, THIRD, CALM, URGENT, LISTENING, VOICE_FAIL, OFFLINE, PREPARE, PAUSE, NAP, REPORT, ONBOARD1, ONBOARD2, ONBOARD3, SETTINGS }

/** 화면 → ViewModel. 화면은 이 이벤트만 보내고, 무엇이 일어나는지는 ViewModel이 정한다. */
sealed interface UiEvent {
    /** 로고 탭. 3초 안 5번이면 발표 모드 패널. 모든 화면 공통. */
    data object LogoTap : UiEvent

    // 주행 (제안 카드·음성 실패 시트)
    data object AcceptSuggestion : UiEvent
    data object DeclineSuggestion : UiEvent

    // 출발 준비
    data class ToggleMember(val id: String) : UiEvent
    /** 4..8 (8 = 8시간 이상), null = 고르지 않음. */
    data class SetSleep(val hours: Int?) : UiEvent
    data object ToggleProactive : UiEvent
    /** 출발. 처음이면 마이크 권한을 먼저 묻는다(화면 밖 Activity 처리). */
    data object StartTrip : UiEvent
    data object OpenSettings : UiEvent

    // 멈춤
    data class StartActivity(val kind: ActivityKind) : UiEvent
    data class SetRestMinutes(val minutes: Int) : UiEvent
    /** 원정대 미션 하나 완료. */
    data class CompleteMission(val id: String) : UiEvent
    /** 라디오 켜기·끄기 (정차 중 화면 버튼. 주행 중에는 음성으로만). */
    data class SetRadio(val on: Boolean) : UiEvent

    // 파워냅
    data object Wake : UiEvent
    data object NapMore : UiEvent
    data object ToggleNapSound : UiEvent

    // 리포트
    data object FinishReport : UiEvent

    // 온보딩
    data object OnboardNext : UiEvent
    /** 마이크 권한 요청 후 온보딩 끝. 권한 창은 Activity가 띄운다. */
    data object AllowMic : UiEvent
    /** 마이크 없이 버튼만으로 쓰기. */
    data object ButtonsOnly : UiEvent

    // 설정
    data class SelectSettingsSection(val section: SettingsSection) : UiEvent
    /** 0 빠르게 · 1 보통 · 2 여유. */
    data class SetTiming(val value: Int) : UiEvent
    data object ToggleVoice : UiEvent
    data object ToggleKidAnswer : UiEvent
    data object ToggleReduceMotion : UiEvent
    data object DeleteData : UiEvent
    data object CloseSettings : UiEvent
}
