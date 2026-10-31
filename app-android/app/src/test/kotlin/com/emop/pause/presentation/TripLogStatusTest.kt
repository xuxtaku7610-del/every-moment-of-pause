package com.emop.pause.presentation

import com.emop.pause.engine.pet.LimitEstimate
import com.emop.pause.engine.pet.PetTripRecord
import com.emop.pause.trip.StopVisit
import com.emop.pause.uistate.PetStatusUi
import com.emop.pause.uistate.TrendMarkKind
import com.emop.pause.uistate.TrendMarkUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 이동 기록 v7: 오늘 상태 판정(사용자 결정 10/31) — 규칙 표 한 줄에 테스트 하나 (test_plan F7-06),
 * 관찰 기록 배타 고르기(F7-07), 그래프 표시 시각(F7-08), 병원 공유 요약 표(F7-09).
 */
class TripLogStatusTest {
    private val m = UiMapper
    private val points = 30

    // ───── F7-06 상태 판정 ─────

    @Test
    fun `F7-06 확인 중 - 측정 점 2개 미만이고 관찰 기록도 없음`() {
        assertEquals(PetStatusUi.CHECKING, m.petStatus(0, null, emptySet(), emptyList()))
        assertEquals(PetStatusUi.CHECKING, m.petStatus(1, 80, emptySet(), emptyList()))
        assertNull(m.statusReason(PetStatusUi.CHECKING, emptySet(), emptyList(), 0))
        // 점이 적어도 관찰 기록이 있으면 판정한다
        assertEquals(PetStatusUi.CAUTION, m.petStatus(1, null, setOf("헐떡임"), emptyList()))
    }

    @Test
    fun `F7-06 관찰 필요 - 구토 기록 한 번`() {
        assertEquals(PetStatusUi.ATTENTION, m.petStatus(points, 10, setOf("구토"), emptyList()))
        assertEquals(PetStatusUi.ATTENTION, m.petStatus(points, 10, emptySet(), listOf("구토")))
    }

    @Test
    fun `F7-06 관찰 필요 - 같은 증상 2회 이상`() {
        assertEquals(PetStatusUi.ATTENTION, m.petStatus(points, 10, emptySet(), listOf("헐떡임", "헐떡임")))
        // 주행 중 "침 흘림·하품" 2번 = 침 흘림 2번
        assertEquals(PetStatusUi.ATTENTION, m.petStatus(points, 10, emptySet(), listOf("침 흘림·하품", "침 흘림·하품")))
        // 서로 다른 가벼운 증상 1번씩은 2회가 아니다
        assertEquals(PetStatusUi.CAUTION, m.petStatus(points, 10, emptySet(), listOf("헐떡임", "침 흘림·하품")))
        // 운전 중 헐떡임 1번 + 도착 뒤 헐떡임 고르기 = 같은 관찰이라 1번
        assertEquals(PetStatusUi.CAUTION, m.petStatus(points, 10, setOf("헐떡임"), listOf("헐떡임")))
    }

    @Test
    fun `F7-06 관찰 필요 - 도착 시점 지수 70 이상`() {
        assertEquals(PetStatusUi.ATTENTION, m.petStatus(points, 70, emptySet(), emptyList()))
        assertEquals(PetStatusUi.CAUTION, m.petStatus(points, 69, emptySet(), emptyList()))
    }

    @Test
    fun `F7-06 주의 - 도착 시점 지수 40 이상`() {
        assertEquals(PetStatusUi.CAUTION, m.petStatus(points, 40, emptySet(), emptyList()))
        assertEquals(PetStatusUi.GOOD, m.petStatus(points, 39, emptySet(), emptyList()))
    }

    @Test
    fun `F7-06 주의 - 가벼운 증상 1회`() {
        listOf("침 흘림", "헐떡임", "낑낑거림").forEach { assertEquals(PetStatusUi.CAUTION, m.petStatus(points, 5, setOf(it), emptyList()), it) }
        listOf("헐떡임", "침 흘림·하품", "낑낑·떨림").forEach { assertEquals(PetStatusUi.CAUTION, m.petStatus(points, 5, emptySet(), listOf(it)), it) }
    }

    @Test
    fun `F7-06 양호 - 그 밖, 괜찮았어요만 골랐을 때`() {
        assertEquals(PetStatusUi.GOOD, m.petStatus(points, 12, emptySet(), emptyList()))
        assertEquals(PetStatusUi.GOOD, m.petStatus(points, 12, setOf("괜찮았어요"), emptyList()))
        // 측정 점이 적어도 "괜찮았어요"를 골랐으면 양호 (확인 중 아님)
        assertEquals(PetStatusUi.GOOD, m.petStatus(0, null, setOf("괜찮았어요"), emptyList()))
    }

    @Test
    fun `F7-06 근거 한 줄 - 증상 횟수와 멀미 주의 횟수`() {
        assertEquals("헐떡임 1번 · 멀미 주의 1번", m.statusReason(PetStatusUi.CAUTION, emptySet(), listOf("헐떡임"), 1))
        assertEquals("침 흘림 1번 · 헐떡임 2번 · 멀미 주의 없음", m.statusReason(PetStatusUi.ATTENTION, setOf("침 흘림"), listOf("헐떡임", "헐떡임"), 0))
        assertEquals("괜찮았어요 · 멀미 주의 없음", m.statusReason(PetStatusUi.GOOD, setOf("괜찮았어요"), emptyList(), 0))
        assertEquals("증상 기록 없음 · 멀미 주의 2번", m.statusReason(PetStatusUi.CAUTION, emptySet(), emptyList(), 2))
    }

    // ───── F7-07 관찰 기록 배타 ─────

    @Test
    fun `F7-07 괜찮았어요를 고르면 다른 증상이 풀리고, 다른 증상을 고르면 괜찮았어요가 풀린다`() {
        assertEquals(setOf("괜찮았어요"), m.toggleSymptom(setOf("침 흘림", "헐떡임"), "괜찮았어요"))
        assertEquals(setOf("구토"), m.toggleSymptom(setOf("괜찮았어요"), "구토"))
        assertEquals(setOf("침 흘림", "구토"), m.toggleSymptom(setOf("침 흘림"), "구토"))
        // 다시 누르면 풀린다
        assertEquals(emptySet<String>(), m.toggleSymptom(setOf("괜찮았어요"), "괜찮았어요"))
        assertEquals(setOf("구토"), m.toggleSymptom(setOf("구토", "헐떡임"), "헐떡임"))
    }

    // ───── F7-08 그래프 표시 ─────

    @Test
    fun `F7-08 주행 분을 그래프 시계(정차 포함)로 바꾼다`() {
        val stops = listOf(60 to 20, 150 to 10)
        assertEquals(39, m.elapsedMin(39, stops))
        assertEquals(60 + 20, m.elapsedMin(60, stops))
        assertEquals(170, m.elapsedMin(140, stops))
    }

    @Test
    fun `F7-08 휴식 구간과 증상 기록만 표시하고, 출발 시각을 모르면 표시하지 않는다`() {
        val visit = StopVisit("횡성휴게소", arrivedMinute = 540 + 90, stayedMin = 15)
        val marks = m.trendMarks(540, listOf(visit), listOf("헐떡임" to 39, "침 흘림·하품" to 100))
        assertEquals(
            listOf(
                TrendMarkUi(TrendMarkKind.STOP, 90, "휴식", 15),
                TrendMarkUi(TrendMarkKind.SYMPTOM, 39, "헐떡임"),
                TrendMarkUi(TrendMarkKind.SYMPTOM, 115, "침 흘림"),
            ),
            marks,
        )
        assertTrue(m.trendMarks(null, listOf(visit), listOf("헐떡임" to 39)).isEmpty())
        // 0분 정차(지나가기만 함)는 구간이 없다
        assertTrue(m.trendMarks(540, listOf(StopVisit("길가", 600)), emptyList()).isEmpty())
    }

    // ───── F7-09 병원 공유 요약 ─────

    @Test
    fun `F7-09 병원 요약 표 - 값이 있는 줄만, 지어낸 값 없음`() {
        val r = PetTripRecord(0, drivingMin = 159, firstCautionMin = 134, motionPeak = 55, stops = 1)
        val rows = m.vetRows(r, setOf("헐떡임"), "오늘 헐떡임 1회(출발 39분 뒤)", 48, 1, null).associate { it.key to it.value }
        assertEquals(listOf("관찰 증상", "주행 중 기록", "멀미 지수", "주의 도달", "주행"), rows.keys.toList())
        assertEquals("헐떡임", rows["관찰 증상"])
        assertEquals("헐떡임 1회(출발 39분 뒤)", rows["주행 중 기록"])
        assertEquals("최고 55 · 도착 시점 48 (0~100, 진단 아님)", rows["멀미 지수"])
        assertEquals("출발 2시간 14분 뒤 · 1번", rows["주의 도달"])
        assertEquals("2시간 39분 · 휴식 1번", rows["주행"])

        val calm = m.vetRows(PetTripRecord(0, 40, null, 12, 0), emptySet(), null, null, 0, LimitEstimate(70, 3)).associate { it.key to it.value }
        assertEquals(listOf("멀미 지수", "주의 도달", "주행", "평소 한계"), calm.keys.toList())
        assertEquals("최고 12 (0~100, 진단 아님)", calm["멀미 지수"])
        assertEquals("없음", calm["주의 도달"])
        assertEquals("약 1시간 10분 (기록 3번)", calm["평소 한계"])
    }
}
