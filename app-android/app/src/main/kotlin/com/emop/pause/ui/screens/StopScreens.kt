package com.emop.pause.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.components.ActivityRow
import com.emop.pause.ui.components.AppBar
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.CardKey
import com.emop.pause.ui.components.CountdownHero
import com.emop.pause.ui.components.Fireplace
import com.emop.pause.ui.components.JourneyLine
import com.emop.pause.ui.components.Landscape
import com.emop.pause.ui.components.LandscapeKind
import com.emop.pause.ui.components.MemberTile
import com.emop.pause.ui.components.NapRing
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PauseCard
import com.emop.pause.ui.components.PauseIcons
import com.emop.pause.ui.components.SettingRow
import com.emop.pause.ui.components.StopTimeline
import com.emop.pause.ui.components.Tag
import com.emop.pause.uistate.ActivityKind
import com.emop.pause.uistate.ActivityUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.NapUi
import com.emop.pause.uistate.PauseScreenUi
import com.emop.pause.uistate.PrepareScreenUi
import com.emop.pause.uistate.ReportUi
import com.emop.pause.ui.theme.NapColors
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass

// ═════════════════════════ 출발 준비 (02) ═════════════════════════

class PrepareActions(
    val toggleMember: (String) -> Unit = {},
    val setSleep: (Int?) -> Unit = {},
    val toggleProactive: () -> Unit = {},
    val start: () -> Unit = {},
    val change: () -> Unit = {},
    val logoTap: () -> Unit = {},
)

/** 출발 준비 (UI_SPEC §4.2, AppMode = PREPARE). 정차 중이라 전체 화면을 쓴다. */
@Composable
fun PrepareScreen(ui: PrepareScreenUi, actions: PrepareActions = PrepareActions()) {
    val c = Pause.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.paper)) {
        val wide = WidthClass.of(maxWidth) == WidthClass.WIDE
        Column(Modifier.fillMaxSize()) {
            AppBar(ui.meta, onLogoTap = actions.logoTap)
            if (wide) {
                Row(Modifier.weight(1f).padding(start = 56.dp, end = 56.dp, top = 24.dp, bottom = 56.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        MembersCard(ui, actions)
                        SleepCard(ui, actions)
                        ProactiveCard(ui, actions, Modifier.weight(1f))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        PlanCard(ui, Modifier.weight(1f))
                        StartRow(actions)
                    }
                }
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    MembersCard(ui, actions)
                    SleepCard(ui, actions)
                    PlanCard(ui, Modifier)
                }
                Box(Modifier.padding(horizontal = 40.dp, vertical = 32.dp)) { StartRow(actions) }
            }
        }
    }
}

@Composable
private fun MembersCard(ui: PrepareScreenUi, actions: PrepareActions) {
    PauseCard(Modifier.fillMaxWidth()) {
        Text("오늘도 이 멤버로 가요?", style = Pause.type.title, color = Pause.colors.ink)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ui.members.forEach { m -> MemberTile(m.label, m.kind, m.selected, { actions.toggleMember(m.id) }, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SleepCard(ui: PrepareScreenUi, actions: PrepareActions) {
    val c = Pause.colors
    PauseCard(Modifier.fillMaxWidth()) {
        Text("어젯밤 몇 시간 주무셨어요?", style = Pause.type.title, color = c.ink)
        Spacer(Modifier.height(10.dp))
        Text(
            if (ui.sleepHours == null) "고르지 않으면 7시간으로 계산해요." else "덜 주무셨으면 첫 휴식을 앞당겨요.",
            style = Pause.type.body,
            color = c.ink2,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            (4..8).forEach { h ->
                val sel = ui.sleepHours == h
                Row(
                    Modifier
                        .weight(1f)
                        .height(112.dp)
                        .clip(RoundedCornerShape(56.dp))
                        .background(if (sel) c.ink else c.paper)
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { actions.setSleep(if (sel) null else h) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val fg = if (sel) c.paper else c.ink
                    Text("$h", style = Pause.type.button, color = fg)
                    Text(if (h == 8) "시간+" else "시간", Modifier.padding(start = 2.dp), style = Pause.type.captionStrong, color = fg, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ProactiveCard(ui: PrepareScreenUi, actions: PrepareActions, modifier: Modifier) {
    PauseCard(modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        SettingRow("피로가 오르기 전에 먼저 알려 주기", "주의 단계 전에 제안하고, 2시간을 넘기지 않아요.", ui.proactive, actions.toggleProactive)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PlanCard(ui: PrepareScreenUi, modifier: Modifier) {
    val c = Pause.colors
    PauseCard(modifier.fillMaxWidth()) {
        Text(if (ui.plan.isEmpty()) "오늘의 쉼표" else "오늘의 쉼표 ${ui.plan.size}번", style = Pause.type.title, color = c.ink)
        Spacer(Modifier.height(12.dp))
        if (ui.plan.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("출발하면 피로와 가족의 필요를 보고 쉴 곳을 잡아 드려요.", style = Pause.type.body, color = c.ink2)
        }
        ui.plan.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(thickness = 2.dp, color = c.paper)
            Row(Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(PauseIcons.Comma, null, Modifier.width(56.dp).height(48.dp).padding(end = 26.dp), tint = c.rest)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = Pause.type.item.copy(fontWeight = FontWeight.ExtraBold), color = c.ink, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Text(p.facilities, style = Pause.type.caption, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(p.eta, style = Pause.type.bodyStrong, color = c.ink, maxLines = 1)
            }
        }
        ui.journey?.let {
            Spacer(Modifier.height(36.dp))
            JourneyLine(it)
        }
        Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
        Text(ui.rationale, style = Pause.type.body, color = c.ink2, maxLines = 2)
    }
}

@Composable
private fun StartRow(actions: PrepareActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PauseButton("이대로 출발", actions.start, Modifier.weight(1f))
        PauseButton("바꾸기", actions.change, kind = ButtonKind.QUIET)
    }
}

// ═════════════════════════ 멈춤 큐레이터 (03) ═════════════════════════

class PauseActions(
    val activity: (ActivityKind) -> Unit = {},
    val restMinutes: (Int) -> Unit = {},
    val logoTap: () -> Unit = {},
)

/** 멈춤 큐레이터 (UI_SPEC §4.3, AppMode = PAUSE). */
@Composable
fun PauseScreen(ui: PauseScreenUi, actions: PauseActions = PauseActions()) {
    val c = Pause.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.paper)) {
        val wide = WidthClass.of(maxWidth) == WidthClass.WIDE
        val g = if (wide) 56.dp else 40.dp
        Column(Modifier.fillMaxSize()) {
            AppBar(ui.meta, gutter = g, onLogoTap = actions.logoTap)
            Column(Modifier.weight(1f).then(if (wide) Modifier else Modifier.verticalScroll(rememberScrollState())).padding(horizontal = g)) {
                if (wide) {
                    Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                        PauseHero(ui, actions, Modifier.weight(1f))
                        ui.battery?.let { BatteryCard(it, Modifier.width(560.dp)) }
                    }
                } else {
                    PauseHero(ui, actions, Modifier.padding(top = 24.dp))
                    ui.battery?.let { Spacer(Modifier.height(24.dp)); BatteryCard(it, Modifier.fillMaxWidth()) }
                }
                Spacer(Modifier.height(if (wide) 44.dp else 32.dp))
                if (ui.notice != null) {
                    Text(ui.notice!!, style = Pause.type.body, color = c.ink2, maxLines = 2)
                    Spacer(Modifier.height(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ui.activities.forEach { a -> ActivityFor(a, actions) }
                }
                if (wide) Spacer(Modifier.weight(1f))
                if (ui.timeline.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    StopTimeline(ui.timeline, ui.nowFraction, Modifier.padding(bottom = 40.dp))
                }
            }
        }
    }
}

@Composable
private fun PauseHero(ui: PauseScreenUi, actions: PauseActions, modifier: Modifier) {
    Column(modifier) {
        Text(ui.lead, style = Pause.type.lead, color = Pause.colors.ink2)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            CountdownHero(ui.number, ui.unit, numberStyle = Pause.type.display.copy(fontSize = Pause.type.display.fontSize * 0.9f))
            if (ui.restChoice != null) {
                Spacer(Modifier.width(40.dp))
                Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(10, 15, 20).forEach { m ->
                        PauseButton("${m}분", { actions.restMinutes(m) }, kind = if (m == ui.restChoice) ButtonKind.PRIMARY else ButtonKind.QUIET, small = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryCard(b: com.emop.pause.uistate.BatteryUi, modifier: Modifier) {
    val c = Pause.colors
    PauseCard(modifier) {
        CardKey("배터리")
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${b.percent}%", style = Pause.type.number, color = c.ink)
            Spacer(Modifier.width(12.dp))
            Text("목표 ${b.target}%", Modifier.padding(bottom = 8.dp), style = Pause.type.label, color = c.ink2)
        }
        Spacer(Modifier.height(18.dp))
        Canvas(Modifier.fillMaxWidth().height(52.dp)) {
            val top = 6.dp.toPx()
            val h = 40.dp.toPx()
            val r = androidx.compose.ui.geometry.CornerRadius(h / 2)
            drawRoundRect(c.line, Offset(0f, top), androidx.compose.ui.geometry.Size(size.width, h), r)
            drawRoundRect(c.rest, Offset(0f, top), androidx.compose.ui.geometry.Size(size.width * b.percent / 100f, h), r)
            val tx = size.width * b.target / 100f
            drawRoundRect(c.ink, Offset(tx - 2.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(b.power ?: "충전 속도 정보 없음", style = Pause.type.caption, color = c.ink2, maxLines = 1)
            Spacer(Modifier.weight(1f))
            b.doneAt?.let { Text(it, style = Pause.type.caption, color = c.ink2, maxLines = 1) }
        }
    }
}

@Composable
private fun ActivityFor(a: ActivityUi, actions: PauseActions) {
    val c = Pause.colors
    val (icon, bg, tint) = when (a.kind) {
        ActivityKind.NAP -> Triple(PauseIcons.Moon, c.restSoft, c.rest)
        ActivityKind.EXPEDITION -> Triple(PauseIcons.Flag, c.kidSoft, c.kidInk)
        ActivityKind.PET_WALK -> Triple(PauseIcons.Paw, c.petSoft, c.petInk)
        ActivityKind.FOCUS -> Triple(PauseIcons.Briefcase, c.sunk, c.ink2)
        ActivityKind.STRETCH -> Triple(PauseIcons.Stretch, c.restSoft, c.rest)
    }
    ActivityRow(a.who, a.title, a.description, icon, bg, tint, a.action, { actions.activity(a.kind) }, a.recommended, badge = a.badge, enabled = a.enabled)
}

// ═════════════════════════ 파워냅 (04) ═════════════════════════

class NapActions(val wake: () -> Unit = {}, val more: () -> Unit = {}, val sound: () -> Unit = {})

/**
 * 파워냅 (UI_SPEC §4.4). 잠든 차 안에서 밝은 화면은 휴식을 방해하므로 이 화면만 어두운 난로 테마.
 * 화면 밝기 낮춤은 Activity가 window 밝기로 처리한다. 주행이 시작되면 상위(PauseRoot)가 즉시 주행 화면으로 바꾼다.
 */
@Composable
fun NapScreen(ui: NapUi, actions: NapActions = NapActions(), logoTap: () -> Unit = {}) {
    // 1분마다 갱신, 마지막 1분만 초 단위 (UI_SPEC §3 CountdownHero).
    val time = if (ui.remainingSec >= 60) "${(ui.remainingSec + 59) / 60}:00" else "0:%02d".format(ui.remainingSec)
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(NapColors.night)
            drawRect(
                Brush.radialGradient(
                    0f to NapColors.glow,
                    0.45f to NapColors.night2,
                    1f to NapColors.night,
                    center = Offset(size.width / 2, size.height * 1.15f),
                    radius = size.height * 0.95f,
                ),
            )
        },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            AppBar(
                if (ui.waking) "곧 깨워 드릴게요" else "화면을 어둡게 했어요",
                brandColor = NapColors.brand,
                markColor = NapColors.ember,
                metaColor = NapColors.brand,
                onLogoTap = logoTap,
            )
            Spacer(Modifier.weight(0.35f))
            NapRing(ui.remainingSec / ui.totalSec.coerceAtLeast(1).toFloat(), "남은 시간", time, Modifier.size(560.dp))
            Spacer(Modifier.height(30.dp))
            Text(ui.wakeLine, style = Pause.type.item.copy(fontWeight = FontWeight.Normal), color = NapColors.muted, maxLines = 1)
            Spacer(Modifier.weight(0.2f))
            Fireplace(Modifier.size(480.dp, 240.dp))
            Spacer(Modifier.weight(0.15f))
            Row(Modifier.padding(bottom = 56.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                PauseButton("지금 일어나기", actions.wake, kind = ButtonKind.EMBER)
                PauseButton("10분 더", actions.more, kind = ButtonKind.DIM)
                PauseButton(if (ui.soundOn) "장작 소리 끄기" else "장작 소리 켜기", actions.sound, kind = ButtonKind.DIM)
            }
        }
    }
}

// ═════════════════════════ 여행 리포트 (05) ═════════════════════════

@Composable
fun ReportScreen(ui: ReportUi, onDone: () -> Unit = {}, logoTap: () -> Unit = {}) {
    val c = Pause.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.paper)) {
        val wide = WidthClass.of(maxWidth) == WidthClass.WIDE
        Column(Modifier.fillMaxSize()) {
            AppBar(ui.meta, onLogoTap = logoTap)
            if (wide) {
                Row(Modifier.weight(1f).padding(start = 56.dp, end = 56.dp, top = 24.dp, bottom = 56.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(Modifier.weight(1f)) { ReportLeft(ui) }
                    Column(Modifier.width(560.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        FatigueFlowCard(ui)
                        TipCard(ui)
                        Spacer(Modifier.weight(1f))
                        PauseButton("확인", onDone, Modifier.fillMaxWidth())
                    }
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    ReportLeft(ui)
                    FatigueFlowCard(ui)
                    TipCard(ui)
                }
                PauseButton("확인", onDone, Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 32.dp))
            }
        }
    }
}

@Composable
private fun ReportLeft(ui: ReportUi) {
    val c = Pause.colors
    Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(28.dp))) {
        Landscape(LandscapeKind.REPORT, Modifier.fillMaxSize())
        val text = buildAnnotatedString {
            ui.sentence.forEachIndexed { i, part ->
                if (i > 0) withStyle(SpanStyle(color = c.rest)) { append(",") }
                append(part)
            }
        }
        Text(text, Modifier.padding(44.dp), style = Pause.type.sentence, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        StatCard("달린 거리", listOf("${ui.distanceKm}" to "km"), Modifier.weight(1f))
        StatCard("쉼표", listOf("${ui.stops}" to "번"), Modifier.weight(1f))
        val h = ui.longestMin / 60
        val m = ui.longestMin % 60
        StatCard("가장 오래 쉬지 않고", if (h > 0) listOf("$h" to "시간", "$m" to "분") else listOf("$m" to "분"), Modifier.weight(1f))
    }
    if (ui.log.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        PauseCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 40.dp, vertical = 20.dp)) {
            ui.log.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(thickness = 2.dp, color = c.paper)
                Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(PauseIcons.Comma, null, Modifier.width(48.dp).height(42.dp).padding(end = 22.dp), tint = c.rest)
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = Pause.type.bodyStrong.copy(fontWeight = FontWeight.ExtraBold), color = c.ink, maxLines = 1)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { s.tags.forEach { Tag(it) } }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(s.arrived, style = Pause.type.caption, color = c.ink2)
                        Text(s.stayed, style = Pause.type.caption, color = c.ink2)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(key: String, parts: List<Pair<String, String>>, modifier: Modifier) {
    PauseCard(modifier, padding = androidx.compose.foundation.layout.PaddingValues(30.dp)) {
        CardKey(key)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            parts.forEach { (v, u) ->
                Text(v, style = Pause.type.number, color = Pause.colors.ink, maxLines = 1)
                Text(u, Modifier.padding(start = 4.dp, end = 6.dp, bottom = 8.dp), style = Pause.type.label, color = Pause.colors.ink, maxLines = 1)
            }
        }
    }
}

/** 피로 흐름 그래프: "주의" 점선 기준선(caution), 쉼표 지점 표시 (UI_SPEC §4.5). */
@Composable
private fun FatigueFlowCard(ui: ReportUi) {
    val c = Pause.colors
    val comma = remember { androidx.compose.ui.graphics.vector.PathParser().parsePathString(PauseIcons.COMMA_PATH).toPath() }
    PauseCard(Modifier.fillMaxWidth()) {
        CardKey("운전 피로 흐름")
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(240.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val baseY = size.height * 0.875f
                val top = 0f
                fun y(v: Int) = baseY - (baseY - top) * v / 100f
                val cautionY = y(40)
                drawLine(c.caution, Offset(0f, cautionY), Offset(size.width, cautionY), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f)))
                drawLine(c.line, Offset(0f, baseY), Offset(size.width, baseY), 2.dp.toPx())
                val n = ui.fatigueSeries.size
                if (n >= 2) {
                    val p = Path()
                    ui.fatigueSeries.forEachIndexed { i, v ->
                        val x = size.width * i / (n - 1)
                        if (i == 0) p.moveTo(x, y(v)) else p.lineTo(x, y(v))
                    }
                    drawPath(p, c.ink, style = Stroke(5.dp.toPx(), join = StrokeJoin.Round))
                }
                ui.restMarks.forEach { f ->
                    val s = 0.5f * density
                    val dx = size.width * f - 10.dp.toPx()
                    val dy = baseY + 4.dp.toPx()
                    withTransform({ translate(dx, dy); scale(s, s, Offset.Zero) }) { drawPath(comma, c.rest) }
                }
            }
            Text("주의", Modifier.align(Alignment.TopEnd).padding(top = 60.dp), style = Pause.type.small.copy(fontWeight = FontWeight.Bold), color = c.caution)
        }
    }
}

@Composable
private fun TipCard(ui: ReportUi) {
    PauseCard(Modifier.fillMaxWidth()) {
        CardKey("다음 여행 팁")
        Spacer(Modifier.height(12.dp))
        Text(ui.tip, style = Pause.type.item.copy(fontWeight = FontWeight.Normal, lineHeight = Pause.type.item.fontSize * 1.5f), color = Pause.colors.ink)
    }
}

// ═════════════════════════ 미리보기 ═════════════════════════

@Preview(name = "02 출발 준비", widthDp = 1600, heightDp = 1280)
@Composable
private fun PreparePreview() = PauseTheme(dark = false) { PrepareScreen(Fixtures.prepare) }

@Preview(name = "03 멈춤", widthDp = 1600, heightDp = 1280)
@Composable
private fun PausePreview() = PauseTheme(dark = false) { PauseScreen(Fixtures.pause) }

@Preview(name = "04 파워냅", widthDp = 1600, heightDp = 1280)
@Composable
private fun NapPreview() = PauseTheme(dark = false) { NapScreen(Fixtures.nap) }

@Preview(name = "05 리포트", widthDp = 1600, heightDp = 1280)
@Composable
private fun ReportPreview() = PauseTheme(dark = false) { ReportScreen(Fixtures.report) }
