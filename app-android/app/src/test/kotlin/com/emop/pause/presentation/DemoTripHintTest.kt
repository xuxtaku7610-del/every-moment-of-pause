package com.emop.pause.presentation

import com.emop.pause.uistate.DepartUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan DM-10: 심사위원 첫 실행 — 차량 신호가 없거나 에뮬레이터면 출발 전 화면에 시연 주행 안내 띠. */
class DemoTripHintTest {

    private fun DemoTripHint.at(sec: Int, vehicle: Boolean = true, onDepart: Boolean = true, signal: Boolean = false) =
        update(sec * 1000L, vehicle, onDepart, signal)

    @Test fun `신호 없음 10초가 지나야 띠가 뜬다`() {
        val h = DemoTripHint(emulator = false)
        assertFalse(h.at(0))
        assertFalse(h.at(9))
        assertTrue(h.at(10))
    }

    @Test fun `실차에서 속도나 기어가 오면 띠가 안 뜬다 (뒤에 끊겨도)`() {
        val h = DemoTripHint(emulator = false)
        assertFalse(h.at(0, signal = true))
        assertFalse(h.at(30, signal = true))
        assertFalse(h.at(60, signal = false))
    }

    @Test fun `에뮬레이터는 속도 0·기어 P가 와도 10초 뒤 띠`() {
        val h = DemoTripHint(emulator = true)
        assertFalse(h.at(0, signal = true))
        assertTrue(h.at(10, signal = true))
    }

    @Test fun `시뮬레이터 모드와 주행 화면에는 안 뜬다`() {
        val h = DemoTripHint(emulator = true)
        assertFalse(h.at(0, vehicle = false))
        assertFalse(h.at(20, vehicle = false))
        assertFalse(h.at(30, onDepart = false))
        // 출발 전 화면으로 돌아오면 다시 10초를 센다
        assertFalse(h.at(31))
        assertTrue(h.at(41))
    }

    @Test fun `한 번 닫으면 그 실행 동안 다시 안 뜬다`() {
        val h = DemoTripHint(emulator = true)
        h.at(0)
        assertTrue(h.at(10))
        h.dismiss()
        assertFalse(h.at(11))
        assertFalse(h.at(100))
    }

    @Test fun `에뮬레이터 판별은 Build 값만`() {
        assertTrue(DemoTripHint.isEmulator("ranchu", "Connect on x86_64 emulator", "Pleos/crp_avd/crp_avd_x86_64:14/x:user/test-keys"))
        assertTrue(DemoTripHint.isEmulator("goldfish", "", ""))
        assertFalse(DemoTripHint.isEmulator("qcom", "Pleos Connect", "Hyundai/car/car:14/x:user/release-keys"))
    }

    @Test fun `문구는 wording 기준 그대로`() {
        assertEquals("차량 신호가 없어요. 시연 주행으로 둘러볼까요?", DepartUi.DEMO_HINT)
        assertEquals("시연 주행 시작", DepartUi.DEMO_START)
        assertFalse(DepartUi(null, com.emop.pause.uistate.Fixtures.depart.pet, emptyList(), false, emptyList(), "").demoHint)
    }
}
