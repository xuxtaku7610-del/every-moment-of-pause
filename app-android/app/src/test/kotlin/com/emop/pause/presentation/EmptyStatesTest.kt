package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.SignalSample
import com.emop.pause.data.StopSource
import com.emop.pause.runtime.Snapshot
import com.emop.pause.trip.TripUi
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DrivingRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * COMMERCIAL_READINESS H P0: 빈 상태·로딩·오류 상태를 모든 화면에 정의 (docs/ui_contract.md "빈·로딩·오류 상태").
 * 신호·경로·휴게소가 하나도 없을 때도 화면 상태가 만들어지고 주행 제한을 지킨다.
 */
class EmptyStatesTest {
    private val empty = Snapshot(mode = AppMode.DRIVING)

    @Test
    fun `H 주행 - 경로·피로·휴게소가 아직 없으면 편안 문장만, 메타·경로선·피로 카드 없음`() {
        val ui = UiMapper.drive(empty, TripUi())
        assertNull(ui.meta)
        assertNull(ui.journey)
        assertNull(ui.fatigue)
        assertTrue(ui.family.isEmpty())
        val c = ui.content as DriveContent.Calm
        assertEquals("지금 컨디션이면 목적지까지 그대로 가도 괜찮아요.", c.line)
        assertEquals(emptyList<String>(), DrivingRules.violations(ui))
    }

    @Test
    fun `H 주행 - 서버 대신 캐시·내장 데이터면 오프라인 배너`() {
        assertTrue(UiMapper.drive(empty, TripUi(stopSource = StopSource.BUNDLED)).offline)
        assertTrue(UiMapper.drive(empty, TripUi(stopSource = StopSource.CACHE)).offline)
        assertEquals(false, UiMapper.drive(empty, TripUi(stopSource = StopSource.SERVER)).offline)
        // 아직 휴게소를 받는 중(로딩)이면 배너를 띄우지 않는다
        assertEquals(false, UiMapper.drive(empty, TripUi(stopSource = null)).offline)
    }

    @Test
    fun `H 출발 준비 - 계획이 없으면 빈 목록`() {
        val (rows, _) = UiMapper.preparePlan(TripUi())
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `H 멈춤 - 충전 정보가 없으면 시간 고르기 안내, 배터리 카드 없음`() {
        val s = Snapshot(mode = AppMode.PAUSE, pauseCause = PauseCause.CHARGING, sample = SignalSample(0, batterySocPercent = 40))
        val ui = UiMapper.pause(s, TripUi(chargeInfoAvailable = false), UiMapper.PauseExtra(600, stopName = null), Names())
        assertNull(ui.battery)
        assertEquals("충전 정보 없음 — 쉬실 시간을 골라 주세요.", ui.notice)
        assertEquals("쉬는 중", ui.meta)
        assertNotNull(ui.timeline.firstOrNull())
    }

    @Test
    fun `H 멈춤 - 배터리 신호가 없는 차의 휴식 정차`() {
        val s = Snapshot(mode = AppMode.PAUSE, pauseCause = PauseCause.REST)
        val ui = UiMapper.pause(s, TripUi(), UiMapper.PauseExtra(600, stopName = "죽암휴게소"), Names())
        assertNull(ui.battery)
        assertNull(ui.notice)
        assertEquals(15, ui.restChoice)
        assertTrue(ui.activities.isNotEmpty())
    }
}
