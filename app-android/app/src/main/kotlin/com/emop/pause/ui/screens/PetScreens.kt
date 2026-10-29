package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.components.AppBar
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.CheckRow
import com.emop.pause.ui.components.Choice
import com.emop.pause.ui.components.MotionChart
import com.emop.pause.ui.components.MotionLine
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PauseCard
import com.emop.pause.ui.components.Pill
import com.emop.pause.ui.components.SectionTitle
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.uistate.ArrivalUi
import com.emop.pause.uistate.DepartUi
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.PetEditUi
import com.emop.pause.uistate.PlacesUi
import com.emop.pause.uistate.RestUi
import com.emop.pause.uistate.SettingsUi
import com.emop.pause.uistate.SizeUi
import com.emop.pause.uistate.TendencyUi
import com.emop.pause.uistate.TripLogUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.VetsUi
import com.emop.pause.uistate.VoiceSheetState
import com.emop.pause.uistate.WaitUi

/*
 * 같이가개 기본 화면 (PIVOT 4단계, docs/ui_contract.md). 디자인 v5가 오면 이 파일만 갈아 끼운다.
 * 모든 화면은 상태(UiState)만 보고 그리며, 사용자 동작은 [UiEvent]로만 돌려보낸다.
 */

private typealias On = (UiEvent) -> Unit

@Composable
private fun Page(meta: String?, on: On, scroll: Boolean, background: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Pause.colors
    Column(Modifier.fillMaxSize().background(background ?: c.paper)) {
        AppBar(meta, { on(UiEvent.LogoTap) })
        val body = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 48.dp, vertical = 16.dp)
        Column(if (scroll) body.verticalScroll(rememberScrollState()) else body, verticalArrangement = Arrangement.spacedBy(24.dp), content = content)
    }
}

// ───────────── 주행 (F1·F2): 텍스트 2줄·버튼 2개·스크롤 없음 ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriveScreen(ui: DriveUi, on: On) {
    val c = Pause.colors
    Page(ui.meta, on, scroll = false) {
        if (!ui.restricted) ui.motion?.let { MotionLine(it.label, it.tone) }
        when (val ct = ui.content) {
            is DriveContent.Calm -> Text(ct.line, style = Pause.type.sentence, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            is DriveContent.Forecast -> Text(ct.line, style = Pause.type.sentence, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            is DriveContent.Heading -> Text("${ct.minutes}분 뒤 ${ct.place}로 가는 중", style = Pause.type.sentence, color = c.ink, maxLines = 2)
            is DriveContent.Suggest -> {
                Text("${ct.minutes}분 뒤", style = Pause.type.title, color = c.ink2)
                Text(ct.place, style = Pause.type.headline, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!ui.restricted) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ct.reasons.forEach { Pill(it) } }
                if (ui.voice == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, kind = if (ct.urgent) ButtonKind.ALERT else ButtonKind.PRIMARY)
                        if (!ct.urgent) PauseButton("나중에", { on(UiEvent.DeclineSuggestion) }, kind = ButtonKind.QUIET)
                    }
                    Text("\"여기서 쉴게요\"라고 말해도 돼요", style = Pause.type.label, color = c.ink2)
                }
            }
        }
        ui.toast?.let { Pill(it) }
        Spacer(Modifier.weight(1f))
        when (ui.voice) {
            VoiceSheetState.LISTENING -> PauseCard(Modifier.fillMaxWidth(), tint = c.restSoft) { Text("듣고 있어요…", style = Pause.type.title, color = c.restInk) }
            VoiceSheetState.FAILED -> PauseCard(Modifier.fillMaxWidth()) {
                Text("잘 못 들었어요. 버튼으로 골라 주세요.", style = Pause.type.title, color = c.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) })
                    PauseButton("나중에", { on(UiEvent.DeclineSuggestion) }, kind = ButtonKind.QUIET)
                }
            }
            null -> if (ui.offline) Text("인터넷 없이 내장 데이터로 안내 중", style = Pause.type.label, color = c.ink3)
        }
    }
}

// ───────────── 출발 체크 (F6) ─────────────

@Composable
fun DepartScreen(ui: DepartUi, on: On) {
    val c = Pause.colors
    Page(ui.meta, on, scroll = true) {
        PauseCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ui.pet.name, style = Pause.type.headline, color = c.ink)
                    Text(ui.pet.line, style = Pause.type.body, color = c.ink2)
                }
                PauseButton("바꾸기", { on(UiEvent.OpenPet) }, kind = ButtonKind.GHOST, small = true)
            }
            ui.limitLine?.let { Text(it, style = Pause.type.bodyStrong, color = c.restInk) }
        }
        PauseCard(Modifier.fillMaxWidth()) {
            Text("타기 전에 확인해요", style = Pause.type.title, color = c.ink)
            ui.checks.forEach { ch -> CheckRow(ch.label, ch.checked, { on(UiEvent.ToggleCheck(ch.kind)) }) }
        }
        PauseCard(Modifier.fillMaxWidth()) {
            Text("오늘의 쉼표", style = Pause.type.title, color = c.ink)
            if (ui.plan.isEmpty()) Text("경로를 받으면 쉴 곳을 잡아 드려요.", style = Pause.type.body, color = c.ink2)
            ui.plan.forEach { p ->
                Row {
                    Text(p.eta, Modifier.width(220.dp), style = Pause.type.bodyStrong, color = c.ink2)
                    Column { Text(p.name, style = Pause.type.item, color = c.ink); Text(p.facilities, style = Pause.type.label, color = c.ink2) }
                }
            }
            Text(ui.rationale, style = Pause.type.body, color = c.ink2)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            PauseButton("이대로 출발", { on(UiEvent.StartTrip) }, enabled = ui.ready)
            if (!ui.ready) Text("세 가지를 확인하면 출발할 수 있어요", style = Pause.type.label, color = c.ink2)
            Spacer(Modifier.weight(1f))
            PauseButton("설정", { on(UiEvent.OpenSettings) }, kind = ButtonKind.GHOST, small = true)
        }
    }
}

// ───────────── 쉼표 (정차) ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestScreen(ui: RestUi, on: On) {
    val c = Pause.colors
    Page(ui.meta, on, scroll = true) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${ui.minutes}", style = Pause.type.display, color = c.rest)
            Text("분", Modifier.padding(start = 8.dp, bottom = 24.dp), style = Pause.type.displayUnit, color = c.rest)
            Spacer(Modifier.width(32.dp))
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(ui.lead, style = Pause.type.title, color = c.ink)
                ui.motion?.let { Text(it.label, style = Pause.type.body, color = c.ink2) }
                ui.battery?.let { Text(it, style = Pause.type.label, color = c.ink2) }
            }
        }
        ui.restChoice?.let { sel ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(10, 15, 20).forEach { m -> Choice("${m}분", sel == m, { on(UiEvent.SetRestMinutes(m)) }) }
            }
        }
        ui.activities.forEach { a ->
            PauseCard(Modifier.fillMaxWidth(), tint = if (a.recommended) c.restSoft else null) {
                Text("${a.who} · ${a.title}" + if (a.recommended) "  (추천)" else "", style = Pause.type.activity, color = c.ink)
                Text(a.description, style = Pause.type.body, color = c.ink2)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PauseButton("잠깐 다녀올게 (${ui.waitLimitMin}분)", { on(UiEvent.StartWait) })
            PauseButton("함께 가는 장소", { on(UiEvent.OpenPlaces) }, kind = ButtonKind.QUIET)
            PauseButton("동물병원", { on(UiEvent.OpenVets) }, kind = ButtonKind.QUIET)
        }
    }
}

// ───────────── 잠깐 대기 (F3): 바깥 사람이 읽는 차 화면 ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaitScreen(ui: WaitUi, on: On) {
    val c = Pause.colors
    val ink = if (ui.overdue) Color.White else c.ink
    Page(null, on, scroll = false, background = if (ui.overdue) c.alert else c.restSoft) {
        Text(if (ui.overdue) "보호자에게 알렸어요" else ui.headline, style = Pause.type.hero, color = ink, maxLines = 2)
        Text(ui.elapsed, style = Pause.type.display, color = ink)
        Text(listOfNotNull(ui.cabin, ui.battery).joinToString(" · "), style = Pause.type.title, color = ink)
        Spacer(Modifier.weight(1f))
        Text(
            ui.share.url?.let { "보호자 폰: $it" + if (ui.share.connected) "" else " (연결 확인 중)" } ?: "폰 공유 연결 안 됨 — 차 화면 안내는 계속돼요",
            style = Pause.type.label, color = ink,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PauseButton("돌아왔어요", { on(UiEvent.StopWait) }, kind = if (ui.overdue) ButtonKind.QUIET else ButtonKind.PRIMARY)
            listOf(5, 10, 15, 20).forEach { m -> Choice("${m}분", ui.limitMin == m, { on(UiEvent.SetWaitLimit(m)) }) }
        }
    }
}

// ───────────── 함께 가는 장소·동물병원 (F4·F5) ─────────────

@Composable
fun PlacesScreen(ui: PlacesUi, on: On) {
    val c = Pause.colors
    Page(ui.title, on, scroll = true) {
        CloseRow(on)
        if (ui.loading) Text("찾는 중…", style = Pause.type.body, color = c.ink2)
        if (!ui.loading && ui.items.isEmpty()) Text("근처에 찾은 곳이 없어요.", style = Pause.type.body, color = c.ink2)
        ui.items.forEach { p ->
            PauseCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = Pause.type.item, color = c.ink)
                        Text("${p.category} · ${p.distance}", style = Pause.type.label, color = c.ink2)
                        p.policy?.let { Text(it, style = Pause.type.label, color = c.ink2) }
                    }
                    if (p.sample) Pill("샘플", sample = true)
                    Spacer(Modifier.width(16.dp))
                    PauseButton("경유지로", { on(UiEvent.GoPlace(p.id)) }, small = true)
                }
            }
        }
        ui.note?.let { Text(it, style = Pause.type.label, color = c.ink3) }
    }
}

@Composable
fun VetsScreen(ui: VetsUi, on: On) {
    val c = Pause.colors
    Page("동물병원 가는 길", on, scroll = true) {
        CloseRow(on)
        Text("가까운 곳부터 보여요. 가는 동안 흔들림을 줄이도록 안내할게요.", style = Pause.type.body, color = c.ink2)
        if (ui.loading) Text("찾는 중…", style = Pause.type.body, color = c.ink2)
        ui.items.forEach { v ->
            PauseCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = Pause.type.item, color = c.ink)
                        Text(listOfNotNull(v.distance, v.address, v.phone).joinToString(" · "), style = Pause.type.label, color = c.ink2)
                    }
                    if (v.sample) Pill("샘플", sample = true)
                    Spacer(Modifier.width(16.dp))
                    PauseButton("안내 시작", { on(UiEvent.GoVet(v.id)) }, small = true)
                }
            }
        }
        ui.note?.let { Text(it, style = Pause.type.label, color = c.ink3) }
    }
}

@Composable
private fun CloseRow(on: On) {
    Row { Spacer(Modifier.weight(1f)); PauseButton("닫기", { on(UiEvent.Close) }, kind = ButtonKind.GHOST, small = true) }
}

// ───────────── 도착 체크·이동 기록 (F6·F7) ─────────────

@Composable
fun ArrivalScreen(ui: ArrivalUi, on: On) {
    val c = Pause.colors
    val ink = if (ui.strong) Color.White else c.ink
    Page(null, on, scroll = false, background = if (ui.strong) c.alert else c.paper) {
        Spacer(Modifier.weight(0.3f))
        Text(ui.question, style = Pause.type.hero, color = ink)
        ui.detail?.let { Text(it, style = Pause.type.title, color = ink) }
        Spacer(Modifier.weight(0.4f))
        PauseButton("내렸어요", { on(UiEvent.ConfirmLeft) }, kind = if (ui.strong) ButtonKind.QUIET else ButtonKind.PRIMARY)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripLogScreen(ui: TripLogUi, on: On) {
    val c = Pause.colors
    Page(ui.meta, on, scroll = true) {
        Text(ui.summary, style = Pause.type.sentence, color = c.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat("이동", "${ui.durationMin / 60}시간 ${ui.durationMin % 60}분")
            Stat("휴식", "${ui.stops}번")
            Stat("멀미 예보 최고", "${ui.motionPeak}")
        }
        PauseCard(Modifier.fillMaxWidth()) {
            Text("멀미 주의 예보 흐름 (5분 간격, 진단 아님)", style = Pause.type.label, color = c.ink2)
            MotionChart(ui.motionSeries)
            Text(ui.limitLine, style = Pause.type.bodyStrong, color = c.restInk)
        }
        PauseCard(Modifier.fillMaxWidth()) {
            Text("오늘 보인 모습", style = Pause.type.title, color = c.ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.symptoms.forEach { s -> Choice(s.label, s.selected, { on(UiEvent.ToggleSymptom(s.label)) }) }
            }
        }
        PauseCard(Modifier.fillMaxWidth()) {
            Text("동물병원에 보여 줄 요약", style = Pause.type.title, color = c.ink)
            ui.vetSummary.forEach { Text(it, style = Pause.type.body, color = c.ink2) }
        }
        PauseButton("확인", { on(UiEvent.FinishTrip) })
    }
}

@Composable
private fun Stat(label: String, value: String) {
    PauseCard {
        Text(label, style = Pause.type.label, color = Pause.colors.ink2)
        Text(value, style = Pause.type.number, color = Pause.colors.ink)
    }
}

// ───────────── 반려견 프로필·설정 (정차 중에만, DS-04) ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetProfileScreen(ui: PetEditUi, on: On) {
    val c = Pause.colors
    Page(if (ui.firstRun) "반려견을 알려 주세요" else "반려견 프로필", on, scroll = true) {
        SectionTitle("이름")
        BasicTextField(
            ui.name, { on(UiEvent.SetPetName(it)) },
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(28.dp),
            textStyle = Pause.type.title.copy(color = c.ink), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), cursorBrush = SolidColor(c.rest),
        )
        SectionTitle("체급")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(SizeUi.SMALL to "소형", SizeUi.MEDIUM to "중형", SizeUi.LARGE to "대형").forEach { (s, l) -> Choice(l, ui.size == s, { on(UiEvent.SetPetSize(s)) }) }
        }
        SectionTitle("나이")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PauseButton("−", { on(UiEvent.SetPetAge(ui.ageYears - 1)) }, kind = ButtonKind.QUIET, small = true)
            Text("${ui.ageYears}살", style = Pause.type.title, color = c.ink)
            PauseButton("+", { on(UiEvent.SetPetAge(ui.ageYears + 1)) }, kind = ButtonKind.QUIET, small = true)
        }
        SectionTitle("차멀미 성향")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(TendencyUi.LOW to "거의 안 해요", TendencyUi.NORMAL to "가끔", TendencyUi.HIGH to "자주 해요").forEach { (t, l) -> Choice(l, ui.tendency == t, { on(UiEvent.SetPetTendency(t)) }) }
        }
        SectionTitle("배변·산책 간격")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(60, 90, 120, 150, 180).forEach { m -> Choice("${m}분", ui.pottyIntervalMin == m, { on(UiEvent.SetPotty(m)) }) }
        }
        CheckRow("반려견 놀이터가 있는 휴게소를 먼저", ui.petZonePreferred, { on(UiEvent.TogglePetZone) })
        ui.problems.forEach { Text(it, style = Pause.type.label, color = c.alertInk) }
        Text("프로필은 이 차에만 저장되고 서버로 보내지 않아요.", style = Pause.type.label, color = c.ink3)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            PauseButton("저장", { on(UiEvent.SavePet) }, enabled = ui.problems.isEmpty())
            PauseButton(if (ui.firstRun) "나중에" else "닫기", { on(UiEvent.Close) }, kind = ButtonKind.GHOST)
        }
    }
}

@Composable
fun SettingsScreen(ui: SettingsUi, on: On, licenses: String) {
    val c = Pause.colors
    Page("설정", on, scroll = true) {
        CloseRow(on)
        PauseCard(Modifier.fillMaxWidth()) {
            Text("제안과 알림", style = Pause.type.title, color = c.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("빠르게", "보통", "여유 있게").forEachIndexed { i, l -> Choice(l, ui.timing == i, { on(UiEvent.SetTiming(i)) }) }
            }
            CheckRow("음성으로 제안", ui.voiceSuggest, { on(UiEvent.ToggleVoice) }, "끄면 화면 카드와 버튼만")
            CheckRow("움직임 줄이기", ui.reduceMotion, { on(UiEvent.ToggleReduceMotion) })
        }
        PauseCard(Modifier.fillMaxWidth()) {
            Text("이 차에서 쓰는 신호", style = Pause.type.title, color = c.ink)
            ui.signals.forEach { s -> Text("${if (s.inUse) "●" else "○"} ${s.name}" + (s.source?.let { " — $it" } ?: " — 미지원"), style = Pause.type.body, color = if (s.inUse) c.ink else c.ink3) }
        }
        PauseCard(Modifier.fillMaxWidth()) {
            Text("개인정보", style = Pause.type.title, color = c.ink)
            Text("반려견 프로필·이동 기록은 이 차에만 있어요. 대기 공유에는 위치를 보내지 않아요.", style = Pause.type.body, color = c.ink2)
            PauseButton("내 데이터 삭제", { on(UiEvent.DeleteData) }, kind = ButtonKind.ALERT, small = true)
        }
        Text("같이가개 ${ui.version} · 멀미 지수는 주의 예보이며 의학적 진단이 아니에요.", style = Pause.type.label, color = c.ink3)
        if (licenses.isNotBlank()) Text(licenses.take(400), style = Pause.type.caption, color = c.ink3)
    }
}

// ───────────── 미리보기 ─────────────

@Preview(widthDp = 1600, heightDp = 1000) @Composable private fun PDrive() = PauseTheme(false) { DriveScreen(Fixtures.suggest) {} }
@Preview(widthDp = 1600, heightDp = 1000) @Composable private fun PDepart() = PauseTheme(false) { DepartScreen(Fixtures.depart) {} }
@Preview(widthDp = 1600, heightDp = 1000) @Composable private fun PWait() = PauseTheme(false) { WaitScreen(Fixtures.waitOverdue) {} }
@Preview(widthDp = 1600, heightDp = 1000) @Composable private fun PLog() = PauseTheme(false) { TripLogScreen(Fixtures.tripLog) {} }
