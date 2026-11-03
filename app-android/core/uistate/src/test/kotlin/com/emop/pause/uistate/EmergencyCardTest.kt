package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** 긴급 도움 응급 카드 (2026-11-03, test_plan F10-04·F10-06·F10-08): 형식, 금지어, 릴리스 미검수 차단. */
class EmergencyCardTest {
    private val dir = File("../../../content/care")
    private val ids = EmergencyUi.OPTIONS.map { it.id }
    private fun sheet(id: String) = CareSheet.parse(id, File(dir, "care_$id.md").readText())!!

    @Test
    fun `F10-04 응급 카드 4개 형식 — 첫 줄 바로 동물병원, 3줄, voice 40자, action VET, 검수 끝`() {
        assertEquals(listOf("breathing", "seizure", "bleeding", "toxin"), ids)
        ids.forEach { id ->
            val s = sheet(id)
            assertEquals(3, s.lines.size, id)
            assertEquals("바로 동물병원에 연락해요.", s.lines.first(), id)
            assertEquals("VET", s.action, id)
            assertEquals("emergency", s.category, id)
            assertNotNull(s.voice, id)
            assertTrue(s.voice!!.length <= 40, "$id voice ${s.voice!!.length}자")
            assertTrue(s.reviewed, "$id — 11/03 사용자 검수 끝 → reviewed=true")
            assertTrue(s.sources.isNotBlank(), id)
            assertTrue(s.lines.all { it.length <= 48 }, id)
        }
    }

    @Test
    fun `F10-04 약 이름·용량·진단 표현을 쓰지 않는다`() {
        val banned = Regex("mg|ml|정\\s*\\d|알약|약을|투약|복용|해열제|진통제|항구토|소화제|\\d+\\s*(mg|ml|cc)|진단|감지|측정|24시")
        ids.forEach { id ->
            val body = File(dir, "care_$id.md").readText().substringAfter("\n---")
            assertFalse(banned.containsMatchIn(body), "$id: ${banned.find(body)?.value}")
        }
    }

    @Test
    fun `F10-06 릴리스(발표 모드 아님)는 미검수 카드 본문·출처를 숨기고 경고 띠만`() {
        assertFalse(EmergencyCardUi.showDrafts(debugBuild = false, demoMode = false))
        assertTrue(EmergencyCardUi.showDrafts(debugBuild = true, demoMode = false))
        assertTrue(EmergencyCardUi.showDrafts(debugBuild = false, demoMode = true))
        ids.forEach { id ->
            // 차단 규칙은 그대로: 검수 전(reviewed=false) 카드면 릴리스에서 본문을 숨긴다
            val draft = sheet(id).copy(reviewed = false)
            val hidden = EmergencyCardUi.from(draft, showDrafts = false)
            assertTrue(hidden.hidden, id)
            assertEquals(emptyList<String>(), hidden.lines, id)
            assertNull(hidden.source, id)
            val shown = EmergencyCardUi.from(draft, showDrafts = true)
            assertFalse(shown.hidden, id)
            assertEquals(2, shown.lines.size, "$id — 첫 줄은 경고 띠에")
            assertFalse(shown.reviewed, id)
        }
        // 검수가 끝난 지금 카드(reviewed=true)는 릴리스에도 본문이 나온다 (11/03)
        ids.forEach { id ->
            val ok = EmergencyCardUi.from(sheet(id), showDrafts = false)
            assertFalse(ok.hidden, id)
            assertEquals(2, ok.lines.size, id)
            assertNotNull(ok.source, id)
        }
        assertEquals("바로 동물병원에 연락해요", EmergencyCardUi.LEAD)
        assertEquals("응급 판단이나 진단을 대신하지 않아요.", EmergencyUi.DISCLAIMER)
    }

    @Test
    fun `F10-06 출처 줄은 확인된 기관명만`() {
        assertEquals("출처: RSPCA", EmergencyCardUi.sourceLine(sheet("toxin").sources))
        assertEquals("출처: Merck Veterinary Manual", EmergencyCardUi.sourceLine(sheet("breathing").sources))
        assertNull(EmergencyCardUi.sourceLine("(비움) 사용자 확인 필요"))
    }

    @Test
    fun `F10-08 가까운 병원 - 번호 없으면 전화 확인 줄 숨김, 제휴 병원만 사전 접수 버튼`() {
        val noPhone = VetUi("v1", "가까운 병원", "1.2km", null, phone = null, sample = true)
        assertNull(noPhone.callNote)
        assertFalse(noPhone.intakeButton)
        val withPhone = noPhone.copy(phone = "033-000-0000")
        assertEquals(VetUi.CALL_NOTE, withPhone.callNote)
        assertFalse(withPhone.intakeButton)
        val partner = noPhone.copy(partner = true)
        assertTrue(partner.intakeButton)
        assertNull(partner.callNote)
    }
}
