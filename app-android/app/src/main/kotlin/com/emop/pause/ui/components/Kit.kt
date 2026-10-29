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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.Pause
import com.emop.pause.uistate.MotionTone

/**
 * v5 부품 (docs/design/v5/STYLE_V5.md §6). 화면은 UiState만 보고 이 부품으로 그린다.
 * 움직이는 효과는 쓰지 않는다 — 주행 중 자동 애니메이션 금지(CONCEPT 5장)를 부품 단계에서 지킨다.
 * 그림자 없음: 바탕 명도 + 테두리로 구분한다.
 */

enum class ButtonKind { PRIMARY, SECONDARY, DANGER }

/** 주 버튼(파랑)·보조 버튼(흰 바탕 + 테두리). 높이 56×배율, 최소 터치 76dp. 비활성은 실행 차단 + disabled 색. */
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
        kind == ButtonKind.DANGER -> (if (pressed) c.border else c.surface) to c.danger
        else -> (if (pressed) c.primarySoft else c.surface) to c.text
    }
    val shape = RoundedCornerShape(d.buttonRadius)
    Row(
        modifier
            .heightIn(min = maxOf(if (compact) d.u(44f) else d.buttonHeight, d.touch))
            .clip(shape)
            .background(bg)
            .then(if (kind != ButtonKind.PRIMARY) Modifier.border(d.border, if (enabled) c.border else c.border.copy(alpha = 0.5f), shape) else Modifier)
            .clickable(src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = d.u(if (compact) 16f else 24f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, Modifier.size(d.u(22f)), tint = fg)
            Spacer(Modifier.width(d.unit))
        }
        Text(label, style = Pause.type.button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 앱 이름 줄 (시스템 시간·온도는 그리지 않는다). 로고 = 발표 모드 진입점(5번 탭). */
@Composable
fun AppBar(meta: String?, onLogoTap: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(modifier.fillMaxWidth().height(d.appBar).padding(horizontal = d.margin), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.heightIn(min = d.touch).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onLogoTap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(Modifier.size(d.u(26f)))
            Spacer(Modifier.width(d.unit))
            Text("같이가개", style = Pause.type.h2, color = c.text)
        }
        Spacer(Modifier.width(d.u(16f)))
        meta?.let { Text(it, Modifier.weight(1f), style = Pause.type.label, color = c.sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            ?: Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** 카드: surface + 테두리 1dp×배율 + 모서리 16×배율. */
@Composable
fun V5Card(
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
        verticalArrangement = Arrangement.spacedBy(spacing ?: d.unit),
        content = content,
    )
}

/** 상태 알약: 색 + 글자(색만으로 구분하지 않는다). */
@Composable
fun StatusChip(text: String, tone: MotionTone? = null, modifier: Modifier = Modifier, neutral: Boolean = false) {
    val c = Pause.colors
    val d = Pause.dimens
    val (bg, fg) = when {
        neutral -> c.bg to c.sub
        tone == MotionTone.CALM -> c.comfortSoft to c.comfort
        tone == MotionTone.CAUTION || tone == MotionTone.HIGH -> c.warningSoft to c.warningInk
        else -> c.primarySoft to c.primary
    }
    Box(modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = d.u(12f), vertical = d.u(4f))) {
        Text(text, style = Pause.type.label, color = fg, maxLines = 1)
    }
}

/** 멀미 단계 색 (편안 = 민트, 주의·높음 = 앰버). */
@Composable
fun toneColor(t: MotionTone): Color = when (t) {
    MotionTone.CALM -> Pause.colors.comfort
    MotionTone.CAUTION, MotionTone.HIGH -> Pause.colors.warning
}

/** 단계 글자 (색과 함께 쓴다). */
fun toneWord(t: MotionTone): String = when (t) {
    MotionTone.CALM -> "편안해요"
    MotionTone.CAUTION -> "멀미 주의"
    MotionTone.HIGH -> "멀미 높음"
}

/** 체크리스트 줄 (1번): 원 체크(파랑) + 글. 눌림 시 배경 강조. */
@Composable
fun ChecklistRow(label: String, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Row(
        modifier.fillMaxWidth().heightIn(min = maxOf(d.u(44f), d.touch)).clip(RoundedCornerShape(d.buttonRadius))
            .background(if (pressed) c.primarySoft else Color.Transparent)
            .clickable(src, indication = null, enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = d.unit),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(checked)
        Spacer(Modifier.width(d.u(12f)))
        Text(label, style = Pause.type.body, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CheckCircle(checked: Boolean, size: Dp = Pause.dimens.u(24f)) {
    val c = Pause.colors
    Box(
        Modifier.size(size).clip(CircleShape).background(if (checked) c.primary else c.surface)
            .then(if (checked) Modifier else Modifier.border(Pause.dimens.border, c.border, CircleShape)),
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(Icons.Rounded.Check, "확인함", Modifier.size(size * 0.7f), tint = c.onPrimary) }
}

/** 경고 띠 (3·9번): warningSoft + 경고 아이콘 + 글. */
@Composable
fun WarningBanner(modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, iconAsset: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(
        modifier.clip(RoundedCornerShape(d.cardRadius)).background(c.warningSoft).padding(d.pad),
        verticalAlignment = Alignment.Top,
    ) {
        icon?.let { StatusIcon(iconAsset ?: "", it, d.u(40f)); Spacer(Modifier.width(d.u(16f))) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(4f)), content = content)
    }
}

/** 토글 줄 (11번 설정). */
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
            sub?.let { Text(it, style = Pause.type.caption, color = c.sub) }
        }
        Switch(
            checked, { onClick() },
            colors = SwitchDefaults.colors(checkedTrackColor = c.primary, checkedThumbColor = c.onPrimary, uncheckedTrackColor = c.bg, uncheckedBorderColor = c.border),
        )
    }
}

/** 고르기 (분류·체급·답). 선택 = 파랑 테두리 + 연한 파랑. */
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
            .border(d.border, if (selected) c.primary else c.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = d.u(16f)),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Pause.type.label, color = if (selected) c.primary else c.text, maxLines = 1) }
}

/** 원 안 번호 (4·8번 목록). */
@Composable
fun NumberBadge(n: Int, active: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    Box(Modifier.size(d.u(28f)).clip(CircleShape).background(if (active) c.primary else c.bg), contentAlignment = Alignment.Center) {
        Text("$n", style = Pause.type.label, color = if (active) c.onPrimary else c.sub)
    }
}

/** Fluent UI Emoji Flat 벡터 (MIT, licenses/ASSETS.md). 화면당 1~2개, 주행 중에는 Flat만. */
@Composable
fun Fluent(@DrawableRes id: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(id), null, modifier.size(size))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, style: TextStyle = Pause.type.h2) {
    Text(text, modifier, style = style, color = Pause.colors.text)
}

// ───────────── 그림 자리: 사용자가 넣은 그림 → 일러스트 ─────────────

private val imageExt = listOf("png", "jpg", "jpeg", "webp")

/** assets에서 [names] 중 처음 있는 그림. 없으면 null (docs/design/v5/assets가 앱 assets로 묶인다). */
@Composable
fun rememberAssetImage(vararg names: String): ImageBitmap? {
    val context = LocalContext.current
    if (LocalInspectionMode.current) return null
    return remember(names.joinToString()) {
        names.asSequence().flatMap { n -> if ('.' in n) sequenceOf(n) else imageExt.asSequence().map { "$n.$it" } }
            .firstNotNullOfOrNull { f -> runCatching { context.assets.open(f).use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull() }
    }
}

/** 그림 자리: assets 그림이 있으면 그것, 없으면 [fallback] 일러스트. */
@Composable
fun ArtSlot(vararg names: String, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(Pause.dimens.cardRadius), fallback: @Composable BoxScope.() -> Unit) {
    val img = rememberAssetImage(*names)
    Box(modifier.clip(shape)) {
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else fallback()
    }
}

/**
 * 반려견 사진: 프로필 사진(없음) → debug `demo_pet.jpg` → 디자이너 그림(`pet_kongi`, 원형은 `pet_kongi_avatar`) → 직접 그린 일러스트.
 * 디자이너 그림 출처: licenses/ASSETS.md (docs/design/v5/assets/README.md).
 */
@Composable
fun PetPhoto(modifier: Modifier = Modifier, shape: Shape = CircleShape, vararg extra: String) {
    val names = if (shape == CircleShape) arrayOf(*extra, "demo_pet", "pet_kongi_avatar", "pet_kongi") else arrayOf(*extra, "demo_pet", "pet_kongi")
    ArtSlot(*names, modifier = modifier, shape = shape) { DogIllustration(Modifier.fillMaxSize()) }
}

/** 컬러 상태 아이콘: 디자이너 아이콘(assets `ic_*`) → 없으면 Fluent UI Emoji Flat 벡터. 화면당 1~2개. */
@Composable
fun StatusIcon(asset: String, @DrawableRes fallback: Int, size: Dp, modifier: Modifier = Modifier) {
    val img = rememberAssetImage(asset)
    if (img != null) Image(img, null, modifier.size(size), contentScale = ContentScale.Fit) else Fluent(fallback, size, modifier)
}

// ───────────── 그래프 (정지된 그림) ─────────────

/** 3번: 지금 → 예상 최고, 20분. 축 0·50·100. */
@Composable
fun ForecastChart(now: Int, peak: Int, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val line = c.warning
    val grid = c.border
    val nowColor = c.comfort
    Canvas(modifier) {
        fun y(v: Int) = size.height * (1f - v.coerceIn(0, 100) / 100f)
        listOf(0, 50, 100).forEach { g -> drawLine(grid, Offset(0f, y(g)), Offset(size.width, y(g)), strokeWidth = 2f, pathEffect = if (g == 0) null else PathEffect.dashPathEffect(floatArrayOf(10f, 10f))) }
        // 지금 → 최고까지 부드럽게 오르고(구간 끝 60%), 이후 조금 내려온다. 예측 모양을 보여 주는 그림이지 측정값이 아니다.
        val pts = (0..20).map { i ->
            val t = i / 20f
            val v = if (t < 0.6f) now + (peak - now) * (1 - (1 - t / 0.6f) * (1 - t / 0.6f)) else peak - (peak - now) * 0.25f * ((t - 0.6f) / 0.4f)
            Offset(size.width * t, y(v.toInt()))
        }
        val p = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(p, line, style = Stroke(width = 6f, cap = StrokeCap.Round))
        drawCircle(nowColor, 12f, pts.first())
        drawCircle(line, 12f, pts[12])
        drawCircle(c.surface, 5f, pts[12])
    }
}

/** 10번: 이동 중 지수 흐름(5분 간격), 최고 지점 표시. 축 0·50·100. */
@Composable
fun TrendChart(series: List<Int>, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val line = c.primary
    val grid = c.border
    val peakColor = c.warning
    Canvas(modifier) {
        fun y(v: Int) = size.height * (1f - v.coerceIn(0, 100) / 100f)
        listOf(0, 50, 100).forEach { g -> drawLine(grid, Offset(0f, y(g)), Offset(size.width, y(g)), strokeWidth = 2f) }
        if (series.size < 2) return@Canvas
        val step = size.width / (series.size - 1)
        val p = Path().apply { series.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * step, y(v)) } }
        drawPath(p, line, style = Stroke(width = 6f, cap = StrokeCap.Round))
        val pk = series.indices.maxBy { series[it] }
        drawCircle(peakColor, 12f, Offset(pk * step, y(series[pk])))
    }
}

/** 축 라벨 세로 줄 (0·50·100). */
@Composable
fun AxisLabels(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
        listOf("100", "50", "0").forEach { Text(it, style = Pause.type.caption, color = Pause.colors.sub) }
    }
}
