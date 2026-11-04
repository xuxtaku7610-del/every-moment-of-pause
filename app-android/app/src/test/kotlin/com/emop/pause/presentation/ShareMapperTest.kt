package com.emop.pause.presentation

import com.emop.pause.uistate.TimelineKind
import com.emop.pause.uistate.TimelineUi
import com.emop.pause.uistate.TrendMarkKind
import com.emop.pause.uistate.TrendMarkUi
import com.emop.pause.uistate.TripLogUi
import com.emop.pause.uistate.VetRowUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** test_plan F12-04 (v10-3 폰으로 기록 받기): 차 안 리포트와 같은 값, 위치 없음, 서버 길이 제한. */
class ShareMapperTest {
    @Test
    fun `F12-04 리포트 값 그대로 · 날짜·목적지 · 이름 3마리까지 · 위치 칸 없음`() {
        val ui = TripLogUi(
            meta = "11월 4일, 강릉까지", totalMin = 180, distance = "230km", stops = 1, reports = 1,
            timeline = listOf(TimelineUi(TimelineKind.REPORT, "0:39", "헐떡임 신고 ".repeat(10), "주행 중")),
            stimulusSeries = listOf(10, 55, 130), marks = listOf(TrendMarkUi(TrendMarkKind.HIGH, 15, "", 10)),
            insight = "한 줄", share = listOf(VetRowUi("관찰 신고", "헐떡임 1회")),
        )
        val r = ShareMapper.report(ui, listOf("콩이", "보리", "두부", "넷째"))
        assertEquals(listOf("콩이", "보리", "두부"), r.petNames)
        assertEquals("11월 4일", r.date)
        assertEquals("강릉", r.destination)
        assertEquals(listOf(10, 55, 100), r.series)
        assertEquals(60, r.timeline.single().title.length)
        assertEquals("HIGH", r.marks.single().kind)
        // 계약에 위치 칸이 없다 (서버도 정의되지 않은 필드는 422)
        val fields = com.emop.pause.data.net.ReportShareReq::class.java.declaredFields.map { it.name.lowercase() }
        listOf("lat", "lng", "route", "location", "points").forEach { k -> assertFalse(fields.any { k in it }, k) }
    }
}
