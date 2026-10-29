package com.emop.pause.ui.components

import com.emop.pause.uistate.AvatarKind
import com.emop.pause.uistate.FatigueTone
import com.emop.pause.uistate.toneOf
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme

// ───────────────────────── 앱바 ─────────────────────────

/** 앱바 88dp: 왼쪽 쉼표 + "쉼표", 오른쪽 메타(24sp) 또는 알약. 로고는 발표 모드 진입점(5번 탭)이다. */
@Composable
fun AppBar(
    meta: String?,
    modifier: Modifier = Modifier,
    gutter: Dp = Pause.dimens.gutter,
    brandColor: Color = Pause.colors.ink,
    markColor: Color = Pause.colors.rest,
    metaColor: Color = Pause.colors.ink2,
    onLogoTap: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().height(Pause.dimens.appBar).padding(horizontal = gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClick = onLogoTap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(PauseIcons.Comma, null, Modifier.size(22.dp, 36.dp).offset(y = 4.dp), tint = markColor)
            Spacer(Modifier.width(12.dp))
            Text("쉼표", style = Pause.type.item.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = brandColor)
        }
        Spacer(Modifier.weight(1f))
        when {
            trailing != null -> trailing()
            meta != null -> Text(meta, style = Pause.type.caption, color = metaColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 앱바 오른쪽 알약 (1/2·1/3 창의 "피로 주의"). */
@Composable
fun Pill(text: String, background: Color, color: Color) {
    Box(
        Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(background).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Pause.type.tag, color = color, maxLines = 1) }
}

// ───────────────────────── 카드 ─────────────────────────

@Composable
fun PauseCard(
    modifier: Modifier = Modifier,
    color: Color = Pause.colors.surface,
    padding: PaddingValues = PaddingValues(Pause.dimens.cardPad),
    radius: Dp = Pause.dimens.cardRadius,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.clip(RoundedCornerShape(radius)).background(color).padding(padding), content = content)
}

/** 카드 머리글 (24sp SemiBold ink2). */
@Composable
fun CardKey(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = Pause.type.captionStrong, color = Pause.colors.ink2)
}

// ───────────────────────── 버튼 ─────────────────────────

enum class ButtonKind { PRIMARY, QUIET, GHOST, ALERT, EMBER, DIM }

/**
 * 버튼 (UI_SPEC §3). 글자는 한 줄 고정 — 넘치면 글자를 줄이지 않고 폭이 늘어난다(wrap content).
 * QUIET는 배경과 다른 색이어야 하므로 [onCard]=true면 paper, 아니면 surface (§2.1).
 * 눌림: 8% 어둡게. 비활성: line 배경.
 */
@Composable
fun PauseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    small: Boolean = false,
    onCard: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = Pause.colors
    val (bg, fg) = when (kind) {
        ButtonKind.PRIMARY -> c.rest to Color.White
        ButtonKind.ALERT -> c.alert to Color.White
        ButtonKind.QUIET -> (if (onCard) c.paper else c.surface) to c.ink
        ButtonKind.GHOST -> Color.Transparent to c.ink2
        ButtonKind.EMBER -> com.emop.pause.ui.theme.NapColors.ember to com.emop.pause.ui.theme.NapColors.emberOn
        ButtonKind.DIM -> com.emop.pause.ui.theme.NapColors.dimButton to com.emop.pause.ui.theme.NapColors.dimButtonInk
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val fill = when {
        !enabled -> c.line
        pressed && bg != Color.Transparent -> Color.Black.copy(alpha = 0.08f).compositeOver(bg)
        pressed -> c.ink.copy(alpha = 0.06f)
        else -> bg
    }
    val height = if (small) Pause.dimens.buttonSmall else Pause.dimens.button
    val hPad = when {
        kind == ButtonKind.GHOST -> 32.dp
        small -> 40.dp
        else -> 56.dp
    }
    val style = if (small) Pause.type.button.copy(fontSize = Pause.type.item.fontSize) else Pause.type.button
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(fill)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = hPad),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(36.dp), tint = if (enabled) fg else c.ink3)
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text,
            style = style.copy(fontWeight = if (kind == ButtonKind.GHOST) androidx.compose.ui.text.font.FontWeight.SemiBold else style.fontWeight),
            color = if (enabled) fg else c.ink3,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ───────────────────────── 칩 ─────────────────────────

/** 이유 칩 (UI_SPEC §3 ReasonChip): 60dp, 아이콘 28dp + 26sp SemiBold. 배경은 화면 배경과 다른 색. */
@Composable
fun ReasonChip(text: String, icon: ImageVector? = null, onCard: Boolean = false, iconTint: Color = Pause.colors.rest, modifier: Modifier = Modifier) {
    val c = Pause.colors
    Row(
        modifier.height(Pause.dimens.chip).clip(RoundedCornerShape(30.dp)).background(if (onCard) c.paper else c.surface).padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(28.dp), tint = iconTint)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = Pause.type.label, color = c.ink, maxLines = 1, softWrap = false)
    }
}

/** 작은 태그 (리포트 쉼표 기록). */
@Composable
fun Tag(text: String) {
    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Pause.colors.paper).padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(text, style = Pause.type.small, color = Pause.colors.ink2, maxLines = 1)
    }
}

// ───────────────────────── 음성 힌트 ─────────────────────────

/**
 * 음성 힌트 (UI_SPEC §3 VoiceHint): 16dp 점 + 8dp 링 + 26sp 글자.
 * 듣는 중 링이 천천히 커졌다 작아진다 — 움직임이 허용될 때(정차 중)만. 주행 중엔 정적.
 */
@Composable
fun VoiceHint(
    text: String = "\"응\"이라고 말해도 돼요",
    color: Color = Pause.colors.rest,
    ring: Color = Pause.colors.restSoft,
    listening: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ringWidth = if (listening && Pause.motion) {
        val t = rememberInfiniteTransition(label = "voice")
        t.animateFloat(4f, 12f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "ring").value
    } else {
        8f
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size((16 + ringWidth * 2).dp).clip(CircleShape).background(ring),
            )
            Box(Modifier.size(16.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = Pause.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = color, maxLines = 1, softWrap = false)
    }
}

// ───────────────────────── 피로 게이지 ─────────────────────────

/**
 * 피로 게이지 (UI_SPEC §3 FatigueGauge): 10칸, 높이 16dp, 간격 8dp, 모서리 8dp.
 * 칸 색은 점수 단계 색으로 채운다: 0~3 rest, 4~7 caution, 8~10 alert. 색만으로 의미를 전하지 않도록 단계 글자와 함께 쓴다.
 */
@Composable
fun FatigueGauge(score: Int, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val filled = ((score + 5) / 10).coerceIn(0, 10)
    val tone = toneOf(score)
    val color = when (tone) {
        FatigueTone.CALM -> c.rest
        FatigueTone.CAUTION -> c.caution
        FatigueTone.ALERT -> c.alert
    }
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = "피로 $score" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(10) { i ->
            Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(if (i < filled) color else c.line))
        }
    }
}

@Composable
fun toneColor(tone: FatigueTone): Color = when (tone) {
    FatigueTone.CALM -> Pause.colors.rest
    FatigueTone.CAUTION -> Pause.colors.caution
    FatigueTone.ALERT -> Pause.colors.alert
}

// ───────────────────────── 토글·세그먼트 ─────────────────────────

/** 토글 96×56dp. 터치 영역은 행 전체. */
@Composable
fun PauseToggle(on: Boolean, modifier: Modifier = Modifier) {
    val c = Pause.colors
    Box(modifier.size(96.dp, 56.dp).clip(RoundedCornerShape(28.dp)).background(if (on) c.rest else c.line)) {
        Box(
            Modifier.padding(6.dp).size(44.dp).align(if (on) Alignment.CenterEnd else Alignment.CenterStart).clip(CircleShape).background(Color.White),
        )
    }
}

/** 설정 행: 제목 30sp + 설명 26sp + 오른쪽 토글. 행 전체가 터치 영역. */
@Composable
fun SettingRow(title: String, detail: String?, on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Pause.dimens.touch)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Switch, onClick = onToggle)
            .semantics { stateDescription = if (on) "켜짐" else "꺼짐" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Pause.type.item, color = Pause.colors.ink)
            if (detail != null) {
                Spacer(Modifier.height(8.dp))
                Text(detail, style = Pause.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Pause.colors.ink2)
            }
        }
        Spacer(Modifier.width(24.dp))
        PauseToggle(on)
    }
}

/** 세그먼트 (설정 제안 시점). */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pause.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(c.paper).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(84.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (on) c.surface else Color.Transparent)
                    .then(if (on) Modifier.border(2.dp, c.line, RoundedCornerShape(28.dp)) else Modifier)
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Pause.type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = if (on) c.ink else c.ink2, maxLines = 1)
            }
        }
    }
}

// ───────────────────────── 아바타·멤버 타일 ─────────────────────────

@Composable
fun Avatar(kind: AvatarKind, size: Dp = 64.dp, iconSize: Dp = size * 0.53f) {
    val c = Pause.colors
    val (bg, fg, icon) = when (kind) {
        AvatarKind.ADULT -> Triple(c.restSoft, c.restInk, PauseIcons.User)
        AvatarKind.KID -> Triple(c.kidSoft, c.kidInk, PauseIcons.Kid)
        AvatarKind.PET -> Triple(c.petSoft, c.petInk, PauseIcons.Paw)
        AvatarKind.CHARGE -> Triple(c.restSoft, c.restInk, PauseIcons.Bolt)
        AvatarKind.NAP -> Triple(c.restSoft, c.rest, PauseIcons.Moon)
    }
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(iconSize), tint = fg)
    }
}

/** 멤버 타일 (출발 준비 168dp, 온보딩 220dp). 선택 시 restSoft + 오른쪽 위 체크. */
@Composable
fun MemberTile(
    label: String,
    kind: AvatarKind?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    large: Boolean = false,
) {
    val c = Pause.colors
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .height(height)
            .clip(shape)
            .then(
                when {
                    kind == null -> Modifier.drawBehind {
                        drawRoundRect(
                            c.line,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                3.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                            ),
                        )
                    }
                    selected -> Modifier.background(c.restSoft)
                    else -> Modifier.background(c.paper)
                },
            )
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox, onClick = onClick)
            .semantics { stateDescription = if (selected) "선택됨" else "선택 안 됨" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (kind != null) {
                if (kind == AvatarKind.ADULT) {
                    Box(Modifier.size(if (large) 88.dp else 64.dp), contentAlignment = Alignment.Center) {
                        Icon(PauseIcons.User, null, Modifier.size(if (large) 44.dp else 34.dp), tint = c.restInk)
                    }
                } else {
                    Avatar(kind, if (large) 88.dp else 64.dp)
                }
            }
            Text(
                label,
                style = (if (large) Pause.type.item else Pause.type.label).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = if (kind == null) c.ink2 else if (selected) c.restInk else c.ink,
                maxLines = 1,
            )
        }
        if (selected && kind != null) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(16.dp).size(28.dp).clip(CircleShape).background(c.rest),
                contentAlignment = Alignment.Center,
            ) { Icon(PauseIcons.Check, null, Modifier.size(20.dp), tint = Color.White) }
        }
    }
}

// ───────────────────────── 큰 숫자 ─────────────────────────

/**
 * 가장 중요한 숫자 하나 (UI_SPEC §3 CountdownHero): 숫자 180~200sp + 단위 64~72sp.
 * 1분 단위로 갱신하고 마지막 1분만 초 단위로 보여준다(값은 호출하는 쪽이 정한다).
 */
@Composable
fun CountdownHero(
    number: String,
    unit: String,
    modifier: Modifier = Modifier,
    numberStyle: TextStyle = Pause.type.display,
    unitStyle: TextStyle = Pause.type.displayUnit,
    color: Color = Pause.colors.ink,
) {
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Bottom) {
        Text(number, style = numberStyle, color = color, maxLines = 1, softWrap = false)
        Spacer(Modifier.width(10.dp))
        Text(unit, Modifier.padding(bottom = (numberStyle.fontSize.value * 0.06f).dp), style = unitStyle, color = color, maxLines = 1)
    }
}

/** 화면 하단 버튼 행 같은 곳에서 쓰는 박스 (BoxScope 확장). */
@Composable
fun FillBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) = Box(modifier, content = content)

// ───────────────────────── 미리보기 ─────────────────────────

@Preview(widthDp = 900, heightDp = 700, showBackground = true, backgroundColor = 0xFFEEF2F4)
@Composable
private fun BasicsPreview() = PauseTheme(dark = false) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AppBar("부산 해운대까지 3시간 10분")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PauseButton("여기서 쉴게요", {})
            PauseButton("다음 곳으로", {}, kind = ButtonKind.GHOST)
            VoiceHint()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReasonChip("키즈존", PauseIcons.Kid)
            ReasonChip("급속충전 4대", PauseIcons.Bolt)
            ReasonChip("반려견 놀이터", PauseIcons.Paw)
        }
        PauseCard(Modifier.width(500.dp)) { FatigueGauge(61) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MemberTile("운전자", AvatarKind.ADULT, true, {}, Modifier.width(150.dp))
            MemberTile("지우 5살", AvatarKind.KID, true, {}, Modifier.width(150.dp))
            MemberTile("+ 추가", null, false, {}, Modifier.width(150.dp))
            PauseToggle(true)
        }
    }
}
