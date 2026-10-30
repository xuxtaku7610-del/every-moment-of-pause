package com.emop.pause.uistate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** UPGRADE 1 (test_plan DM-05): 발표 모드 고갯길 비교 큰 그래프 — 값은 docs/plan/demo_compare.csv·summary 그대로. */
class CompareChartTest {
    private val csv = File("../../../docs/plan/demo_compare.csv").readText()
    private val summary = File("../../../docs/plan/demo_compare_summary.txt").readText()

    @Test
    fun `DM-05 CSV와 요약에서 두 선과 사실 3칸을 만든다`() {
        val ui = CompareChart.parse(csv, summary)
        assertEquals(55, ui.unguidedFinal)
        assertEquals(30, ui.guidedFinal)
        assertEquals(-45, ui.dropPct)
        assertEquals(432, ui.extraSec)
        assertEquals("+7분 12초", CompareChart.extraLabel(ui.extraSec))
        assertEquals("15.0km", CompareChart.kmLabel(ui.roadKm))
        // 가로축은 거리(km): 두 선 모두 0에서 시작해 끝(약 15km)까지, 거리가 줄지 않는다
        listOf(ui.unguided, ui.guided).forEach { line ->
            assertTrue(line.first().km < 0.1f)
            assertTrue(line.last().km > 14.9f, "${line.last()}")
            assertTrue(line.zipWithNext().all { (a, b) -> b.km >= a.km })
        }
        // 끝점 값 = 최종 지수
        assertEquals(55, ui.unguided.last().index)
        assertEquals(30, ui.guided.last().index)
        // 도착한 뒤 멈춘 행(같은 거리)은 한 번만
        assertEquals(ui.unguided.map { it.km }.distinct().size, ui.unguided.size)
    }
}
