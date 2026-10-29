package com.emop.pause.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SignalSmootherTest {
    private fun at(sec: Int, speed: Float? = null, gear: Gear? = null, charging: Boolean? = null) =
        SignalSample(timestampMs = sec * 1000L, speedKph = speed, gear = gear, charging = charging)

    @Test
    fun `T6-15 속도는 3초 이동평균, 0은 바로 0`() {
        val f = SignalSmoother()
        assertEquals(90f, f.apply(at(0, speed = 90f)).speedKph!!, 0.01f)
        assertEquals(100f, f.apply(at(1, speed = 110f)).speedKph!!, 0.01f)
        assertEquals(100f, f.apply(at(2, speed = 100f)).speedKph!!, 0.01f)
        // 0초 값(90)은 창 밖 → (110+100+60)/3
        assertEquals(90f, f.apply(at(3, speed = 60f)).speedKph!!, 0.01f)
        assertEquals(0f, f.apply(at(4, speed = 0f)).speedKph!!, 0.01f)
        assertNull(f.apply(at(5)).speedKph)
    }

    @Test
    fun `T6-16 기어 깜빡임은 2초 디바운스`() {
        val f = SignalSmoother()
        assertEquals(Gear.DRIVE, f.apply(at(0, gear = Gear.DRIVE)).gear)
        // 1초만 N으로 튀었다 돌아옴 → 무시
        assertEquals(Gear.DRIVE, f.apply(at(1, gear = Gear.NEUTRAL)).gear)
        assertEquals(Gear.DRIVE, f.apply(at(2, gear = Gear.DRIVE)).gear)
        // P가 2초 유지되면 확정
        assertEquals(Gear.DRIVE, f.apply(at(3, gear = Gear.PARK)).gear)
        assertEquals(Gear.DRIVE, f.apply(at(4, gear = Gear.PARK)).gear)
        assertEquals(Gear.PARK, f.apply(at(5, gear = Gear.PARK)).gear)
    }

    @Test
    fun `T6-17 충전 상태 변화는 5초 유지해야 확정`() {
        val f = SignalSmoother()
        assertEquals(false, f.apply(at(0, charging = false)).charging)
        for (t in 1..5) assertEquals(false, f.apply(at(t, charging = true)).charging)
        assertEquals(true, f.apply(at(6, charging = true)).charging)
        // 끊김이 3초만 이어지면 충전 중 유지
        for (t in 7..9) assertEquals(true, f.apply(at(t, charging = false)).charging)
        assertEquals(true, f.apply(at(10, charging = true)).charging)
    }

    @Test
    fun `첫 값과 값 없음은 그대로`() {
        val f = SignalSmoother()
        assertEquals(Gear.PARK, f.apply(at(0, gear = Gear.PARK)).gear)
        assertNull(f.apply(at(1)).gear)
        assertEquals(Gear.DRIVE, f.apply(at(2, gear = Gear.DRIVE)).gear)
    }
}
