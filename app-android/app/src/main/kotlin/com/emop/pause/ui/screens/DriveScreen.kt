package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.components.AppBar
import com.emop.pause.ui.components.Avatar
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.CardKey
import com.emop.pause.ui.components.CountdownHero
import com.emop.pause.ui.components.FatigueGauge
import com.emop.pause.uistate.FatigueTone
import com.emop.pause.ui.components.JourneyLine
import com.emop.pause.ui.components.Landscape
import com.emop.pause.ui.components.LandscapeKind
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PauseCard
import com.emop.pause.ui.components.PauseIcons
import com.emop.pause.ui.components.Pill
import com.emop.pause.ui.components.ReasonChip
import com.emop.pause.ui.components.VoiceHint
import com.emop.pause.ui.components.VoiceSheet
import com.emop.pause.ui.components.toneColor
import com.emop.pause.uistate.toneLabel
import com.emop.pause.uistate.ChipIcon
import com.emop.pause.uistate.ChipUi
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.FatigueUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass

class DriveActions(
    val accept: () -> Unit = {},
    val decline: () -> Unit = {},
    val logoTap: () -> Unit = {},
)

/**
 * 주행 화면 묶음 (UI_SPEC §4.1, §4.6~4.10). 주행 중이라 스크롤·자동 애니메이션 없음, 버튼 ≤ 2개.
 * 폭에 따라 2단(≥1200) / 1/2 창(700~1199) / 1/3 창(<700).
 */
@Composable
fun DriveScreen(ui: DriveUi, actions: DriveActions = DriveActions(), modifier: Modifier = Modifier) {
    val c = Pause.colors
    BoxWithConstraints(modifier.fillMaxSize().background(c.paper)) {
        val width = WidthClass.of(maxWidth)
        val dim = ui.voice != null
        Box(Modifier.fillMaxSize().alpha(if (dim) 0.35f else 1f)) {
            when (width) {
                WidthClass.WIDE -> DriveWide(ui, actions, dim)
                WidthClass.HALF -> DriveNarrow(ui, actions, third = false)
                WidthClass.THIRD -> DriveNarrow(ui, actions, third = true)
            }
        }
        if (ui.voice != null) {
            Box(Modifier.fillMaxSize().background(c.scrim))
            // 앱바는 막 위에서도 그대로 보인다 (시안 10·11).
            if (width == WidthClass.WIDE) AppBar(ui.meta, onLogoTap = actions.logoTap)
            val gutter = gutterOf(width)
            VoiceSheet(
                ui.voice!!,
                Modifier.align(Alignment.BottomCenter).padding(start = gutter, end = gutter, bottom = 56.dp),
                primary = if (ui.content is DriveContent.Urgent) "졸음쉼터로 안내" else "여기서 쉴게요",
                onPrimary = actions.accept,
                onSecondary = actions.decline,
                compact = width != WidthClass.WIDE,
            )
        }
    }
}

private fun gutterOf(w: WidthClass): Dp = when (w) {
    WidthClass.WIDE -> 56.dp
    WidthClass.HALF -> 40.dp
    WidthClass.THIRD -> 32.dp
}

// ───────────────────────── 2단 (≥ 1200dp) ─────────────────────────

@Composable
private fun DriveWide(ui: DriveUi, actions: DriveActions, dimmed: Boolean) {
    val g = Pause.dimens.gutter
    Column(Modifier.fillMaxSize()) {
        AppBar(ui.meta, onLogoTap = actions.logoTap)
        if (ui.offline) OfflineBanner(Modifier.padding(horizontal = g).padding(top = 8.dp))
        when (val content = ui.content) {
            is DriveContent.Next -> {
                Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = g).padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    Column(Modifier.weight(1f).padding(top = if (ui.offline) 24.dp else 72.dp)) {
                        NextLead(content, ui, wide = true, showActions = !dimmed, actions = actions)
                    }
                    if (!ui.restricted && !dimmed) {
                        Column(Modifier.width(500.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            ui.fatigue?.let { FatigueCard(it) }
                            if (ui.family.isNotEmpty()) FamilyCard(ui)
                        }
                    }
                }
                ui.journey?.let { JourneyLine(it, Modifier.padding(horizontal = g).padding(bottom = 72.dp)) }
            }
            is DriveContent.Calm -> {
                CalmCard(content, Modifier.padding(horizontal = g).padding(top = 24.dp).weight(1f).fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                ui.journey?.let { JourneyLine(it, Modifier.padding(horizontal = g).padding(bottom = 72.dp)) }
            }
            is DriveContent.Urgent -> {
                UrgentCard(content, Modifier.padding(horizontal = g).padding(top = 24.dp).weight(1f).fillMaxWidth())
                Spacer(Modifier.height(48.dp))
                if (!dimmed) {
                    Row(Modifier.padding(horizontal = g).padding(bottom = 72.dp), verticalAlignment = Alignment.CenterVertically) {
                        PauseButton("졸음쉼터로 안내", actions.accept, Modifier.weight(1f), kind = ButtonKind.ALERT)
                        Spacer(Modifier.width(20.dp))
                        VoiceHint(color = Pause.colors.alert, ring = Pause.colors.alertSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun NextLead(content: DriveContent.Next, ui: DriveUi, wide: Boolean, showActions: Boolean, actions: DriveActions) {
    val c = Pause.colors
    Text("다음 쉼표까지", style = Pause.type.lead, color = c.ink2)
    Spacer(Modifier.height(16.dp))
    CountdownHero(content.minutes.toString(), "분")
    Spacer(Modifier.height(28.dp))
    PlaceName(content.place, Pause.type.headline)
    if (content.placeSub != null) {
        Spacer(Modifier.height(12.dp))
        Text(content.placeSub!!, style = Pause.type.body, color = c.ink2, maxLines = 1)
    }
    if (!showActions) return
    val chips = chipsFor(content, ui)
    if (chips.isNotEmpty()) {
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { chips.forEach { ReasonChip(it.text, iconOf(it.icon)) } }
    }
    if (content.asking) {
        Spacer(Modifier.height(56.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PauseButton("여기서 쉴게요", actions.accept)
            PauseButton("다음 곳으로", actions.decline, kind = ButtonKind.GHOST)
            if (!ui.offline && wide) VoiceHint(modifier = Modifier.padding(start = 12.dp))
        }
    } else if (ui.toast != null) {
        Spacer(Modifier.height(56.dp))
        Text(ui.toast!!, style = Pause.type.bodyStrong, color = c.rest, maxLines = 1)
    }
}

/** 이유 칩 최대 3개. RESTRICTED면 가족 카드의 가장 급한 한 줄을 칩으로 내린다 (UI_SPEC §4.1). */
private fun chipsFor(content: DriveContent.Next, ui: DriveUi): List<ChipUi> {
    val base = content.chips.take(3)
    if (!ui.restricted) return base
    val urgent = ui.family.firstOrNull() ?: return base.take(2)
    return base.take(2) + ChipUi("${urgent.name} ${urgent.text}")
}

/** 휴게소 이름은 한 줄. 넘치면 "휴게소" 뒤 방향(괄호)을 생략한다. */
@Composable
private fun PlaceName(name: String, style: androidx.compose.ui.text.TextStyle) {
    val short = name.substringBefore("(").trim()
    Text(short, style = style, color = Pause.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun FatigueCard(f: FatigueUi, modifier: Modifier = Modifier) {
    PauseCard(modifier.fillMaxWidth()) {
        CardKey("운전 피로")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(toneLabel(f.tone), style = Pause.type.level, color = toneColor(f.tone))
            Spacer(Modifier.width(16.dp))
            Text(f.score.toString(), Modifier.padding(bottom = 4.dp), style = Pause.type.bodyStrong.copy(fontWeight = FontWeight.SemiBold), color = Pause.colors.ink2)
        }
        Spacer(Modifier.height(18.dp))
        FatigueGauge(f.score)
        Spacer(Modifier.height(20.dp))
        Text(f.detail, style = Pause.type.label.copy(fontWeight = FontWeight.Normal, lineHeight = Pause.type.body.lineHeight), color = Pause.colors.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FamilyCard(ui: DriveUi) {
    PauseCard(Modifier.fillMaxWidth()) {
        CardKey("함께 탄 가족")
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ui.family.take(3).forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(line.kind, 56.dp, 30.dp)
                    Spacer(Modifier.width(20.dp))
                    Text(line.name, style = Pause.type.bodyStrong, color = Pause.colors.ink, maxLines = 1)
                    Spacer(Modifier.width(10.dp))
                    Text(line.text, style = Pause.type.body, color = Pause.colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier, text: String = "인터넷이 끊겨 저장된 휴게소 정보로 안내하고 있어요") {
    Row(
        modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp)).background(Pause.colors.sunk).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PauseIcons.CloudOff, null, Modifier.size(32.dp), tint = Pause.colors.ink2)
        Spacer(Modifier.width(16.dp))
        Text(text, style = Pause.type.label, color = Pause.colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CalmCard(content: DriveContent.Calm, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(36.dp))) {
        Landscape(LandscapeKind.CALM, Modifier.fillMaxSize())
        Column(Modifier.padding(start = 64.dp, top = 80.dp, end = 64.dp)) {
            Text("지금은 편안해요", style = Pause.type.calm, color = Pause.colors.ink, maxLines = 1)
            Spacer(Modifier.height(20.dp))
            Text(content.line, style = Pause.type.lead.copy(fontWeight = FontWeight.Normal), color = Pause.colors.ink2, maxLines = 2)
        }
        Row(Modifier.align(Alignment.BottomStart).padding(start = 64.dp, bottom = 56.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            content.chips.take(2).forEach { ReasonChip(it.text, iconOf(it.icon)) }
        }
    }
}

@Composable
private fun UrgentCard(content: DriveContent.Urgent, modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = Pause.colors
    Column(modifier.clip(RoundedCornerShape(36.dp)).background(c.alertSoft).padding(if (compact) 40.dp else 64.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PauseIcons.Warn, null, Modifier.size(40.dp), tint = c.alert)
            Spacer(Modifier.width(14.dp))
            Text("피로 위험 ${content.score}", style = Pause.type.lead.copy(fontWeight = FontWeight.ExtraBold), color = c.alert)
        }
        Spacer(Modifier.height(28.dp))
        Text("지금 쉬어야 해요", style = if (compact) Pause.type.headline else Pause.type.hero, color = c.ink, maxLines = 1)
        Spacer(Modifier.height(16.dp))
        Text("가장 가까운 졸음쉼터로 안내할게요.", style = Pause.type.lead.copy(fontWeight = FontWeight.Normal), color = c.alertInk, maxLines = 2)
        Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.surface).padding(horizontal = 40.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(content.place, style = Pause.type.sheet, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(content.placeSub, style = Pause.type.label, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(content.km, style = Pause.type.hero, color = c.ink)
                Text("km", Modifier.padding(bottom = 14.dp), style = Pause.type.title.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
            }
        }
    }
}

// ───────────────────────── 1/2 창·1/3 창 ─────────────────────────

@Composable
private fun DriveNarrow(ui: DriveUi, actions: DriveActions, third: Boolean) {
    val c = Pause.colors
    val g = if (third) 32.dp else 40.dp
    val tone = ui.fatigue?.tone
    Column(Modifier.fillMaxSize()) {
        AppBar(
            null,
            gutter = g,
            onLogoTap = actions.logoTap,
            trailing = tone?.takeIf { it != FatigueTone.CALM }?.let {
                {
                    val (bg, fg) = if (it == FatigueTone.ALERT) c.alertSoft to c.alert else c.cautionSoft to c.cautionInk
                    Pill(if (third) toneLabel(it) else "피로 ${toneLabel(it)}", bg, fg)
                }
            },
        )
        if (ui.offline && !third) OfflineBanner(Modifier.padding(horizontal = g).padding(top = 8.dp), "저장된 휴게소 정보로 안내 중")
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = g).padding(top = 32.dp)) {
            val numberStyle = if (third) Pause.type.display.copy(fontSize = Pause.type.display.fontSize * 0.8f) else Pause.type.display.copy(fontSize = Pause.type.display.fontSize * 0.9f)
            val unitStyle = Pause.type.displayUnit.copy(fontSize = if (third) Pause.type.lead.fontSize * 1.75f else Pause.type.headline.fontSize)
            val placeStyle = Pause.type.headline.copy(fontSize = if (third) Pause.type.level.fontSize else Pause.type.headline.fontSize * 0.875f)
            when (val content = ui.content) {
                is DriveContent.Next -> {
                    Text("다음 쉼표까지", style = if (third) Pause.type.bodyStrong.copy(fontWeight = FontWeight.SemiBold) else Pause.type.lead, color = c.ink2)
                    Spacer(Modifier.height(16.dp))
                    CountdownHero(content.minutes.toString(), "분", numberStyle = numberStyle, unitStyle = unitStyle)
                    Spacer(Modifier.height(20.dp))
                    PlaceName(content.place, placeStyle)
                    if (!third && ui.voice == null) {
                        val chips = chipsFor(content, ui).take(2)
                        if (chips.isNotEmpty()) {
                            Spacer(Modifier.height(28.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { chips.forEach { ReasonChip(it.text, iconOf(it.icon)) } }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (content.asking && ui.voice == null) {
                        PauseButton("여기서 쉴게요", actions.accept, Modifier.fillMaxWidth())
                        if (!third) {
                            Spacer(Modifier.height(20.dp))
                            PauseButton("다음 곳으로", actions.decline, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET)
                            Spacer(Modifier.height(72.dp))
                            VoiceHint(modifier = Modifier.padding(bottom = 72.dp))
                        } else {
                            Spacer(Modifier.height(96.dp))
                        }
                    } else if (ui.toast != null) {
                        Text(ui.toast!!, Modifier.padding(bottom = 96.dp), style = Pause.type.bodyStrong, color = c.rest, maxLines = 2)
                    }
                }
                is DriveContent.Calm -> {
                    Text("지금은 편안해요", style = placeStyle, color = c.ink, maxLines = 1)
                    Spacer(Modifier.height(20.dp))
                    Text(content.line, style = Pause.type.body, color = c.ink2, maxLines = 2)
                }
                is DriveContent.Urgent -> {
                    UrgentCard(content, Modifier.weight(1f).fillMaxWidth(), compact = true)
                    Spacer(Modifier.height(32.dp))
                    if (ui.voice == null) PauseButton("졸음쉼터로 안내", actions.accept, Modifier.fillMaxWidth().padding(bottom = 72.dp), kind = ButtonKind.ALERT)
                }
            }
        }
    }
}

fun iconOf(icon: ChipIcon): ImageVector? = when (icon) {
    ChipIcon.KID -> PauseIcons.Kid
    ChipIcon.BOLT -> PauseIcons.Bolt
    ChipIcon.PAW -> PauseIcons.Paw
    ChipIcon.BABY -> PauseIcons.Baby
    ChipIcon.BED -> PauseIcons.Bed
    ChipIcon.COMMA -> PauseIcons.Comma
    ChipIcon.NONE -> null
}

// ───────────────────────── 미리보기 ─────────────────────────

@Preview(name = "01 주행", widthDp = 1600, heightDp = 1280)
@Composable
private fun DrivePreview() = PauseTheme(dark = false) { DriveScreen(Fixtures.drive) }

@Preview(name = "06 1/2 창", widthDp = 800, heightDp = 1280)
@Composable
private fun HalfPreview() = PauseTheme(dark = false) { DriveScreen(Fixtures.half) }

@Preview(name = "07 1/3 창", widthDp = 600, heightDp = 1280)
@Composable
private fun ThirdPreview() = PauseTheme(dark = false) { DriveScreen(Fixtures.half) }

@Preview(name = "08 제안 없음", widthDp = 1600, heightDp = 1280)
@Composable
private fun CalmPreview() = PauseTheme(dark = false) { DriveScreen(Fixtures.calm) }

@Preview(name = "09 긴급", widthDp = 1600, heightDp = 1280)
@Composable
private fun UrgentPreview() = PauseTheme(dark = false) { DriveScreen(Fixtures.urgent) }

@Preview(name = "11 음성 실패", widthDp = 1600, heightDp = 1280)
@Composable
private fun VoiceFailPreview() = PauseTheme(dark = false) { DriveScreen(Fixtures.voiceFail) }

@Preview(name = "12 오프라인", widthDp = 1600, heightDp = 1280)
@Composable
private fun OfflinePreview() = PauseTheme(dark = false) { DriveScreen(Fixtures.offline) }
