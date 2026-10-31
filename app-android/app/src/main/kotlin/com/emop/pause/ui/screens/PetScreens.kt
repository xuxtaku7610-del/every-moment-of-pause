package com.emop.pause.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.emop.pause.R
import com.emop.pause.ui.components.AlertStrip
import com.emop.pause.ui.components.ArtSlot
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.CheckCircle
import com.emop.pause.ui.components.ChecklistRow
import com.emop.pause.ui.components.Choice
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.DogIllustration
import com.emop.pause.ui.components.KeyValue
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.NumberBadge
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PetPhoto
import com.emop.pause.ui.components.SectionHeader
import com.emop.pause.ui.components.StatusChip
import com.emop.pause.ui.components.StatusIcon
import com.emop.pause.ui.components.SuvIllustration
import com.emop.pause.ui.components.ToggleRow
import com.emop.pause.ui.components.toneInk
import com.emop.pause.ui.components.toneWord
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.ArrivalUi
import com.emop.pause.uistate.DepartUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.MotionQuiz
import com.emop.pause.uistate.MotionTone
import com.emop.pause.uistate.OnboardingUi
import com.emop.pause.uistate.PetEditUi
import com.emop.pause.uistate.PlaceFilter
import com.emop.pause.uistate.PlacesUi
import com.emop.pause.uistate.RestUi
import com.emop.pause.uistate.SettingsUi
import com.emop.pause.uistate.SizeUi
import com.emop.pause.uistate.TendencyUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.VetsUi
import com.emop.pause.uistate.WaitUi

/*
 * 정차 중 화면 v6 (docs/design/v6/STYLE_V6.md §7.3~7.12, docs/ui_contract.md).
 * 모든 화면은 상태(UiState)만 보고 그리며, 사용자 동작은 [UiEvent]로만 돌려보낸다.
 * 규칙: 화면당 큰 패널 1~3개, 패널 안은 구분선으로 나누고 카드를 겹쳐 쌓지 않는다. 사진은 원형(프로필)이나 고정 비율만.
 */

// ═════════════ 출발 체크 (F6) ═════════════

@Composable
fun DepartScreen(ui: DepartUi, on: On) {
    val d = Pause.dimens
    // 정차 화면 = 전체 창이 기준(프로필 · 체크 · 경로 3칸). 분할 창으로 볼 때는 같은 순서로 쌓고 스크롤 — 글자를 자르지 않는다.
    Page(ui.meta, on, scroll = false) { wc ->
        if (wc == WidthClass.FULL) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
                DepartPet(ui, on, Modifier.weight(0.28f).fillMaxHeight(), wide = true)
                DepartChecks(ui, on, Modifier.weight(0.38f).fillMaxHeight())
                DepartRoute(ui, on, Modifier.weight(0.34f).fillMaxHeight(), wide = true)
            }
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                DepartPet(ui, on, Modifier.fillMaxWidth(), wide = false)
                DepartChecks(ui, on, Modifier.fillMaxWidth())
                DepartRoute(ui, on, Modifier.fillMaxWidth(), wide = false)
            }
        }
    }
}

@Composable
private fun DepartPet(ui: DepartUi, on: On, m: Modifier, wide: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(m, pad = d.u(24f), spacing = d.gapS) {
        if (wide) {
            PetPhoto(Modifier.size(d.u(120f)).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(d.unit))
            Text(ui.pet.name, style = Pause.type.title, color = c.text, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(ui.pet.line, style = Pause.type.sub, color = c.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else Row(verticalAlignment = Alignment.CenterVertically) {
            PetPhoto(Modifier.size(d.u(72f)))
            Spacer(Modifier.width(d.gap))
            Column(Modifier.weight(1f)) {
                Text(ui.pet.name, style = Pause.type.title, color = c.text)
                Text(ui.pet.line, style = Pause.type.sub, color = c.sub)
            }
        }
        if (ui.pet.sample) Text("프로필을 저장하면 맞춤 안내가 시작돼요", style = Pause.type.caption, color = c.sub, textAlign = if (wide) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth())
        if (wide) Spacer(Modifier.weight(1f))
        ui.limitLine?.let {
            Divider()
            Text(it, style = Pause.type.sub, color = c.text)
        }
        PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, compact = true)
    }
}

@Composable
private fun DepartChecks(ui: DepartUi, on: On, m: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(m, pad = d.u(24f), spacing = d.gapS) {
        SectionHeader("출발 전 체크", trailing = "${ui.checks.count { it.checked }} / ${ui.checks.size}")
        Divider()
        ui.checks.forEachIndexed { i, ch ->
            if (i > 0) Divider()
            ChecklistRow(ch.label, ch.checked, { on(UiEvent.ToggleCheck(ch.kind)) })
        }
        Divider()
        Text(ui.rationale, style = Pause.type.sub, color = c.sub)
    }
}

@Composable
private fun DepartRoute(ui: DepartUi, on: On, m: Modifier, wide: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(m, pad = d.u(24f), spacing = d.gapS) {
        // 목적지는 기본 내비에서 정한다 (같이가개는 내비를 만들지 않는다). 이름은 2줄까지.
        Text(ui.route ?: "기본 내비에서 목적지를 정해 주세요", style = Pause.type.section, color = if (ui.route != null) c.text else c.sub, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            ui.duration?.let { Metric("예상 시간", it, small = true) }
            Metric("예상 쉼표", "${ui.plan.size}", unit = "번", small = true)
        }
        Divider()
        ui.plan.take(3).forEach { p ->
            Column {
                Text(p.eta, style = Pause.type.bodyStrong, color = c.text, maxLines = 1)
                Text(p.name + (p.facilities.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), style = Pause.type.sub, color = c.sub, maxLines = 2)
            }
        }
        if (wide) Spacer(Modifier.weight(1f))
        if (!ui.ready) Text("세 가지를 확인하면 출발할 수 있어요", style = Pause.type.sub, color = c.sub)
        PauseButton("이대로 출발", { on(UiEvent.StartTrip) }, Modifier.fillMaxWidth(), enabled = ui.ready)
        PauseButton("설정", { on(UiEvent.OpenSettings) }, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET, compact = true)
    }
}

// ═════════════ 쉼표 (정차) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestScreen(ui: RestUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(ui.meta, on, scroll = true) { wc ->
        // 전체 창은 왼쪽(지금 상태·행동) | 오른쪽(돌보기·할 일), 1/2·1/3 창은 같은 순서로 위아래 (글자·버튼을 자르지 않는다)
        val wide = wc == WidthClass.FULL
        RestColumns(wide, { m ->
            Panel(m, pad = d.u(24f), spacing = d.gapS) {
                Text(ui.lead, style = Pause.type.title, color = c.text)
                // 콩이 관찰 상태 (보호자 음성 신고만) — 주행 자극 숫자는 "주행 자극 보기"에서만
                Text("콩이 관찰 상태", style = Pause.type.metricLabel, color = c.sub)
                Text(ui.observation.line, style = Pause.type.bodyStrong, color = if (ui.observation.count > 0) c.warningInk else c.text)
                ui.battery?.let { IconLine(Icons.Rounded.BatteryStd, it) }
                ui.restChoice?.let { sel ->
                    Divider()
                    Text("쉬는 시간", style = Pause.type.metricLabel, color = c.sub)
                    Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                        listOf(10, 15, 20).forEach { m -> Choice("${m}분", sel == m, { on(UiEvent.SetRestMinutes(m)) }) }
                    }
                }
                if (wide) Spacer(Modifier.weight(1f))
                PauseButton("잠깐 다녀올게 (${ui.waitLimitMin}분)", { on(UiEvent.StartWait) }, Modifier.fillMaxWidth())
                PauseButton("함께 가는 장소", { on(UiEvent.OpenPlaces) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Place, compact = true)
                PauseButton("동물병원", { on(UiEvent.OpenVets) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, compact = true)
                if (ui.stimulusAvailable) PauseButton("주행 자극 보기", { on(UiEvent.OpenStimulus) }, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET, compact = true)
            }
        }) { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(d.gap)) {
                // "콩이 돌보기" (UPGRADE 7): 주행 중에 말한 증상의 체크리스트, 정차 중에만
                ui.care?.let { cu ->
                    Panel(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.gapS) {
                        AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_warning, iconAsset = "ic_warning") {
                            Text("콩이 돌보기 — ${cu.title}", style = Pause.type.section, color = c.text)
                        }
                        cu.lines.forEach { line ->
                            Row(verticalAlignment = Alignment.Top) {
                                CheckCircle(false, d.u(20f))
                                Spacer(Modifier.width(d.gapS))
                                Text(line, style = Pause.type.body, color = c.text)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cu.disclaimer, style = Pause.type.caption, color = c.sub, modifier = Modifier.weight(1f))
                            if (!cu.reviewed) { StatusChip("검수 전", neutral = true); Spacer(Modifier.width(d.unit)) }
                            PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, kind = ButtonKind.SECONDARY, compact = true)
                        }
                    }
                }
                run {
                    Panel(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.gapS) {
                        SectionHeader("지금 하면 좋은 것")
                        ui.activities.forEachIndexed { i, a ->
                            if (i > 0) Divider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${a.who} · ${a.title}", style = Pause.type.bodyStrong, color = c.text)
                                    Text(a.description, style = Pause.type.sub, color = c.sub)
                                }
                                if (a.recommended) StatusChip("추천")
                            }
                        }
                    }
                }
                if (ui.distressChips.isNotEmpty()) {
                    Panel(Modifier.fillMaxWidth(), pad = d.u(20f), spacing = d.gapS) {
                        SectionHeader("콩이가 이상해요")
                        Text("고르면 기록하고 체크리스트를 보여 줘요", style = Pause.type.sub, color = c.sub)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                            ui.distressChips.forEach { l -> Choice(l, false, { on(UiEvent.ReportDistress(l)) }) }
                        }
                    }
                }
            }
        }
    }
}

/** 쉼표 화면 두 칸: 넓으면 0.38 | 0.62 나란히(높이 맞춤), 좁으면 위아래. */
@Composable
private fun RestColumns(wide: Boolean, a: @Composable (Modifier) -> Unit, b: @Composable (Modifier) -> Unit) {
    val gap = Pause.dimens.gap
    if (wide) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
            a(Modifier.weight(0.38f).fillMaxHeight())
            b(Modifier.weight(0.62f))
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            a(Modifier.fillMaxWidth())
            b(Modifier.fillMaxWidth())
        }
    }
}

// ═════════════ 잠깐 대기 (F3): 바깥 사람이 읽는 차 화면 ═════════════

@Composable
fun WaitScreen(ui: WaitUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false, background = if (ui.overdue) c.warningSoft else c.bg) { wc ->
        ui.notice?.let { n -> AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_warning, iconAsset = "ic_warning") { Text(n, style = Pause.type.section, color = c.text) } }
        ui.heat?.let { h -> AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_thermometer, iconAsset = "ic_thermometer") { Text(h, style = Pause.type.section, color = c.warningInk) } }
        // 전체 창은 바깥 사람용 큰 글 | 차 안 상태 나란히, 1/2·1/3 창은 위아래(스크롤) — 숫자·버튼이 꺾이지 않게
        val wide = wc == WidthClass.FULL
        val body: @Composable (Modifier, Modifier) -> Unit = { left, right ->
            Panel(left, pad = d.u(32f), spacing = d.gap) {
                if (wide) Spacer(Modifier.weight(1f))
                if (ui.overdue) StatusIcon("ic_warning", R.drawable.fe_warning, d.u(88f)) else StatusIcon("wait_house_dog", R.drawable.fe_house, d.u(112f))
                Text(
                    if (ui.overdue) "보호자에게 알렸어요" else ui.headline,
                    style = Pause.type.title.copy(fontSize = Pause.type.title.fontSize * 1.5f, lineHeight = Pause.type.title.lineHeight * 1.5f),
                    color = if (ui.overdue) c.warningInk else c.text,
                )
                Text(ui.elapsed, style = Pause.type.metric, color = c.text)
                if (wide) Spacer(Modifier.weight(1f))
                Text(
                    ui.share.url?.let { "보호자 폰: $it" + if (ui.share.connected) "" else " (연결 확인 중)" } ?: "폰 공유 연결 안 됨 — 차 화면 안내는 계속돼요",
                    style = Pause.type.sub, color = c.sub,
                )
            }
            Panel(right, pad = d.u(24f), spacing = d.gapS) {
                SectionHeader("차 안 상태")
                Divider()
                KeyValue("설정 시간", "${ui.limitMin}분")
                ui.cabin?.let { KeyValue("실내 온도", it.removePrefix("실내 "), valueColor = if (ui.heat != null) c.warningInk else c.text) }
                ui.batteryUsed?.let { KeyValue("배터리 사용", it) }
                ui.seasonNote?.let { IconLine(Icons.Rounded.Thermostat, it, c.sub, maxLines = 2) }
                if (wide) Spacer(Modifier.weight(1f))
                Text("제한 시간 (분)", style = Pause.type.metricLabel, color = c.sub)
                Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    listOf(5, 10, 15, 20).forEach { m -> Choice("$m", ui.limitMin == m, { on(UiEvent.SetWaitLimit(m)) }, Modifier.weight(1f)) }
                }
                PauseButton("돌아왔어요", { on(UiEvent.StopWait) }, Modifier.fillMaxWidth())
            }
        }
        if (wide) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
                body(Modifier.weight(0.62f).fillMaxHeight(), Modifier.weight(0.38f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                body(Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
            }
        }
    }
}

// ═════════════ 함께 가는 장소 (F4) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlacesScreen(ui: PlacesUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var largeDog by rememberSaveable { mutableStateOf(false) }
    var indoor by rememberSaveable { mutableStateOf(false) }
    BackHandler { on(UiEvent.Close) }
    Page(null, on, scroll = true, title = ui.title, trailing = { CloseButton(on) }) { wc ->
        if (ui.items.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                PlaceFilter.groups(ui.items).forEach { (g, n) -> Choice("$g $n", (filter ?: PlaceFilter.ALL) == g, { filter = g }) }
                Box(Modifier.width(d.border).height(d.u(40f)).background(c.border).align(Alignment.CenterVertically))
                Choice("대형견 가능", largeDog, { largeDog = !largeDog })
                Choice("실내 동반 가능", indoor, { indoor = !indoor })
            }
            PlaceFilter.unknownNote(ui.items, filter, largeDog, indoor)?.let { Text(it, style = Pause.type.sub, color = c.sub) }
        }
        Panel(Modifier.fillMaxWidth(), pad = d.u(8f), spacing = d.u(0f)) {
            if (ui.loading) Text("찾는 중…", style = Pause.type.body, color = c.sub, modifier = Modifier.padding(d.gap))
            if (!ui.loading && ui.items.isEmpty()) Text("근처에 찾은 곳이 없어요.", style = Pause.type.body, color = c.sub, modifier = Modifier.padding(d.gap))
            PlaceFilter.apply(ui.items, filter, largeDog, indoor).shown.forEachIndexed { i, p ->
                if (i > 0) Divider(Modifier.padding(horizontal = d.gapS))
                Row(Modifier.fillMaxWidth().padding(d.gapS), verticalAlignment = Alignment.CenterVertically) {
                    if (wc != WidthClass.THIRD) {
                        AsyncImage(
                            p.imageUrl, null,
                            Modifier.width(d.u(120f)).aspectRatio(1.5f).clip(RoundedCornerShape(d.buttonRadius)),
                            placeholder = ColorPainter(c.surfaceAlt), error = ColorPainter(c.surfaceAlt), fallback = ColorPainter(c.surfaceAlt),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(d.gap))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(2f))) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (p.sample) p.name.removePrefix("[샘플] ") else p.name, style = Pause.type.section, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (p.sample) { Spacer(Modifier.width(d.unit)); StatusChip("샘플", neutral = true) }
                        }
                        Text((listOf(p.distance, p.category) + p.tags).joinToString(" · "), style = Pause.type.body, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // 동반 조건 원문 문장 그대로 (실내·대형견 여부를 화면이 판정하지 않는다)
                        p.notes.take(2).forEach { Text(it, style = Pause.type.caption, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    Spacer(Modifier.width(d.gapS))
                    PauseButton("경유지로", { on(UiEvent.GoPlace(p.id)) }, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Place, compact = true)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            ui.note?.let { Text(it, Modifier.weight(1f), style = Pause.type.caption, color = c.sub) }
            ui.credit?.let { Text(it, style = Pause.type.caption, color = c.sub) }
        }
    }
}

// ═════════════ 동물병원 (F5, 정차 중) — 목록 + 사전 접수 ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VetsScreen(ui: VetsUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page(null, on, scroll = true, title = "동물병원", trailing = { CloseButton(on) }) { wc ->
        // 전체 창은 목록 | 사전 접수 나란히, 1/2·1/3 창은 위아래로(병원 이름이 잘리지 않게, 페이지가 스크롤)
        SideBySide(wc == WidthClass.FULL, 0.55f, { m ->
            Panel(m, pad = d.u(24f), spacing = d.gapS) {
                SectionHeader("가까운 동물병원", trailing = "${ui.items.size}곳")
                if (ui.loading) Text("찾는 중…", style = Pause.type.body, color = c.sub)
                if (!ui.loading && ui.items.isEmpty()) Text("근처에 찾은 곳이 없어요.", style = Pause.type.body, color = c.sub)
                ui.items.forEachIndexed { i, v ->
                    Divider()
                    Row(verticalAlignment = Alignment.Top) {
                        NumberBadge(i + 1, active = i == 0)
                        Spacer(Modifier.width(d.gap))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
                            // 병원 이름은 줄이지 않는다(2줄까지), 상태 알약은 이름 아래
                            Text(if (v.sample) v.name.removePrefix("[샘플] ") else v.name, style = Pause.type.section, color = c.text, maxLines = 2)
                            Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                                v.status?.let { StatusChip(it, MotionTone.CALM) }
                                if (v.partner) StatusChip("시연용 제휴") else if (v.sample) StatusChip("샘플", neutral = true)
                            }
                            Text(listOfNotNull(v.distance, v.minutes?.let { "약 ${it}분" }, v.phone).joinToString(" · "), style = Pause.type.body, color = c.sub, maxLines = 1)
                        }
                        Spacer(Modifier.width(d.gapS))
                        PauseButton("안내 시작", { on(UiEvent.GoVet(v.id)) }, icon = Icons.Rounded.Navigation, compact = true, kind = if (i == 0) ButtonKind.PRIMARY else ButtonKind.SECONDARY)
                    }
                }
                ui.note?.let { Text(it, style = Pause.type.caption, color = c.sub) }
            }
        }) { m ->
            val target = ui.items.firstOrNull()
            if (target?.partner == true || ui.intake != null) IntakePanel(ui, on, m) else ContactPanel(ui, on, m)
        }
    }
}

/**
 * 시연용 제휴 병원 사전 접수 (상태 7): 증상·메모 → **보낼 내용 미리보기** → "동의하고 보내기". 보낸 뒤 도착 예정·병원 응답.
 * 제휴 병원(시연용 가상 병원)만 이 패널을 쓴다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntakePanel(ui: VetsUi, on: On, m: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(m, pad = d.u(24f), spacing = d.gapS) {
        SectionHeader("사전 접수", trailing = "시연용 제휴 병원")
        val ik = ui.intake
        if (ik == null) {
            Text("증상을 미리 보내면 병원이 도착 전에 준비해요.", style = Pause.type.sub, color = c.sub)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                ui.memo.forEach { s -> Choice(s.label, s.selected, { on(UiEvent.ToggleMemoChip(s.label)) }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                // 받아쓴 글은 그대로 보여 주고 고칠 수 있다 (정차 중 화면에서만)
                BasicTextField(
                    ui.memoText, { on(UiEvent.SetMemoText(it)) },
                    Modifier.weight(1f).heightIn(min = d.touch).clip(RoundedCornerShape(d.buttonRadius)).background(c.surfaceAlt)
                        .border(d.border, c.border, RoundedCornerShape(d.buttonRadius)).padding(horizontal = d.u(16f), vertical = d.gapS),
                    textStyle = Pause.type.body.copy(color = c.text),
                    cursorBrush = SolidColor(c.primary),
                    decorationBox = { inner ->
                        if (ui.memoText.isEmpty()) Text("예: 구토 두 번, 물은 조금 마셨어요", style = Pause.type.body, color = c.sub)
                        inner()
                    },
                )
                PauseButton(if (ui.memoListening) "듣는 중…" else "음성", { on(UiEvent.StartMemoVoice) }, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Mic, compact = true, enabled = !ui.memoListening)
            }
            ChecklistRow("증상 메모와 오늘 요약 함께 보내기", ui.memoInclude, { on(UiEvent.ToggleMemoInclude) })
            Divider()
            // 전달 동의: 보낼 내용을 먼저 보여 준다
            Text("병원에 보낼 내용", style = Pause.type.metricLabel, color = c.sub)
            ui.preview.forEach { r -> KeyValue(r.key, r.value, valueColor = if (r.key == "보내지 않음") c.sub else c.text) }
            val first = ui.items.firstOrNull()
            PauseButton(
                "동의하고 보내기", { first?.let { on(UiEvent.ShareVetEta(it.id)) } }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Send, enabled = first != null,
            )
            Text("병원 접수함 링크를 만들어요. 도착 예정 시각만 자동으로 갱신돼요.", style = Pause.type.caption, color = c.sub)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Metric(ik.vetName, if (ik.arrived) "도착" else "${ik.etaMinutes}", unit = if (ik.arrived) null else "분 뒤", small = true, modifier = Modifier.weight(1f))
                StatusChip(if (ik.replied) "병원 응답" else "접수함 전송", if (ik.busy) MotionTone.CAUTION else null)
            }
            Text(ik.status, style = Pause.type.bodyStrong, color = if (ik.busy) c.warningInk else c.text)
            KeyValue("병원 접수함", ik.clinicUrl, valueColor = c.primary)
            ui.shareUrl?.let { KeyValue("보호자 링크", it) }
            Text("링크는 보호자가 병원에 문자로 보내요. 2시간 뒤 사라져요.", style = Pause.type.caption, color = c.sub)
        }
    }
}

/**
 * 제휴가 없는 병원 (공공 인허가 데이터의 실제 병원): 사전 접수·"접수 완료"를 보이지 않는다. 전화번호와 도착 예정 링크 공유만.
 */
@Composable
private fun ContactPanel(ui: VetsUi, on: On, m: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    val v = ui.items.firstOrNull()
    Panel(m, pad = d.u(24f), spacing = d.gapS) {
        SectionHeader("병원에 연락")
        Text("이 병원은 사전 접수 연동이 없어요. 전화로 증상과 도착 시간을 알려 주세요.", style = Pause.type.body, color = c.text)
        v?.let { Text(it.name.removePrefix("[샘플] "), style = Pause.type.section, color = c.text) }
        Text(v?.phone ?: "전화번호 정보 없음", style = Pause.type.title, color = if (v?.phone != null) c.primary else c.sub)
        ui.tripLine?.let { KeyValue("오늘 요약", it) }
        Divider()
        PauseButton("도착 예정 링크 만들기", { v?.let { on(UiEvent.ShareVetEta(it.id)) } }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Send, enabled = v != null)
        ui.shareUrl?.let { KeyValue("도착 예정 링크", it, valueColor = c.primary) }
        Text("링크에는 도착 예정 시각만 있어요(위치 없음). 보호자가 병원에 문자로 보내요.", style = Pause.type.caption, color = c.sub)
    }
}

// ═════════════ 도착 체크 (F6) ═════════════

@Composable
fun ArrivalScreen(ui: ArrivalUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false) { wc ->
        // 1/3 창: 사진을 위로, 글은 아래 전체 폭(세로로 한 글자씩 꺾이지 않게)
        val narrow = wc == WidthClass.THIRD
        Panel(Modifier.fillMaxWidth().weight(1f), pad = d.u(if (narrow) 24f else 40f), spacing = d.gap) {
            Spacer(Modifier.weight(1f))
            val body: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                    if (ui.warning == null) Text("시동이 꺼졌어요", style = Pause.type.metricLabel, color = c.sub)
                    Text(ui.question, style = Pause.type.title.copy(fontSize = Pause.type.title.fontSize * 1.5f, lineHeight = Pause.type.title.lineHeight * 1.5f), color = c.text)
                    val warning = ui.warning
                    if (warning != null) {
                        AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_thermometer, iconAsset = "ic_thermometer") {
                            Text(warning, style = Pause.type.section, color = c.warningInk)
                            // 제목에 숫자가 이미 있으면 다시 쓰지 않는다
                            ui.cabin?.takeIf { it !in warning }?.let { Text(it, style = Pause.type.bodyStrong, color = c.warningInk) }
                            ui.detail?.let { Text(it, style = Pause.type.body, color = c.text) }
                        }
                    } else ui.detail?.let { Text(it, style = Pause.type.body, color = c.sub) }
                    Spacer(Modifier.height(d.unit))
                    PauseButton("내렸어요", { on(UiEvent.ConfirmLeft) }, if (narrow) Modifier.fillMaxWidth() else Modifier.widthIn(min = d.u(320f)))
                }
            }
            if (narrow) {
                PetPhoto(Modifier.size(d.u(120f)), CircleShape, "pet_arrival")
                body(Modifier.fillMaxWidth())
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.u(40f))) {
                    PetPhoto(Modifier.size(d.u(180f)), CircleShape, "pet_arrival")
                    body(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

// ═════════════ 반려견 프로필·설정·온보딩 (정차 중에만, DS-04) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileForm(ui: PetEditUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.gapS)) {
        FieldLabel("이름")
        BasicTextField(
            ui.name, { on(UiEvent.SetPetName(it)) },
            Modifier.fillMaxWidth().heightIn(min = d.touch).clip(RoundedCornerShape(d.buttonRadius)).background(c.surfaceAlt)
                .border(d.border, c.border, RoundedCornerShape(d.buttonRadius)).padding(horizontal = d.u(16f), vertical = d.gapS),
            textStyle = Pause.type.section.copy(color = c.text), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), cursorBrush = SolidColor(c.primary),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(d.u(40f))) {
            Column(verticalArrangement = Arrangement.spacedBy(d.unit)) {
                FieldLabel("체급")
                Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    listOf(SizeUi.SMALL to "소형", SizeUi.MEDIUM to "중형", SizeUi.LARGE to "대형").forEach { (s, l) -> Choice(l, ui.size == s, { on(UiEvent.SetPetSize(s)) }) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(d.unit)) {
                FieldLabel("나이")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                    PauseButton("−", { on(UiEvent.SetPetAge(ui.ageYears - 1)) }, kind = ButtonKind.SECONDARY, compact = true)
                    Text("${ui.ageYears}살", style = Pause.type.section, color = c.text)
                    PauseButton("+", { on(UiEvent.SetPetAge(ui.ageYears + 1)) }, kind = ButtonKind.SECONDARY, compact = true)
                }
            }
        }
        FieldLabel("흔들림에 예민한 정도 (보호자 응답)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
            listOf(TendencyUi.LOW to "거의 안 해요", TendencyUi.NORMAL to "가끔", TendencyUi.HIGH to "자주 해요").forEach { (t, l) -> Choice(l, ui.tendency == t, { on(UiEvent.SetPetTendency(t)) }) }
        }
        FieldLabel("배변·산책 간격")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
            listOf(60, 90, 120, 150, 180).forEach { m -> Choice("${m}분", ui.pottyIntervalMin == m, { on(UiEvent.SetPotty(m)) }) }
        }
        ToggleRow("반려견 놀이터가 있는 휴게소를 먼저", ui.petZonePreferred, { on(UiEvent.TogglePetZone) })
        ui.problems.forEach { Text(it, style = Pause.type.sub, color = c.danger) }
    }
}

@Composable
private fun FieldLabel(text: String) = Text(text, style = Pause.type.metricLabel, color = Pause.colors.sub)

private fun tendencyWord(t: TendencyUi) = when (t) {
    TendencyUi.LOW -> "덜 예민"
    TendencyUi.NORMAL -> "보통"
    TendencyUi.HIGH -> "예민"
}

private fun sizeWord(s: SizeUi) = when (s) {
    SizeUi.SMALL -> "소형"
    SizeUi.MEDIUM -> "중형"
    SizeUi.LARGE -> "대형"
}

/** 반려견 상태·프로필: 왼쪽 원형 사진·이름·요약·맞춤 정도, 오른쪽 편집. */
@Composable
fun PetProfileScreen(ui: PetEditUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page(null, on, scroll = true, title = if (ui.firstRun) "반려견을 알려 주세요" else "반려견 프로필") { wc ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            if (wc != WidthClass.THIRD) {
                Panel(Modifier.weight(0.34f), pad = d.u(28f), spacing = d.gapS) {
                    PetPhoto(Modifier.size(d.u(140f)).align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(d.unit))
                    Text(ui.name.ifBlank { "이름 없음" }, style = Pause.type.title, color = c.text, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("${sizeWord(ui.size)} · ${ui.ageYears}살 · ${tendencyWord(ui.tendency)}", style = Pause.type.sub, color = c.sub, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Divider()
                    KeyValue("배변·산책 간격", "${ui.pottyIntervalMin}분")
                    KeyValue("놀이터 휴게소 먼저", if (ui.petZonePreferred) "켜짐" else "꺼짐")
                    ui.calibration?.let { cal ->
                        Divider()
                        Text("맞춤 정도", style = Pause.type.metricLabel, color = c.sub)
                        Text(cal.substringAfter(": "), style = Pause.type.bodyStrong, color = c.primary)
                        PauseButton("맞춤 초기화", { on(UiEvent.ResetCalibration) }, kind = ButtonKind.QUIET, compact = true)
                    }
                    Text("프로필은 이 차에만 저장되고 서버로 보내지 않아요.", style = Pause.type.caption, color = c.sub)
                }
            }
            Panel(Modifier.weight(0.66f), pad = d.u(28f)) { ProfileForm(ui, on) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            PauseButton(if (ui.firstRun) "나중에" else "닫기", { on(UiEvent.Close) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            PauseButton("저장", { on(UiEvent.SavePet) }, Modifier.weight(1f), enabled = ui.problems.isEmpty())
        }
    }
}

/** 첫 실행 3단계: 반려견 프로필 → 차량 연동(신호 확인 결과) → 멀미 민감도 진단(질문 3개 → 3단계 결과). */
@Composable
fun OnboardingScreen(ui: OnboardingUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = true, title = "처음 설정") { _ ->
        StepIndicator(ui.step)
        Panel(Modifier.fillMaxWidth(), pad = d.u(28f), spacing = d.gapS) {
            when (ui.step) {
                1 -> {
                    SectionHeader("반려견 프로필", trailing = "엔진이 쓰는 값만 받아요")
                    Divider()
                    Row(horizontalArrangement = Arrangement.spacedBy(d.u(28f))) {
                        PetPhoto(Modifier.size(d.u(120f)))
                        Box(Modifier.weight(1f)) { ProfileForm(ui.pet, on) }
                    }
                }
                2 -> {
                    SectionHeader("차량 연동", trailing = "확인된 신호만 써요")
                    Divider()
                    Row(horizontalArrangement = Arrangement.spacedBy(d.u(28f)), verticalAlignment = Alignment.CenterVertically) {
                        ArtSlot("onboarding", "onboarding_cars", modifier = Modifier.width(d.u(260f)).aspectRatio(1.75f)) { SuvIllustration(Modifier.fillMaxSize().background(c.surfaceAlt)) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                            ui.signals.forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CheckCircle(s.inUse, d.u(20f))
                                    Spacer(Modifier.width(d.gapS))
                                    Text(s.name, style = Pause.type.body, color = if (s.inUse) c.text else c.sub, modifier = Modifier.weight(1f))
                                    Text(s.source ?: "이 차에서 못 받음", style = Pause.type.sub, color = c.sub)
                                }
                            }
                        }
                    }
                    Text("받지 못한 신호는 지어내지 않고 그 기능만 쉬어요.", style = Pause.type.sub, color = c.sub)
                }
                else -> {
                    SectionHeader("흔들림에 예민한 정도", trailing = "보호자 응답 · 질문 3개")
                    Divider()
                    ui.quiz.forEachIndexed { qi, q ->
                        Text("${qi + 1}. ${q.question}", style = Pause.type.bodyStrong, color = c.text)
                        Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                            q.options.forEachIndexed { oi, o -> Choice(o, q.selected == oi, { on(UiEvent.AnswerQuiz(qi, oi)) }) }
                        }
                    }
                    ui.result?.let { r ->
                        AlertStrip(Modifier.fillMaxWidth(), accent = c.primary, soft = c.primarySoft) {
                            Text("결과: ${MotionQuiz.label(r)}", style = Pause.type.section, color = c.text)
                            Text("측정이나 진단이 아니라 보호자 답으로 예보 민감도를 정해요. 프로필에서 언제든 바꿀 수 있어요.", style = Pause.type.sub, color = c.sub)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            PauseButton(if (ui.step == 1) "나중에" else "이전", { on(if (ui.step == 1) UiEvent.Close else UiEvent.OnboardBack) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            PauseButton(if (ui.step == 3) "시작하기" else "다음", { on(UiEvent.OnboardNext) }, Modifier.weight(1f), enabled = ui.canNext)
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
        listOf("반려견 프로필", "차량 연동", "흔들림에 예민한 정도").forEachIndexed { i, label ->
            if (i > 0) Box(Modifier.width(d.u(40f)).height(d.u(2f)).background(if (i < step) c.primary else c.border))
            NumberBadge(i + 1, active = i + 1 <= step)
            Text(label, style = Pause.type.bodyStrong, color = if (i + 1 == step) c.text else c.sub)
        }
    }
}

@Composable
fun SettingsScreen(ui: SettingsUi, on: On, licenses: String) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page(null, on, scroll = true, title = "설정", trailing = { CloseButton(on) }) { wc ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            Panel(Modifier.weight(1f).fillMaxHeight(), pad = d.u(24f)) {
                SectionHeader("제안과 알림")
                Divider()
                Text("쉼표 제안 시점", style = Pause.type.metricLabel, color = c.sub)
                Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    listOf("빠르게", "보통", "여유 있게").forEachIndexed { i, l -> Choice(l, ui.timing == i, { on(UiEvent.SetTiming(i)) }) }
                }
                ToggleRow("음성으로 제안", ui.voiceSuggest, { on(UiEvent.ToggleVoice) }, "끄면 화면 카드와 버튼만")
                Divider()
                ToggleRow("움직임 줄이기", ui.reduceMotion, { on(UiEvent.ToggleReduceMotion) })
            }
            if (wc != WidthClass.THIRD) {
                Panel(Modifier.weight(1f).fillMaxHeight(), pad = d.u(24f)) {
                    SectionHeader("이 차에서 쓰는 신호")
                    Divider()
                    ui.shapeSource?.let { KeyValue("앞 구간 형상", it) }
                    ui.signals.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CheckCircle(s.inUse, d.u(20f))
                            Spacer(Modifier.width(d.gapS))
                            Text(s.name, style = Pause.type.body, color = if (s.inUse) c.text else c.sub, modifier = Modifier.weight(1f))
                            Text(s.source ?: "미지원", style = Pause.type.sub, color = c.sub)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            Panel(Modifier.weight(1f).fillMaxHeight(), pad = d.u(24f)) {
                SectionHeader("개인정보")
                Text("반려견 프로필·이동 기록은 이 차에만 있어요. 대기 공유·병원 사전 접수에는 위치를 보내지 않아요.", style = Pause.type.body, color = c.sub)
                PauseButton("내 데이터 삭제", { on(UiEvent.DeleteData) }, kind = ButtonKind.DANGER, compact = true)
            }
            Panel(Modifier.weight(1f).fillMaxHeight(), pad = d.u(24f)) {
                SectionHeader("데이터 출처")
                ui.credits.forEach { Text(it, style = Pause.type.caption, color = c.sub) }
            }
        }
        Text("같이가개 ${ui.version} · 주행 자극은 차량 흔들림 기준이며 반려견 측정·진단이 아니에요.", style = Pause.type.sub, color = c.sub)
        if (licenses.isNotBlank()) Text(licenses.take(400), style = Pause.type.caption, color = c.sub)
    }
}

// ───────────── 미리보기 (앱 영역 1680×1210dp, 배율 2) ─────────────

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PDepart() = PauseTheme(false, 2f) { DepartScreen(Fixtures.depart) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PWait() = PauseTheme(false, 2f) { WaitScreen(Fixtures.wait) {} }

/** 미리보기 전용 자리 표시 (사진이 없을 때의 일러스트 확인). */
@Preview(widthDp = 400, heightDp = 400) @Composable private fun PDog() = PauseTheme(false, 2f) { DogIllustration(Modifier.fillMaxSize()) }
