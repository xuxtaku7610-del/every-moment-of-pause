package com.emop.pause.engine.pet

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.PetProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WaitModeTest {
    private fun p(sec: Int, gear: Gear? = Gear.PARK, drive: DriveState = DriveState.PARKED, soc: Int? = 62, temp: Float? = null, brake: Boolean? = null) =
        WaitInput(sec * 1000L, drive, gear, brake, soc, temp)

    @Test
    fun `F3-01 P단에서 시작하면 Started, 경과 시간이 흐른다`() {
        val w = WaitMode()
        assertEquals(listOf(WaitEvent.Started), w.start(p(0)))
        w.tick(p(185, soc = 61))
        assertTrue(w.status.active)
        assertEquals(185, w.status.elapsedSec)
    }

    @Test
    fun `F3-02 배터리 변화를 시작 대비로 낸다`() {
        val w = WaitMode()
        w.start(p(0, soc = 62))
        w.tick(p(60, soc = 61))
        assertEquals(-1, w.status.batteryDelta)
        w.tick(p(61, soc = null)) // 신호가 잠깐 없어도 마지막 값을 쓴다
        assertEquals(61, w.status.batteryNow)
    }

    @Test
    fun `F3-03 제한 시간을 넘으면 Overdue 한 번만`() {
        val w = WaitMode(limitMin = 10)
        w.start(p(0))
        assertTrue(w.tick(p(599)).isEmpty())
        assertEquals(listOf(WaitEvent.Overdue), w.tick(p(600)))
        assertTrue(w.tick(p(700)).isEmpty())
        assertTrue(w.status.overdue)
    }

    @Test
    fun `F3-04 기어가 P에서 벗어나거나 주행 상태면 즉시 끝`() {
        val a = WaitMode().apply { start(p(0)) }
        assertEquals(listOf(WaitEvent.Ended(WaitEndReason.LEFT_PARK)), a.tick(p(30, gear = Gear.DRIVE)))
        assertFalse(a.status.active)
        val b = WaitMode().apply { start(p(0)) }
        assertEquals(listOf(WaitEvent.Ended(WaitEndReason.DRIVING)), b.tick(p(30, drive = DriveState.DRIVING)))
    }

    @Test
    fun `F3-05 P단이 아니면 시작할 수 없다`() {
        val w = WaitMode()
        assertTrue(w.start(p(0, gear = Gear.DRIVE)).isEmpty())
        assertTrue(w.start(p(0, gear = Gear.NEUTRAL)).isEmpty())
        assertTrue(w.start(p(0, drive = DriveState.RESTRICTED)).isEmpty())
        assertFalse(w.status.active)
        // 기어 신호가 없는 차는 주차 브레이크로 본다
        assertTrue(w.canStart(p(0, gear = null, brake = true)))
        assertFalse(w.canStart(p(0, gear = Gear.UNKNOWN, brake = false)))
    }

    @Test
    fun `F3-06 실내 온도가 없으면 null 그대로 (지어내지 않음)`() {
        val w = WaitMode().apply { start(p(0)) }
        w.tick(p(10, temp = null))
        assertNull(w.status.cabinTempC)
        w.tick(p(11, temp = 27.5f))
        assertEquals(27.5f, w.status.cabinTempC)
    }

    @Test
    fun `사용자가 끝내면 USER, 제한 시간은 1~120분`() {
        val w = WaitMode()
        w.start(p(0))
        assertEquals(listOf(WaitEvent.Ended(WaitEndReason.USER)), w.stop())
        w.setLimit(500)
        assertEquals(120 * 60, w.status.limitSec)
        w.setLimit(0)
        assertEquals(60, w.status.limitSec)
    }
}

class BoardingCheckTest {
    private fun arrive(sec: Int, gear: Gear? = Gear.PARK, ending: Boolean = true, temp: Float? = null, month: Int = 11) =
        ArrivalInput(sec * 1000L, gear, null, ending, temp, month)

    @Test
    fun `F6-01 세 항목을 모두 확인해야 출발할 수 있다`() {
        val b = BoardingCheck()
        assertFalse(b.readyToDepart)
        b.toggle(CheckItem.HARNESS)
        b.toggle(CheckItem.WATER)
        assertFalse(b.readyToDepart)
        b.toggle(CheckItem.POTTY_BAGS)
        assertTrue(b.readyToDepart)
        b.toggle(CheckItem.WATER)
        assertFalse(b.readyToDepart)
    }

    @Test
    fun `F6-02 도착 후 P단 60초면 내렸나요 한 번`() {
        val b = BoardingCheck()
        assertNull(b.tick(arrive(0)))
        assertNull(b.tick(arrive(59)))
        val prompt = b.tick(arrive(60))
        assertTrue(prompt != null && !prompt.strong)
        assertNull(b.tick(arrive(120)), "다시 묻지 않는다")
        assertEquals(prompt, b.pending)
    }

    @Test
    fun `F6-02 P단이 끊기면 60초를 다시 센다`() {
        val b = BoardingCheck()
        b.tick(arrive(0))
        b.tick(arrive(50, gear = Gear.REVERSE))
        assertNull(b.tick(arrive(100)))
        assertNull(b.tick(arrive(159)))
        assertTrue(b.tick(arrive(160)) != null)
    }

    @Test
    fun `F6-03 26도 이상이거나 온도를 모르는 여름이면 강한 알림`() {
        assertTrue(BoardingCheck().run { tick(arrive(0, temp = 27f)); tick(arrive(60, temp = 27f)) }!!.strong)
        assertFalse(BoardingCheck().run { tick(arrive(0, temp = 22f, month = 7)); tick(arrive(60, temp = 22f, month = 7)) }!!.strong)
        assertTrue(BoardingCheck().run { tick(arrive(0, month = 7)); tick(arrive(60, month = 7)) }!!.strong)
    }

    @Test
    fun `F6-04 휴게소 정차(여행 중)에는 묻지 않고, 내렸어요 전까지 기다린다`() {
        val b = BoardingCheck()
        b.tick(arrive(0, ending = false))
        assertNull(b.tick(arrive(300, ending = false)))
        b.tick(arrive(301))
        b.tick(arrive(361))
        assertFalse(b.confirmedLeft)
        b.confirmLeft()
        assertTrue(b.confirmedLeft)
        assertNull(b.pending)
    }
}

class PetTripLogTest {
    private fun trip(day: Int, first: Int?) = PetTripRecord(day * 86_400_000L, 150, first, if (first != null) 55 else 20, 2)

    @Test
    fun `F7-02 주의 도달 시간의 중앙값이 한계 시간`() {
        val e = PetTripLog.limit(listOf(trip(1, 60), trip(2, 80), trip(3, null), trip(4, 70)))!!
        assertEquals(70, e.minutes)
        assertEquals(3, e.basedOn)
        assertEquals("지난 여행들은 출발 약 1시간 10분 뒤 주행 자극이 보통 이상이 됐어요.", PetTripLog.limitPhrase("콩이", e))
    }

    @Test
    fun `F7-02 짝수 개면 가운데 둘의 평균, 근거가 적으면 표시`() {
        val e = PetTripLog.limit(listOf(trip(1, 60), trip(2, 80)))!!
        assertEquals(70, e.minutes)
        assertEquals("지난 여행들은 출발 약 1시간 10분 뒤 주행 자극이 보통 이상이 됐어요. (기록 2번 기준)", PetTripLog.limitPhrase("밤", e))
        assertNull(PetTripLog.limit(listOf(trip(1, null))))
        assertEquals("지난 여행에서 주행 자극이 보통 단계에 닿은 적은 없어요.", PetTripLog.limitPhrase("콩이", null))
    }

    @Test
    fun `F7-02 이동 기록 한 줄은 이번 여행 값을 먼저 본다 (주의 n번과 어긋나지 않게)`() {
        val reached = trip(1, 151)
        assertEquals("이번엔 출발 2시간 31분 뒤 주행 자극이 보통 이상이 됐어요.", PetTripLog.insight("콩이", reached, null))
        val e = LimitEstimate(70, 3)
        assertEquals("이번엔 출발 2시간 31분 뒤 주행 자극이 보통 이상이 됐어요. 지난 여행들은 출발 약 1시간 10분 뒤 주행 자극이 보통 이상이 됐어요.", PetTripLog.insight("콩이", reached, e))
        assertEquals("이번엔 주행 자극이 낮은 채로 도착했어요. 지난 여행들은 출발 약 1시간 10분 뒤 주행 자극이 보통 이상이 됐어요.", PetTripLog.insight("콩이", trip(2, null), e))
        assertEquals("지난 여행에서 주행 자극이 보통 단계에 닿은 적은 없어요.", PetTripLog.insight("콩이", trip(2, null), null))
    }

    @Test
    fun `F7-02 최근 10번만 본다`() {
        val old = (1..10).map { trip(it, 30) }
        val recent = (11..20).map { trip(it, 90) }
        assertEquals(90, PetTripLog.limit(old + recent)!!.minutes)
    }

    @Test
    fun `F7-03 병원용 요약 - 반려견 측정 아님 표기와 관찰 신고`() {
        val t = PetTripRecord(0, 130, 75, 62, 2, "출발 1시간쯤 침을 많이 흘림")
        val lines = PetTripLog.vetSummary("콩이", t, LimitEstimate(70, 3))
        assertEquals("이동 시간 2시간 10분 · 휴식 2번", lines[1])
        assertTrue(lines.any { "반려견 측정 아님" in it })
        assertTrue(lines.last().startsWith("관찰 신고: 출발 1시간쯤"))
    }

    @Test
    fun `조사 - 받침 유무`() {
        assertEquals("콩이는", Josa.eunNeun("콩이"))
        assertEquals("밤은", Josa.eunNeun("밤"))
        assertEquals("밤이", Josa.iGa("밤"))
        assertEquals("Max는", Josa.eunNeun("Max"))
        assertEquals("반려견과", Josa.waGwa("반려견"))
    }

    @Test
    fun `프로필 검사`() {
        assertTrue(PetProfile("콩이").problems().isEmpty())
        assertEquals(3, PetProfile("", ageYears = 40, pottyIntervalMin = 10).problems().size)
    }
}
