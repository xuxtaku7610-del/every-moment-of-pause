package com.emop.pause.ui.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.Pause
import com.emop.pause.uistate.MotionTone

/**
 * v6 부품 (docs/design/v6/STYLE_V6.md §6). 화면은 UiState만 보고 이 부품으로 그린다.
 * 움직이는 효과는 쓰지 않는다 — 주행 중 자동 애니메이션 금지(CONCEPT 5장)를 부품 단계에서 지킨다.
 * 그림자 없음. 영역은 패널(흰 면 + 얇은 테두리) 하나에 구분선으로 나누고, 카드를 겹쳐 쌓지 않는다.
 */

enum class ButtonKind { PRIMARY, SECONDARY, DANGER, QUIET }

/** 주 버튼(파랑)·보조(흰 면 + 테두리)·조용한(글자만). 높이 56×배율(최소 76dp). 비활성은 실행 차단 + disabled 색. */
@Composable
fun PauseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    val c = Pause.colors
    val d = Pause.dimens
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val (bg, fg) = when {
        !enabled && kind == ButtonKind.PRIMARY -> c.disabled to c.surface
        !enabled -> c.surface to c.disabled
        kind == ButtonKind.PRIMARY -> (if (pressed) c.primaryPressed else c.primary) to c.onPrimary
        kind == ButtonKind.DANGER -> (if (pressed) c.surfaceAlt else c.surface) to c.danger
        kind == ButtonKind.QUIET -> (if (pressed) c.surfaceAlt else Color.Transparent) to c.primary
        else -> (if (pressed) c.surfaceAlt else c.surface) to c.text
    }
    val shape = RoundedCornerShape(d.buttonRadius)
    Row(
        modifier
            .heightIn(min = maxOf(if (compact) d.u(44f) else d.buttonHeight, d.touch))
            .clip(shape)
            .background(bg)
            .then(if (kind == ButtonKind.SECONDARY || kind == ButtonKind.DANGER) Modifier.border(d.border, c.border, shape) else Modifier)
            .clickable(src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = d.u(if (compact) 16f else 24f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, Modifier.size(d.u(20f)), tint = fg)
            Spacer(Modifier.width(d.unit))
        }
        Text(label, style = Pause.type.button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 앱 바: 로고(발표 모드 진입점, 5번 탭) · 화면 이름 · 오른쪽 정보(목적지·남은 시간). 시스템 시간·온도는 그리지 않는다. */
@Composable
fun AppBar(meta: String?, onLogoTap: () -> Unit, modifier: Modifier = Modifier, title: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(modifier.fillMaxWidth().height(d.appBar).padding(horizontal = d.margin), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.heightIn(min = d.touch).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onLogoTap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(Modifier.size(d.u(24f)))
            Spacer(Modifier.width(d.unit))
            Text(title ?: "같이가개", style = Pause.type.section, color = c.text, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        meta?.let { Text(it, style = Pause.type.bodyStrong, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        trailing()
    }
}

/** 패널: 흰 면 + 얇은 테두리 + 모서리 14. 화면마다 큰 영역 1~3개만. [tint]는 상태 면(앰버·초록·파랑 연한 색, 테두리 없음). */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    borderColor: Color? = null,
    pad: Dp? = null,
    spacing: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pause.colors
    val d = Pause.dimens
    val shape = RoundedCornerShape(d.cardRadius)
    Column(
        modifier.clip(shape).background(tint ?: c.surface).border(d.border, borderColor ?: if (tint == null) c.border else Color.Transparent, shape).padding(pad ?: d.pad),
        verticalArrangement = Arrangement.spacedBy(spacing ?: d.gapS),
        content = content,
    )
}

/** v5 이름 (비교 화면이 쓴다) → [Panel]. */
@Composable
fun V5Card(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    borderColor: Color? = null,
    pad: Dp? = null,
    spacing: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) = Panel(modifier, tint, borderColor, pad, spacing, content)

/** 가로 구분선 (패널 안 영역 나누기). */
@Composable
fun Divider(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(Pause.dimens.border).background(Pause.colors.border))

/** 세로 구분선 (KPI 사이). */
@Composable
fun VDivider(modifier: Modifier = Modifier) = Box(modifier.width(Pause.dimens.border).fillMaxHeight().background(Pause.colors.border))

/** 섹션 머리: 제목 18 + (선택) 오른쪽 보조 글. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null, trailingContent: @Composable RowScope.() -> Unit = {}) {
    val c = Pause.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Pause.type.section, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f).width(Pause.dimens.gap))
        trailing?.let { Text(it, style = Pause.type.sub, color = c.sub, maxLines = 1) }
        trailingContent()
    }
}

/**
 * 수치 묶음: 레이블(14) → 값(36 또는 26) + 단위 → 상태 글(선택). 값이 먼저 읽히되 레이블·단위와 함께 붙는다.
 * [small]이면 보조 수치 크기.
 */
@Composable
fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    note: String? = null,
    color: Color = Pause.colors.text,
    noteColor: Color = Pause.colors.sub,
    small: Boolean = false,
    align: Alignment.Horizontal = Alignment.Start,
) {
    val c = Pause.colors
    val t = Pause.type
    Column(modifier, horizontalAlignment = align) {
        Text(label, style = t.metricLabel, color = c.sub, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = if (small) t.metricSmall else t.metric, color = color, maxLines = 1)
            unit?.let {
                Spacer(Modifier.width(Pause.dimens.u(4f)))
                Text(it, style = t.unit, color = c.sub, maxLines = 1, modifier = Modifier.padding(bottom = Pause.dimens.u(if (small) 2f else 5f)))
            }
        }
        note?.let { Text(it, style = t.bodyStrong, color = noteColor, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** 이름 · 값 한 줄 (패널 안 목록). */
@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Pause.colors.text) {
    val c = Pause.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(key, style = Pause.type.sub, color = c.sub, maxLines = 1)
        Spacer(Modifier.width(Pause.dimens.gap))
        Spacer(Modifier.weight(1f))
        Text(value, style = Pause.type.bodyStrong, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 상태 알약: 색 점 + 글(색만으로 구분하지 않는다). 모서리 8. */
@Composable
fun StatusChip(text: String, tone: MotionTone? = null, modifier: Modifier = Modifier, neutral: Boolean = false) {
    val c = Pause.colors
    val d = Pause.dimens
    val (bg, fg) = when {
        neutral -> c.surfaceAlt to c.sub
        tone == MotionTone.CALM -> c.comfortSoft to c.comfort
        tone == MotionTone.CAUTION || tone == MotionTone.HIGH -> c.warningSoft to c.warningInk
        else -> c.primarySoft to c.primary
    }
    Row(
        modifier.clip(RoundedCornerShape(d.chipRadius)).background(bg).padding(horizontal = d.u(10f), vertical = d.u(4f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!neutral) {
            Box(Modifier.size(d.u(8f)).clip(CircleShape).background(fg))
            Spacer(Modifier.width(d.u(6f)))
        }
        Text(text, style = Pause.type.metricLabel, color = fg, maxLines = 1)
    }
}

/** 멀미 단계 색 (편안 = 초록, 주의·높음 = 앰버). 글자는 [toneInk]. */
@Composable
fun toneColor(t: MotionTone): Color = when (t) {
    MotionTone.CALM -> Pause.colors.comfort
    MotionTone.CAUTION, MotionTone.HIGH -> Pause.colors.warning
}

@Composable
fun toneInk(t: MotionTone): Color = when (t) {
    MotionTone.CALM -> Pause.colors.comfort
    MotionTone.CAUTION, MotionTone.HIGH -> Pause.colors.warningInk
}

/** 단계 글자 (색과 함께 쓴다). */
fun toneWord(t: MotionTone): String = when (t) {
    MotionTone.CALM -> "편안해요"
    MotionTone.CAUTION -> "멀미 주의"
    MotionTone.HIGH -> "멀미 높음"
}

/** 0~100 막대: 지금 값 + 주의 기준(40)·높음 기준(70) 눈금. 정지된 그림. */
@Composable
fun IndexGauge(value: Int, tone: MotionTone, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val fill = toneColor(tone)
    Canvas(modifier) {
        val h = size.height
        val r = h / 2
        drawRoundRect(c.surfaceAlt, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
        val w = size.width * value.coerceIn(0, 100) / 100f
        if (w > 0) drawRoundRect(fill, size = Size(maxOf(w, h), h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
        listOf(40, 70).forEach { g ->
            val x = size.width * g / 100f
            drawLine(c.surface, Offset(x, 0f), Offset(x, h), strokeWidth = 3f)
        }
    }
}

/** [IndexGauge] 아래 눈금 글자: 편안 · 주의 40 · 높음 70. */
@Composable
fun GaugeScale(modifier: Modifier = Modifier) {
    val c = Pause.colors
    Row(modifier.fillMaxWidth()) {
        Text("편안", style = Pause.type.caption, color = c.sub, modifier = Modifier.weight(0.4f))
        Text("주의 40", style = Pause.type.caption, color = c.sub, modifier = Modifier.weight(0.3f))
        Text("높음 70", style = Pause.type.caption, color = c.sub, modifier = Modifier.weight(0.3f))
    }
}

/** 체크리스트 줄: 원 체크(파랑) + 글. 눌림 시 배경 강조. */
@Composable
fun ChecklistRow(label: String, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Row(
        modifier.fillMaxWidth().heightIn(min = maxOf(d.u(44f), d.touch)).clip(RoundedCornerShape(d.buttonRadius))
            .background(if (pressed) c.surfaceAlt else Color.Transparent)
            .clickable(src, indication = null, enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = d.unit),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked)
        Spacer(Modifier.width(d.gapS))
        Text(label, style = Pause.type.body, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CheckCircle(checked: Boolean, size: Dp = Pause.dimens.u(24f)) {
    val c = Pause.colors
    Box(
        Modifier.size(size).clip(CircleShape).background(if (checked) c.primary else c.surface)
            .then(if (checked) Modifier else Modifier.border(Pause.dimens.u(1.5f), c.disabled, CircleShape)),
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(Icons.Rounded.Check, "확인함", Modifier.size(size * 0.7f), tint = c.onPrimary) }
}

/**
 * 경고 띠 (v6): 앰버 연한 면 + 왼쪽 앰버 굵은 선 + 아이콘. 화면을 덮지 않게 내용 높이만 차지한다.
 * 글자 크기는 섹션(18)·본문(16) — 띠가 화면 전체를 압도하지 않게.
 */
@Composable
fun WarningBanner(modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, iconAsset: String? = null, content: @Composable ColumnScope.() -> Unit) {
    AlertStrip(modifier, icon, iconAsset, content = content)
}

@Composable
fun AlertStrip(modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, iconAsset: String? = null, accent: Color? = null, soft: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(
        modifier.clip(RoundedCornerShape(d.cardRadius)).background(soft ?: c.warningSoft).height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(d.u(6f)).fillMaxHeight().background(accent ?: c.warning))
        Row(Modifier.weight(1f).padding(horizontal = d.pad, vertical = d.gapS), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { StatusIcon(iconAsset ?: "", it, d.u(32f)); Spacer(Modifier.width(d.gapS)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(2f)), content = content)
        }
    }
}

/** 토글 줄 (설정). */
@Composable
fun ToggleRow(label: String, checked: Boolean, onClick: () -> Unit, sub: String? = null) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(
        Modifier.fillMaxWidth().heightIn(min = d.touch).clickable(role = Role.Switch, onClick = onClick).padding(vertical = d.u(4f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Pause.type.body, color = c.text)
            sub?.let { Text(it, style = Pause.type.sub, color = c.sub) }
        }
        Switch(
            checked, { onClick() },
            colors = SwitchDefaults.colors(checkedTrackColor = c.primary, checkedThumbColor = c.onPrimary, uncheckedTrackColor = c.surfaceAlt, uncheckedBorderColor = c.border),
        )
    }
}

/** 고르기 (분류·체급·답). 선택 = 파랑 테두리 + 연한 파랑. 모서리 12. */
@Composable
fun Choice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    val shape = RoundedCornerShape(d.buttonRadius)
    Box(
        modifier
            .heightIn(min = d.touch)
            .clip(shape)
            .background(if (selected) c.primarySoft else c.surface)
            .border(if (selected) d.u(1.5f) else d.border, if (selected) c.primary else c.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = d.u(16f)),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Pause.type.body, color = if (selected) c.primary else c.text, maxLines = 1) }
}

/** 원 안 번호 (목록 순위·단계). */
@Composable
fun NumberBadge(n: Int, active: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    Box(Modifier.size(d.u(28f)).clip(CircleShape).background(if (active) c.primary else c.surfaceAlt), contentAlignment = Alignment.Center) {
        Text("$n", style = Pause.type.metricLabel, color = if (active) c.onPrimary else c.sub)
    }
}

/** Fluent UI Emoji Flat 벡터 (MIT, licenses/ASSETS.md). 화면당 1~2개. */
@Composable
fun Fluent(@DrawableRes id: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(id), null, modifier.size(size))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, style: TextStyle = Pause.type.section) {
    Text(text, modifier, style = style, color = Pause.colors.text)
}

// ───────────── 그림 자리: 사용자가 넣은 그림 → 일러스트 ─────────────

private val imageExt = listOf("png", "jpg", "jpeg", "webp")

/** assets에서 [names] 중 처음 있는 그림. 없으면 null. */
@Composable
fun rememberAssetImage(vararg names: String): ImageBitmap? {
    val context = LocalContext.current
    if (LocalInspectionMode.current) return null
    return remember(names.joinToString()) {
        names.asSequence().flatMap { n -> if ('.' in n) sequenceOf(n) else imageExt.asSequence().map { "$n.$it" } }
            .firstNotNullOfOrNull { f -> runCatching { context.assets.open(f).use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull() }
    }
}

/** 그림 자리: assets 그림이 있으면 그것(Crop), 없으면 [fallback] 일러스트. 비율은 호출하는 쪽이 aspectRatio로 고정한다. */
@Composable
fun ArtSlot(vararg names: String, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(Pause.dimens.cardRadius), fallback: @Composable BoxScope.() -> Unit) {
    val img = rememberAssetImage(*names)
    Box(modifier.clip(shape)) {
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else fallback()
    }
}

/** 반려견 사진 (기본 원형): debug `demo_pet.jpg` → 디자이너 그림(`pet_kongi_avatar`/`pet_kongi`) → 일러스트. 출처 licenses/ASSETS.md. */
@Composable
fun PetPhoto(modifier: Modifier = Modifier, shape: Shape = CircleShape, vararg extra: String) {
    val names = if (shape == CircleShape) arrayOf(*extra, "demo_pet", "pet_kongi_avatar", "pet_kongi") else arrayOf(*extra, "demo_pet", "pet_kongi")
    ArtSlot(*names, modifier = modifier, shape = shape) { DogIllustration(Modifier.fillMaxSize()) }
}

/** 컬러 상태 아이콘: 디자이너 아이콘(assets `ic_*`) → 없으면 Fluent 벡터. 화면당 1~2개. */
@Composable
fun StatusIcon(asset: String, @DrawableRes fallback: Int, size: Dp, modifier: Modifier = Modifier) {
    val img = rememberAssetImage(asset)
    if (img != null) Image(img, null, modifier.size(size), contentScale = ContentScale.Fit) else Fluent(fallback, size, modifier)
}

// ───────────── 그래프 (정지된 그림, 실제 값만) ─────────────

/**
 * 멀미 예보 그래프 (v6): 가로 = 앞으로 20분, 세로 = 지수 0~100.
 * 선 = 엔진 값 이어 그리기 — 지금([now])에서 굽은 길 시작([startMin])까지 그대로, 굽은 길 동안([durationMin]) 예상 최고([peak])까지 오르고 그 뒤 유지.
 * 지어낸 곡선 모양을 쓰지 않는다(엔진은 지금 값과 구간 끝 예상 값만 준다). 주의 이상(40~100) 띠와 굽은 길 시간대를 연하게 칠한다.
 */
@Composable
fun ForecastChart(now: Int, peak: Int, startMin: Int?, durationMin: Int?, modifier: Modifier = Modifier, windowMin: Int = 20) {
    val c = Pause.colors
    val t = Pause.type
    val measurer = rememberTextMeasurer()
    val axisStyle = t.caption.copy(color = c.sub)
    Canvas(modifier) {
        val left = 44.dp.toPx()
        val bottom = 30.dp.toPx()
        val plotW = size.width - left
        val plotH = size.height - bottom
        fun x(m: Float) = left + plotW * (m / windowMin).coerceIn(0f, 1f)
        fun y(v: Int) = plotH * (1f - v.coerceIn(0, 100) / 100f)
        // 주의 이상 띠 (40~100)
        drawRect(c.warningSoft, Offset(left, 0f), Size(plotW, y(40)))
        // 굽은 길 시간대
        val s0 = (startMin ?: 0).toFloat()
        val s1 = s0 + (durationMin ?: 0)
        if (startMin != null && durationMin != null && s0 < windowMin) {
            drawRect(c.warning.copy(alpha = 0.16f), Offset(x(s0), 0f), Size(x(s1) - x(s0), plotH))
            drawLine(c.warning, Offset(x(s0), 0f), Offset(x(s0), plotH), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
        // 눈금
        listOf(0, 40, 70, 100).forEach { g ->
            drawLine(if (g == 0) c.sub.copy(alpha = 0.5f) else c.border, Offset(left, y(g)), Offset(size.width, y(g)), strokeWidth = if (g == 0) 2f else 1.5f)
            if (g > 0) label(measurer, "$g", axisStyle, Offset(left - 8.dp.toPx(), y(g)), alignRight = true)
        }
        listOf(0, 5, 10, 15, 20).forEach { m ->
            label(measurer, if (m == 0) "지금" else "${m}분", axisStyle, Offset(x(m.toFloat()), plotH + 16.dp.toPx()), center = true)
        }
        // 선
        val pts = buildList {
            add(Offset(x(0f), y(now)))
            if (startMin != null && durationMin != null) {
                add(Offset(x(s0), y(now)))
                add(Offset(x(s1), y(peak)))
                add(Offset(x(windowMin.toFloat()), y(peak)))
            } else add(Offset(x(windowMin.toFloat()), y(peak)))
        }
        val p = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(p, c.navy, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // 지금 점 · 예상 최고 점
        drawCircle(c.surface, 9.dp.toPx(), pts.first())
        drawCircle(c.navy, 6.dp.toPx(), pts.first())
        val peakAt = if (startMin != null && durationMin != null && s1 <= windowMin) Offset(x(s1), y(peak)) else pts.last()
        drawCircle(c.surface, 9.dp.toPx(), peakAt)
        drawCircle(c.warning, 6.dp.toPx(), peakAt)
    }
}

/** [minute]분 뒤 [ForecastChart] 선의 값 (그래프 옆 "20분 뒤" 수치와 같은 계산). */
fun forecastAt(minute: Int, now: Int, peak: Int, startMin: Int?, durationMin: Int?): Int {
    if (startMin == null || durationMin == null) return peak
    return when {
        minute <= startMin -> now
        minute >= startMin + durationMin -> peak
        else -> now + (peak - now) * (minute - startMin) / durationMin
    }
}

/**
 * 이동 중 지수 흐름 (v6): 5분 간격 실제 기록. 가로 = 출발 뒤 시간(30분 눈금), 세로 = 0~100, 주의 이상 띠, 최고 지점 값 표시.
 */
@Composable
fun TrendChart(series: List<Int>, modifier: Modifier = Modifier, sampleMin: Int = 5) {
    val c = Pause.colors
    val t = Pause.type
    val measurer = rememberTextMeasurer()
    val axisStyle = t.caption.copy(color = c.sub)
    val peakStyle = t.metricLabel.copy(color = c.warningInk)
    Canvas(modifier) {
        val left = 44.dp.toPx()
        val bottom = 30.dp.toPx()
        val top = 24.dp.toPx()
        val plotW = size.width - left
        val plotH = size.height - bottom - top
        val total = maxOf(1, (series.size - 1) * sampleMin)
        fun x(m: Float) = left + plotW * (m / total)
        fun y(v: Int) = top + plotH * (1f - v.coerceIn(0, 100) / 100f)
        drawRect(c.warningSoft, Offset(left, top), Size(plotW, y(40) - top))
        listOf(0, 40, 70, 100).forEach { g ->
            drawLine(if (g == 0) c.sub.copy(alpha = 0.5f) else c.border, Offset(left, y(g)), Offset(size.width, y(g)), strokeWidth = if (g == 0) 2f else 1.5f)
            if (g > 0) label(measurer, "$g", axisStyle, Offset(left - 8.dp.toPx(), y(g)), alignRight = true)
        }
        val tick = if (total > 180) 60 else 30
        var m = 0
        while (m <= total) {
            val txt = if (m == 0) "출발" else if (m % 60 == 0) "${m / 60}시간" else "${m / 60}:${"%02d".format(m % 60)}"
            label(measurer, txt, axisStyle, Offset(x(m.toFloat()), size.height - bottom + 16.dp.toPx()), center = true)
            m += tick
        }
        if (series.size < 2) return@Canvas
        val pts = series.mapIndexed { i, v -> Offset(x((i * sampleMin).toFloat()), y(v)) }
        val p = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(p, c.primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val pk = series.indices.maxBy { series[it] }
        if (series[pk] > 0) {
            drawCircle(c.surface, 9.dp.toPx(), pts[pk])
            drawCircle(c.warning, 6.dp.toPx(), pts[pk])
            label(measurer, "최고 ${series[pk]}", peakStyle, Offset(pts[pk].x, pts[pk].y - 16.dp.toPx()), center = true)
        }
    }
}

private fun DrawScope.label(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    style: TextStyle,
    at: Offset,
    alignRight: Boolean = false,
    center: Boolean = false,
) {
    val r = measurer.measure(text, style)
    val x = when {
        alignRight -> at.x - r.size.width
        center -> (at.x - r.size.width / 2f).coerceIn(0f, size.width - r.size.width)
        else -> at.x
    }
    drawText(r, topLeft = Offset(x, at.y - r.size.height / 2f))
}

/** 축 라벨 세로 줄 (v5 호환, 새 그래프는 축을 직접 그린다). */
@Composable
fun AxisLabels(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
        listOf("100", "50", "0").forEach { Text(it, style = Pause.type.caption, color = Pause.colors.sub) }
    }
}
