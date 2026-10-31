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
import com.emop.pause.ui.components.TrendChart
import com.emop.pause.ui.components.VDivider
import com.emop.pause.ui.components.toneInk
import com.emop.pause.ui.components.toneWord
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.ArrivalUi
import com.emop.pause.uistate.DepartUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.MotionAnswerUi
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
import com.emop.pause.uistate.TripLogUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.VetsUi
import com.emop.pause.uistate.WaitUi
import com.emop.pause.uistate.motionTone

/*
 * 정차 중 화면 v6 (docs/design/v6/STYLE_V6.md §7.3~7.12, docs/ui_contract.md).
 * 모든 화면은 상태(UiState)만 보고 그리며, 사용자 동작은 [UiEvent]로만 돌려보낸다.
 * 규칙: 화면당 큰 패널 1~3개, 패널 안은 구분선으로 나누고 카드를 겹쳐 쌓지 않는다. 사진은 원형(프로필)이나 고정 비율만.
 */

// ═════════════ 출발 체크 (F6) ═════════════

@Composable
fun DepartScreen(ui: DepartUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(ui.meta, on, scroll = false) { wc ->
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            if (wc == WidthClass.FULL) {
                Panel(Modifier.weight(0.28f).fillMaxHeight(), pad = d.u(24f), spacing = d.gapS) {
                    PetPhoto(Modifier.size(d.u(120f)).align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(d.unit))
                    Text(ui.pet.name, style = Pause.type.title, color = c.text, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(ui.pet.line, style = Pause.type.sub, color = c.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                    if (ui.pet.sample) Text("프로필을 저장하면 맞춤 안내가 시작돼요", style = Pause.type.caption, color = c.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    ui.limitLine?.let {
                        Divider()
                        Text(it, style = Pause.type.bodyStrong, color = c.warningInk)
                    }
                    PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, compact = true)
                }
            }
            Panel(Modifier.weight(0.38f).fillMaxHeight(), pad = d.u(24f), spacing = d.gapS) {
                SectionHeader("출발 전 체크", trailing = "${ui.checks.count { it.checked }} / ${ui.checks.size}")
                Divider()
                ui.checks.forEachIndexed { i, ch ->
                    if (i > 0) Divider()
                    ChecklistRow(ch.label, ch.checked, { on(UiEvent.ToggleCheck(ch.kind)) })
                }
                Divider()
                Text(ui.rationale, style = Pause.type.sub, color = c.sub)
                Spacer(Modifier.weight(1f))
                if (wc != WidthClass.FULL) ui.limitLine?.let { Text(it, style = Pause.type.bodyStrong, color = c.warningInk) }
                if (wc != WidthClass.FULL) PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, kind = ButtonKind.SECONDARY, compact = true)
            }
            Panel(Modifier.weight(0.34f).fillMaxHeight(), pad = d.u(24f), spacing = d.gapS) {
                Text(ui.route ?: "경로를 기다리는 중", style = Pause.type.section, color = c.text, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
                    ui.duration?.let { Metric("예상 시간", it, small = true) }
                    Metric("예상 쉼표", "${ui.plan.size}", unit = "번", small = true)
                }
                Divider()
                ui.plan.take(3).forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.eta, style = Pause.type.bodyStrong, color = c.text, maxLines = 1)
                        Spacer(Modifier.width(d.gapS))
                        Text(p.name, style = Pause.type.body, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (!ui.ready) Text("세 가지를 확인하면 출발할 수 있어요", style = Pause.type.sub, color = c.sub)
                PauseButton("이대로 출발", { on(UiEvent.StartTrip) }, Modifier.fillMaxWidth(), enabled = ui.ready)
                PauseButton("설정", { on(UiEvent.OpenSettings) }, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET, compact = true)
            }
        }
    }
}

// ═════════════ 쉼표 (정차) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestScreen(ui: RestUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(ui.meta, on, scroll = true) { wc ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            Panel(Modifier.weight(0.38f).fillMaxHeight(), pad = d.u(24f), spacing = d.gapS) {
                Text(ui.lead, style = Pause.type.title, color = c.text)
                ui.motion?.let { m ->
                    Metric("멀미 지수", "${m.index}", unit = "/ 100", note = toneWord(m.tone), color = toneInk(m.tone), noteColor = toneInk(m.tone), small = true)
                }
                ui.battery?.let { IconLine(Icons.Rounded.BatteryStd, it) }
                ui.restChoice?.let { sel ->
                    Divider()
                    Text("쉬는 시간", style = Pause.type.metricLabel, color = c.sub)
                    Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                        listOf(10, 15, 20).forEach { m -> Choice("${m}분", sel == m, { on(UiEvent.SetRestMinutes(m)) }) }
                    }
                }
                Spacer(Modifier.weight(1f))
                PauseButton("잠깐 다녀올게 (${ui.waitLimitMin}분)", { on(UiEvent.StartWait) }, Modifier.fillMaxWidth())
                PauseButton("함께 가는 장소", { on(UiEvent.OpenPlaces) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Place, compact = true)
                PauseButton("동물병원", { on(UiEvent.OpenVets) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, compact = true)
            }
            Column(Modifier.weight(0.62f), verticalArrangement = Arrangement.spacedBy(d.gap)) {
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
                if (wc != WidthClass.THIRD) {
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
                        SectionHeader("콩이가 이상해요", trailing = "고르면 체크리스트를 보여 줘요")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                            ui.distressChips.forEach { l -> Choice(l, false, { on(UiEvent.ReportDistress(l)) }) }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════ 잠깐 대기 (F3): 바깥 사람이 읽는 차 화면 ═════════════

@Composable
fun WaitScreen(ui: WaitUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false, background = if (ui.overdue) c.warningSoft else c.bg) { _ ->
        ui.notice?.let { n -> AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_warning, iconAsset = "ic_warning") { Text(n, style = Pause.type.section, color = c.text) } }
        ui.heat?.let { h -> AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_thermometer, iconAsset = "ic_thermometer") { Text(h, style = Pause.type.section, color = c.warningInk) } }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            Panel(Modifier.weight(0.62f).fillMaxHeight(), pad = d.u(32f), spacing = d.gap) {
                Spacer(Modifier.weight(1f))
                if (ui.overdue) StatusIcon("ic_warning", R.drawable.fe_warning, d.u(88f)) else StatusIcon("wait_house_dog", R.drawable.fe_house, d.u(112f))
                Text(
                    if (ui.overdue) "보호자에게 알렸어요" else ui.headline,
                    style = Pause.type.title.copy(fontSize = Pause.type.title.fontSize * 1.5f, lineHeight = Pause.type.title.lineHeight * 1.5f),
                    color = if (ui.overdue) c.warningInk else c.text,
                )
                Text(ui.elapsed, style = Pause.type.metric, color = c.text)
                Spacer(Modifier.weight(1f))
                Text(
                    ui.share.url?.let { "보호자 폰: $it" + if (ui.share.connected) "" else " (연결 확인 중)" } ?: "폰 공유 연결 안 됨 — 차 화면 안내는 계속돼요",
                    style = Pause.type.sub, color = c.sub,
                )
            }
            Panel(Modifier.weight(0.38f).fillMaxHeight(), pad = d.u(24f), spacing = d.gapS) {
                SectionHeader("차 안 상태")
                Divider()
                KeyValue("설정 시간", "${ui.limitMin}분")
                ui.cabin?.let { KeyValue("실내 온도", it.removePrefix("실내 "), valueColor = if (ui.heat != null) c.warningInk else c.text) }
                ui.batteryUsed?.let { KeyValue("배터리 사용", it) }
                ui.seasonNote?.let { IconLine(Icons.Rounded.Thermostat, it, c.sub, maxLines = 2) }
                Spacer(Modifier.weight(1f))
                Text("제한 시간", style = Pause.type.metricLabel, color = c.sub)
                Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    listOf(5, 10, 15, 20).forEach { m -> Choice("${m}분", ui.limitMin == m, { on(UiEvent.SetWaitLimit(m)) }, Modifier.weight(1f)) }
                }
                PauseButton("돌아왔어요", { on(UiEvent.StopWait) }, Modifier.fillMaxWidth())
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
    Page(null, on, scroll = true, title = "동물병원", trailing = { CloseButton(on) }) { _ ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            Panel(Modifier.weight(0.55f), pad = d.u(24f), spacing = d.gapS) {
                SectionHeader("가까운 동물병원", trailing = "${ui.items.size}곳")
                if (ui.loading) Text("찾는 중…", style = Pause.type.body, color = c.sub)
                if (!ui.loading && ui.items.isEmpty()) Text("근처에 찾은 곳이 없어요.", style = Pause.type.body, color = c.sub)
                ui.items.forEachIndexed { i, v ->
                    Divider()
                    Row(verticalAlignment = Alignment.Top) {
                        NumberBadge(i + 1, active = i == 0)
                        Spacer(Modifier.width(d.gap))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (v.sample) v.name.removePrefix("[샘플] ") else v.name, style = Pause.type.section, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                Spacer(Modifier.width(d.unit))
                                v.status?.let { StatusChip(it, MotionTone.CALM) }
                                if (v.sample) StatusChip("샘플", neutral = true)
                            }
                            Text(listOfNotNull(v.distance, v.minutes?.let { "약 ${it}분" }, v.phone).joinToString(" · "), style = Pause.type.body, color = c.sub, maxLines = 1)
                        }
                        Spacer(Modifier.width(d.gapS))
                        PauseButton("안내 시작", { on(UiEvent.GoVet(v.id)) }, icon = Icons.Rounded.Navigation, compact = true, kind = if (i == 0) ButtonKind.PRIMARY else ButtonKind.SECONDARY)
                    }
                }
                ui.note?.let { Text(it, style = Pause.type.caption, color = c.sub) }
            }
            Panel(Modifier.weight(0.45f), pad = d.u(24f), spacing = d.gapS) {
                SectionHeader("사전 접수", trailing = "이 차에만 저장")
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
                ui.tripLine?.let { KeyValue("오늘 요약", it) }
                Divider()
                val first = ui.items.firstOrNull()
                val ik = ui.intake
                if (ik == null) {
                    PauseButton(
                        first?.let { "${it.name.removePrefix("[샘플] ")}에 사전 접수" } ?: "사전 접수 보내기",
                        { first?.let { on(UiEvent.ShareVetEta(it.id)) } }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Send, enabled = first != null,
                    )
                    Text("병원 접수함 링크를 만들어요. 위치는 보내지 않고 도착 예정 시각만 갱신돼요.", style = Pause.type.caption, color = c.sub)
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
    }
}

// ═════════════ 도착 체크 (F6) ═════════════

@Composable
fun ArrivalScreen(ui: ArrivalUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false) { _ ->
        Panel(Modifier.fillMaxWidth().weight(1f), pad = d.u(40f), spacing = d.gap) {
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.u(40f))) {
                PetPhoto(Modifier.size(d.u(180f)), CircleShape, "pet_arrival")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
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
                    PauseButton("내렸어요", { on(UiEvent.ConfirmLeft) }, Modifier.widthIn(min = d.u(320f)))
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

// ═════════════ 이동 기록 (F7) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripLogScreen(ui: TripLogUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(ui.meta, on, scroll = false) { wc ->
        // 개인 맞춤 보정 (UPGRADE 4): 도착 뒤 한 번만, 정차 중. 음성으로도 답할 수 있다.
        ui.askMotion?.let { q ->
            AlertStrip(Modifier.fillMaxWidth(), accent = c.primary, soft = c.primarySoft) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    Column(Modifier.weight(1f)) {
                        Text(q, style = Pause.type.section, color = c.text)
                        Text("답할수록 다음 예보가 이 아이에게 맞춰져요", style = Pause.type.sub, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PauseButton("네", { on(UiEvent.AnswerMotion(MotionAnswerUi.YES)) }, Modifier.width(d.u(96f)), compact = true)
                    PauseButton("조금", { on(UiEvent.AnswerMotion(MotionAnswerUi.LITTLE)) }, Modifier.width(d.u(96f)), kind = ButtonKind.SECONDARY, compact = true)
                    PauseButton("아니요", { on(UiEvent.AnswerMotion(MotionAnswerUi.NO)) }, Modifier.width(d.u(96f)), kind = ButtonKind.SECONDARY, compact = true)
                    PauseButton("건너뛰기", { on(UiEvent.AnswerMotion(null)) }, kind = ButtonKind.QUIET, compact = true)
                }
            }
        }
        // 위: 요약 수치 + 핵심 인사이트 한 줄 (수치와 아래 그래프가 같은 값을 말한다)
        Panel(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.gap) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Metric("총 주행 시간", hm(ui.durationMin), small = true, modifier = Modifier.weight(1f))
                VDivider()
                Metric("휴식", "${ui.stops}", unit = "번", small = true, modifier = Modifier.weight(1f).padding(start = d.u(24f)))
                VDivider()
                Metric("멀미 주의", "${ui.warnings}", unit = "번", small = true, color = if (ui.warnings > 0) c.warningInk else c.text, modifier = Modifier.weight(1f).padding(start = d.u(24f)))
                VDivider()
                Metric("최고 지수", "${ui.motionPeak}", unit = "/ 100", small = true, color = toneInk(motionTone(ui.motionPeak)), modifier = Modifier.weight(1f).padding(start = d.u(24f)))
            }
            // 핵심 인사이트: 큰 배너 대신 왼쪽 선 한 줄
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(d.u(4f)).fillMaxHeight().background(c.navy))
                Column(Modifier.padding(start = d.gap), verticalArrangement = Arrangement.spacedBy(d.u(2f))) {
                    Text(ui.limitLine, style = Pause.type.section, color = c.text)
                    listOfNotNull(ui.events, ui.calibration).takeIf { it.isNotEmpty() }?.let { lines ->
                        Text(lines.joinToString("  ·  "), style = Pause.type.bodyStrong, color = if (ui.events != null) c.warningInk else c.primary)
                    }
                }
            }
        }
        // 아래: 추이 그래프(왼쪽) · 관찰 기록과 병원 공유 요약(오른쪽) — 한 화면에
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            if (wc != WidthClass.THIRD) {
                Panel(Modifier.weight(0.6f).fillMaxHeight(), pad = d.u(24f), spacing = d.gapS) {
                    SectionHeader("멀미 지수 추이", trailing = "5분 간격 · 진단 아님")
                    TrendChart(ui.motionSeries, Modifier.fillMaxWidth().weight(1f))
                    Text(ui.summary, style = Pause.type.sub, color = c.sub, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                Panel(Modifier.fillMaxWidth(), pad = d.u(24f)) {
                    SectionHeader("오늘의 관찰 기록")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                        ui.symptoms.forEach { s -> Choice(s.label, s.selected, { on(UiEvent.ToggleSymptom(s.label)) }) }
                    }
                }
                Panel(Modifier.fillMaxWidth(), pad = d.u(24f)) {
                    SectionHeader("동물병원 공유용 요약")
                    ui.vetSummary.forEach { Text(it, style = Pause.type.body, color = c.text) }
                    if (ui.history.isNotEmpty()) {
                        Divider()
                        Text("이전 기록", style = Pause.type.metricLabel, color = c.sub)
                        ui.history.forEach { h -> KeyValue(h.date, h.line) }
                    }
                }
                PauseButton("확인", { on(UiEvent.FinishTrip) }, Modifier.fillMaxWidth())
            }
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
        FieldLabel("차멀미 성향")
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
    TendencyUi.LOW -> "멀미 거의 안 함"
    TendencyUi.NORMAL -> "멀미 가끔"
    TendencyUi.HIGH -> "멀미 자주"
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
                    SectionHeader("멀미 민감도 진단", trailing = "질문 3개")
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
                            Text("진단이 아니라 멀미 예보의 출발점이에요. 프로필에서 언제든 바꿀 수 있어요.", style = Pause.type.sub, color = c.sub)
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
        listOf("반려견 프로필", "차량 연동", "멀미 민감도 진단").forEachIndexed { i, label ->
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
        Text("같이가개 ${ui.version} · 멀미 지수는 주의 예보이며 의학적 진단이 아니에요.", style = Pause.type.sub, color = c.sub)
        if (licenses.isNotBlank()) Text(licenses.take(400), style = Pause.type.caption, color = c.sub)
    }
}

// ───────────── 미리보기 (앱 영역 1680×1210dp, 배율 2) ─────────────

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PDepart() = PauseTheme(false, 2f) { DepartScreen(Fixtures.depart) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PWait() = PauseTheme(false, 2f) { WaitScreen(Fixtures.wait) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PLog() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog) {} }

/** 미리보기 전용 자리 표시 (사진이 없을 때의 일러스트 확인). */
@Preview(widthDp = 400, heightDp = 400) @Composable private fun PDog() = PauseTheme(false, 2f) { DogIllustration(Modifier.fillMaxSize()) }
