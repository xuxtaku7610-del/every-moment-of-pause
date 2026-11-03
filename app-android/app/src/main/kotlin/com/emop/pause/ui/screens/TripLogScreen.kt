package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.SectionHeader
import com.emop.pause.ui.components.TrendChart
import com.emop.pause.ui.components.TrendLegend
import com.emop.pause.ui.components.VDivider
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.STIMULUS_NOTE
import com.emop.pause.uistate.TimelineKind
import com.emop.pause.uistate.TimelineUi
import com.emop.pause.uistate.TrendMarkKind
import com.emop.pause.uistate.TripLogUi
import com.emop.pause.uistate.UiEvent

/*
 * 도착 후 기록 리포트 (2026-10-31 재정비 상태 8, 기존 이동 기록 대체). 정차 화면이라 전체 창 레이아웃이 기준이다.
 * 첫 화면(11/02 간소화): 요약 한 줄(총 이동 시간 · 거리 · 휴식 · 관찰 신고) + 시간순 타임라인(맞춤 정도 한 줄 포함)만.
 * "주행 자극 자세히 보기"를 누르면 주행 자극 그래프(타임라인 위치 표시)와 병원에 보여 줄 내용(공유 버튼·지난 기록)으로 바꾼다.
 * 경로선은 그리지 않는다(위치를 남기지 않음). 값은 기록만, 없는 줄은 뺀다. 1/2·1/3 창은 같은 순서로 쌓고 스크롤한다.
 */

/** 한국어 줄바꿈을 어절 단위로 ("기 / 록이"처럼 낱자에서 끊지 않는다). */
private val phrase = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

@Composable
fun TripLogScreen(ui: TripLogUi, on: On) {
    val d = Pause.dimens
    var detail by rememberSaveable { mutableStateOf(false) }
    // 긴급 도움 (11/03): 전체 창은 앱 바 맨 앞, 1/2·1/3 창은 앱 바가 좁아 기존 두 버튼이 밀려나므로 화면 맨 아래 (기존 버튼은 그대로)
    var wideBar by remember { mutableStateOf(true) }
    Page(ui.meta, on, scroll = false, trailing = {
        Spacer(Modifier.width(d.gap))
        if (wideBar) {
            EmergencyButton(on, compact = true)
            Spacer(Modifier.width(d.gapS))
        }
        PauseButton(if (detail) "타임라인 보기" else "주행 자극 자세히 보기", { detail = !detail }, kind = ButtonKind.SECONDARY, compact = true)
        Spacer(Modifier.width(d.gapS))
        PauseButton("기록 확인", { on(UiEvent.FinishTrip) }, compact = true)
    }) { wc ->
        SideEffect { wideBar = wc == WidthClass.FULL }
        if (wc == WidthClass.FULL) {
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                SummaryBar(ui, wide = true)
                if (detail) {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                        ChartPanel(ui, Modifier.weight(0.6f).fillMaxHeight())
                        SharePanel(ui, on, Modifier.weight(0.4f))
                    }
                } else {
                    TimelinePanel(ui, Modifier.fillMaxWidth().weight(1f), scroll = true)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                SummaryBar(ui, wide = wc == WidthClass.HALF)
                if (detail) {
                    ChartPanel(ui, Modifier.fillMaxWidth().height(d.u(240f)))
                    SharePanel(ui, on, Modifier.fillMaxWidth())
                } else {
                    TimelinePanel(ui, Modifier.fillMaxWidth(), scroll = false)
                }
                EmergencyButton(on, Modifier.fillMaxWidth(), compact = true)
            }
        }
    }
}

/** 요약 한 줄: 총 이동 시간 · 거리 · 휴식 · 관찰 신고. 거리를 모르면 칸을 뺀다. */
@Composable
private fun SummaryBar(ui: TripLogUi, wide: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val items = listOfNotNull(
        Triple("총 이동 시간", hm(ui.totalMin), null),
        ui.distance?.let { Triple("거리", it, null) },
        Triple("휴식", "${ui.stops}", "번"),
        Triple("관찰 신고", "${ui.reports}", "건"),
    )
    Panel(Modifier.fillMaxWidth(), pad = d.u(16f)) {
        if (wide) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, (k, v, u) ->
                if (i > 0) VDivider(Modifier.padding(vertical = d.u(4f)))
                Metric(
                    k, v, Modifier.weight(1f).padding(start = if (i > 0) d.u(20f) else d.u(0f)), unit = u, small = true,
                    color = if (k == "관찰 신고" && ui.reports > 0) c.warningInk else c.text,
                )
            }
        } else {
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (k, v, u) -> Metric(k, v, Modifier.weight(1f), unit = u, small = true) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class KindLook(val icon: ImageVector, val tint: Color, val bg: Color)

@Composable
private fun look(k: TimelineKind): KindLook {
    val c = Pause.colors
    return when (k) {
        TimelineKind.START -> KindLook(Icons.Rounded.DirectionsCar, c.navy, c.surfaceAlt)
        TimelineKind.STIMULUS -> KindLook(Icons.Rounded.Timeline, c.warningInk, c.warningSoft)
        TimelineKind.REPORT -> KindLook(Icons.Rounded.RecordVoiceOver, c.warningInk, c.warningSoft)
        TimelineKind.REST -> KindLook(Icons.Rounded.LocalCafe, c.primary, c.primarySoft)
        TimelineKind.VET -> KindLook(Icons.Rounded.LocalHospital, c.primary, c.primarySoft)
        TimelineKind.ARRIVE -> KindLook(Icons.Rounded.Flag, c.comfort, c.comfortSoft)
    }
}

/** 시간순 타임라인: 아이콘 원 · 시각(출발 뒤) · 제목 · 설명. 왼쪽 세로선으로 잇는다. */
@Composable
private fun TimelinePanel(ui: TripLogUi, modifier: Modifier, scroll: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(20f), spacing = d.unit) {
        SectionHeader("오늘의 길", trailing = "출발 뒤 시간")
        Column(if (scroll) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier) {
            if (ui.timeline.isEmpty()) Text("기록이 아직 없어요", style = Pause.type.body, color = c.sub)
            ui.timeline.forEachIndexed { i, item -> TimelineRow(item, last = i == ui.timeline.lastIndex) }
        }
        // 한 줄 요약과 개인 보정 한 줄(PT3 "쓸수록 맞춰짐")은 스크롤 밖 아래에 고정 — 타임라인이 길어도 첫 화면에 보인다
        ui.insight?.let {
            Divider(Modifier.padding(vertical = d.unit))
            Text(it, style = Pause.type.body.copy(lineBreak = phrase), color = c.text)
        }
        ui.calibration?.let { Text(it, style = Pause.type.caption, color = c.sub) }
    }
}

@Composable
private fun TimelineRow(item: TimelineUi, last: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val lk = look(item.kind)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(d.u(40f)).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(d.u(36f)).clip(CircleShape).background(lk.bg), contentAlignment = Alignment.Center) {
                Icon(lk.icon, null, Modifier.size(d.u(20f)), tint = lk.tint)
            }
            if (!last) Box(Modifier.width(d.u(2f)).weight(1f).background(c.border))
        }
        Spacer(Modifier.width(d.gapS))
        Text(item.time, style = Pause.type.bodyStrong, color = c.sub, modifier = Modifier.width(d.u(52f)).padding(top = d.u(6f)))
        Column(Modifier.weight(1f).padding(top = d.u(6f), bottom = d.gapS)) {
            Text(item.title, style = Pause.type.bodyStrong.copy(lineBreak = phrase), color = c.text, maxLines = 2)
            item.detail?.let { Text(it, style = Pause.type.sub.copy(lineBreak = phrase), color = c.sub, maxLines = 2) }
        }
    }
}

/** 주행 자극 그래프: 시간축, 타임라인 항목 위치 표시, 고정 문구 "차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님". */
@Composable
private fun ChartPanel(ui: TripLogUi, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(16f), spacing = d.u(4f)) {
        SectionHeader("주행 자극")
        // 고정 문구는 제목 아래 한 줄 (좁은 창에서 제목 옆에 두면 잘린다, 11/03 문구를 늘림)
        Text(STIMULUS_NOTE, style = Pause.type.caption, color = c.sub)
        TrendChart(ui.stimulusSeries, Modifier.fillMaxWidth().weight(1f), marks = ui.marks)
        if (ui.stimulusSeries.size >= 2) {
            TrendLegend(
                symptoms = ui.marks.any { it.kind == TrendMarkKind.SYMPTOM },
                stops = ui.marks.any { it.kind == TrendMarkKind.STOP },
                arrival = false,
                high = ui.marks.any { it.kind == TrendMarkKind.HIGH },
            )
        }
        Text("5분 최고값 · 낮음 < 40 ≤ 보통 < 70 ≤ 높음", style = Pause.type.caption, color = c.sub)
    }
}

/** 병원에 보여 줄 내용(이름 | 값) + "병원에 공유" 버튼 + 지난 기록 (맞춤 정도는 첫 화면 타임라인 아래). */
@Composable
private fun SharePanel(ui: TripLogUi, on: On, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    val keyStyle = Pause.type.sub
    val valueStyle = Pause.type.sub.copy(fontWeight = FontWeight.SemiBold, lineBreak = phrase)
    Panel(modifier, pad = d.u(16f), spacing = d.u(6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("병원에 보여 줄 내용", style = Pause.type.section, color = c.text, modifier = Modifier.weight(1f))
            PauseButton("병원에 공유", { on(UiEvent.ShareReport) }, kind = ButtonKind.SECONDARY, compact = true)
        }
        ui.share.forEach { r ->
            Row(Modifier.fillMaxWidth()) {
                Text(r.key, style = keyStyle, color = c.sub, maxLines = 1, modifier = Modifier.width(d.u(84f)))
                Text(r.value, style = valueStyle, color = c.text, maxLines = 2, modifier = Modifier.weight(1f))
            }
        }
        listOfNotNull(ui.history.firstOrNull()?.let { "지난 기록 ${it.date} · ${it.line}" }).forEach {
            Text(it, style = Pause.type.caption, color = c.sub, maxLines = 1)
        }
    }
}

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PReport() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog) {} }
@Preview(widthDp = 1264, heightDp = 1210) @Composable private fun PReportHalf() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog) {} }
@Preview(widthDp = 853, heightDp = 1210) @Composable private fun PReportThird() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog.copy(timeline = emptyList(), stimulusSeries = emptyList())) {} }
