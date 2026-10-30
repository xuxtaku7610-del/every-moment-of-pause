package com.emop.pause.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.emop.pause.R
import com.emop.pause.ui.components.AppBar
import com.emop.pause.ui.components.ArtSlot
import com.emop.pause.ui.components.AxisLabels
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.CheckCircle
import com.emop.pause.ui.components.ChecklistRow
import com.emop.pause.ui.components.Choice
import com.emop.pause.ui.components.DogIllustration
import com.emop.pause.ui.components.ForecastChart
import com.emop.pause.ui.components.HospitalIllustration
import com.emop.pause.ui.components.NumberBadge
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PetPhoto
import com.emop.pause.ui.components.RestStopIllustration
import com.emop.pause.ui.components.RoadIllustration
import com.emop.pause.ui.components.SectionTitle
import com.emop.pause.ui.components.StatusChip
import com.emop.pause.ui.components.StatusIcon
import com.emop.pause.ui.components.SuvIllustration
import com.emop.pause.ui.components.ToggleRow
import com.emop.pause.ui.components.TrendChart
import com.emop.pause.ui.components.V5Card
import com.emop.pause.ui.components.WarningBanner
import com.emop.pause.ui.components.toneColor
import com.emop.pause.ui.components.toneWord
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.ArrivalUi
import com.emop.pause.uistate.CareSheet
import com.emop.pause.uistate.DepartUi
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.MotionQuiz
import com.emop.pause.uistate.MotionAnswerUi
import com.emop.pause.uistate.MotionTone
import com.emop.pause.uistate.OnboardingUi
import com.emop.pause.uistate.PetEditUi
import com.emop.pause.uistate.PlacesUi
import com.emop.pause.uistate.PlaceFilter
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
 * 같이가개 화면 v5 (docs/design/v5/STYLE_V5.md §7, docs/ui_contract.md).
 * 모든 화면은 상태(UiState)만 보고 그리며, 사용자 동작은 [UiEvent]로만 돌려보낸다.
 * 창 폭(WidthClass)에 따라 배치만 바꾸고 글자 크기는 그대로 둔다 (§5).
 */

private typealias On = (UiEvent) -> Unit

/** 바깥 틀: 앱 바 + 본문. 주행 화면은 scroll = false (DS-02). */
@Composable
private fun Page(
    meta: String?,
    on: On,
    scroll: Boolean,
    background: Color? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.(WidthClass) -> Unit,
) {
    val c = Pause.colors
    val d = Pause.dimens
    BoxWithConstraints(Modifier.fillMaxSize().background(background ?: c.bg)) {
        val wc = WidthClass.of(maxWidth)
        Column(Modifier.fillMaxSize()) {
            AppBar(meta, { on(UiEvent.LogoTap) }, trailing = trailing)
            val body = Modifier.fillMaxWidth().weight(1f).padding(start = d.margin, end = d.margin, bottom = d.margin)
            Column(if (scroll) body.verticalScroll(rememberScrollState()) else body, verticalArrangement = Arrangement.spacedBy(d.gap)) { content(wc) }
        }
    }
}

@Composable
private fun CloseButton(on: On) {
    PauseButton("닫기", { on(UiEvent.Close) }, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Close, compact = true)
}

@Composable
private fun IconLine(icon: ImageVector, text: String, color: Color = Pause.colors.sub, maxLines: Int = 1) {
    val d = Pause.dimens
    Row(verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top) {
        Icon(icon, null, Modifier.size(d.u(20f)).padding(top = if (maxLines == 1) d.u(0f) else d.u(2f)), tint = color)
        Spacer(Modifier.width(d.unit))
        Text(text, style = Pause.type.body, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

// ═════════════ 2·3·4 주행 (F1·F2): 텍스트 2줄 · 버튼 2개 · 음성 힌트 · 스크롤 없음 ═════════════

@Composable
fun DriveScreen(ui: DriveUi, on: On) {
    Page(ui.meta, on, scroll = false) { wc ->
        val content = ui.content
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                ui.restricted -> RestrictedDrive(ui)
                wc == WidthClass.THIRD -> ThirdDrive(ui, on)
                content is DriveContent.Distress -> DistressDrive(content, on)
                content is DriveContent.Forecast -> ForecastDrive(ui, content, wc)
                // 음성으로 묻는 중(LISTENING)에도 추천 카드는 보이고 버튼만 숨긴다. 실패하면 아래 시트가 버튼 2개.
                content is DriveContent.Suggest -> SuggestDrive(ui, content, wc, on, buttons = ui.voice == null)
                else -> CalmDrive(ui, wc)
            }
        }
        DriveFooter(ui, on)
    }
}

/** 상태 패널 (2번 왼쪽): 사진·이름·단계 글자·마지막 휴식. */
@Composable
private fun StatusPanel(ui: DriveUi, modifier: Modifier = Modifier, line: String? = null) {
    val c = Pause.colors
    val d = Pause.dimens
    V5Card(modifier, spacing = d.u(12f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetPhoto(Modifier.size(d.u(80f)))
            Spacer(Modifier.width(d.u(16f)))
            Column {
                Text(ui.pet.ifEmpty { "반려견" }, style = Pause.type.h1, color = c.text, maxLines = 1)
                ui.motion?.let { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(d.u(14f)).clip(CircleShape).background(toneColor(m.tone)))
                        Spacer(Modifier.width(d.unit))
                        Text(toneWord(m.tone), style = Pause.type.h2, color = if (m.tone == MotionTone.CALM) c.comfort else c.warningInk, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ui.motion?.let { m ->
            Text("멀미 지수", style = Pause.type.label, color = c.sub)
            Text("${m.index}", style = Pause.type.number.copy(fontSize = Pause.type.number.fontSize * 1.6f, lineHeight = Pause.type.number.lineHeight * 1.6f), color = if (m.tone == MotionTone.CALM) c.comfort else c.warningInk)
        }
        Spacer(Modifier.weight(1f))
        ui.sinceRest?.let { Text(it, style = Pause.type.h2, color = c.text, maxLines = 1) }
        line?.let { Text(it, style = Pause.type.body, color = c.sub, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

/** 주행 기본 (2번): 상태 35% · 도로 그림 65% + "다음 쉼표 예정" 알약. 1/2 창은 50:50. */
@Composable
private fun CalmDrive(ui: DriveUi, wc: WidthClass) {
    val d = Pause.dimens
    val line = when (val ct = ui.content) {
        is DriveContent.Calm -> ct.line
        is DriveContent.Heading -> "${ct.minutes}분 뒤 ${ct.place}로 가는 중"
        is DriveContent.Forecast -> ct.line
        is DriveContent.Suggest -> "${ct.minutes}분 뒤 ${ct.place}에서 쉬어 갈까요?"
        is DriveContent.Vet -> null
        is DriveContent.Distress -> ct.line
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
        val vet = ui.content as? DriveContent.Vet
        if (vet != null) VetDrivePanel(vet, Modifier.weight(if (wc == WidthClass.FULL) 0.35f else 0.5f).fillMaxHeight())
        else StatusPanel(ui, Modifier.weight(if (wc == WidthClass.FULL) 0.35f else 0.5f).fillMaxHeight(), line)
        Box(Modifier.weight(if (wc == WidthClass.FULL) 0.65f else 0.5f).fillMaxHeight().clip(RoundedCornerShape(d.cardRadius))) {
            RoadIllustration(ui.roadAhead, Modifier.fillMaxSize())
            ui.nextStop?.let { NextStopPill(it, Modifier.align(Alignment.TopEnd).padding(d.u(16f))) }
        }
    }
}

/** 8번 주행 중: 1순위 병원 한 곳 2줄만. */
@Composable
private fun VetDrivePanel(v: DriveContent.Vet, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    V5Card(modifier, spacing = d.u(12f)) {
        StatusIcon("ic_hospital", R.drawable.fe_hospital, d.u(56f))
        Text("동물병원 가는 길", style = Pause.type.h2, color = c.sub)
        Text(v.name, style = Pause.type.h1, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(v.distance, v.minutes?.let { "${it}분" }).joinToString(" · "), style = Pause.type.h2, color = c.text)
        Spacer(Modifier.weight(1f))
        Text("흔들림을 줄여 천천히 안내할게요", style = Pause.type.body, color = c.sub)
    }
}

@Composable
private fun NextStopPill(text: String, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(
        modifier.clip(RoundedCornerShape(d.cardRadius)).background(c.elevated.copy(alpha = 0.95f)).border(d.border, c.border, RoundedCornerShape(d.cardRadius)).padding(d.u(16f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusIcon("ic_rest_cup", R.drawable.fe_hot_beverage, d.u(40f))
        Spacer(Modifier.width(d.u(12f)))
        Column {
            Text("다음 쉼표 예정", style = Pause.type.label, color = c.sub)
            Text(text, style = Pause.type.h1, color = c.text)
        }
    }
}

@Composable
private fun Toast(text: String, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Text(
        text, modifier.clip(RoundedCornerShape(d.cardRadius)).background(c.text.copy(alpha = 0.88f)).padding(horizontal = d.u(20f), vertical = d.u(12f)),
        style = Pause.type.h2, color = c.surface, maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

/** 멀미 예보 (3번): 왼쪽 굽은 길 30% · 오른쪽 경고(제목 1줄 + 코칭 1줄) + 20분 그래프(정지 그림). 1/2 창은 그래프 없이. */
@Composable
private fun ForecastDrive(ui: DriveUi, f: DriveContent.Forecast, wc: WidthClass) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
        if (wc == WidthClass.FULL) {
            ArtSlot("road", "road_curve", modifier = Modifier.weight(0.3f).fillMaxHeight()) { RoadIllustration(ui.roadAhead, Modifier.fillMaxSize(), showCar = true) }
        }
        Column(Modifier.weight(0.7f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(d.gap)) {
            WarningBanner(Modifier.fillMaxWidth(), icon = R.drawable.fe_warning, iconAsset = "ic_warning") {
                Text(f.title ?: f.line, style = Pause.type.h1, color = c.warningInk, maxLines = if (f.title != null) 1 else 2, overflow = TextOverflow.Ellipsis)
                f.coach?.let { Text(it, style = Pause.type.h2, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            val now = f.now
            val peak = f.peak
            if (wc == WidthClass.FULL && now != null && peak != null) {
                V5Card(Modifier.fillMaxWidth().weight(1f)) {
                    Text("멀미 지수 예보 (다음 20분)", style = Pause.type.h2, color = c.text)
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        AxisLabels(Modifier.width(d.u(36f)).fillMaxHeight())
                        Spacer(Modifier.width(d.unit))
                        ForecastChart(now, peak, Modifier.weight(1f).fillMaxHeight().padding(vertical = d.u(8f)))
                    }
                    Row {
                        Stat("지금", "$now", c.comfort)
                        Spacer(Modifier.weight(1f))
                        Stat("예상 최고", "$peak", c.warningInk, Alignment.End)
                    }
                }
            } else {
                StatusPanel(ui, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, align: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = align) {
        Text(label, style = Pause.type.label, color = Pause.colors.sub)
        Text(value, style = Pause.type.h1, color = color)
    }
}

/** 쉼표 추천 (4번): 휴게소 그림 35% · 장소·분·거리·이유 2개 · 버튼 2개(피로 위험이면 1개) + 음성 힌트. */
@Composable
private fun SuggestDrive(ui: DriveUi, s: DriveContent.Suggest, wc: WidthClass, on: On, buttons: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    V5Card(Modifier.fillMaxSize(), pad = d.u(24f), spacing = d.gap) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.u(24f))) {
            if (wc == WidthClass.FULL) {
                ArtSlot("reststop", modifier = Modifier.weight(0.35f).fillMaxHeight()) { RestStopIllustration(Modifier.fillMaxSize()) }
            }
            Column(Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(d.u(12f))) {
                if (s.urgent) StatusChip("운전자 쉬어 갈 때", MotionTone.HIGH)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(s.place, style = Pause.type.display, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    s.direction?.let { Spacer(Modifier.width(d.u(12f))); Text("($it)", style = Pause.type.h2, color = c.sub, maxLines = 1) }
                }
                IconLine(Icons.Rounded.Schedule, listOfNotNull("${s.minutes}분", s.distance).joinToString(" · "), c.text)
                if (!ui.restricted) s.reasons.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CheckCircle(true)
                        Spacer(Modifier.width(d.u(12f)))
                        Text(r, style = Pause.type.h2, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (!buttons) return@V5Card
        Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.weight(1f))
            if (!s.urgent) PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
        }
        VoiceHint(if (s.urgent) "“여기서 쉴게요”라고 말해도 돼요" else "“여기서 쉴게요” 또는 “나중에”라고 말해도 돼요")
    }
}

/** "콩이가 이상해요" (UPGRADE 7): 안내 2줄 이하 + 버튼 2개 + 하단 고정 문구(진단 아님). 애니메이션 없음. */
@Composable
private fun DistressDrive(dc: DriveContent.Distress, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    V5Card(Modifier.fillMaxSize(), tint = c.warningSoft, pad = d.u(28f), spacing = d.gap) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIcon(if (dc.vet) "ic_hospital" else "ic_warning", if (dc.vet) R.drawable.fe_hospital else R.drawable.fe_warning, d.u(48f))
            Spacer(Modifier.width(d.u(16f)))
            Text("반려견 안내", style = Pause.type.h2, color = c.warningInk)
        }
        Text(dc.line, style = Pause.type.display, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        dc.target?.let { t -> Text(listOfNotNull(t, dc.minutes?.let { "약 ${it}분" }).joinToString(" · "), style = Pause.type.h2, color = c.text, maxLines = 1) }
        Spacer(Modifier.weight(1f))
        DistressButtons(dc, on, vertical = false)
        Text(CareSheet.DISCLAIMER, style = Pause.type.label, color = c.sub)
    }
}

@Composable
private fun DistressButtons(dc: DriveContent.Distress, on: On, vertical: Boolean) {
    val d = Pause.dimens
    val primary = when {
        dc.vet -> "동물병원으로"
        dc.target != null -> "${dc.target}로"
        else -> "알겠어요"
    }
    if (vertical) {
        PauseButton(primary, { on(UiEvent.AcceptDistress) }, Modifier.fillMaxWidth())
        PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            PauseButton(primary, { on(UiEvent.AcceptDistress) }, Modifier.weight(1f))
            PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
        }
    }
}

@Composable
private fun VoiceHint(text: String) {
    val d = Pause.dimens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Mic, null, Modifier.size(d.u(18f)), tint = Pause.colors.sub)
        Spacer(Modifier.width(d.unit))
        Text(text, style = Pause.type.label, color = Pause.colors.sub, maxLines = 1)
    }
}

/** RESTRICTED: 가장 급한 한 줄만 (DS-03). */
@Composable
private fun RestrictedDrive(ui: DriveUi) {
    val c = Pause.colors
    val text = when (val ct = ui.content) {
        is DriveContent.Calm -> ct.line
        is DriveContent.Forecast -> ct.title ?: ct.line
        is DriveContent.Suggest -> "${ct.minutes}분 뒤 ${ct.place}"
        is DriveContent.Heading -> "${ct.minutes}분 뒤 ${ct.place}"
        is DriveContent.Vet -> "${ct.name} ${ct.distance}"
        is DriveContent.Distress -> ct.line
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = Pause.type.display, color = c.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 1/3 창: 상태와 다음 행동만 (그래프·사진·다중 버튼 없음). */
@Composable
private fun ThirdDrive(ui: DriveUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(d.u(12f))) {
        ui.motion?.let { m -> Text("${ui.pet.ifEmpty { "반려견" }} · ${toneWord(m.tone)}", style = Pause.type.h1, color = if (m.tone == MotionTone.CALM) c.comfort else c.warningInk, maxLines = 1) }
        ui.sinceRest?.let { Text(it, style = Pause.type.h2, color = c.text) }
        ui.nextStop?.let { Text("다음 쉼표 $it", style = Pause.type.h2, color = c.text) }
        val line = when (val ct = ui.content) {
            is DriveContent.Calm -> null
            is DriveContent.Forecast -> ct.title ?: ct.line
            is DriveContent.Suggest -> "${ct.minutes}분 뒤 ${ct.place}"
            is DriveContent.Heading -> "${ct.minutes}분 뒤 ${ct.place}로 가는 중"
            is DriveContent.Vet -> "${ct.name} · ${ct.distance}"
            is DriveContent.Distress -> ct.line
        }
        line?.let { Text(it, style = Pause.type.h2, color = c.warningInk.takeIf { ui.content is DriveContent.Forecast } ?: c.text, maxLines = 2) }
        Spacer(Modifier.weight(1f))
        (ui.content as? DriveContent.Distress)?.let { dc -> DistressButtons(dc, on, vertical = true) }
        val s = ui.content as? DriveContent.Suggest
        if (s != null && ui.voice == null) {
            PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.fillMaxWidth())
            if (!s.urgent) PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
        }
    }
}

/** 음성 시트 (듣는 중 / 실패하면 버튼 2개) · 오프라인 한 줄. */
@Composable
private fun DriveFooter(ui: DriveUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    // 결과 한 줄("경로에 넣었어요", "곧 도착해요 … 산책 어때요?")은 어느 주행 배치에서도 보인다.
    if (!ui.restricted) ui.toast?.let { Toast(it, Modifier.fillMaxWidth()) }
    when (ui.voice) {
        VoiceSheetState.LISTENING -> V5Card(Modifier.fillMaxWidth(), tint = c.primarySoft) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Mic, null, Modifier.size(d.u(28f)), tint = c.primary)
                Spacer(Modifier.width(d.u(12f)))
                Text("듣고 있어요…", style = Pause.type.h2, color = c.primary)
            }
        }
        VoiceSheetState.FAILED -> V5Card(Modifier.fillMaxWidth()) {
            Text("잘 못 들었어요. 버튼으로 골라 주세요.", style = Pause.type.h2, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
                PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.weight(1f))
                PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }
        }
        null -> if (ui.offline) Text("인터넷 없이 내장 데이터로 안내 중", style = Pause.type.label, color = c.sub)
    }
}

// ═════════════ 1 출발 체크 (F6) ═════════════

@Composable
fun DepartScreen(ui: DepartUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(ui.meta, on, scroll = false) { wc ->
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            if (wc == WidthClass.FULL) {
                Box(Modifier.weight(0.28f).fillMaxHeight().clip(RoundedCornerShape(d.cardRadius))) {
                    PetPhoto(Modifier.fillMaxSize(), RoundedCornerShape(d.cardRadius))
                    Column(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                            .padding(d.pad),
                        verticalArrangement = Arrangement.spacedBy(d.u(4f)),
                    ) {
                        Text(ui.pet.name, style = Pause.type.h1, color = Color.White)
                        Text(ui.pet.line, style = Pause.type.label, color = Color.White, maxLines = 2)
                        if (ui.pet.sample) Text("프로필을 저장하면 맞춤 안내가 시작돼요", style = Pause.type.caption, color = Color.White)
                        Spacer(Modifier.height(d.unit))
                        PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, kind = ButtonKind.SECONDARY, compact = true)
                    }
                }
            }
            V5Card(Modifier.weight(0.38f).fillMaxHeight(), pad = d.u(24f)) {
                SectionTitle("출발 전 체크리스트")
                Spacer(Modifier.height(d.unit))
                ui.checks.forEach { ch -> ChecklistRow(ch.label, ch.checked, { on(UiEvent.ToggleCheck(ch.kind)) }) }
                Spacer(Modifier.weight(1f))
                ui.limitLine?.let { Text(it, style = Pause.type.bodyStrong, color = c.warningInk) }
                Text(ui.rationale, style = Pause.type.body, color = c.sub)
                if (wc != WidthClass.FULL) PauseButton("프로필 바꾸기", { on(UiEvent.OpenPet) }, kind = ButtonKind.SECONDARY, compact = true)
            }
            V5Card(Modifier.weight(0.34f).fillMaxHeight(), pad = d.u(24f), spacing = d.u(12f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DirectionsCar, null, Modifier.size(d.u(36f)), tint = c.text)
                    Spacer(Modifier.width(d.u(16f)))
                    Column {
                        Text(ui.route ?: "경로를 기다리는 중", style = Pause.type.h2, color = c.text)
                        ui.duration?.let { Text(it, style = Pause.type.h1, color = c.text) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon("ic_rest_cup", R.drawable.fe_hot_beverage, d.u(36f))
                    Spacer(Modifier.width(d.u(16f)))
                    Column {
                        Text("예상 쉼표", style = Pause.type.h2, color = c.text)
                        Text("${ui.plan.size}번", style = Pause.type.h1, color = c.text)
                    }
                }
                ui.plan.take(3).forEach { p ->
                    Text("${p.eta} · ${p.name}", style = Pause.type.body, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                if (!ui.ready) Text("세 가지를 확인하면 출발할 수 있어요", style = Pause.type.label, color = c.sub)
                PauseButton("이대로 출발", { on(UiEvent.StartTrip) }, Modifier.fillMaxWidth(), enabled = ui.ready)
                PauseButton("설정", { on(UiEvent.OpenSettings) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, compact = true)
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
        // "콩이 돌보기" (UPGRADE 7): 주행 중에 말한 증상의 체크리스트, 정차 중에만
        ui.care?.let { cu ->
            V5Card(Modifier.fillMaxWidth(), tint = c.warningSoft, pad = d.u(24f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("콩이 돌보기 — ${cu.title}", style = Pause.type.h1, color = c.text, modifier = Modifier.weight(1f))
                    if (!cu.reviewed) StatusChip("검수 전", neutral = true)
                }
                cu.lines.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        CheckCircle(false)
                        Spacer(Modifier.width(d.u(12f)))
                        Text(line, style = Pause.type.body, color = c.text)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cu.disclaimer, style = Pause.type.label, color = c.sub, modifier = Modifier.weight(1f))
                    PauseButton("동물병원", { on(UiEvent.OpenVets) }, kind = ButtonKind.SECONDARY, compact = true)
                    Spacer(Modifier.width(d.unit))
                    PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, kind = ButtonKind.SECONDARY, compact = true)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            V5Card(Modifier.weight(0.4f), pad = d.u(24f)) {
                Text(ui.lead, style = Pause.type.display, color = c.text)
                ui.motion?.let { m -> StatusChip("${toneWord(m.tone)} · 멀미 지수 ${m.index}", m.tone) }
                ui.battery?.let { IconLine(Icons.Rounded.BatteryStd, it) }
                ui.restChoice?.let { sel ->
                    Spacer(Modifier.height(d.unit))
                    Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                        listOf(10, 15, 20).forEach { m -> Choice("${m}분", sel == m, { on(UiEvent.SetRestMinutes(m)) }) }
                    }
                }
            }
            if (wc != WidthClass.THIRD) {
                Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                    ui.activities.forEach { a ->
                        V5Card(Modifier.fillMaxWidth(), tint = if (a.recommended) c.primarySoft else null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${a.who} · ${a.title}", style = Pause.type.h2, color = c.text, modifier = Modifier.weight(1f))
                                if (a.recommended) StatusChip("추천")
                            }
                            Text(a.description, style = Pause.type.body, color = c.sub)
                        }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(d.gap), verticalArrangement = Arrangement.spacedBy(d.unit)) {
            PauseButton("잠깐 다녀올게 (${ui.waitLimitMin}분)", { on(UiEvent.StartWait) })
            PauseButton("함께 가는 장소", { on(UiEvent.OpenPlaces) }, kind = ButtonKind.SECONDARY)
            PauseButton("동물병원", { on(UiEvent.OpenVets) }, kind = ButtonKind.SECONDARY)
        }
        if (ui.distressChips.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("콩이가 이상해요", style = Pause.type.h2, color = c.text)
                Spacer(Modifier.width(d.u(16f)))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                    ui.distressChips.forEach { l -> Choice(l, false, { on(UiEvent.ReportDistress(l)) }) }
                }
            }
        }
    }
}

// ═════════════ 5 잠깐 대기 (F3): 바깥 사람이 읽는 차 화면 ═════════════

@Composable
fun WaitScreen(ui: WaitUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false, background = if (ui.overdue) c.warningSoft else c.bg) { _ ->
        ui.notice?.let { n ->
            WarningBanner(Modifier.fillMaxWidth(), icon = R.drawable.fe_warning, iconAsset = "ic_warning") { Text(n, style = Pause.type.h2, color = c.text) }
        }
        ui.heat?.let { h ->
            WarningBanner(Modifier.fillMaxWidth(), icon = R.drawable.fe_thermometer, iconAsset = "ic_thermometer") { Text(h, style = Pause.type.h2, color = c.warningInk) }
        }
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (ui.overdue) StatusIcon("ic_warning", R.drawable.fe_warning, d.u(120f)) else StatusIcon("wait_house_dog", R.drawable.fe_house, d.u(160f))
            Spacer(Modifier.height(d.gap))
            Text(if (ui.overdue) "보호자에게 알렸어요" else ui.headline, style = Pause.type.display.copy(fontSize = Pause.type.display.fontSize * 1.4f, lineHeight = Pause.type.display.lineHeight * 1.4f), color = if (ui.overdue) c.warningInk else c.text, textAlign = TextAlign.Center)
            Text(ui.elapsed, style = Pause.type.display, color = c.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(d.gap))
            Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f)), verticalAlignment = Alignment.CenterVertically) {
                ui.batteryUsed?.let { IconLine(Icons.Rounded.BatteryStd, "배터리 $it 사용", c.text) }
                IconLine(Icons.Rounded.Timer, "설정 시간 ${ui.limitMin}분", c.text)
                ui.cabin?.let { IconLine(Icons.Rounded.Thermostat, it, c.text) }
                ui.seasonNote?.let { IconLine(Icons.Rounded.Thermostat, it, c.sub) }
            }
            Spacer(Modifier.height(d.unit))
            Text(
                ui.share.url?.let { "보호자 폰: $it" + if (ui.share.connected) "" else " (연결 확인 중)" } ?: "폰 공유 연결 안 됨 — 차 화면 안내는 계속돼요",
                style = Pause.type.label, color = c.sub,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalAlignment = Alignment.CenterVertically) {
            PauseButton("돌아왔어요", { on(UiEvent.StopWait) }, Modifier.widthIn(min = d.u(200f)))
            Spacer(Modifier.width(d.unit))
            Text("제한 시간", style = Pause.type.label, color = c.sub)
            listOf(5, 10, 15, 20).forEach { m -> Choice("${m}분", ui.limitMin == m, { on(UiEvent.SetWaitLimit(m)) }) }
        }
    }
}

// ═════════════ 7 함께 가는 장소 (F4) ═════════════

@Composable
fun PlacesScreen(ui: PlacesUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var largeDog by rememberSaveable { mutableStateOf(false) }
    var indoor by rememberSaveable { mutableStateOf(false) }
    BackHandler { on(UiEvent.Close) }
    Page(ui.title, on, scroll = true, trailing = { CloseButton(on) }) { wc ->
        if (ui.loading) Text("찾는 중…", style = Pause.type.body, color = c.sub)
        if (!ui.loading && ui.items.isEmpty()) Text("근처에 찾은 곳이 없어요.", style = Pause.type.body, color = c.sub)
        if (ui.items.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                PlaceFilter.groups(ui.items).forEach { (g, n) -> Choice("$g $n", (filter ?: PlaceFilter.ALL) == g, { filter = g }) }
                Spacer(Modifier.width(d.u(16f)))
                Choice("대형견 가능", largeDog, { largeDog = !largeDog })
                Choice("실내 동반 가능", indoor, { indoor = !indoor })
            }
            PlaceFilter.unknownNote(ui.items, filter, largeDog, indoor)?.let { Text(it, style = Pause.type.label, color = c.sub) }
        }
        V5Card(Modifier.fillMaxWidth(), pad = d.u(8f), spacing = d.u(0f)) {
            PlaceFilter.apply(ui.items, filter, largeDog, indoor).shown.forEachIndexed { i, p ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(d.border).background(c.border))
                Row(Modifier.fillMaxWidth().heightIn(min = d.u(80f)).padding(d.u(12f)), verticalAlignment = Alignment.CenterVertically) {
                    if (wc != WidthClass.THIRD) {
                        AsyncImage(
                            p.imageUrl, null,
                            Modifier.width(d.u(100f)).aspectRatio(1.6f).clip(RoundedCornerShape(d.buttonRadius)),
                            placeholder = ColorPainter(c.border), error = ColorPainter(c.border), fallback = ColorPainter(c.border),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(d.u(16f)))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (p.sample) p.name.removePrefix("[샘플] ") else p.name, style = Pause.type.h2, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (p.sample) { Spacer(Modifier.width(d.unit)); StatusChip("샘플", neutral = true) }
                        }
                        Text((listOf(p.distance, p.category) + p.tags).joinToString(" · "), style = Pause.type.body, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // 동반 조건 원문 문장 그대로 (실내·대형견 여부를 화면이 판정하지 않는다)
                        p.notes.take(2).forEach { Text(it, style = Pause.type.caption, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    Spacer(Modifier.width(d.u(12f)))
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

// ═════════════ 8 동물병원 (F5, 정차 중) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VetsScreen(ui: VetsUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page("동물병원 가는 길", on, scroll = true, trailing = { CloseButton(on) }) { wc ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            if (wc == WidthClass.FULL) {
                ArtSlot("hospital", "vet_clinic", modifier = Modifier.weight(0.2f).height(d.u(240f))) { HospitalIllustration(Modifier.fillMaxSize()) }
            }
            V5Card(Modifier.weight(0.55f), pad = d.u(20f), spacing = d.u(12f)) {
                Text("가까운 동물병원 ${ui.items.size}곳", style = Pause.type.h2, color = c.text)
                if (ui.loading) Text("찾는 중…", style = Pause.type.body, color = c.sub)
                if (!ui.loading && ui.items.isEmpty()) Text("근처에 찾은 곳이 없어요.", style = Pause.type.body, color = c.sub)
                ui.items.forEachIndexed { i, v ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(d.border).background(c.border))
                    Row(verticalAlignment = Alignment.Top) {
                        NumberBadge(i + 1, active = i == 0)
                        Spacer(Modifier.width(d.u(16f)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(6f))) {
                            Text(if (v.sample) v.name.removePrefix("[샘플] ") else v.name, style = Pause.type.h2, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(listOfNotNull(v.distance, v.minutes?.let { "${it}분" }).joinToString(" · "), style = Pause.type.body, color = c.sub)
                                Spacer(Modifier.width(d.unit))
                                v.status?.let { StatusChip(it, MotionTone.CALM) }
                                if (v.sample) StatusChip("샘플", neutral = true)
                            }
                            v.phone?.let { Text(it, style = Pause.type.caption, color = c.sub) }
                            Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                                PauseButton("안내 시작", { on(UiEvent.GoVet(v.id)) }, icon = Icons.Rounded.Navigation, compact = true)
                                if (i == 0) PauseButton("도착 예정 공유", { on(UiEvent.ShareVetEta(v.id)) }, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Share, compact = true)
                            }
                        }
                    }
                }
            }
            if (wc != WidthClass.THIRD) {
                V5Card(Modifier.weight(0.25f), pad = d.u(20f), spacing = d.u(12f)) {
                    Text("흔들림을 줄여 천천히 이동하세요", style = Pause.type.h2, color = c.text)
                    IconLine(Icons.Rounded.Speed, "급한 굽이는 속도를 줄여요", maxLines = 2)
                    IconLine(Icons.Rounded.SwapCalls, "큰 흔들림이 생기는 조작은 피해요", maxLines = 2)
                    IconLine(Icons.Rounded.Timer, "부드럽게 제동하고 가속해요", maxLines = 2)
                }
            }
        }
        V5Card(Modifier.fillMaxWidth(), pad = d.u(20f), spacing = d.u(12f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIcon("ic_mic", R.drawable.fe_microphone, d.u(32f))
                Spacer(Modifier.width(d.u(12f)))
                Text("증상 메모", style = Pause.type.h2, color = c.text)
                Spacer(Modifier.width(d.u(12f)))
                Text("이 차에만 저장돼요", style = Pause.type.label, color = c.sub, modifier = Modifier.weight(1f))
                PauseButton(
                    if (ui.memoListening) "듣고 있어요…" else "음성 메모", { on(UiEvent.StartMemoVoice) },
                    kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Mic, compact = true, enabled = !ui.memoListening,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                ui.memo.forEach { s -> Choice(s.label, s.selected, { on(UiEvent.ToggleMemoChip(s.label)) }) }
            }
            // 받아쓴 글은 그대로 보여 주고 고칠 수 있다 (정차 중 화면에서만 — 주행 중엔 이 화면이 열리지 않는다)
            BasicTextField(
                ui.memoText, { on(UiEvent.SetMemoText(it)) },
                Modifier.fillMaxWidth().heightIn(min = d.touch).clip(RoundedCornerShape(d.buttonRadius)).background(c.bg)
                    .border(d.border, c.border, RoundedCornerShape(d.buttonRadius)).padding(horizontal = d.u(16f), vertical = d.u(12f)),
                textStyle = Pause.type.body.copy(color = c.text),
                cursorBrush = SolidColor(c.primary),
                decorationBox = { inner ->
                    if (ui.memoText.isEmpty()) Text("예: 구토 두 번, 물은 조금 마셨어요", style = Pause.type.body, color = c.sub)
                    inner()
                },
            )
            ChecklistRow("도착 예정 공유에 증상 메모와 오늘 요약 넣기", ui.memoInclude, { on(UiEvent.ToggleMemoInclude) })
            ui.tripLine?.let { Text("오늘 요약: $it", style = Pause.type.label, color = c.sub) }
            ui.shareUrl?.let { Text("공유 링크: $it (보호자 폰에서 병원·가족에게 직접 전달해 주세요. 위치는 보내지 않아요.)", style = Pause.type.label, color = c.primary) }
        }
        ui.note?.let { Text(it, style = Pause.type.caption, color = c.sub) }
    }
}

// ═════════════ 9 도착 체크 (F6) ═════════════

@Composable
fun ArrivalScreen(ui: ArrivalUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(null, on, scroll = false) { wc ->
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            if (wc == WidthClass.FULL) PetPhoto(Modifier.weight(0.35f).fillMaxHeight(), RoundedCornerShape(d.cardRadius), "pet_arrival")
            Column(Modifier.weight(0.65f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(d.gap, Alignment.CenterVertically)) {
                val warning = ui.warning
                if (warning != null) {
                    WarningBanner(Modifier.fillMaxWidth(), icon = R.drawable.fe_thermometer, iconAsset = "ic_thermometer") {
                        Text(warning, style = Pause.type.h1, color = c.warningInk)
                        // 제목에 숫자가 이미 있으면 다시 쓰지 않는다
                        ui.cabin?.takeIf { it !in warning }?.let { Text(it, style = Pause.type.h1, color = c.warningInk) }
                        ui.detail?.let { Text(it, style = Pause.type.h2, color = c.text) }
                    }
                } else {
                    Text("시동이 꺼졌어요.", style = Pause.type.h2, color = c.sub)
                }
                Text(ui.question, style = Pause.type.display.copy(fontSize = Pause.type.display.fontSize * 1.4f, lineHeight = Pause.type.display.lineHeight * 1.4f), color = c.text)
                if (ui.warning == null) ui.detail?.let { Text(it, style = Pause.type.h2, color = c.sub) }
                PauseButton("내렸어요", { on(UiEvent.ConfirmLeft) }, Modifier.fillMaxWidth())
            }
        }
    }
}

// ═════════════ 10 이동 기록 (F7) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripLogScreen(ui: TripLogUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page(ui.meta, on, scroll = true) { wc ->
        // 개인 맞춤 보정 (UPGRADE 4): 도착 뒤 한 번만, 정차 중. 음성으로도 답할 수 있다.
        ui.askMotion?.let { q ->
            V5Card(Modifier.fillMaxWidth(), tint = c.primarySoft, pad = d.u(24f)) {
                Text(q, style = Pause.type.h1, color = c.text)
                Text("답할수록 다음 멀미 예보가 이 아이에게 맞춰져요. \"네·조금·아니요\"라고 말해도 돼요.", style = Pause.type.body, color = c.sub)
                Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    PauseButton("네", { on(UiEvent.AnswerMotion(MotionAnswerUi.YES)) }, Modifier.weight(1f))
                    PauseButton("조금", { on(UiEvent.AnswerMotion(MotionAnswerUi.LITTLE)) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
                    PauseButton("아니요", { on(UiEvent.AnswerMotion(MotionAnswerUi.NO)) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
                    PauseButton("건너뛰기", { on(UiEvent.AnswerMotion(null)) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            V5Card(Modifier.weight(0.6f), pad = d.u(24f)) {
                Text("오늘의 이동 기록", style = Pause.type.h2, color = c.text)
                Row(horizontalArrangement = Arrangement.spacedBy(d.u(40f))) {
                    Kpi(hm(ui.durationMin), "총 주행 시간")
                    Kpi("${ui.stops}번", "쉼표")
                    Kpi("${ui.warnings}번", "멀미 주의")
                }
            }
            V5Card(Modifier.weight(0.4f), tint = c.warningSoft, pad = d.u(24f)) {
                Text(ui.limitLine, style = Pause.type.h1, color = c.warningInk)
            }
        }
        V5Card(Modifier.fillMaxWidth(), pad = d.u(24f)) {
            Text("멀미 지수 흐름 (5분 간격 · 최고 ${ui.motionPeak} · 진단 아님)", style = Pause.type.h2, color = c.text)
            if (wc != WidthClass.THIRD) {
                Row(Modifier.fillMaxWidth().height(d.u(150f))) {
                    AxisLabels(Modifier.width(d.u(36f)).fillMaxHeight())
                    Spacer(Modifier.width(d.unit))
                    TrendChart(ui.motionSeries, Modifier.weight(1f).fillMaxHeight().padding(vertical = d.u(8f)))
                }
            }
            Text(ui.summary, style = Pause.type.body, color = c.sub)
            ui.events?.let { Text(it, style = Pause.type.bodyStrong, color = c.warningInk) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            V5Card(Modifier.weight(1f)) {
                Text("오늘 보인 모습", style = Pause.type.h2, color = c.text)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                    ui.symptoms.forEach { s -> Choice(s.label, s.selected, { on(UiEvent.ToggleSymptom(s.label)) }) }
                }
            }
            V5Card(Modifier.weight(1f)) {
                Text("동물병원에 보여 줄 요약", style = Pause.type.h2, color = c.text)
                ui.vetSummary.forEach { Text(it, style = Pause.type.body, color = c.sub) }
            }
            if (ui.history.isNotEmpty() && wc == WidthClass.FULL) {
                V5Card(Modifier.weight(1f)) {
                    Text("이전 기록", style = Pause.type.h2, color = c.text)
                    ui.history.forEach { h -> Text("${h.date} · ${h.line}", style = Pause.type.body, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        PauseButton("확인", { on(UiEvent.FinishTrip) }, Modifier.widthIn(min = d.u(240f)))
    }
}

/** "2시간 58분". */
private fun hm(min: Int): String = when {
    min < 60 -> "${min}분"
    min % 60 == 0 -> "${min / 60}시간"
    else -> "${min / 60}시간 ${min % 60}분"
}

@Composable
private fun Kpi(value: String, label: String) {
    Column {
        Text(value, style = Pause.type.h1, color = Pause.colors.text)
        Text(label, style = Pause.type.label, color = Pause.colors.sub)
    }
}

// ═════════════ 11 반려견 프로필·설정·온보딩 (정차 중에만, DS-04) ═════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileForm(ui: PetEditUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(horizontalArrangement = Arrangement.spacedBy(d.u(24f))) {
        PetPhoto(Modifier.size(d.u(120f)), RoundedCornerShape(d.cardRadius))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(12f))) {
            FieldLabel("이름")
            BasicTextField(
                ui.name, { on(UiEvent.SetPetName(it)) },
                Modifier.fillMaxWidth().heightIn(min = d.touch).clip(RoundedCornerShape(d.buttonRadius)).background(c.surface)
                    .border(d.border, c.border, RoundedCornerShape(d.buttonRadius)).padding(horizontal = d.u(16f), vertical = d.u(12f)),
                textStyle = Pause.type.h2.copy(color = c.text), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), cursorBrush = SolidColor(c.primary),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
                Column(verticalArrangement = Arrangement.spacedBy(d.unit)) {
                    FieldLabel("체급")
                    Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                        listOf(SizeUi.SMALL to "소형", SizeUi.MEDIUM to "중형", SizeUi.LARGE to "대형").forEach { (s, l) -> Choice(l, ui.size == s, { on(UiEvent.SetPetSize(s)) }) }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(d.unit)) {
                    FieldLabel("나이")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.u(12f))) {
                        PauseButton("−", { on(UiEvent.SetPetAge(ui.ageYears - 1)) }, kind = ButtonKind.SECONDARY, compact = true)
                        Text("${ui.ageYears}살", style = Pause.type.h2, color = c.text)
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
            ui.problems.forEach { Text(it, style = Pause.type.label, color = c.danger) }
            ui.calibration?.let { cal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cal, style = Pause.type.body, color = c.text, modifier = Modifier.weight(1f))
                    PauseButton("맞춤 초기화", { on(UiEvent.ResetCalibration) }, kind = ButtonKind.SECONDARY, compact = true)
                }
            }
            Text("프로필은 이 차에만 저장되고 서버로 보내지 않아요.", style = Pause.type.caption, color = c.sub)
        }
    }
}

@Composable
private fun FieldLabel(text: String) = Text(text, style = Pause.type.label, color = Pause.colors.sub)

@Composable
fun PetProfileScreen(ui: PetEditUi, on: On) {
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page(if (ui.firstRun) "반려견을 알려 주세요" else "프로필 편집", on, scroll = true) { _ ->
        V5Card(Modifier.fillMaxWidth(), pad = d.u(24f)) { ProfileForm(ui, on) }
        Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            PauseButton(if (ui.firstRun) "나중에" else "닫기", { on(UiEvent.Close) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            PauseButton("저장", { on(UiEvent.SavePet) }, Modifier.weight(1f), enabled = ui.problems.isEmpty())
        }
    }
}

/** 첫 실행 3단계 (11번): 반려견 프로필 → 차량 연동(신호 확인 결과) → 멀미 민감도 진단(질문 3개 → 3단계 결과). */
@Composable
fun OnboardingScreen(ui: OnboardingUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page("처음 설정 ${ui.step} / 3", on, scroll = true) { _ ->
        StepIndicator(ui.step)
        V5Card(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.u(12f)) {
            when (ui.step) {
                1 -> {
                    SectionTitle("1. 반려견 프로필")
                    ProfileForm(ui.pet, on)
                }
                2 -> {
                    SectionTitle("2. 차량 연동")
                    Row(horizontalArrangement = Arrangement.spacedBy(d.u(24f)), verticalAlignment = Alignment.CenterVertically) {
                        ArtSlot("onboarding", "onboarding_cars", modifier = Modifier.width(d.u(280f)).height(d.u(160f))) { SuvIllustration(Modifier.fillMaxSize().background(c.sky)) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                            ui.signals.forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CheckCircle(s.inUse)
                                    Spacer(Modifier.width(d.u(12f)))
                                    Text(s.name, style = Pause.type.body, color = if (s.inUse) c.text else c.sub, modifier = Modifier.weight(1f))
                                    Text(s.source ?: "이 차에서 못 받음", style = Pause.type.label, color = c.sub)
                                }
                            }
                        }
                    }
                    Text("확인된 신호만 써요. 받지 못한 신호는 지어내지 않고 그 기능만 쉬어요.", style = Pause.type.body, color = c.sub)
                }
                else -> {
                    SectionTitle("3. 멀미 민감도 진단")
                    ui.quiz.forEachIndexed { qi, q ->
                        Text("${qi + 1}. ${q.question}", style = Pause.type.h2, color = c.text)
                        Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                            q.options.forEachIndexed { oi, o -> Choice(o, q.selected == oi, { on(UiEvent.AnswerQuiz(qi, oi)) }) }
                        }
                    }
                    ui.result?.let { r ->
                        WarningBanner(Modifier.fillMaxWidth()) {
                            Text("결과: ${MotionQuiz.label(r)}", style = Pause.type.h1, color = c.warningInk)
                            Text("진단이 아니라 멀미 예보의 출발점이에요. 프로필에서 언제든 바꿀 수 있어요.", style = Pause.type.body, color = c.text)
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.u(12f))) {
        listOf("반려견 프로필", "차량 연동", "멀미 민감도 진단").forEachIndexed { i, label ->
            if (i > 0) Box(Modifier.width(d.u(32f)).height(d.border * 2).background(c.border))
            NumberBadge(i + 1, active = i + 1 == step)
            Text(label, style = Pause.type.label, color = if (i + 1 == step) c.text else c.sub)
        }
    }
}

@Composable
fun SettingsScreen(ui: SettingsUi, on: On, licenses: String) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page("설정", on, scroll = true, trailing = { CloseButton(on) }) { wc ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            V5Card(Modifier.weight(1f), pad = d.u(24f)) {
                SectionTitle("제안과 알림")
                Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    listOf("빠르게", "보통", "여유 있게").forEachIndexed { i, l -> Choice(l, ui.timing == i, { on(UiEvent.SetTiming(i)) }) }
                }
                ToggleRow("음성으로 제안", ui.voiceSuggest, { on(UiEvent.ToggleVoice) }, "끄면 화면 카드와 버튼만")
                ToggleRow("움직임 줄이기", ui.reduceMotion, { on(UiEvent.ToggleReduceMotion) })
            }
            if (wc != WidthClass.THIRD) {
                V5Card(Modifier.weight(1f), pad = d.u(24f)) {
                    SectionTitle("이 차에서 쓰는 신호")
                    ui.shapeSource?.let { Text("앞 구간 형상: $it", style = Pause.type.body, color = c.text) }
                    ui.signals.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CheckCircle(s.inUse, d.u(20f))
                            Spacer(Modifier.width(d.u(12f)))
                            Text(s.name + (s.source?.let { " — $it" } ?: " — 미지원"), style = Pause.type.body, color = if (s.inUse) c.text else c.sub)
                        }
                    }
                }
            }
        }
        V5Card(Modifier.fillMaxWidth(), pad = d.u(24f)) {
            SectionTitle("개인정보")
            Text("반려견 프로필·이동 기록은 이 차에만 있어요. 대기 공유에는 위치를 보내지 않아요.", style = Pause.type.body, color = c.sub)
            PauseButton("내 데이터 삭제", { on(UiEvent.DeleteData) }, kind = ButtonKind.DANGER, compact = true)
        }
        V5Card(Modifier.fillMaxWidth(), pad = d.u(24f)) {
            SectionTitle("데이터 출처")
            ui.credits.forEach { Text(it, style = Pause.type.caption, color = c.sub) }
        }
        Text("같이가개 ${ui.version} · 멀미 지수는 주의 예보이며 의학적 진단이 아니에요.", style = Pause.type.label, color = c.sub)
        if (licenses.isNotBlank()) Text(licenses.take(400), style = Pause.type.caption, color = c.sub)
    }
}

// ───────────── 미리보기 (2560×1440dp, 배율 2) ─────────────

@Preview(widthDp = 2560, heightDp = 1440) @Composable private fun PDrive() = PauseTheme(false, 2f) { DriveScreen(Fixtures.drive) {} }
@Preview(widthDp = 2560, heightDp = 1440) @Composable private fun PForecast() = PauseTheme(false, 2f) { DriveScreen(Fixtures.forecast) {} }
@Preview(widthDp = 2560, heightDp = 1440) @Composable private fun PSuggest() = PauseTheme(false, 2f) { DriveScreen(Fixtures.suggest) {} }
@Preview(widthDp = 2560, heightDp = 1440) @Composable private fun PDepart() = PauseTheme(false, 2f) { DepartScreen(Fixtures.depart) {} }
@Preview(widthDp = 2560, heightDp = 1440) @Composable private fun PWait() = PauseTheme(false, 2f) { WaitScreen(Fixtures.wait) {} }
@Preview(widthDp = 2560, heightDp = 1440) @Composable private fun PLog() = PauseTheme(false, 2f) { TripLogScreen(Fixtures.tripLog) {} }

/** 미리보기 전용 자리 표시 (사진이 없을 때의 일러스트 확인). */
@Preview(widthDp = 400, heightDp = 400) @Composable private fun PDog() = PauseTheme(false, 2f) { DogIllustration(Modifier.fillMaxSize()) }

