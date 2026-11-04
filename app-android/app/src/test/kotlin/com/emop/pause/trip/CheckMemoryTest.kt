package com.emop.pause.trip

import com.emop.pause.engine.pet.CheckItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** test_plan F12-03 (v10-3 출발 체크 기억): 지난번 체크, 최근 3번 중 빠뜨린 항목, 저장 문자열. */
class CheckMemoryTest {
    private val all = CheckItem.entries.toSet()

    @Test
    fun `F12-03 지난번과 같아요 · 최근 3번 중 빠뜨린 항목만 강조`() {
        var m = CheckMemory()
        assertNull(m.last)
        assertEquals(emptySet<CheckItem>(), m.missed, "기록이 없으면 강조하지 않는다")
        m = m.onDeparted(setOf(CheckItem.HARNESS, CheckItem.WATER))
        m = m.onDeparted(all)
        m = m.onDeparted(all)
        assertEquals(all, m.last)
        assertEquals(setOf(CheckItem.POTTY_BAGS), m.missed)
        // 네 번째 출발이면 가장 오래된 기록이 빠진다
        m = m.onDeparted(all)
        assertEquals(3, m.recent.size)
        assertEquals(emptySet<CheckItem>(), m.missed)
        // 저장 문자열 왕복 (모르는 이름은 버린다)
        val back = CheckMemory.decode(m.onDeparted(setOf(CheckItem.WATER)).encode())
        assertEquals(setOf(CheckItem.WATER), back.last)
        assertEquals(setOf(CheckItem.HARNESS, CheckItem.POTTY_BAGS), back.missed)
        assertEquals(setOf(CheckItem.WATER), CheckMemory.decode("WATER,NOPE|HARNESS").last)
        assertNull(CheckMemory.decode(null).last)
    }
}
