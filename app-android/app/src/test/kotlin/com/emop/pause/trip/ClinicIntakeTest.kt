package com.emop.pause.trip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** UPGRADE 3 동물병원 사전 접수 + 실시간 도착 알림 (test_plan F5-07~10). */
class ClinicIntakeTest {
    private val base = ClinicIntake("tok", "http://h/clinic/c", "http://h/eta/tok", "여주동물병원", etaMin = 14, sentEtaMin = 14, sentAtMs = 0, startClockMin = 600)

    @Test fun `F5-08 도착 예정은 1분 이상 바뀌었거나 60초가 지나면 보낸다`() {
        assertFalse(ClinicIntake.shouldPush(base, etaMin = 14, nowMs = 10_000))
        assertFalse(ClinicIntake.shouldPush(base, etaMin = 13, nowMs = 10_000)) // 너무 잦게는 안 보냄(15초 간격)
        assertTrue(ClinicIntake.shouldPush(base, etaMin = 13, nowMs = 15_000))
        assertTrue(ClinicIntake.shouldPush(base, etaMin = 14, nowMs = 60_000))
        assertFalse(ClinicIntake.shouldPush(base.copy(arrived = true), etaMin = 0, nowMs = 600_000))
    }

    @Test fun `F5-08 시뮬레이터 도착 예정은 처음 예상에서 주행 시간만큼 줄어든다`() {
        assertEquals(14, ClinicIntake.simEta(base, clockMin = 600))
        assertEquals(9, ClinicIntake.simEta(base, clockMin = 605))
        assertEquals(0, ClinicIntake.simEta(base, clockMin = 640))
    }

    @Test fun `F5-09 병원 응답 코드를 문구로 바꾸고 모르는 값은 무시`() {
        assertEquals(ClinicReply.READY, ClinicReply.of("ready"))
        assertNull(ClinicReply.of("xyz"))
        assertNull(ClinicReply.of(null))
        // 주행 중 한 줄(24자) 안
        ClinicReply.entries.forEach { assertTrue(it.drive.length <= 24, it.drive) }
    }

    @Test fun `F5-09 상태 한 줄 - 응답이 먼저, 그다음 열람, 도착`() {
        assertEquals("병원 접수함에 보냈어요 · 14분 뒤 도착", ClinicIntake.status(base))
        assertEquals("병원이 접수함을 열어 봤어요 · 14분 뒤 도착", ClinicIntake.status(base.copy(seen = true)))
        assertEquals("병원: 도착하면 바로 볼게요 · 14분 뒤 도착", ClinicIntake.status(base.copy(seen = true, reply = ClinicReply.READY)))
        assertEquals("병원에 도착했어요", ClinicIntake.status(base.copy(arrived = true, etaMin = 0)))
    }
}
