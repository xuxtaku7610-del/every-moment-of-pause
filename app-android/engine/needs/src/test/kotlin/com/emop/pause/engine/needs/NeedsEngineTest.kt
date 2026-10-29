package com.emop.pause.engine.needs

import com.emop.pause.core.model.Signal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NeedsEngineTest {
    private val ev = setOf(Signal.BATTERY_SOC)

    private fun NeedsEngine.tick(sec: Int, paused: Boolean = false, soc: Int? = null, remainKm: Int? = null, remainMin: Int? = null, caps: Set<Signal> = ev) =
        update(NeedsInput(sec * 1000L, paused, caps, soc, remainKm?.let { it * 1000 }, remainMin?.let { it * 60 }))

    private fun NeedsEngine.runMinutes(fromMin: Int, minutes: Int, paused: Boolean = false): NeedsResult {
        var r: NeedsResult? = null
        for (m in fromMin..fromMin + minutes) r = tick(m * 60, paused)
        return r!!
    }

    @Test
    fun `P0 5세 아이 화장실과 반려견 산책 타이머가 각자 진행된다`() {
        val e = NeedsEngine(listOf(Passenger.Child(5), Passenger.Dog()))
        val r = e.runMinutes(0, 60)
        val toilet = r.needs.first { it.kind == NeedKind.TOILET }
        val walk = r.needs.first { it.kind == NeedKind.PET_WALK }
        assertEquals(60.0 / 90, toilet.urgency, 1e-6)
        assertEquals(30, toilet.dueInMin)
        assertEquals(0.5, walk.urgency, 1e-6)
        assertEquals(60, walk.dueInMin)
        assertEquals(NeedKind.TOILET, r.needs.first().kind, "긴급한 순서")
    }

    @Test
    fun `긴급도는 1을 넘지 않는다`() {
        val r = NeedsEngine(listOf(Passenger.Child(5))).runMinutes(0, 200)
        assertEquals(1.0, r.needs.single().urgency)
        assertEquals(0, r.needs.single().dueInMin)
    }

    @Test
    fun `P1 휴게소 멈춤 5분이 지나면 타이머 리셋, 신호 대기 같은 짧은 멈춤은 유지`() {
        val e = NeedsEngine(listOf(Passenger.Child(5)))
        e.runMinutes(0, 60)
        val short = e.runMinutes(61, 3, paused = true)
        assertEquals(60.0 / 90, short.needs.single().urgency, 1e-6)
        val reset = e.runMinutes(65, 2, paused = true)
        assertEquals(0.0, reset.needs.single().urgency)
    }

    @Test
    fun `P1 동승자 없음이면 니즈 타이머가 없다`() {
        val r = NeedsEngine(emptyList()).runMinutes(0, 180)
        assertTrue(r.needs.isEmpty())
    }

    @Test
    fun `P0 배터리가 목적지까지 부족하면 충전 니즈를 만든다`() {
        // seoul-busan-family 출발: 82%, 395km. 예비 10%까지 360km만 갈 수 있음.
        val e = NeedsEngine(emptyList(), EvProfile())
        val r = e.tick(0, soc = 82, remainKm = 395, remainMin = 270)
        val charge = r.needs.single()
        assertEquals(NeedKind.CHARGE, charge.kind)
        assertEquals(360.0, charge.reachKm!!, 1e-6)
        assertEquals((360 / (395.0 / 270)).toInt(), charge.dueInMin)
        assertTrue(r.chargeInfoAvailable)
    }

    @Test
    fun `충전 후 목적지까지 충분하면 충전 니즈가 없다`() {
        // 충전 완료: 80%, 245km 남음 → 350km 갈 수 있음.
        val r = NeedsEngine(emptyList(), EvProfile()).tick(0, soc = 80, remainKm = 245, remainMin = 180)
        assertNull(r.needs.firstOrNull { it.kind == NeedKind.CHARGE })
    }

    @Test
    fun `충전 긴급도는 예비 잔량까지 남은 시간이 짧을수록 높다`() {
        val far = NeedsEngine(emptyList(), EvProfile()).tick(0, soc = 40, remainKm = 300, remainMin = 180).needs.single()
        val near = NeedsEngine(emptyList(), EvProfile()).tick(0, soc = 15, remainKm = 300, remainMin = 180).needs.single()
        assertTrue(near.urgency > far.urgency)
        assertEquals(1.0, NeedsEngine(emptyList(), EvProfile()).tick(0, soc = 10, remainKm = 300).needs.single().urgency)
    }

    @Test
    fun `P1 배터리 신호 미지원이면 충전 니즈를 만들지 않고 충전 정보 없음`() {
        val r = NeedsEngine(listOf(Passenger.Dog()), EvProfile()).tick(0, soc = null, remainKm = 300, caps = setOf(Signal.SPEED))
        assertFalse(r.chargeInfoAvailable)
        assertNotNull(r.needs.single { it.kind == NeedKind.PET_WALK })
    }

    @Test
    fun `내연기관 차는 충전 니즈가 없다`() {
        val r = NeedsEngine(emptyList(), ev = null).tick(0, soc = 5, remainKm = 300)
        assertFalse(r.chargeInfoAvailable)
        assertTrue(r.needs.isEmpty())
    }

    @Test
    fun `나이별 간격`() {
        assertEquals(NeedKind.NURSING to 150, NeedsEngine.intervalFor(1))
        assertEquals(NeedKind.TOILET to 90, NeedsEngine.intervalFor(5))
        assertEquals(NeedKind.TOILET to 120, NeedsEngine.intervalFor(9))
    }
}
