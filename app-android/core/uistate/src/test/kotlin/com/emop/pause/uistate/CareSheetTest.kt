package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** UPGRADE 7 (test_plan F9-05): content/care 체크리스트 — 형식, 3~4줄, 약 이름·용량 없음, 검수 전 표시. */
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
            assertFalse(sheet.reviewed, "$id — 사용자 검수 전에는 reviewed=false")
        }
    }

    @Test
    fun `F9-05 약 이름·용량을 쓰지 않고, 열 관련은 얼음물 금지·미지근한 물`() {
        val banned = Regex("mg|ml|정\\s*\\d|알약|약을|투약|복용|해열제|진통제|항구토|소화제|\\d+\\s*(mg|ml|cc)")
        ids.forEach { id ->
            val text = File(dir, "care_$id.md").readText()
            assertFalse(banned.containsMatchIn(text.substringAfter("\n---")), "$id: ${banned.find(text.substringAfter("\n---"))?.value}")
        }
        val hot = CareSheet.parse("panting_hot", File(dir, "care_panting_hot.md").readText())!!
        assertTrue(hot.lines.any { "얼음물" in it && "미지근한" in it && "병원" in it })
        assertEquals("일반 안내예요. 심하면 동물병원에 연락하세요", CareSheet.DISCLAIMER)
    }
}
