package com.emop.pause.engine.pet

import com.emop.pause.core.model.DoorState
import com.emop.pause.core.model.Gear
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 문 이벤트 (차량 이벤트 연결 2026-11-02, test_plan F6-06~F6-09). */
class DoorWatchTest {
    private val closed = DoorState.CLOSED
    private val driver = closed.copy(driverOpen = true)
    private val rear = closed.copy(rearLeftOpen = true)

    private fun arrive(doors: DoorState?, leftConfirmed: Boolean = false, gear: Gear = Gear.PARK) =
        DoorInput(gear, doors = doors, tripActive = true, tripEnding = true, leftConfirmed = leftConfirmed)

    private fun restStop(doors: DoorState?, waitActive: Boolean = false, gear: Gear = Gear.PARK) =
        DoorInput(gear, doors = doors, tripActive = true, tripEnding = false, waitActive = waitActive)

    @Test
    fun `F6-06 도착 P단에서 뒷문이 열리면 내렸나요를 바로 (정차마다 한 번)`() {
        val w = DoorWatch()
        assertEquals(emptyList<DoorEvent>(), w.tick(arrive(closed)))
        assertEquals(listOf(DoorEvent.ARRIVAL_CHECK), w.tick(arrive(rear)))
        w.tick(arrive(closed))
        assertEquals(emptyList<DoorEvent>(), w.tick(arrive(rear)), "같은 정차에서 다시 묻지 않는다")
    }

    @Test
    fun `F6-06 BoardingCheck는 문 이벤트로 60초를 기다리지 않고 묻고, 한 번만 묻는다`() {
        val b = BoardingCheck()
        val i = ArrivalInput(nowMs = 1_000, gear = Gear.PARK, tripEnding = true, cabinTempC = 22f, month = 3)
        assertNull(b.tick(i), "보통은 60초를 기다린다")
        val p = b.promptNow(i)
        assertNotNull(p)
        assertEquals(p, b.pending)
        assertNull(b.promptNow(i))
        assertNull(b.tick(i.copy(nowMs = 120_000)), "60초가 지나도 다시 묻지 않는다")
        assertNull(BoardingCheck().promptNow(i.copy(tripEnding = false)), "휴게소 정차에서는 묻지 않는다")
        assertNull(BoardingCheck().promptNow(i.copy(gear = Gear.DRIVE)))
    }

    @Test
    fun `F6-07 휴게소 P단에서 운전석 문이 열리면 대기 모드 제안 한 번만 (대기 중이면 없음)`() {
        val w = DoorWatch()
        w.tick(restStop(closed))
        assertEquals(listOf(DoorEvent.WAIT_OFFER), w.tick(restStop(driver)))
        w.tick(restStop(closed))
        assertEquals(emptyList<DoorEvent>(), w.tick(restStop(driver)), "같은 정차에서 다시 제안하지 않는다")

        val waiting = DoorWatch()
        waiting.tick(restStop(closed, waitActive = true))
        assertEquals(emptyList<DoorEvent>(), waiting.tick(restStop(driver, waitActive = true)))
    }

    @Test
    fun `F6-07 다시 출발했다가 다음 정차에서는 또 제안한다, 주행 중 문은 무시`() {
        val w = DoorWatch()
        w.tick(restStop(closed))
        w.tick(restStop(driver))
        w.tick(restStop(closed, gear = Gear.DRIVE))
        assertEquals(emptyList<DoorEvent>(), w.tick(restStop(driver, gear = Gear.DRIVE)), "주행 기어에서는 아무것도 하지 않는다")
        w.tick(restStop(closed))
        assertEquals(listOf(DoorEvent.WAIT_OFFER), w.tick(restStop(driver)))
    }

    @Test
    fun `F6-08 도착 후 운전석 문이 열렸는데 답이 없으면 안전 안내 한 번 (여행마다 한 번)`() {
        val w = DoorWatch()
        w.tick(arrive(closed))
        assertEquals(listOf(DoorEvent.LEFT_BEHIND), w.tick(arrive(driver)))
        w.tick(arrive(closed))
        assertEquals(emptyList<DoorEvent>(), w.tick(arrive(driver)), "반복 알림 없음")
    }

    @Test
    fun `F6-08 내렸어요를 눌렀어도 뒷문이 한 번도 안 열렸으면 한 번, 뒷문이 열렸으면 없음`() {
        val never = DoorWatch()
        never.tick(arrive(closed, leftConfirmed = true))
        assertEquals(listOf(DoorEvent.LEFT_BEHIND), never.tick(arrive(driver, leftConfirmed = true)))

        val opened = DoorWatch()
        opened.tick(arrive(closed))
        opened.tick(arrive(rear))
        opened.tick(arrive(closed, leftConfirmed = true))
        assertEquals(emptyList<DoorEvent>(), opened.tick(arrive(driver, leftConfirmed = true)))
    }

    @Test
    fun `F6-08 뒷문 신호가 없으면 답 여부만 본다`() {
        val frontOnly = DoorState(driverOpen = false)
        val w = DoorWatch()
        w.tick(arrive(frontOnly, leftConfirmed = true))
        assertEquals(emptyList<DoorEvent>(), w.tick(arrive(frontOnly.copy(driverOpen = true), leftConfirmed = true)))
        val u = DoorWatch()
        u.tick(arrive(frontOnly))
        assertEquals(listOf(DoorEvent.LEFT_BEHIND), u.tick(arrive(frontOnly.copy(driverOpen = true))))
    }

    @Test
    fun `F6-09 문 신호가 없으면(null) 문 이벤트는 모두 꺼진다`() {
        val w = DoorWatch()
        repeat(3) {
            assertEquals(emptyList<DoorEvent>(), w.tick(arrive(null)))
            assertEquals(emptyList<DoorEvent>(), w.tick(restStop(null)))
        }
    }

    @Test
    fun `F6-09 처음 받은 값이 열림이면 바뀐 순간이 아니라서 알리지 않고, 출발 전에는 아무것도 하지 않는다`() {
        val w = DoorWatch()
        assertEquals(emptyList<DoorEvent>(), w.tick(arrive(driver)))
        val before = DoorWatch()
        before.tick(DoorInput(Gear.PARK, doors = closed, tripActive = false, tripEnding = false))
        assertEquals(emptyList<DoorEvent>(), before.tick(DoorInput(Gear.PARK, doors = driver, tripActive = false, tripEnding = false)))
    }

    @Test
    fun `F6-09 문 위치 값은 0만 닫힘 (실차 확인 예정)`() {
        assertEquals(false, DoorState.isOpen(0))
        assertEquals(true, DoorState.isOpen(1))
        assertEquals(true, DoorState.isOpen(100))
        assertNull(DoorState.isOpen(null))
        assertNull(DoorState().rearOpen)
        assertTrue(DoorState(rearRightOpen = true).rearOpen!!)
    }
}
