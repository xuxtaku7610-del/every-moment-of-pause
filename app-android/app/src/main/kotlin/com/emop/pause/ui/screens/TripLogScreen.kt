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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Sick
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.KeyValue
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.SectionHeader
import com.emop.pause.ui.components.StatCard
import com.emop.pause.ui.components.SymptomTile
import com.emop.pause.ui.components.TrendChart
import com.emop.pause.ui.components.TrendLegend
import com.emop.pause.ui.components.VDivider
import com.emop.pause.ui.components.toneInk
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.MotionAnswerUi
import com.emop.pause.uistate.PetStatusUi
import com.emop.pause.uistate.TrendMarkKind
import com.emop.pause.uistate.TripLogUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.motionTone

/*
 * 이동 기록 v7 (F7, docs/design/v6/STYLE_V6.md §7.13). 도착 뒤 "내렸어요"를 누른 다음에만 보인다(정차 중).
 * 위에서 아래로 우선순위: 오늘 상태 → 통계 3칸 → 멀미 지수 추이(가장 넓게) → 관찰 기록 | 병원 공유 요약.
 * 전체 창은 스크롤 없이 한 화면, 1/2·1/3 창은 같은 순서로 쌓고 스크롤한다. 값은 모두 TripLogUi(실제 기록)만 쓴다.
 */

@Composable
fun TripLogScreen(ui: TripLogUi, on: On) {
    val d = Pause.dimens
    Page(ui.meta, on, scroll = false, trailing = {
        Spacer(Modifier.width(d.gap))
        PauseButton("기록 확인", { on(UiEvent.FinishTrip) }, compact = true)
    }) { wc ->
        if (wc == WidthClass.FULL) {
            // 영역 사이를 16 → 12로: 그래프에 높이를 준다 (v7 검수 1회차: 그래프 선 높이 30px)
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                StatusCard(ui, on, wide = true)
                Stats(ui, wc)
                ChartPanel(ui, Modifier.fillMaxWidth().weight(1f), legendInHeader = true)
                Row(Modifier.fillMaxWidth().height(d.u(164f)), horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                    ObservePanel(ui, on, columns = 3, Modifier.weight(0.5f).fillMaxHeight())
                    VetPanel(ui, Modifier.weight(0.5f).fillMaxHeight())
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                StatusCard(ui, on, wide = false)
                Stats(ui, wc)
                ChartPanel(ui, Modifier.fillMaxWidth().height(d.u(240f)), legendInHeader = false)
                ObservePanel(ui, on, columns = if (wc == WidthClass.HALF) 5 else 3, Modifier.fillMaxWidth())
                VetPanel(ui, Modifier.fillMaxWidth(), scroll = false)
            }
        }
    }
}

/** 한국어 줄바꿈을 어절(띄어쓰기) 단위로 — "기 / 록이"처럼 낱자에서 끊지 않는다. */
private val phrase = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

/** 상태별 색·아이콘·글자 (색만으로 구분하지 않는다). 빨강은 "관찰 필요"에만. */
private data class StatusLook(val ink: Color, val soft: Color, val icon: ImageVector, val word: String)

@Composable
private fun look(s: PetStatusUi): StatusLook {
    val c = Pause.colors
    return when (s) {
        PetStatusUi.GOOD -> StatusLook(c.comfort, c.comfortSoft, Icons.Rounded.CheckCircle, "양호")
        PetStatusUi.CAUTION -> StatusLook(c.warningInk, c.warningSoft, Icons.Rounded.WarningAmber, "주의")
        PetStatusUi.ATTENTION -> StatusLook(c.attentionInk, c.attentionSoft, Icons.Rounded.Error, "관찰 필요")
        PetStatusUi.CHECKING -> StatusLook(c.sub, c.surfaceAlt, Icons.Rounded.HourglassTop, "상태 확인 중")
    }
}

/** ② 오늘 상태: 아이콘 + 상태 글자 + 근거 한 줄 | 도착 시점 지수 | "오늘 멀미했어요?" 또는 이번 여행 한 줄. */
@Composable
private fun StatusCard(ui: TripLogUi, on: On, wide: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val lk = look(ui.status)
    val title = if (ui.petName.isBlank()) "오늘 상태" else "오늘 ${ui.petName} 상태"
    val status: @Composable (Modifier) -> Unit = { m ->
        Row(m, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(d.u(56f)).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                Icon(lk.icon, null, Modifier.size(d.u(34f)), tint = lk.ink)
            }
            Spacer(Modifier.width(d.gap))
            Column {
                Text(title, style = Pause.type.metricLabel, color = c.sub, maxLines = 1)
                Text(lk.word, style = Pause.type.title, color = lk.ink, maxLines = 1)
                // 증상이 여러 개면 두 줄까지 (말줄임으로 근거를 숨기지 않게)
                Text(ui.statusReason ?: "기록이 쌓이면 상태를 알려 드려요", style = Pause.type.bodyStrong.copy(lineBreak = phrase), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    val arrival: @Composable (Modifier) -> Unit = { m ->
        Metric(
            "도착 시점 지수", ui.arrivalIndex?.toString() ?: "–", m, unit = "/ 100", small = true,
            color = ui.arrivalIndex?.let { toneInk(motionTone(it)) } ?: c.sub,
        )
    }
    Panel(Modifier.fillMaxWidth(), tint = lk.soft, pad = d.u(16f), spacing = d.gapS) {
        if (wide) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                // 질문(버튼 4개)이 있을 때만 오른쪽을 넓게. 답한 뒤에는 근거 줄이 한 줄에 들어가게 반반
                val statusW = if (ui.askMotion != null) 0.42f else 0.5f
                status(Modifier.weight(statusW))
                VDivider(Modifier.padding(vertical = d.u(4f)))
                arrival(Modifier.padding(horizontal = d.u(24f)))
                VDivider(Modifier.padding(vertical = d.u(4f)))
                Column(Modifier.weight(1f - statusW).padding(start = d.u(24f)), verticalArrangement = Arrangement.spacedBy(d.unit)) { Aside(ui, on, wide = true) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                status(Modifier.weight(1f))
                arrival(Modifier.padding(start = d.gap))
            }
            Divider()
            Aside(ui, on, wide = false)
        }
    }
}

/** 상태 카드 오른쪽: 도착 뒤 한 번 "오늘 멀미했어요?"(UPGRADE 4), 답한 뒤에는 이번 여행 한 줄 + 맞춤 정도. */
@Composable
private fun Aside(ui: TripLogUi, on: On, wide: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val q = ui.askMotion
    if (q != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 질문이 먼저, 보조 글은 남는 자리만 (좁으면 말줄임)
            Text(q, style = Pause.type.section, color = c.text, maxLines = 1)
            Spacer(Modifier.width(d.unit))
            Text("답할수록 예보가 맞춰져요", style = Pause.type.sub, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalAlignment = Alignment.CenterVertically) {
            PauseButton("네", { on(UiEvent.AnswerMotion(MotionAnswerUi.YES)) }, Modifier.weight(1f), compact = true)
            PauseButton("조금", { on(UiEvent.AnswerMotion(MotionAnswerUi.LITTLE)) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY, compact = true)
            PauseButton("아니요", { on(UiEvent.AnswerMotion(MotionAnswerUi.NO)) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY, compact = true)
            PauseButton("건너뛰기", { on(UiEvent.AnswerMotion(null)) }, kind = ButtonKind.QUIET, compact = true)
        }
    } else {
        // 첫 문장(이번 여행 사실)은 굵게, 나머지(평소 한계·안내)는 보조 글 — 두 문장을 굵게 두 줄로 쓰면 넓은 창에서도 잘린다 (v7 검수 4회차)
        val cut = ui.limitLine.indexOf(". ").takeIf { it > 0 }
        val first = cut?.let { ui.limitLine.substring(0, it + 1) } ?: ui.limitLine
        Text(first, style = Pause.type.bodyStrong.copy(lineBreak = phrase), color = c.text, maxLines = if (wide) 2 else 3, overflow = TextOverflow.Ellipsis)
        // 넓은 창은 상태 카드 높이를 지키려고 첫 문장만 (평소 한계 값은 병원 요약 "평소 한계" 줄에 있다)
        if (!wide) cut?.let { Text(ui.limitLine.substring(it + 2), style = Pause.type.sub.copy(lineBreak = phrase), color = c.sub, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        ui.calibration?.let { Text(it, style = Pause.type.sub, color = c.primary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** ③ 통계 3칸: 총 주행 시간 · 휴식 횟수 · 최고 멀미 지수. 멀미 주의 횟수는 상태 근거 줄에 있다. */
@Composable
private fun Stats(ui: TripLogUi, wc: WidthClass) {
    val d = Pause.dimens
    val cards: List<@Composable (Modifier) -> Unit> = listOf(
        { m -> StatCard(Icons.Rounded.Schedule, "총 주행 시간", durationParts(ui.durationMin), m, vertical = wc == WidthClass.HALF) },
        { m -> StatCard(Icons.Rounded.LocalCafe, "휴식 횟수", listOf("${ui.stops}" to "번"), m, vertical = wc == WidthClass.HALF) },
        { m -> StatCard(Icons.Rounded.Timeline, "최고 멀미 지수", listOf("${ui.motionPeak}" to "/ 100"), m, color = toneInk(motionTone(ui.motionPeak)), vertical = wc == WidthClass.HALF) },
    )
    if (wc == WidthClass.THIRD) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.unit)) { cards.forEach { it(Modifier.fillMaxWidth()) } }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) { cards.forEach { it(Modifier.weight(1f)) } }
    }
}

/** "2시간 39분" → ("2","시간"),("39","분"). [hm]과 같은 값. */
private fun durationParts(min: Int): List<Pair<String, String?>> = when {
    min < 60 -> listOf("$min" to "분")
    min % 60 == 0 -> listOf("${min / 60}" to "시간")
    else -> listOf("${min / 60}" to "시간", "${min % 60}" to "분")
}

/** ④ 멀미 지수 추이: 5분 최고값 선 + 최고·도착 시점·증상 기록·휴식 표시. */
@Composable
private fun ChartPanel(ui: TripLogUi, modifier: Modifier, legendInHeader: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val hasSymptoms = ui.marks.any { it.kind == TrendMarkKind.SYMPTOM }
    val hasStops = ui.marks.any { it.kind == TrendMarkKind.STOP }
    val legend: @Composable () -> Unit = { TrendLegend(symptoms = hasSymptoms, stops = hasStops, arrival = ui.arrivalIndex != null) }
    Panel(modifier, pad = d.u(16f), spacing = d.u(4f)) {
        SectionHeader("멀미 지수 추이", trailingContent = {
            if (legendInHeader && ui.motionSeries.size >= 2) {
                legend()
                Spacer(Modifier.width(d.gap))
            }
            Text("5분 최고값 · 진단 아님", style = Pause.type.sub, color = c.sub, maxLines = 1)
        })
        TrendChart(ui.motionSeries, Modifier.fillMaxWidth().weight(1f), marks = ui.marks, arrival = ui.arrivalIndex)
        if (!legendInHeader && ui.motionSeries.size >= 2) legend()
    }
}

private fun symptomIcon(label: String): ImageVector = when (label) {
    "침 흘림" -> Icons.Rounded.WaterDrop
    "헐떡임" -> Icons.Rounded.Air
    "구토" -> Icons.Rounded.Sick
    "낑낑거림" -> Icons.Rounded.GraphicEq
    "괜찮았어요" -> Icons.Rounded.SentimentSatisfied
    else -> Icons.Rounded.Pets
}

/** ⑤ 왼쪽: 오늘의 관찰 기록 (키보드 없이 고르기, "괜찮았어요"와 다른 증상은 서로 배타). */
@Composable
private fun ObservePanel(ui: TripLogUi, on: On, columns: Int, modifier: Modifier) {
    val d = Pause.dimens
    Panel(modifier, pad = d.u(16f), spacing = d.unit) {
        SectionHeader("오늘의 관찰 기록", trailing = "도착 뒤 보이는 대로")
        ui.symptoms.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                row.forEach { s -> SymptomTile(s.label, symptomIcon(s.label), s.selected, { on(UiEvent.ToggleSymptom(s.label)) }, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** ⑤ 오른쪽: 동물병원 공유용 요약 — 이름 | 값 표. 아래(스크롤)에 기록 요약 문장·이전 기록. */
@Composable
private fun VetPanel(ui: TripLogUi, modifier: Modifier, scroll: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    // 줄 높이를 줄여 표 다섯 줄이 스크롤 없이 보이게 (v7 검수 3회차)
    val keyStyle = Pause.type.sub.copy(lineHeight = Pause.type.sub.fontSize * 1.25f)
    val valueStyle = keyStyle.copy(fontWeight = FontWeight.SemiBold, lineBreak = phrase)
    Panel(modifier, pad = d.u(16f), spacing = d.u(4f)) {
        SectionHeader("동물병원 공유용 요약")
        Column(if (scroll) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier, verticalArrangement = Arrangement.spacedBy(d.u(3f))) {
            ui.vetRows.forEach { r ->
                Row(Modifier.fillMaxWidth()) {
                    Text(r.key, style = keyStyle, color = c.sub, maxLines = 1, modifier = Modifier.width(d.u(96f)))
                    Text(r.value, style = valueStyle, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
            Divider(Modifier.padding(vertical = d.u(4f)))
            Text(ui.summary, style = Pause.type.sub.copy(lineBreak = phrase), color = c.sub)
            if (ui.history.isNotEmpty()) {
                Text("이전 기록", style = Pause.type.metricLabel, color = c.sub)
                ui.history.forEach { h -> KeyValue(h.date, h.line) }
            }
        }
    }
}

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PLog() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PLogAsk() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog.copy(askMotion = "오늘 콩이 멀미했어요?", status = PetStatusUi.ATTENTION, statusReason = "구토 1번 · 멀미 주의 1번")) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PLogEmpty() = PauseTheme(true, 2f) { TripLogScreen(Fixtures.tripLog.copy(motionSeries = emptyList(), marks = emptyList(), arrivalIndex = null, status = PetStatusUi.CHECKING, statusReason = null)) {} }
@Preview(widthDp = 1264, heightDp = 1210) @Composable private fun PLogHalf() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog) {} }
@Preview(widthDp = 853, heightDp = 1210) @Composable private fun PLogThird() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog) {} }
