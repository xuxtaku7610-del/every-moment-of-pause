package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.presentation.ScreenRouter.Input
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScreenRouterTest {
    private fun route(i: Input) = ScreenRouter.route(i)

    @Test
    fun `T6-6 파워냅 중 DRIVING이나 RESTRICTED가 되면 즉시 주행 화면`() {
        for (d in listOf(DriveState.DRIVING, DriveState.RESTRICTED)) {
            assertEquals(Route.DRIVE, route(Input(d, AppMode.PAUSE, simulation = false, napping = true)))
        }
    }

    @Test
    fun `T1-6 T10-1 주행 중에는 설정과 온보딩이 열리지 않는다`() {
        assertEquals(Route.DRIVE, route(Input(DriveState.DRIVING, AppMode.PREPARE, simulation = false, settingsOpen = true)))
        assertEquals(Route.DRIVE, route(Input(DriveState.RESTRICTED, AppMode.PREPARE, simulation = false, onboarding = true)))
    }

    @Test
    fun `장면 고정은 시뮬레이션에서만 주행 상태보다 앞선다`() {
        assertEquals(Route.SCENE, route(Input(DriveState.DRIVING, AppMode.DRIVING, simulation = true, sceneLocked = true)))
        assertEquals(Route.DRIVE, route(Input(DriveState.DRIVING, AppMode.DRIVING, simulation = false, sceneLocked = true)))
        assertEquals(Route.SCENE, route(Input(DriveState.PARKED, AppMode.PREPARE, simulation = false, sceneLocked = true)))
    }

    @Test
    fun `정차 중 AppMode별 화면`() {
        val p = DriveState.PARKED
        assertEquals(Route.PREPARE, route(Input(p, AppMode.PREPARE, false)))
        assertEquals(Route.DRIVE, route(Input(p, AppMode.DRIVING, false)))
        assertEquals(Route.PAUSE, route(Input(p, AppMode.PAUSE, false)))
        assertEquals(Route.NAP, route(Input(p, AppMode.PAUSE, false, napping = true)))
        assertEquals(Route.REPORT, route(Input(p, AppMode.ARRIVED, false)))
        assertEquals(Route.SETTINGS, route(Input(p, AppMode.PREPARE, false, settingsOpen = true)))
    }

    @Test
    fun `온보딩은 멈춤 중에는 미룬다`() {
        assertEquals(Route.ONBOARD, route(Input(DriveState.PARKED, AppMode.PREPARE, false, onboarding = true)))
        assertEquals(Route.PAUSE, route(Input(DriveState.PARKED, AppMode.PAUSE, false, onboarding = true)))
    }

    @Test
    fun `T6-11 애니메이션은 PAUSE이고 PARKED일 때만`() {
        assertTrue(ScreenRouter.motionAllowed(AppMode.PAUSE, DriveState.PARKED, reduceMotion = false))
        assertFalse(ScreenRouter.motionAllowed(AppMode.PAUSE, DriveState.RESTRICTED, false))
        assertFalse(ScreenRouter.motionAllowed(AppMode.PAUSE, DriveState.DRIVING, false))
        assertFalse(ScreenRouter.motionAllowed(AppMode.PREPARE, DriveState.PARKED, false))
        assertFalse(ScreenRouter.motionAllowed(AppMode.PAUSE, DriveState.PARKED, reduceMotion = true))
    }

    @Test
    fun `발표 모드 패널은 실차 주행 중에 숨긴다`() {
        assertFalse(ScreenRouter.demoVisible(true, simulation = false, DriveState.DRIVING))
        assertTrue(ScreenRouter.demoVisible(true, simulation = true, DriveState.DRIVING))
        assertTrue(ScreenRouter.demoVisible(true, simulation = false, DriveState.PARKED))
    }

    @Test
    fun `T1-6 NO_SETUP·NO_KEYBOARD면 정차 중이어도 설정·온보딩을 열지 않는다`() {
        assertEquals(Route.PREPARE, route(Input(DriveState.PARKED, AppMode.PREPARE, false, settingsOpen = true, setupAllowed = false)))
        assertEquals(Route.PREPARE, route(Input(DriveState.PARKED, AppMode.PREPARE, false, onboarding = true, setupAllowed = false)))
    }
}
