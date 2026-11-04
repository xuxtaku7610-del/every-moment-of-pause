package com.emop.pause.trip

import com.emop.pause.engine.pet.HelperFeature
import com.emop.pause.engine.pet.HelperRoute
import com.emop.pause.engine.pet.HelperRouter
import com.emop.pause.uistate.CareSheet
import com.emop.pause.uistate.EmergencyCardUi
import com.emop.pause.uistate.HelperUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * test_plan F9-30~35 (콩이 도우미 답): **릴리스 빌드는 검수 전 카드(reviewed: false)를 답으로 쓰지 않는다**, 앱 데이터는 지어내지 않는다,
 * 주행 중 음성은 40자 이하. 카드는 저장소의 content/care(검수본)와 content/care/_drafts(초안, 디버그 assets에만)를 그대로 읽는다.
 */
class HelperAnswersTest {
    private val root = File("../..")
    private val reviewedFiles = File(root, "content/care").listFiles { f -> f.name.startsWith("care_") && f.extension == "md" }.orEmpty().toList()
    private val draftFiles = File(root, "content/care/_drafts").listFiles { f -> f.name.startsWith("care_") && f.extension == "md" }.orEmpty().toList()
    private fun texts(files: List<File>) = files.associate { it.nameWithoutExtension.removePrefix("care_") to it.readText() }
    private val all = texts(reviewedFiles) + texts(draftFiles)
    private val sheets = all.mapValues { (id, t) -> CareSheet.parse(id, t)!! }

    @Test
    fun `F9-30 릴리스 카드 목록에 검수 전 카드가 없다 - 규칙 분류도 서버 허용 목록도 못 고른다`() {
        assertTrue(draftFiles.isNotEmpty())
        // 초안 폴더는 모두 reviewed: false, 검수본 폴더는 모두 true (섞이면 릴리스 assets로 새어 나간다)
        draftFiles.forEach { assertFalse(sheets.getValue(it.nameWithoutExtension.removePrefix("care_")).reviewed, it.name) }
        reviewedFiles.forEach { assertTrue(sheets.getValue(it.nameWithoutExtension.removePrefix("care_")).reviewed, it.name) }
        // 릴리스 assets에 초안 파일이 들어와도(설정 실수) 목록에서 빠진다
        val release = HelperAnswers.catalog(all, showDrafts = false)
        val debug = HelperAnswers.catalog(all, showDrafts = true)
        assertEquals(reviewedFiles.size, release.size)
        assertTrue(release.none { !sheets.getValue(it.id).reviewed })
        assertEquals(all.size, debug.size)
        assertEquals("emergency", debug.first().category)
        // "물은 얼마나 줘?"는 디버그에서 초안 water 카드, 릴리스에서는 모름 → 쉼터 안내
        assertEquals(HelperRoute.Card("water"), HelperRouter.route("물은 얼마나 줘?", debug))
        assertEquals(HelperRoute.Unknown(false), HelperRouter.route("물은 얼마나 줘?", release))
    }

    @Test
    fun `F9-31 릴리스에서 검수 전 카드 본문은 답에 나오지 않는다 - VET이면 병원 한 줄, 아니면 검수 중 + 쉼터`() {
        draftFiles.map { sheets.getValue(it.nameWithoutExtension.removePrefix("care_")) }.forEach { s ->
            val r = HelperAnswers.card("q", s, showDrafts = false)
            val shown = r.ui.lines + r.voice
            s.lines.forEach { line -> assertTrue(shown.none { it.contains(line) }, "${s.id}: $line") }
            assertFalse(r.ui.reviewed)
            if (s.action == "VET") {
                assertEquals(listOf(EmergencyCardUi.LEAD), r.ui.lines)
                assertEquals("VET", r.ui.action)
            } else {
                assertEquals(listOf(HelperAnswers.DRAFT_HIDDEN, HelperUi.UNKNOWN_REST), r.ui.lines)
            }
            // 서버 설명이 와도 릴리스에서는 쓰지 않는다
            val ex = HelperAnswers.card("q", s, showDrafts = false, explained = "지어낸 설명이에요.")
            assertFalse(ex.ui.explained)
        }
        // 디버그(발표·시뮬레이터 포함)에서는 본문 + "검수 전" 표시
        val water = HelperAnswers.card("q", sheets.getValue("water"), showDrafts = true)
        assertEquals(sheets.getValue("water").lines.take(4), water.ui.lines)
        assertFalse(water.ui.reviewed)
    }

    @Test
    fun `F9-32 앱 데이터 답은 가진 값만 - 실내 온도가 없으면 숫자를 지어내지 않는다`() {
        val sheet = { id: String -> sheets[id] }
        val none = HelperAnswers.app("차 안 몇 도야?", HelperFeature.CABIN_TEMP, HelperFacts(), sheet, showDrafts = false)
        assertEquals("이 차는 실내 온도를 알려 주지 않아요", none.ui.lines.first())
        assertTrue(none.ui.lines.none { Regex("\\d").containsMatchIn(it) })
        val hot = HelperAnswers.app("차 안 몇 도야?", HelperFeature.CABIN_TEMP, HelperFacts(cabinTempC = 27.6f), sheet, showDrafts = true)
        assertEquals("지금 실내 28℃예요", hot.ui.lines.first())
        val potty = HelperAnswers.app("쉬할 데 있어?", HelperFeature.POTTY_SPOT, HelperFacts(pottySpot = PottySpot("s1", "가평휴게소", "12km 앞", 37.0, 127.0, "반려견 놀이터 (휴게소 데이터)")), sheet, false)
        assertEquals("POTTY_SPOT", potty.ui.action)
        val nothing = HelperAnswers.app("쉬할 데 있어?", HelperFeature.POTTY_SPOT, HelperFacts(), sheet, false)
        assertNull(nothing.ui.action)
        val rest = HelperAnswers.app("다음 휴게소 언제야?", HelperFeature.NEXT_REST, HelperFacts(nextRest = "횡성휴게소" to 75), sheet, false)
        assertEquals(listOf("횡성휴게소 · 1시간 15분 뒤"), rest.ui.lines)
    }

    @Test
    fun `F9-33 주행 중 음성은 40자 이하 - 카드 voice 줄 또는 첫 문장`() {
        sheets.values.forEach { s ->
            val r = HelperAnswers.card("q", s, showDrafts = true)
            assertTrue(r.voice.length <= HelperAnswers.VOICE_MAX, "${s.id}: ${r.voice}")
            assertTrue(r.voice.isNotBlank(), s.id)
        }
        val u = HelperAnswers.unknown("q", risky = true)
        assertEquals("${HelperUi.UNKNOWN} ${HelperUi.UNKNOWN_VET}", u.voice)
        assertEquals("VET", u.ui.action)
    }

    @Test
    fun `F9-34 응급 말은 도우미 분류에서도 응급 - 기존 긴급 도움 흐름으로`() {
        val release = HelperAnswers.catalog(all, showDrafts = false)
        listOf("콩이가 초콜릿 먹은 것 같아", "숨을 못 쉬어", "경련해", "피가 나").forEach { q ->
            assertTrue(HelperRouter.route(q, release) is HelperRoute.Emergency, q)
        }
        // 릴리스: 응급 카드가 검수 전이어도 본문 없이 병원 안내
        val r = HelperAnswers.build("q", HelperRoute.Emergency(null, "toxin"), HelperFacts(), { sheets[it]?.copy(reviewed = false) }, showDrafts = false)
        assertEquals(listOf(EmergencyCardUi.LEAD), r.ui.lines)
        assertTrue(r.ui.emergency)
    }

    @Test
    fun `F9-35 카드 본문에 약 이름·용량이 없다`() {
        val bad = Regex("mg|밀리그램|정\\s*\\d|알씩|타이레놀|아스피린|이부프로펜|아세트아미노펜|진통제|해열제")
        sheets.values.forEach { s -> s.lines.forEach { assertFalse(bad.containsMatchIn(it), "${s.id}: $it") } }
    }

    @Test
    fun `F9-37 릴리스 APK 설정 - 초안 폴더는 디버그 sourceSet에만, main은 _drafts·_candidates 폴더를 뺀다`() {
        val gradle = File("build.gradle.kts").readText()
        // ignoreAssetsPatterns를 정하면 aapt 기본 "<dir>_*"가 사라진다 → 직접 적어야 main assets의 content/care/_drafts가 빠진다
        assertTrue(gradle.contains("\"!<dir>_drafts\"") && gradle.contains("\"!<dir>_candidates\""))
        val draftDirs = Regex("""sourceSets\.getByName\("(\w+)"\)\.assets\.srcDir\("[^"]*_drafts"\)""").findAll(gradle).map { it.groupValues[1] }.toList()
        assertEquals(listOf("debug"), draftDirs)
    }
}
