package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.presentation.ScreenRouter.Input
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan DS-01·DS-04, F3-04, F6-04: 화면 선택 규칙. */
class ScreenRouterTest {
    private fun route(i: Input) = ScreenRouter.route(i)
    private val p = DriveState.PARKED

    @Test
    fun `DS-01 DRIVING이나 RESTRICTED면 무엇이 열려 있든 즉시 주행 화면`() {
        for (d in listOf(DriveState.DRIVING, DriveState.RESTRICTED)) {
            assertEquals(Route.DRIVE, route(Input(d, AppMode.PAUSE, simulation = false, waiting = true)))
            assertEquals(Route.DRIVE, route(Input(d, AppMode.PREPARE, simulation = false, overlay = Overlay.SETTINGS)))
            assertEquals(Route.DRIVE, route(Input(d, AppMode.PREPARE, simulation = false, needsProfile = true)))
            assertEquals(Route.DRIVE, route(Input(d, AppMode.PAUSE, simulation = false, overlay = Overlay.PLACES)))
        }
    }

    @Test
    fun `장면 고정은 시뮬레이션에서만 주행 상태보다 앞선다`() {
        assertEquals(Route.SCENE, route(Input(DriveState.DRIVING, AppMode.DRIVING, simulation = true, sceneLocked = true)))
        assertEquals(Route.DRIVE, route(Input(DriveState.DRIVING, AppMode.DRIVING, simulation = false, sceneLocked = true)))
        assertEquals(Route.SCENE, route(Input(p, AppMode.PREPARE, simulation = false, sceneLocked = true)))
    }

    @Test
    fun `정차 중 AppMode별 화면`() {
        assertEquals(Route.DEPART, route(Input(p, AppMode.PREPARE, false)))
        assertEquals(Route.DRIVE, route(Input(p, AppMode.DRIVING, false)))
        assertEquals(Route.REST, route(Input(p, AppMode.PAUSE, false)))
        assertEquals(Route.ARRIVAL, route(Input(p, AppMode.ARRIVED, false)))
        assertEquals(Route.TRIPLOG, route(Input(p, AppMode.ARRIVED, false, leftConfirmed = true)))
    }

    @Test
    fun `F3 대기 모드는 쉼표 정차에서 다른 화면보다 앞선다`() {
        assertEquals(Route.WAIT, route(Input(p, AppMode.PAUSE, false, waiting = true, overlay = Overlay.PLACES)))
        assertEquals(Route.DEPART, route(Input(p, AppMode.PREPARE, false, waiting = true)))
    }

    @Test
    fun `첫 실행은 출발 전에 반려견 프로필, 정차 중 위로 여는 화면`() {
        assertEquals(Route.PET, route(Input(p, AppMode.PREPARE, false, needsProfile = true)))
        assertEquals(Route.REST, route(Input(p, AppMode.PAUSE, false, needsProfile = true)))
        assertEquals(Route.SETTINGS, route(Input(p, AppMode.PREPARE, false, overlay = Overlay.SETTINGS)))
        assertEquals(Route.PLACES, route(Input(p, AppMode.PAUSE, false, overlay = Overlay.PLACES)))
        assertEquals(Route.VETS, route(Input(p, AppMode.PAUSE, false, overlay = Overlay.VETS)))
    }

    @Test
    fun `DS-04 NO_SETUP·NO_KEYBOARD면 정차 중이어도 프로필·설정을 열지 않는다`() {
        assertEquals(Route.DEPART, route(Input(p, AppMode.PREPARE, false, overlay = Overlay.SETTINGS, setupAllowed = false)))
        assertEquals(Route.DEPART, route(Input(p, AppMode.PREPARE, false, needsProfile = true, setupAllowed = false)))
    }

    @Test
    fun `애니메이션은 PAUSE이고 PARKED일 때만`() {
        assertTrue(ScreenRouter.motionAllowed(AppMode.PAUSE, p, reduceMotion = false))
        assertFalse(ScreenRouter.motionAllowed(AppMode.PAUSE, DriveState.RESTRICTED, false))
        assertFalse(ScreenRouter.motionAllowed(AppMode.PREPARE, p, false))
        assertFalse(ScreenRouter.motionAllowed(AppMode.PAUSE, p, reduceMotion = true))
    }

    @Test
    fun `발표 모드 패널은 실차 주행 중에 숨긴다`() {
        assertFalse(ScreenRouter.demoVisible(true, simulation = false, DriveState.DRIVING))
        assertTrue(ScreenRouter.demoVisible(true, simulation = true, DriveState.DRIVING))
        assertTrue(ScreenRouter.demoVisible(true, simulation = false, p))
    }
}
