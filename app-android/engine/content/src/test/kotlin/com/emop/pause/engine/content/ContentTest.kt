package com.emop.pause.engine.content

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ContentTest {
    private val drafts = ContentLibrary.parseEpisodes(File("../../../content/drafts/episodes.json").readText())
    private val draftMissions = ContentLibrary.parseMissions(File("../../../content/drafts/missions.json").readText())

    private fun ep(id: String, sec: Int, reviewed: Boolean = true, minAge: Int = 4, maxAge: Int = 12) =
        Episode(id, "proverb", id, sec, minAge, maxAge, reviewed)

    @Test
    fun `F P0 초안 20편·미션 15개, 모두 reviewed=false`() {
        assertEquals(20, drafts.size)
        assertEquals(15, draftMissions.size)
        assertTrue(drafts.none { it.reviewed })
        assertTrue(draftMissions.none { it.reviewed })
        assertEquals(setOf("proverb", "common_sense", "safety", "manners"), drafts.map { it.topic }.toSet())
        // TTS 낭독 규칙 (F P1): 한 문장 40자 이하
        assertTrue(drafts.all { e -> e.lines.all { it.length <= 40 } })
        assertTrue(drafts.all { it.durationSec in 30..900 && it.quiz != null })
    }

    @Test
    fun `T5-8 릴리스는 검수본만, 디버그는 초안도 (DRAFT 표시)`() {
        val release = ContentLibrary.of(emptyList(), drafts, emptyList(), draftMissions, allowDrafts = false)
        assertTrue(release.episodes.isEmpty())
        assertTrue(release.missions.isEmpty())
        val debug = ContentLibrary.of(emptyList(), drafts, emptyList(), draftMissions, allowDrafts = true)
        assertEquals(20, debug.episodes.size)
        assertTrue(debug.isDraft(debug.episodes.first()))
        // 같은 ID는 검수본이 이긴다
        val reviewed = drafts.first().copy(reviewed = true)
        val mixed = ContentLibrary.of(listOf(reviewed), drafts, emptyList(), emptyList(), allowDrafts = false)
        assertEquals(listOf(reviewed.id), mixed.episodes.map { it.id })
    }

    @Test
    fun `T5-2 T9-7 목표 이하에서 가장 가깝게 - 0·1 배낭`() {
        val eps = listOf(ep("a", 300), ep("b", 450), ep("c", 300), ep("d", 240), ep("e", 600))
        val p = PlaylistPlanner.plan(eps, 1080, age = 5)
        assertEquals(1050, p.totalSec) // 300+450+300 또는 450+600? 600+450=1050 → 편수 적은 쪽
        assertEquals(2, p.episodeCount)
        assertTrue(p.totalSec <= 1080)
        // 정확히 맞는 조합이 있으면 그것
        assertEquals(1080, PlaylistPlanner.plan(eps + ep("f", 30), 1080, age = 5).totalSec)
    }

    @Test
    fun `T5-2 같은 여행에서 들은 에피소드와 나이가 안 맞는 에피소드는 뺀다`() {
        val eps = listOf(ep("a", 300), ep("b", 300, minAge = 8), ep("c", 300))
        val p = PlaylistPlanner.plan(eps, 600, age = 5, played = setOf("a"))
        assertEquals(listOf("c"), p.items.map { it.id })
    }

    @Test
    fun `T9-8 목표 0 이하나 가장 짧은 것보다 짧으면 빈 목록`() {
        assertTrue(PlaylistPlanner.plan(listOf(ep("a", 300)), 0, 5).items.isEmpty())
        assertTrue(PlaylistPlanner.plan(listOf(ep("a", 300)), -10, 5).items.isEmpty())
        assertTrue(PlaylistPlanner.plan(listOf(ep("a", 300)), 200, 5).items.isEmpty())
    }

    @Test
    fun `초안으로 10분 편성 (5세가 들을 수 있는 초안은 모두 760초)`() {
        val p = PlaylistPlanner.plan(drafts, 10 * 60, age = 5)
        assertTrue(p.totalSec <= 1080)
        assertTrue(p.totalSec in 540..600, "${p.totalSec}")
        assertEquals(p.items.size, p.items.map { it.id }.toSet().size)
    }

    @Test
    fun `T6-3 원정대 - 나이·정차 원인에 맞는 3개, 다 하면 라디오 열림, 같은 여행에서 반복 없음`() {
        val t = ExpeditionTracker(draftMissions)
        val first = t.start(charging = false, childAge = 5)
        assertEquals(3, first.size)
        assertTrue(first.all { it.context == "rest_stop" && it.minAge <= 5 })
        first.dropLast(1).forEach { t.complete(it.id) }
        assertFalse(t.radioUnlocked)
        t.complete(first.last().id)
        assertTrue(t.radioUnlocked)
        assertFalse(t.complete("ms_unknown"))
        t.onDrivingStarted()
        assertTrue(t.current.isEmpty())
        assertTrue(t.radioUnlocked) // 다음 구간에 쓴다
        val second = t.start(charging = true, childAge = 5)
        assertTrue(second.none { it.id in first.map { f -> f.id } })
        assertEquals("charging", second.first().context)
    }

    @Test
    fun `T6-3 어린 아이에게는 나이 제한 미션을 내지 않는다`() {
        val t = ExpeditionTracker(draftMissions)
        val picked = t.start(charging = true, childAge = 4)
        assertTrue(picked.all { it.minAge <= 4 })
    }
}
