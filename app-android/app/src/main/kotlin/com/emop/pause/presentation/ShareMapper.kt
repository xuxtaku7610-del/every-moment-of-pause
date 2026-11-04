package com.emop.pause.presentation

import com.emop.pause.data.net.MarkDto
import com.emop.pause.data.net.ReportShareReq
import com.emop.pause.data.net.RowDto
import com.emop.pause.data.net.TimelineDto
import com.emop.pause.uistate.TripLogUi

/**
 * 도착 요약 [폰으로 기록 받기] (v10-3 5번): 차 안 리포트([TripLogUi])와 **같은 값**을 서버 기록 페이지로 보낸다.
 * 위치·경로는 담지 않는다(계약에 칸이 없다). 서버 길이 제한(이름 20 · 제목 60 · 설명 120 · 값 200 · 표시 60개)에 맞춰 자른다.
 */
object ShareMapper {
    fun report(ui: TripLogUi, petNames: List<String>, sampleMin: Int = 5): ReportShareReq {
        val (date, dest) = ui.meta.orEmpty().split(", ", limit = 2).let { it.first() to it.getOrNull(1)?.removeSuffix("까지") }
        return ReportShareReq(
            petNames = petNames.map { it.take(20) }.filter { it.isNotBlank() }.ifEmpty { listOf("반려견") }.take(3),
            date = date.take(20),
            destination = dest?.take(40),
            totalMin = ui.totalMin.coerceIn(0, 1440),
            distance = ui.distance?.take(16),
            stops = ui.stops.coerceIn(0, 50),
            reports = ui.reports.coerceIn(0, 100),
            timeline = ui.timeline.take(40).map { TimelineDto(it.kind.name, it.time.take(8), it.title.take(60), it.detail?.take(120)) },
            series = ui.stimulusSeries.take(300).map { it.coerceIn(0, 100) },
            sampleMin = sampleMin,
            marks = ui.marks.take(60).map { MarkDto(it.kind.name, it.minute.coerceIn(0, 1440), it.lengthMin.coerceIn(0, 1440), it.label.take(30)) },
            share = ui.share.take(12).map { RowDto(it.key.take(30), it.value.take(200)) },
            insight = ui.insight?.take(200),
        )
    }
}
