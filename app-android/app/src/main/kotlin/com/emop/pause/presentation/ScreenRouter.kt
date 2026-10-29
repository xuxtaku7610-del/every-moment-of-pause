package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState

/** 어떤 화면을 그릴지 (상태 값은 ViewModel이 채운다). */
enum class Route { SCENE, DRIVE, SETTINGS, ONBOARD, PREPARE, PAUSE, NAP, REPORT }

/**
 * 화면 선택 규칙 — 순수 함수라 단위 테스트로 고정한다.
 * 최우선: DriveState가 DRIVING/RESTRICTED면 AppMode·설정·온보딩·파워냅과 상관없이 주행 화면 (architecture §5).
 * 예외 하나: 시뮬레이션의 "시안 장면 고정"은 발표·검증용이라 시뮬레이션일 때만 주행 상태보다 앞선다.
 */
object ScreenRouter {
    data class Input(
        val driveState: DriveState,
        val mode: AppMode,
        val simulation: Boolean,
        val sceneLocked: Boolean = false,
        val settingsOpen: Boolean = false,
        val onboarding: Boolean = false,
        val napping: Boolean = false,
        /** CarUxRestrictions NO_SETUP·NO_KEYBOARD가 아님 (UxPolicy.setupAllowed). */
        val setupAllowed: Boolean = true,
    )

    fun route(i: Input): Route = when {
        i.sceneLocked && (i.simulation || !i.driveState.forcesDrivingUi) -> Route.SCENE
        i.driveState.forcesDrivingUi -> Route.DRIVE
        i.settingsOpen && i.setupAllowed -> Route.SETTINGS
        // 온보딩은 정차 중에만, 멈춤(휴게소) 중에는 멈춤 화면이 먼저다.
        i.onboarding && i.setupAllowed && i.mode != AppMode.PAUSE -> Route.ONBOARD
        else -> when (i.mode) {
            AppMode.PREPARE -> Route.PREPARE
            AppMode.DRIVING -> Route.DRIVE
            AppMode.PAUSE -> if (i.napping) Route.NAP else Route.PAUSE
            AppMode.ARRIVED -> Route.REPORT
        }
    }

    /** 애니메이션 허용 = AppMode PAUSE && PARKED && 움직임 줄이기 꺼짐 (architecture §5, test_plan T6-11). */
    fun motionAllowed(mode: AppMode, driveState: DriveState, reduceMotion: Boolean): Boolean =
        mode == AppMode.PAUSE && driveState == DriveState.PARKED && !reduceMotion

    /** 발표 모드 패널: 실차 주행 중에는 열지 않는다. */
    fun demoVisible(open: Boolean, simulation: Boolean, driveState: DriveState): Boolean =
        open && (simulation || !driveState.forcesDrivingUi)
}
