package com.emop.pause.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** test_plan F12-01·F12-02 (v10-3 여러 마리): 가장 예민한 아이 기준으로 합치기, 이름으로 신고 구분, "콩이만 타". */
class PetGroupTest {
    private val kongi = PetProfile("콩이", PetSize.SMALL, 9, MotionTendency.HIGH, pottyIntervalMin = 90, facilityPrefs = setOf("pet_zone"), motionScale = 1.3, id = 1)
    private val bori = PetProfile("보리", PetSize.LARGE, 3, MotionTendency.LOW, pottyIntervalMin = 150, facilityPrefs = setOf("shade"), motionScale = 0.9, id = 2)

    @Test
    fun `F12-01 합친 설정 = 배변 간격 가장 짧은 · 가장 예민한 · 체급·나이 큰 값 · 시설 합집합 · 보정 큰 값`() {
        val g = PetGroup.combine(listOf(kongi, bori))
        assertEquals("콩이·보리", g.name)
        assertEquals(90, g.pottyIntervalMin)
        assertEquals(MotionTendency.HIGH, g.motionTendency)
        assertEquals(PetSize.LARGE, g.size)
        assertEquals(9, g.ageYears)
        assertEquals(setOf("pet_zone", "shade"), g.facilityPrefs)
        assertEquals(1.3, g.motionScale)
        assertEquals(0, g.id)
        // 한 마리·없음은 그대로 (기존 동작)
        assertEquals(kongi, PetGroup.combine(listOf(kongi)))
        assertEquals(PetProfile.DEFAULT, PetGroup.combine(emptyList()))
    }

    @Test
    fun `F12-02 신고 이름 찾기와 오늘 타는 아이 말`() {
        assertEquals("보리", PetGroup.findName("보리 토했어", listOf("콩이", "보리")))
        assertNull(PetGroup.findName("토했어", listOf("콩이", "보리")))
        assertEquals("보리차", PetGroup.findName("보리차가 헥헥거려", listOf("보리", "보리차")))
        assertEquals(listOf(kongi), PetGroup.onlyRiders("오늘은 콩이만 타", listOf(kongi, bori)))
        assertEquals(listOf(kongi, bori), PetGroup.onlyRiders("콩이랑 보리만 탔어", listOf(kongi, bori)))
        assertNull(PetGroup.onlyRiders("콩이 토했어", listOf(kongi, bori)))
        assertNull(PetGroup.onlyRiders("두부만 타", listOf(kongi, bori)))
    }
}
