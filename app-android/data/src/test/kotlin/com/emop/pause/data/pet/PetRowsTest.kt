package com.emop.pause.data.pet

import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PetRowsTest {
    @Test
    fun `프로필은 Room 행으로 갔다가 그대로 돌아온다`() {
        val p = PetProfile("콩이", PetSize.SMALL, 9, MotionTendency.HIGH, 90, setOf("pet_zone", "ev_fast"))
        val row = PetRows.toRow(p, id = 3, nowMs = 1000)
        assertEquals("ev_fast,pet_zone", row.facilityPrefs)
        // v10-3 여러 마리: 행 id도 함께 돌아온다 (나머지 값은 그대로)
        assertEquals(p.copy(id = 3), PetRows.toProfile(row))
    }

    @Test
    fun `모르는 값은 기본값으로 읽는다`() {
        val row = PetProfileRow(1, "보리", "HUGE", 2, "EXTREME", 120, "", updatedAt = 0)
        val p = PetRows.toProfile(row)
        assertEquals(PetSize.MEDIUM, p.size)
        assertEquals(MotionTendency.NORMAL, p.motionTendency)
        assertEquals(emptySet<String>(), p.facilityPrefs)
    }

    @Test
    fun `이름 앞뒤 공백은 지운다`() {
        assertEquals("콩이", PetRows.toRow(PetProfile(" 콩이 "), nowMs = 0).name)
    }
}
