package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.ChecklistRow
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PetPhoto
import com.emop.pause.ui.components.SectionHeader
import com.emop.pause.ui.components.StatCard
import com.emop.pause.ui.components.VDivider
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.DepartUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.SummaryUi
import com.emop.pause.uistate.UiEvent

/*
 * UI v9 (2026-11-03, `_private/UI_V9_PROMPT.md` 3·7번) — 출발 전(전체 창) 한 장 카드와 도착 요약.
 * 기존 v6 토큰·Kit 부품만(Panel·ChecklistRow·Metric·StatCard·PauseButton·PetPhoto). 새 색 없음.
 * 반려견 상태를 판정하지 않는다 — 도착 요약의 관찰 신고는 횟수만.
 */

/**
 * 출발 전 (전체 창): 큰 카드 하나 — 위 인사 + 한 줄 + 오른쪽 사진(아래 작은 [프로필 바꾸기]),
 * 아래 왼쪽 체크 3개(촘촘한 목록) | 오른쪽 오늘 경로 요약(목적지·예상 시간·예상 쉼표·첫 쉼표), 맨 아래 [긴급 도움](앰버 보조) [이대로 출발](주).
 */
@Composable
internal fun DepartFull(ui: DepartUi, on: On, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(24f), spacing = d.gapS) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
                Text(GREETING, style = Pause.type.title.copy(fontSize = Pause.type.title.fontSize * 1.15f, lineHeight = Pause.type.title.lineHeight * 1.15f), color = c.text, maxLines = 1)
                Text(LEAD, style = Pause.type.section, color = c.sub, maxLines = 1)
                Text("${ui.pet.name} · ${ui.pet.line}", style = Pause.type.sub, color = c.sub, maxLines = 2)
                if (ui.pet.sample) Text("프로필을 저장하면 맞춤 안내가 시작돼요", style = Pause.type.caption, color = c.sub)
            }
            Spacer(Modifier.width(d.gap))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PetPhoto(Modifier.size(d.u(96f)))
                Row {
                    PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, kind = ButtonKind.QUIET, compact = true)
                    PauseButton("설정", { on(UiEvent.OpenSettings) }, kind = ButtonKind.QUIET, compact = true)
                }
            }
        }
        Divider()
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            // 체크 3개 — 세로로 길게 벌리지 않고 목록으로 촘촘히
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(d.u(2f))) {
                SectionHeader("출발 전 체크", trailing = "${ui.checks.count { it.checked }} / ${ui.checks.size}")
                ui.checks.forEach { ch -> ChecklistRow(ch.label, ch.checked, { on(UiEvent.ToggleCheck(ch.kind)) }) }
                Text(ui.rationale, style = Pause.type.sub.copy(lineBreak = LineBreak.Paragraph), color = c.sub, maxLines = 2)
                ui.limitLine?.let { Text(it, style = Pause.type.sub, color = c.text, maxLines = 2) }
            }
            VDivider()
            // 오늘 경로 요약
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                SectionHeader("오늘 경로")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Flag, null, Modifier.size(d.u(24f)), tint = c.navy)
                    Spacer(Modifier.width(d.unit))
                    Text(ui.route ?: "기본 내비에서 목적지를 정해 주세요", style = Pause.type.section, color = if (ui.route != null) c.text else c.sub, maxLines = 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
                    ui.duration?.let { Metric("예상 시간", it, small = true) }
                    Metric("예상 쉼표", "${ui.plan.size}", unit = "번", small = true)
                }
                ui.plan.firstOrNull()?.let { p ->
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("첫 쉼표", style = Pause.type.metricLabel, color = c.sub)
                        Spacer(Modifier.width(d.gapS))
                        Text("${p.eta} · ${p.name}", style = Pause.type.bodyStrong, color = c.text, maxLines = 1)
                    }
                    p.facilities.takeIf { it.isNotBlank() }?.let { Text(it, style = Pause.type.sub, color = c.sub, maxLines = 1) }
                }
            }
        }
        Divider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
            Text(if (ui.ready) "준비됐어요" else "세 가지를 확인하면 출발할 수 있어요", style = Pause.type.sub, color = c.sub, modifier = Modifier.weight(1f), maxLines = 2)
            EmergencyButton(on, Modifier.widthIn(min = d.u(160f)))
            PauseButton("이대로 출발", { on(UiEvent.StartTrip) }, Modifier.widthIn(min = d.u(260f)), enabled = ui.ready)
        }
    }
}

private const val GREETING = "오늘도 안전한 여행 되세요"
private const val LEAD = "출발 전 세 가지만 확인해 주세요"

// ═════════════ 도착 요약 (UI v9 7번) ═════════════

/** [내렸어요] 뒤 첫 화면: 체크 아이콘 + 인사 + 한 줄 + 4칸 + 사진 + [이동 기록 보기]. 아래가 비지 않게 가운데로 모은다. */
@Composable
fun SummaryScreen(ui: SummaryUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false) { wc ->
        val narrow = wc == WidthClass.THIRD
        // 내용을 화면 세로 가운데로 — 아래가 크게 비지 않게 (11/03)
        Spacer(Modifier.weight(1f))
        Panel(Modifier.fillMaxWidth(), pad = d.u(if (narrow) 24f else 40f), spacing = d.gap) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                    Box(Modifier.size(d.u(56f)).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(d.u(36f)), tint = c.onPrimary)
                    }
                    Text(SummaryUi.TITLE, style = Pause.type.title.copy(fontSize = Pause.type.title.fontSize * 1.3f, lineHeight = Pause.type.title.lineHeight * 1.3f), color = c.text)
                    Text(ui.lead, style = Pause.type.section, color = c.sub)
                }
                if (!narrow) {
                    Spacer(Modifier.width(d.gap))
                    PetPhoto(Modifier.size(d.u(160f)), CircleShape, "pet_arrival")
                }
            }
            val cells: List<@Composable (Modifier) -> Unit> = listOf(
                { m -> StatCard(Icons.Rounded.Route, "이동 거리", listOf((ui.distance?.removeSuffix("km") ?: "-") to ui.distance?.let { "km" }), m, vertical = true) },
                { m -> StatCard(Icons.Rounded.Schedule, "이동 시간", listOf(ui.duration to null), m, vertical = true) },
                { m -> StatCard(Icons.Rounded.Coffee, "휴식", listOf("${ui.stops}" to "번"), m, vertical = true) },
                { m -> StatCard(Icons.Rounded.Pets, "관찰 신고", listOf("${ui.reports}" to "건"), m, vertical = true) },
            )
            // 4칸: 넓은 창 한 줄, 좁은 창 2×2
            cells.chunked(if (narrow) 2 else 4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gapS)) { row.forEach { it(Modifier.weight(1f)) } }
            }
            PauseButton(SummaryUi.OPEN_REPORT, { on(UiEvent.OpenReport) }, if (narrow) Modifier.fillMaxWidth() else Modifier.widthIn(min = d.u(320f)))
        }
        Spacer(Modifier.weight(1f))
    }
}

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PSummary() = PauseTheme(false, 2f) { SummaryScreen(Fixtures.summary) {} }
@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PSummaryThird() = PauseTheme(false, 2f) { SummaryScreen(Fixtures.summary) {} }
