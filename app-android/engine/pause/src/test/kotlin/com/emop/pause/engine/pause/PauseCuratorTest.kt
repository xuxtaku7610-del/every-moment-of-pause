package com.emop.pause.engine.pause

import com.emop.pause.core.model.PauseCause
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PauseCuratorTest {
    private fun input(
        cause: PauseCause = PauseCause.REST,
        charge: Int? = null,
        chosen: Int = 15,
        dog: Boolean = true,
        motion: Int = 10,
    ) = PauseInput(cause, charge, chosen, fatigueScore = 30, dog = dog, motionIndex = motion)

    @Test
    fun `F2-08 반려견 동반 15분 정차 - 산책 추천, 물·배변, 스트레칭 순`() {
        val p = PauseCurator.curate(input())
        assertEquals(15, p.totalMin)
        assertEquals(listOf(PauseActivity.PET_WALK, PauseActivity.PET_CARE, PauseActivity.STRETCH), p.activities.map { it.kind })
        assertEquals(PauseActivity.PET_WALK, p.activities.single { it.recommended }.kind)
    }

    @Test
    fun `F2-08 멀미 주의 이상이면 물·배변(안정)을 먼저 권한다`() {
        assertEquals(PauseActivity.PET_CARE, PauseCurator.curate(input(motion = 55)).activities.single { it.recommended }.kind)
    }

    @Test
    fun `F2-08 충전 중이면 충전 남은 시간, 짧으면 산책을 뺀다`() {
        val p = PauseCurator.curate(input(cause = PauseCause.CHARGING, charge = 7))
        assertEquals(7, p.totalMin)
        assertEquals(listOf(PauseActivity.PET_CARE, PauseActivity.STRETCH), p.activities.map { it.kind })
    }

    @Test
    fun `반려견이 없으면 스트레칭만`() {
        val p = PauseCurator.curate(input(dog = false))
        assertEquals(listOf(PauseActivity.STRETCH), p.activities.map { it.kind })
        assertEquals(PauseActivity.STRETCH, p.activities.single { it.recommended }.kind)
    }
}
