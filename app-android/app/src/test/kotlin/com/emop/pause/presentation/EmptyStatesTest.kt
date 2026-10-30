package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.SignalSample
import com.emop.pause.data.StopSource
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.MotionState
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.pet.CheckItem
import com.emop.pause.engine.pet.WaitStatus
import com.emop.pause.runtime.Snapshot
import com.emop.pause.trip.TripUi
import com.emop.pause.uistate.ActivityKind
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DrivingRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 빈·로딩·오류 상태 (docs/ui_contract.md "빈·로딩·오류 상태"): 신호·경로·휴게소가 하나도 없을 때도
 * 화면 상태가 만들어지고 주행 제한을 지킨다. 그리고 같이가개 화면별 매핑.
 */
class EmptyStatesTest {
    private val empty = Snapshot(mode = AppMode.DRIVING)
    private val kong = TripUi(pet = PetProfile("콩이"), passengers = listOf(Passenger.Dog()))

    @Test
    fun `주행 - 경로·엔진 값이 없으면 안내 문장만, 메타·멀미 없음`() {
        val ui = UiMapper.drive(empty, TripUi())
        assertNull(ui.meta)
        assertNull(ui.motion)
        assertTrue(ui.content is DriveContent.Calm)
        assertEquals(emptyList<String>(), DrivingRules.violations(ui))
    }

    @Test
    fun `주행 - 서버 대신 캐시·내장 데이터면 오프라인 표시`() {
        assertTrue(UiMapper.drive(empty, TripUi(stopSource = StopSource.BUNDLED)).offline)
        assertFalse(UiMapper.drive(empty, TripUi(stopSource = StopSource.SERVER)).offline)
    }

    @Test
    fun `F1 주행 - 5km 안 예보는 한 줄, 멀리 있으면 안내 문장`() {
        val near = kong.copy(motion = MotionState(index = 45), forecast = MotionForecast(3000.0, 2400.0, 480, 52, 39))
        val ui = UiMapper.drive(empty, near)
        assertEquals("3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속해 주세요.", (ui.content as DriveContent.Forecast).line)
        assertEquals("콩이 멀미 주의 45", ui.motion!!.label)
        val far = near.copy(forecast = MotionForecast(9000.0, 2400.0, 480, 52, 39))
        assertTrue(UiMapper.drive(empty, far).content is DriveContent.Calm)
    }

    @Test
    fun `F6-01 출발 체크 - 세 항목을 다 해야 ready, 계획이 없으면 빈 목록`() {
        val s = Snapshot(mode = AppMode.PREPARE)
        val none = UiMapper.depart(s, kong, null, savedProfile = true)
        assertFalse(none.ready)
        assertTrue(none.plan.isEmpty())
        assertEquals("경로를 받으면 쉴 곳을 잡아 드려요.", none.rationale)
        assertTrue(UiMapper.depart(s, kong.copy(checks = CheckItem.entries.toSet()), null, true).ready)
        assertTrue(UiMapper.depart(s, kong, null, savedProfile = false).pet.sample)
    }

    @Test
    fun `F2-08 쉼표 - 반려견이면 산책 추천, 배터리 신호가 없으면 표시 안 함`() {
        val s = Snapshot(mode = AppMode.PAUSE, pauseCause = PauseCause.REST, sample = SignalSample(0))
        val ui = UiMapper.rest(s, kong, 15, "횡성휴게소", 10)
        assertEquals("횡성휴게소에서 쉬는 중", ui.meta)
        assertEquals(ActivityKind.PET_WALK, ui.activities.single { it.recommended }.kind)
        assertNull(ui.battery)
        assertEquals(15, ui.restChoice)
    }

    @Test
    fun `F3-06 대기 - 실내 온도가 없으면 온도 표시를 숨기고(여름엔 일반 문구만), 공유가 없으면 주소 없음`() {
        val w = WaitStatus(active = true, elapsedSec = 185, limitSec = 600, batteryStart = 60, batteryNow = 59)
        val ui = UiMapper.wait(w, kong, month = 11)
        assertEquals("3분째", ui.elapsed)
        assertNull(ui.cabin)
        assertNull(ui.seasonNote)
        assertEquals("여름에는 차 안이 빨리 더워져요", UiMapper.wait(w, kong, month = 7).seasonNote)
        assertNull(UiMapper.wait(w.copy(cabinTempC = 24.4f), kong, month = 7).seasonNote)
        assertEquals("배터리 59%", ui.battery)
        assertNull(ui.share.url)
        assertEquals("실내 24℃", UiMapper.wait(w.copy(cabinTempC = 24.4f), kong).cabin)
    }

    @Test
    fun `F6-03 도착 - 강한 알림 문구`() {
        val strong = UiMapper.arrival(kong.copy(arrival = com.emop.pause.engine.pet.ArrivalPrompt(true, 28f)))
        assertTrue(strong.strong)
        assertEquals("콩이 내렸나요?", strong.question)
    }

    @Test
    fun `장소·병원 - 결과가 없으면 빈 목록과 출처 안내`() {
        assertTrue(UiMapper.places(null, null, loading = false).items.isEmpty())
        assertTrue(UiMapper.vets(null, loading = true).loading)
    }
}
