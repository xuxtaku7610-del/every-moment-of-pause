package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** UPGRADE 7 (test_plan F9-05): content/care 체크리스트 — 형식, 3~4줄, 약 이름·용량 없음, 11/03 사용자 검수 끝(reviewed=true). */
class CareSheetTest {
    private val dir = File("../../../content/care")
    private val ids = listOf("panting_hot", "panting_anxious", "nausea", "vomit", "anxious", "climbing", "emergency", "unsure")

    @Test
    fun `F9-05 모든 증상의 체크리스트가 있고 형식이 맞다`() {
        ids.forEach { id ->
            val sheet = CareSheet.parse(id, File(dir, "care_$id.md").readText())
            assertNotNull(sheet, id)
            sheet!!
            assertTrue(sheet.lines.size in 3..4, "$id ${sheet.lines.size}줄")
            assertTrue(sheet.sources.isNotBlank(), "$id 출처")
            assertTrue(sheet.reviewed, "$id — 11/03 사용자 검수 끝 → reviewed=true")
            assertFalse("사용자 확인 필요" in sheet.sources, "$id 출처에 확인 필요가 남음")
            assertFalse("AVMA" in sheet.sources, "$id — AVMA는 본문을 읽지 못해 출처에서 뺌")
        }
    }

    @Test
    fun `F9-05 약 이름·용량을 쓰지 않고, 열 관련은 시원한 물·담그지 않음`() {
        val banned = Regex("mg|ml|정\\s*\\d|알약|약을|투약|복용|해열제|진통제|항구토|소화제|\\d+\\s*(mg|ml|cc)")
        ids.forEach { id ->
            val text = File(dir, "care_$id.md").readText()
            assertFalse(banned.containsMatchIn(text.substringAfter("\n---")), "$id: ${banned.find(text.substringAfter("\n---"))?.value}")
        }
        val hot = CareSheet.parse("panting_hot", File(dir, "care_panting_hot.md").readText())!!
        assertTrue(hot.lines.any { "시원한 물" in it && "담그지는 않아요" in it && it.endsWith("바로 동물병원에 연락해요.") })
        ids.forEach { id -> assertFalse("미지근" in File(dir, "care_$id.md").readText(), "$id 미지근한 물 → 시원한 물") }
        assertEquals("일반 안내예요. 심하면 동물병원에 연락하세요", CareSheet.DISCLAIMER)
    }
}
