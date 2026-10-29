package com.emop.pause.engine.pause

import com.emop.pause.core.model.PauseCause
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PauseCuratorTest {
    private fun input(
        cause: PauseCause = PauseCause.CHARGING,
        charge: Int? = 27,
        soc: Int? = 46,
        fatigue: Int = 61,
        sleep: Float? = 5f,
        kids: List<Int> = listOf(5),
        dog: Boolean = true,
        chosen: Int = 15,
    ) = PauseInput(cause, charge, chosen, soc, fatigue, sleep, kids, dog)

    @Test
    fun `T6-1 P0 충전 정차 27분이면 카드 3개 — 운전자 파워냅 추천, 아이 원정대, 반려견 산책 순`() {
        val p = PauseCurator.curate(input())
        assertEquals(27, p.totalMin)
        assertEquals(listOf(PauseActivity.NAP, PauseActivity.EXPEDITION, PauseActivity.PET_WALK), p.activities.map { it.kind })
        assertEquals(PauseActivity.NAP, p.activities.single { it.recommended }.kind)
        assertEquals(20, p.napMinutes)
        assertEquals(2, p.wakeBeforeEndMin)
    }

    @Test
    fun `T6-3 P0 원정대 미션은 3세 이상 아이에게만`() {
        assertFalse(PauseCurator.curate(input(kids = listOf(2))).activities.any { it.kind == PauseActivity.EXPEDITION })
        assertEquals(5, PauseCurator.curate(input(kids = listOf(2, 5))).activities.single { it.kind == PauseActivity.EXPEDITION }.childAge)
    }

    @Test
    fun `피곤하지 않으면 아이 원정대를 추천`() {
        val p = PauseCurator.curate(input(fatigue = 20, sleep = 7f))
        assertEquals(PauseActivity.EXPEDITION, p.activities.single { it.recommended }.kind)
    }

    @Test
    fun `동승자가 없으면 업무 집중과 스트레칭`() {
        val p = PauseCurator.curate(input(kids = emptyList(), dog = false, fatigue = 20, sleep = 8f))
        assertEquals(listOf(PauseActivity.NAP, PauseActivity.FOCUS, PauseActivity.STRETCH), p.activities.map { it.kind })
        assertEquals(PauseActivity.STRETCH, p.activities.single { it.recommended }.kind)
    }

    @Test
    fun `T6-5 P1 충전이 아닌 정차는 사용자가 고른 시간 — 10분이면 파워냅 없음`() {
        val p = PauseCurator.curate(input(cause = PauseCause.REST, charge = null, chosen = 10))
        assertEquals(10, p.totalMin)
        assertNull(p.napMinutes)
        assertEquals(listOf(PauseActivity.PET_WALK), p.activities.map { it.kind })
    }

    @Test
    fun `T6-9 P1 배터리 20% 이하면 공조 유지형 파워냅을 권하지 않는다`() {
        assertFalse(PauseCurator.curate(input(soc = 18)).activities.first { it.kind == PauseActivity.NAP }.climateKeep)
        assertTrue(PauseCurator.curate(input(soc = 46)).activities.first { it.kind == PauseActivity.NAP }.climateKeep)
    }

    @Test
    fun `충전 속도를 모르면 고른 시간으로`() {
        assertEquals(20, PauseCurator.curate(input(charge = null, chosen = 20)).totalMin)
    }
}
