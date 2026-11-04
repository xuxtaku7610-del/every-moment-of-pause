package com.emop.pause.engine.pet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * test_plan F9-20~23 (콩이 도우미 1단계 분류): 평가 세트 server/tests/eval/helper_eval.jsonl 전 문항을 규칙 분류에 넣는다.
 * 응급은 100% 응급 우선, 함정은 위험한 카드로 가지 않는다. 결과는 build/helper_rules.json에 남겨 서버 평가(tools/eval_helper.py)가 합친다.
 */
class HelperRouterTest {
    private val root = File("../../..")
    private val cards: List<HelperCard> = (
        File(root, "content/care").listFiles { f -> f.name.startsWith("care_") && f.extension == "md" }.orEmpty().toList() +
            File(root, "content/care/_drafts").listFiles { f -> f.name.startsWith("care_") && f.extension == "md" }.orEmpty().toList()
        ).sortedBy { it.name }
        .mapNotNull { f -> HelperRouter.parseCard(f.nameWithoutExtension.removePrefix("care_"), f.readText()) }
        // 응급 카드가 먼저 (같은 점수면 앞 카드)
        .sortedBy { if (it.category == "emergency") 0 else 1 }

    private data class Item(val id: String, val category: String, val q: String, val expect: String, val risky: Boolean)

    private val items: List<Item> = File(root, "server/tests/eval/helper_eval.jsonl").readLines().filter { it.isNotBlank() }.map { line ->
        fun str(k: String) = Regex("\"$k\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(line)!!.groupValues[1]
        Item(str("id"), str("category"), str("q"), str("expect"), Regex("\"risky\":\\s*true").containsMatchIn(line))
    }

    private fun label(r: HelperRoute): String = when (r) {
        is HelperRoute.Emergency -> "EMERGENCY:${r.cardId}"
        is HelperRoute.Card -> "CARD:${r.id}"
        is HelperRoute.App -> "APP:${r.feature}"
        is HelperRoute.Unknown -> "UNKNOWN"
    }

    @Test
    fun `F9-20 평가 세트 80문항 이상 · 범주 수`() {
        assertTrue(items.size >= 80, "${items.size}")
        val counts = items.groupingBy { it.category }.eachCount()
        assertTrue(counts.getValue("emergency") >= 20 && counts.getValue("care") >= 25 && counts.getValue("app") >= 15 && counts.getValue("out") >= 10 && counts.getValue("trap") >= 10, counts.toString())
        assertTrue(cards.size >= 20, "카드 ${cards.size}")
    }

    @Test
    fun `F9-21 규칙 분류 표 - 응급 100% 응급 우선, 전체 통과, 결과 파일`() {
        val rows = items.map { it to label(HelperRouter.route(it.q, cards)) }
        // 응급 문항은 모두 응급으로 (분류보다 먼저 병원 안내)
        rows.filter { it.first.category == "emergency" }.forEach { (i, got) -> assertTrue(got.startsWith("EMERGENCY:"), "${i.id} ${i.q} → $got") }
        // 기대 "A|B" = 1단계는 A(보통 UNKNOWN), 2단계가 B를 골라도 맞음
        val failed = rows.filter { (i, got) -> got != i.expect.split("|").first() }.map { (i, got) -> "${i.id} ${i.q} 기대 ${i.expect} → $got" }
        File("build").mkdirs()
        File("build/helper_rules.json").writeText(
            rows.joinToString(",\n", "[\n", "\n]") { (i, got) -> "{\"id\":\"${i.id}\",\"rule\":\"$got\",\"risky\":${HelperRouter.risky(i.q)}}" },
        )
        assertEquals(emptyList<String>(), failed)
    }

    @Test
    fun `F9-22 함정 - 사람 약·용량은 모름+병원 안내, 반려견 이름 초코·포도는 음식이 아니다`() {
        assertEquals(HelperRoute.Unknown(true), HelperRouter.route("사람 진통제 줘도 돼?", cards))
        assertEquals(HelperRoute.Unknown(true), HelperRouter.route("약 몇 알 먹여?", cards))
        assertEquals(HelperRoute.Card("barking"), HelperRouter.route("초코가 짖어", cards))
        assertEquals(HelperRoute.Unknown(false), HelperRouter.route("포도가 밥을 안 먹어", cards))
        assertEquals(HelperRoute.Card("toxin"), HelperRouter.route("소금물로 토하게 해도 돼?", cards))
        assertEquals(HelperRoute.Unknown(false), HelperRouter.route("오늘 날씨 어때?", cards))
    }

    @Test
    fun `F9-23 질문인지 - 신고와 도우미 질문을 가른다`() {
        assertTrue(HelperRouter.isQuestion("토하면 어떻게 해?"))
        assertTrue(HelperRouter.isQuestion("쉬할 데 있어"))
        assertTrue(!HelperRouter.isQuestion("콩이 토했어"))
        assertTrue(!HelperRouter.isQuestion("콩이가 헥헥거려"))
        assertEquals(listOf("a", "b c"), HelperRouter.parseList("[a, b c]"))
    }
}
