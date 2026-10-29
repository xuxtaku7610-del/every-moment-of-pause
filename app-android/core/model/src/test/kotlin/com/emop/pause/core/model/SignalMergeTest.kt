package com.emop.pause.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** COMMERCIAL_READINESS A P0: 신호별 출처 기록, SDK에 없는 속도는 AAOS 속성으로. */
class SignalMergeTest {
    private val pleos = SourceReading(
        SignalSource.PLEOS_SDK,
        setOf(Signal.GEAR, Signal.BATTERY_SOC, Signal.HVAC_TEMP),
        SignalSample(1, gear = Gear.PARK, batterySocPercent = 40, hvacTempC = 22f),
    )
    private val aaos = SourceReading(
        SignalSource.AAOS_PROPERTY,
        setOf(Signal.SPEED, Signal.GEAR, Signal.PARKING_BRAKE, Signal.BATTERY_SOC, Signal.CHARGING),
        SignalSample(1, speedKph = 0f, gear = Gear.DRIVE, parkingBrakeOn = true, batterySocPercent = 41, charging = true),
    )
    private val sim = SourceReading(SignalSource.SIM, Signal.entries.toSet(), SignalSample(1, speedKph = 99f, hvacAcOn = true, recirculationOn = false))

    @Test
    fun `T2-5 SDK에 없는 속도·주차 브레이크·충전은 AAOS 속성, 나머지는 SDK 우선`() {
        val m = SignalMerge.merge(listOf(pleos, aaos), 5)
        assertEquals(SignalSource.AAOS_PROPERTY, m.sourceOf(Signal.SPEED))
        assertEquals(SignalSource.PLEOS_SDK, m.sourceOf(Signal.GEAR))
        assertEquals(Gear.PARK, m.sample.gear)
        assertEquals(40, m.sample.batterySocPercent)
        assertEquals(true, m.sample.parkingBrakeOn)
        assertEquals(true, m.sample.charging)
        assertEquals(5, m.sample.timestampMs)
    }

    @Test
    fun `T2-5 어디에도 없는 신호는 NONE이고 값은 null`() {
        val m = SignalMerge.merge(listOf(pleos, aaos), 5)
        assertEquals(SignalSource.NONE, m.sourceOf(Signal.HVAC_AC))
        assertNull(m.sample.hvacAcOn)
        assertEquals(setOf(Signal.SPEED, Signal.GEAR, Signal.PARKING_BRAKE, Signal.BATTERY_SOC, Signal.CHARGING, Signal.HVAC_TEMP), m.capabilities)
    }

    @Test
    fun `시연 보충을 켜면 빈 신호만 시뮬레이터로 채운다`() {
        val m = SignalMerge.merge(listOf(pleos, aaos, sim), 5)
        assertEquals(SignalSource.SIM, m.sourceOf(Signal.HVAC_AC))
        assertEquals(true, m.sample.hvacAcOn)
        assertEquals(0f, m.sample.speedKph) // 실차 값이 있으면 시뮬레이터 값(99)을 쓰지 않는다
    }

    @Test
    fun `지원한다고 알린 공급원 값이 아직 null이면 다른 출처 값을 섞지 않는다`() {
        val empty = pleos.copy(sample = SignalSample(1))
        val m = SignalMerge.merge(listOf(empty, aaos), 5)
        assertEquals(SignalSource.PLEOS_SDK, m.sourceOf(Signal.GEAR))
        assertNull(m.sample.gear)
    }
}
