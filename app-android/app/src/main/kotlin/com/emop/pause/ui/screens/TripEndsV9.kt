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
import com.emop.pause.ui.components.Block
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PetPhoto
import com.emop.pause.ui.components.Heading
import com.emop.pause.ui.components.VDivider
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.DepartUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.SummaryUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.components.Body
import com.emop.pause.ui.components.Caption
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.rounded.QrCode2
import com.emop.pause.ui.components.LinkQr

/*
 * UI v9 (2026-11-03, `_private/UI_V9_PROMPT.md` 3·7번) — 출발 전(전체 창) 한 장 카드와 도착 요약.
 * 기존 v6 토큰·Kit 부품만(v10: Block·ChecklistRow·Metric·PauseButton·PetPhoto). 새 색 없음.
 * 반려견 상태를 판정하지 않는다 — 도착 요약의 관찰 신고는 횟수만.
 */

/**
 * 출발 전 (전체 창, v10 §4): 카드 1장 — 좌 60(인사 · 반려견(사진 원 96) · 체크 n/3) | 우 40(오늘 경로 · 예상 시간 수치 · 첫 쉼표 · 긴급 도움 · 출발).
 * 가장 중요한 한 가지 = 체크 n/3 과 [이대로 출발]. 영역은 세로 구분선과 여백으로만 나눈다.
 */
@Composable
internal fun DepartFull(ui: DepartUi, on: On, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Block(modifier, pad = d.u(28f), spacing = d.gapS) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            Column(Modifier.weight(0.6f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                Title(GREETING, maxLines = 1)
                Caption(LEAD, maxLines = 1)
                // 여러 마리 (v10-3): 반려견 한 줄 대신 "오늘 타는 아이" 칩 (사진 + 이름, 여러 개)
                if (ui.riders.isNotEmpty()) {
                    RiderRow(ui.riders, on)
                    Row {
                        PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, kind = ButtonKind.QUIET, compact = true)
                        PauseButton("설정", { on(UiEvent.OpenSettings) }, kind = ButtonKind.QUIET, compact = true)
                    }
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    PetPhoto(Modifier.size(d.u(96f)))
                    Spacer(Modifier.width(d.gap))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
                        Title(ui.pet.name, maxLines = 1)
                        Caption(ui.pet.line, maxLines = 2)
                        if (ui.pet.sample) Caption("프로필을 저장하면 맞춤 안내가 시작돼요")
                        Row {
                            PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, kind = ButtonKind.QUIET, compact = true)
                            PauseButton("설정", { on(UiEvent.OpenSettings) }, kind = ButtonKind.QUIET, compact = true)
                        }
                    }
                }
                Divider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Caption("출발 전 체크", Modifier.weight(1f), maxLines = 1)
                    if (ui.sameAsLast) PauseButton(DepartUi.SAME_AS_LAST, { on(UiEvent.SameAsLast) }, kind = ButtonKind.SECONDARY, compact = true)
                    Spacer(Modifier.width(d.gapS))
                    Text("${ui.checks.count { it.checked }}", style = Pause.type.metric, color = if (ui.ready) c.primary else c.text)
                    Text(" / ${ui.checks.size}", style = Pause.type.unit, color = c.sub, modifier = Modifier.padding(bottom = d.u(5f)))
                }
                ui.checks.forEach { ch -> ChecklistRow(ch.label, ch.checked, { on(UiEvent.ToggleCheck(ch.kind)) }, note = DepartUi.MISSED.takeIf { ch.missedBefore }) }
            }
            VDivider()
            Column(Modifier.weight(0.4f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                Caption("오늘 경로", maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Flag, null, Modifier.size(d.u(24f)), tint = c.navy)
                    Spacer(Modifier.width(d.unit))
                    Title(ui.route ?: "기본 내비에서 목적지를 정해 주세요", color = if (ui.route != null) c.text else c.sub)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
                    ui.duration?.let { Metric("예상 시간", it) }
                    Metric("예상 쉼표", "${ui.plan.size}", unit = "번")
                }
                ui.plan.firstOrNull()?.let { p ->
                    Divider()
                    Caption("첫 쉼표 · ${p.eta}", maxLines = 1)
                    Body(p.name, maxLines = 1)
                    p.facilities.takeIf { it.isNotBlank() }?.let { Caption(it, maxLines = 1) }
                }
                // 쉼표를 잡은 근거는 경로 칸 아래 (체크 칸이 넘치지 않게)
                Caption(ui.rationale, maxLines = 2)
                Spacer(Modifier.weight(1f))
                Caption(if (ui.ready) "준비됐어요" else "세 가지를 확인하면 출발할 수 있어요", maxLines = 2)
                // 출발(주)과 긴급 도움(앰버 보조)을 한 줄에 — 경로 칸이 넘치지 않게
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                    EmergencyButton(on, Modifier.weight(1f).fillMaxHeight(), compact = true)
                    PauseButton("이대로 출발", { on(UiEvent.StartTrip) }, Modifier.weight(1f), enabled = ui.ready)
                }
            }
        }
    }
}

private const val GREETING = "오늘도 안전한 여행 되세요"
private const val LEAD = "출발 전 세 가지만 확인해 주세요"

// ═════════════ 도착 요약 (UI v9 7번) ═════════════

/** [내렸어요] 뒤 마지막 화면(v10-3): 체크 아이콘 + 인사 + 한 줄 + 수치 4칸 + 사진 + [마치기][폰으로 기록 받기] + 작은 [자세히 보기]. 가운데로 모은다. */
@Composable
fun SummaryScreen(ui: SummaryUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false) { wc ->
        val narrow = wc == WidthClass.THIRD
        // 내용을 화면 세로 가운데로 — 아래가 크게 비지 않게 (11/03)
        Spacer(Modifier.weight(1f))
        Block(Modifier.fillMaxWidth(), pad = d.u(if (narrow) 24f else 40f), spacing = d.gap) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                    Box(Modifier.size(d.u(56f)).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(d.u(36f)), tint = c.onPrimary)
                    }
                    Title(SummaryUi.TITLE)
                    Body(ui.lead, color = c.sub)
                }
                // 폰 기록 QR을 만들면 사진 자리에 QR (장식 그림은 화면당 1개)
                if (!narrow) {
                    Spacer(Modifier.width(d.gap))
                    ui.phoneUrl?.let { LinkQr(SummaryUi.PHONE, it, SummaryUi.PHONE_HINT, Modifier.width(d.u(420f)), size = d.u(120f)) }
                        ?: PetPhoto(Modifier.size(d.u(160f)), CircleShape, "pet_arrival")
                }
            }
            // 수치 4칸 (v10): 카드 안 카드 대신 구분선 사이 수치 — 넓은 창 한 줄, 좁은 창 2×2
            val cells: List<@Composable (Modifier) -> Unit> = listOf(
                { m -> Metric("이동 거리", ui.distance?.removeSuffix("km") ?: "-", m, unit = ui.distance?.let { "km" }) },
                { m -> Metric("이동 시간", ui.duration, m) },
                { m -> Metric("휴식", "${ui.stops}", m, unit = "번") },
                { m -> Metric("관찰 신고", "${ui.reports}", m, unit = "건") },
            )
            Divider()
            cells.chunked(if (narrow) 2 else 4).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
                    row.forEachIndexed { i, cell ->
                        if (i > 0) VDivider()
                        cell(Modifier.weight(1f))
                    }
                }
            }
            Divider()
            if (narrow) ui.phoneUrl?.let { LinkQr(SummaryUi.PHONE, it, SummaryUi.PHONE_HINT, Modifier.fillMaxWidth()) }
            // v10-3: 도착 후 흐름은 요약에서 끝 — [마치기](주) · [폰으로 기록 받기](보조) · 작은 [자세히 보기](차 안 리포트, 오프라인)
            val finish: @Composable (Modifier) -> Unit = { m -> PauseButton(SummaryUi.FINISH, { on(UiEvent.FinishTrip) }, m) }
            val phone: @Composable (Modifier) -> Unit = { m ->
                // 링크를 만들면 버튼은 감춘다 (QR이 그 자리 설명)
                if (ui.phoneUrl != null) Unit
                else if (ui.phoneAvailable) PauseButton(
                    if (ui.phoneBusy) "만드는 중…" else SummaryUi.PHONE, { on(UiEvent.SendReportToPhone) }, m,
                    kind = ButtonKind.SECONDARY, icon = Icons.Rounded.QrCode2, enabled = !ui.phoneBusy && ui.phoneUrl == null,
                ) else Caption(SummaryUi.PHONE_NONE, m)
            }
            if (narrow) {
                finish(Modifier.fillMaxWidth())
                phone(Modifier.fillMaxWidth())
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gapS), verticalAlignment = Alignment.CenterVertically) {
                finish(Modifier.widthIn(min = d.u(260f)))
                phone(Modifier.weight(1f, fill = false).widthIn(min = d.u(260f)))
            }
            PauseButton(SummaryUi.OPEN_REPORT, { on(UiEvent.OpenReport) }, kind = ButtonKind.QUIET, compact = true)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PSummary() = PauseTheme(false, 2f) { SummaryScreen(Fixtures.summary) {} }
@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PSummaryThird() = PauseTheme(false, 2f) { SummaryScreen(Fixtures.summary) {} }
