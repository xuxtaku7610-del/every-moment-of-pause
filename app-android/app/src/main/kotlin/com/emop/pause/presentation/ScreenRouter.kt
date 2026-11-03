package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.ServiceState

/** 어떤 화면을 그릴지 (상태 값은 ViewModel이 채운다). */
enum class Route { SCENE, DRIVE, DEPART, REST, WAIT, PLACES, VETS, ARRIVAL, SUMMARY, TRIPLOG, STIMULUS, PET, SETTINGS, ONBOARDING, EMERGENCY }

/** 정차 중에 위로 여는 화면. */
enum class Overlay { NONE, PET, SETTINGS, PLACES, VETS, STIMULUS, EMERGENCY }

/**
 * 화면 선택 규칙 — 순수 함수라 단위 테스트로 고정한다 (docs/ui_contract.md "화면 선택").
 * 최우선: DriveState가 DRIVING/RESTRICTED면 무엇이 열려 있든 주행 화면 (DS-01).
 * 예외 하나: 시뮬레이션의 "장면 고정"은 발표·검증용이라 시뮬레이션일 때만 주행 상태보다 앞선다.
 * 대기 모드는 정차 중 다른 화면보다 앞선다(차 화면이 바깥 사람용 안내판이 된다, F3).
 */
object ScreenRouter {
    data class Input(
        val driveState: DriveState,
        val mode: AppMode,
        val simulation: Boolean,
        val sceneLocked: Boolean = false,
        val overlay: Overlay = Overlay.NONE,
        /** 프로필을 아직 저장하지 않음(첫 실행) → 출발 전에 온보딩 3단계. */
        val needsProfile: Boolean = false,
        val waiting: Boolean = false,
        val leftConfirmed: Boolean = false,
        /** 도착 요약에서 [이동 기록 보기]를 눌렀음 (UI v9). */
        val reportOpen: Boolean = false,
        /** CarUxRestrictions NO_SETUP·NO_KEYBOARD가 아님 (UxPolicy.setupAllowed). */
        val setupAllowed: Boolean = true,
    )

    fun route(i: Input): Route = when {
        i.sceneLocked && (i.simulation || !i.driveState.forcesDrivingUi) -> Route.SCENE
        i.driveState.forcesDrivingUi -> Route.DRIVE
        // 긴급 도움: 정차 화면(대기 포함) 위 겹침. 주행이면 위에서 DRIVE — 주행 중에는 열리지 않는다.
        i.overlay == Overlay.EMERGENCY -> Route.EMERGENCY
        // 긴급 도움에서 "바로 동물병원 찾기"는 대기 중에도 열린다 (전에는 대기 중 병원을 여는 버튼이 없었다)
        i.overlay == Overlay.VETS && i.waiting -> Route.VETS
        i.waiting && i.mode == AppMode.PAUSE -> Route.WAIT
        i.overlay == Overlay.SETTINGS && i.setupAllowed -> Route.SETTINGS
        i.overlay == Overlay.PET && i.setupAllowed -> Route.PET
        // 첫 실행: 3단계 온보딩(반려견 프로필 → 차량 연동 → 멀미 민감도 진단) — v5 11번
        i.needsProfile && i.mode == AppMode.PREPARE && i.setupAllowed -> Route.ONBOARDING
        i.overlay == Overlay.PLACES -> Route.PLACES
        i.overlay == Overlay.VETS -> Route.VETS
        // "주행 자극 보기"는 정차 중에만 (주행이면 위에서 DRIVE)
        i.overlay == Overlay.STIMULUS -> Route.STIMULUS
        else -> when (i.mode) {
            AppMode.PREPARE -> Route.DEPART
            AppMode.DRIVING -> Route.DRIVE
            AppMode.PAUSE -> Route.REST
            // 내렸어요 → 도착 요약(UI v9) → [이동 기록 보기] → 리포트
            AppMode.ARRIVED -> when {
                !i.leftConfirmed -> Route.ARRIVAL
                i.reportOpen -> Route.TRIPLOG
                else -> Route.SUMMARY
            }
        }
    }

    /** 애니메이션 허용 = AppMode PAUSE && PARKED && 움직임 줄이기 꺼짐 (architecture §5). */
    fun motionAllowed(mode: AppMode, driveState: DriveState, reduceMotion: Boolean): Boolean =
        mode == AppMode.PAUSE && driveState == DriveState.PARKED && !reduceMotion

    /**
     * 8개 상태 (docs/states.md): 화면과 주행 내용으로 지금 서비스 상태를 정한다. 발표 모드 표시·테스트용이고 화면 선택은 [route]가 한다.
     * 동물병원 화면은 병원으로 가는 중이면 7(병원 이동), 아니면 정차 화면.
     */
    fun serviceState(route: Route, content: DriveContent?, goingVet: Boolean): ServiceState = when (route) {
        Route.DEPART, Route.ONBOARDING -> ServiceState.DEPART
        Route.DRIVE -> when (content) {
            is DriveContent.Distress -> ServiceState.DISTRESS
            is DriveContent.Vet -> ServiceState.VET
            is DriveContent.Suggest -> ServiceState.SUGGEST
            is DriveContent.Forecast -> ServiceState.FORECAST
            else -> if (goingVet) ServiceState.VET else ServiceState.DRIVING
        }
        Route.WAIT -> ServiceState.WAIT
        Route.VETS -> if (goingVet) ServiceState.VET else ServiceState.STOPPED
        Route.ARRIVAL, Route.SUMMARY, Route.TRIPLOG -> ServiceState.REPORT
        Route.REST, Route.STIMULUS, Route.PLACES, Route.PET, Route.SETTINGS, Route.SCENE, Route.EMERGENCY -> ServiceState.STOPPED
    }

    /** 발표 모드 패널: 실차 주행 중에는 열지 않는다. */
    fun demoVisible(open: Boolean, simulation: Boolean, driveState: DriveState): Boolean =
        open && (simulation || !driveState.forcesDrivingUi)
}
