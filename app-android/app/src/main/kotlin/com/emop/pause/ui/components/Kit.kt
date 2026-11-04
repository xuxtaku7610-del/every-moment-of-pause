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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.Pause
import kotlin.math.abs
import com.emop.pause.uistate.MotionTone
import com.emop.pause.uistate.TrendMarkKind
import com.emop.pause.uistate.TrendMarkUi
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * v6 부품 (docs/design/v6/STYLE_V6.md §6). 화면은 UiState만 보고 이 부품으로 그린다.
 * 움직이는 효과는 쓰지 않는다 — 주행 중 자동 애니메이션 금지(CONCEPT 5장)를 부품 단계에서 지킨다.
 * 그림자 없음. 영역은 패널(흰 면 + 얇은 테두리) 하나에 구분선으로 나누고, 카드를 겹쳐 쌓지 않는다.
 */

/** [WARNING] = 긴급 도움 (연한 앰버 면 + 앰버 테두리 + warningInk 글자, 파란 주 버튼과 구분 — 11/03). */
enum class ButtonKind { PRIMARY, SECONDARY, DANGER, QUIET, WARNING }

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
        kind == ButtonKind.WARNING -> (if (pressed) c.surfaceAlt else c.warningSoft) to c.warningInk
        else -> (if (pressed) c.surfaceAlt else c.surface) to c.text
    }
    val shape = RoundedCornerShape(d.buttonRadius)
    Row(
        modifier
            .heightIn(min = maxOf(if (compact) d.u(44f) else d.buttonHeight, d.touch))
            .clip(shape)
            .background(bg)
            .then(
                when {
                    kind == ButtonKind.WARNING && enabled -> Modifier.border(d.border * 2, c.warning, shape)
                    kind == ButtonKind.SECONDARY || kind == ButtonKind.DANGER || kind == ButtonKind.WARNING -> Modifier.border(d.border, c.border, shape)
                    else -> Modifier
                },
            )
            .clickable(src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = d.u(if (compact) 16f else 24f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, Modifier.size(d.u(20f)), tint = fg)
            Spacer(Modifier.width(d.unit))
        }
        // 버튼 글자는 줄임표로 자르지 않는다 — 좁으면 두 줄 (2026-10-31 사용자 규칙)
        Text(label, style = Pause.type.button, color = fg, maxLines = 2, textAlign = TextAlign.Center)
    }
}

/** 앱 바: 로고(발표 모드 진입점, 5번 탭) · 화면 이름 · 오른쪽 정보(목적지·남은 시간). 시스템 시간·온도는 그리지 않는다. */
@Composable
fun AppBar(meta: String?, onLogoTap: () -> Unit, modifier: Modifier = Modifier, title: String? = null, trailing: @Composable RowScope.() -> Unit = {}, demo: Boolean = false) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(modifier.fillMaxWidth().height(d.appBar).padding(horizontal = d.margin), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.heightIn(min = d.touch).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onLogoTap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(Modifier.size(d.u(24f)))
            Spacer(Modifier.width(d.unit))
            Text(title ?: "같이가개", style = Pause.type.body, color = c.text, maxLines = 1)
        }
        if (demo) {
            Spacer(Modifier.width(d.gapS))
            StatusChip("시연 데이터", neutral = true)
        }
        // 오른쪽 정보는 남는 자리만 쓰고(2줄까지) 버튼을 밀어내지 않는다 — 좁은 창에서 "기록 확인"이 눌리지 않게
        Spacer(Modifier.width(d.gapS))
        Text(
            meta.orEmpty(), style = Pause.type.body.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph.copy(wordBreak = androidx.compose.ui.text.style.LineBreak.WordBreak.Phrase)),
            color = c.sub, maxLines = 2, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * 면 (v10 §2, 카드도 이것): 배경 명도로만 나눈다 — **테두리·그림자 없음**, 모서리 14. 기본 = surface(바탕 bg 위), [tint] = 상태 면
 * (primarySoft·warningSoft) 또는 면 안의 한 칸(surfaceAlt). 화면당 카드 최대 2장, 카드 안에 카드를 넣지 않는다.
 */
@Composable
fun Block(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    pad: Dp? = null,
    spacing: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pause.colors
    val d = Pause.dimens
    Column(
        modifier.clip(RoundedCornerShape(d.cardRadius)).background(tint ?: c.surface).padding(pad ?: d.pad),
        verticalArrangement = Arrangement.spacedBy(spacing ?: d.gapS),
        content = content,
    )
}

// ───────────── 글자 4단계 (v10 §1): 제목 · 수치 · 설명 · 보조 ─────────────

@Composable
fun Title(text: String, modifier: Modifier = Modifier, color: Color = Pause.colors.text, maxLines: Int = 2) =
    Text(text, modifier, style = Pause.type.title, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = Pause.colors.text, maxLines: Int = Int.MAX_VALUE) =
    Text(text, modifier, style = Pause.type.body, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = Pause.colors.sub, maxLines: Int = Int.MAX_VALUE) =
    Text(text, modifier, style = Pause.type.caption, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

/** 가로 구분선 (패널 안 영역 나누기). */
@Composable
fun Divider(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(Pause.dimens.border).background(Pause.colors.border))

/** 세로 구분선 (KPI 사이). */
@Composable
fun VDivider(modifier: Modifier = Modifier) = Box(modifier.width(Pause.dimens.border).fillMaxHeight().background(Pause.colors.border))

/** 영역 머리: 제목(24) + (선택) 오른쪽 보조 글 (v10 — 전 SectionHeader). */
@Composable
fun Heading(title: String, modifier: Modifier = Modifier, trailing: String? = null, trailingContent: @Composable RowScope.() -> Unit = {}) {
    val c = Pause.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Pause.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f).width(Pause.dimens.gap))
        trailing?.let { Text(it, style = Pause.type.caption, color = c.sub, maxLines = 1) }
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
        Text(label, style = t.caption, color = c.sub, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            // v10: 수치는 한 크기(36). [small]은 예전 호출과 맞추려고 남긴 이름 — 단위 정렬만 다르다
            Text(value, style = t.metric, color = color, maxLines = 1)
            unit?.let {
                Spacer(Modifier.width(Pause.dimens.u(4f)))
                Text(it, style = t.unit, color = c.sub, maxLines = 1, modifier = Modifier.padding(bottom = Pause.dimens.u(if (small) 2f else 5f)))
            }
        }
        note?.let { Text(it, style = t.body, color = noteColor, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

/** 이 글자 수를 넘는 값은 이름 아래 줄로 내린다. */
private const val KEY_VALUE_INLINE = 18

/** 이름 · 값 한 줄 (패널 안 목록). */
@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Pause.colors.text) {
    val c = Pause.colors
    // 값은 줄이지 않는다: 짧으면 오른쪽 정렬 한 줄, 길면(요약 문장·링크) 이름 아래 전체 폭으로 줄바꿈 (11/01 줄임표 없앰)
    if (value.length > KEY_VALUE_INLINE) {
        Column(modifier.fillMaxWidth()) {
            Text(key, style = Pause.type.caption, color = c.sub, maxLines = 1)
            Text(value, style = Pause.type.body.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph.copy(wordBreak = androidx.compose.ui.text.style.LineBreak.WordBreak.Phrase)), color = valueColor)
        }
        return
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(key, style = Pause.type.caption, color = c.sub, maxLines = 1)
        Spacer(Modifier.width(Pause.dimens.gap))
        Text(value, style = Pause.type.body, color = valueColor, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
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
        Text(text, style = Pause.type.caption, color = fg, maxLines = 1)
    }
}

@Composable
fun toneInk(t: MotionTone): Color = when (t) {
    MotionTone.CALM -> Pause.colors.comfort
    MotionTone.CAUTION, MotionTone.HIGH -> Pause.colors.warningInk
}

/** 단계 글자 (색과 함께 쓴다). */
fun toneWord(t: MotionTone): String = when (t) {
    MotionTone.CALM -> "주행 자극 낮음"
    MotionTone.CAUTION -> "주행 자극 보통"
    MotionTone.HIGH -> "주행 자극 높음"
}

/** 체크리스트 줄: 원 체크(파랑) + 글. 눌림 시 배경 강조. */
@Composable
fun ChecklistRow(label: String, checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, note: String? = null) {
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
        // v10-3: 최근에 빠뜨린 항목 다시 강조 (앰버 보조 글, 색만으로 알리지 않는다)
        note?.let {
            Spacer(Modifier.width(d.gapS))
            Text(it, style = Pause.type.caption, color = c.warningInk, maxLines = 1)
        }
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

/** 경고 띠: 상태 연한 면 + 왼쪽 굵은 선 + 아이콘, 내용 높이만. 글자는 제목 이하(v10 §1) — 띠가 화면을 압도하지 않게. 화면당 1개. */
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
            sub?.let { Text(it, style = Pause.type.caption, color = c.sub) }
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
        Text("$n", style = Pause.type.caption, color = if (active) c.onPrimary else c.sub)
    }
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
fun PetPhoto(modifier: Modifier = Modifier, shape: Shape = CircleShape, vararg extra: String, petId: Long? = null) {
    // v10-3: 보호자가 폰으로 올린 사진(기기 저장) → 시연 데이터면 콩이 사진·그림 → 그 밖은 기본 실루엣 (지어낸 사진을 쓰지 않는다)
    val photo = (petId ?: LocalPrimaryPetId.current)?.let { LocalPetPhotos.current[it] }
    val c = Pause.colors
    when {
        photo != null -> Image(photo, null, modifier.clip(shape), contentScale = ContentScale.Crop)
        LocalDemoPhoto.current -> {
            val names = if (shape == CircleShape) arrayOf(*extra, "demo_pet", "pet_kongi_avatar", "pet_kongi") else arrayOf(*extra, "demo_pet", "pet_kongi")
            ArtSlot(*names, modifier = modifier, shape = shape) { DogIllustration(Modifier.fillMaxSize()) }
        }
        else -> Box(modifier.clip(shape).background(c.surfaceAlt), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Pets, "반려견 사진 없음", Modifier.fillMaxSize(0.45f), tint = c.sub)
        }
    }
}

/** 프로필 id → 사진 (MainActivity가 PhotoStore에서 넣는다). */
val LocalPetPhotos = staticCompositionLocalOf<Map<Long, ImageBitmap>> { emptyMap() }

/** id를 주지 않은 [PetPhoto]가 쓸 아이 (오늘 타는 첫 아이). */
val LocalPrimaryPetId = staticCompositionLocalOf<Long?> { null }

/** 시연 데이터(시뮬레이터·장면 고정)면 콩이 사진을, 아니면 실루엣을 (v10-3). 미리보기는 true. */
val LocalDemoPhoto = staticCompositionLocalOf { true }

/** 컬러 상태 아이콘: 디자이너 아이콘(assets `ic_*`) → 없으면 Fluent 벡터. 화면당 1~2개. */
@Composable
fun StatusIcon(asset: String, @DrawableRes fallback: Int, size: Dp, modifier: Modifier = Modifier) {
    val img = rememberAssetImage(asset)
    if (img != null) Image(img, null, modifier.size(size), contentScale = ContentScale.Fit) else Image(painterResource(fallback), null, modifier.size(size))
}

// ───────────── 그래프 (정지된 그림, 실제 값만) ─────────────

/**
 * 이동 중 지수 흐름 (v6 → v7): 5분 간격 실제 기록. 가로 = 출발 뒤 시간(30분 눈금), 세로 = 0~100, 주의 이상 띠, 최고 지점 값.
 * v7 표시: 휴식 구간(회색 면) · 주행 중 증상 기록(점선 + 이름) · 도착 시점 지수(네이비 점). 표시는 모두 [marks]로 받은 실제 기록만이고,
 * 기록이 끝난 뒤로는 선을 긋지 않는다. 점이 2개 미만이면 축과 [emptyText]만.
 */
@Composable
fun TrendChart(
    series: List<Int>,
    modifier: Modifier = Modifier,
    sampleMin: Int = 5,
    marks: List<TrendMarkUi> = emptyList(),
    arrival: Int? = null,
    emptyText: String = "기록이 아직 없어요",
) {
    val c = Pause.colors
    val t = Pause.type
    val measurer = rememberTextMeasurer()
    val axisStyle = t.caption.copy(color = c.sub)
    val peakStyle = t.caption.copy(color = c.warningInk)
    val markStyle = t.caption.copy(color = c.text)
    val arrivalStyle = t.caption.copy(color = c.navy)
    val emptyStyle = t.body.copy(color = c.sub)
    Canvas(modifier) {
        val left = 44.dp.toPx()
        val bottom = 30.dp.toPx()
        // 위 여백: 증상 이름 줄(있을 때만 넓게)
        val top = (if (marks.any { it.kind == TrendMarkKind.SYMPTOM }) 34 else 24).dp.toPx()
        val right = if (arrival != null) 12.dp.toPx() else 0f
        val plotW = size.width - left - right
        val plotH = size.height - bottom - top
        val total = maxOf(1, (series.size - 1) * sampleMin)
        fun x(m: Float) = left + plotW * (m / total).coerceIn(0f, 1f)
        fun y(v: Int) = top + plotH * (1f - v.coerceIn(0, 100) / 100f)
        // v10 §5: 같은 단계 띠(높음 warningSoft · 보통 surfaceAlt)와 40·70 눈금
        drawRect(c.warningSoft, Offset(left, y(100)), Size(plotW, y(70) - y(100)))
        drawRect(c.surfaceAlt, Offset(left, y(70)), Size(plotW, y(40) - y(70)))
        // 흔들림 많은 구간(주행 자극 보통 이상): 앰버 면 (리포트 타임라인과 같은 구간)
        if (series.size >= 2) marks.filter { it.kind == TrendMarkKind.HIGH && it.minute < total }.forEach { m ->
            val x0 = x(m.minute.toFloat())
            val x1 = maxOf(x((m.minute + m.lengthMin).toFloat()), x0 + 4.dp.toPx())
            drawRect(c.warning.copy(alpha = 0.18f), Offset(x0, top), Size(x1 - x0, plotH))
        }
        // 휴식 구간: 그 시간에도 지수는 기록되므로 선은 그대로 두고 면만 깐다
        if (series.size >= 2) marks.filter { it.kind == TrendMarkKind.STOP && it.minute < total }.forEach { m ->
            val x0 = x(m.minute.toFloat())
            val x1 = maxOf(x((m.minute + m.lengthMin).toFloat()), x0 + 4.dp.toPx())
            drawRect(c.sub.copy(alpha = 0.14f), Offset(x0, top), Size(x1 - x0, plotH))
            label(measurer, m.label, axisStyle, Offset((x0 + x1) / 2, top + 12.dp.toPx()), center = true)
        }
        listOf(0, 40, 70).forEach { g ->
            drawLine(if (g == 0) c.sub.copy(alpha = 0.5f) else c.border, Offset(left, y(g)), Offset(left + plotW, y(g)), strokeWidth = if (g == 0) 2f else 1.5f)
            if (g > 0) label(measurer, "$g", axisStyle, Offset(left - 8.dp.toPx(), y(g)), alignRight = true)
        }
        if (series.size < 2) {
            label(measurer, emptyText, emptyStyle, Offset(left + plotW / 2, top + plotH / 2), center = true)
            return@Canvas
        }
        // 눈금 글자가 겹치지 않게: 글자 하나에 약 64dp를 두고 30분·1시간·2시간 중 고른다
        val maxLabels = maxOf(2, (plotW / 64.dp.toPx()).toInt())
        val tick = listOf(30, 60, 120).firstOrNull { total / it + 1 <= maxLabels } ?: 120
        var m = 0
        while (m <= total) {
            val txt = if (m == 0) "출발" else if (m % 60 == 0) "${m / 60}시간" else "${m / 60}:${"%02d".format(m % 60)}"
            label(measurer, txt, axisStyle, Offset(x(m.toFloat()), size.height - bottom + 16.dp.toPx()), center = true)
            m += tick
        }
        // 주행 중 증상 기록: 점선 + 위쪽 이름 (가까운 이름은 겹치지 않게 오른쪽으로 민다)
        var lastRight = -1f
        marks.filter { it.kind == TrendMarkKind.SYMPTOM && it.minute <= total }.sortedBy { it.minute }.forEach { e ->
            val ex = x(e.minute.toFloat())
            drawLine(c.warningInk.copy(alpha = 0.7f), Offset(ex, top), Offset(ex, top + plotH), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawCircle(c.warningInk, 4.dp.toPx(), Offset(ex, top))
            val r = measurer.measure(e.label, markStyle)
            val lx = maxOf(ex - r.size.width / 2f, lastRight + 6.dp.toPx()).coerceIn(0f, size.width - r.size.width)
            drawText(r, topLeft = Offset(lx, top - 6.dp.toPx() - r.size.height))
            lastRight = lx + r.size.width
        }
        val pts = series.mapIndexed { i, v -> Offset(x((i * sampleMin).toFloat()), y(v)) }
        val p = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(p, c.primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val pk = series.indices.maxBy { series[it] }
        if (series[pk] > 0) {
            drawCircle(c.surface, 9.dp.toPx(), pts[pk])
            drawCircle(c.warning, 6.dp.toPx(), pts[pk])
            label(measurer, "최고 ${series[pk]}", peakStyle, Offset(pts[pk].x, pts[pk].y - 16.dp.toPx()), center = true)
        }
        // 도착 시점 지수: 기록 끝 자리에 네이비 점 (최고와 구분). 위가 좁으면 글자를 점 아래로
        arrival?.let { a ->
            val at = Offset(x(total.toFloat()), y(a))
            drawCircle(c.surface, 9.dp.toPx(), at)
            drawCircle(c.navy, 6.dp.toPx(), at)
            // 최고 점과 가까우면 글자를 점 아래로 (최고 글자는 위)
            val below = at.y - top < 28.dp.toPx() || abs(at.x - pts[pk].x) < 120.dp.toPx()
            label(measurer, "도착 $a", arrivalStyle, Offset(at.x - 14.dp.toPx(), if (below) at.y + 18.dp.toPx() else at.y - 18.dp.toPx()), alignRight = true)
        }
    }
}

/** [TrendChart] 범례 한 줄: 무엇이 실제 기록인지 글자로 (색만으로 구분하지 않는다). */
@Composable
fun TrendLegend(modifier: Modifier = Modifier, symptoms: Boolean = true, stops: Boolean = true, arrival: Boolean = true, high: Boolean = false) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(d.gapS), verticalAlignment = Alignment.CenterVertically) {
        LegendItem("최고") { Box(Modifier.size(d.u(10f)).clip(CircleShape).background(c.warning)) }
        if (arrival) LegendItem("도착 시점") { Box(Modifier.size(d.u(10f)).clip(CircleShape).background(c.navy)) }
        if (high) LegendItem("흔들림 많은 구간") { Box(Modifier.size(d.u(14f)).clip(RoundedCornerShape(d.u(3f))).background(c.warning.copy(alpha = 0.35f))) }
        if (symptoms) LegendItem("관찰 신고") { Box(Modifier.width(d.u(3f)).height(d.u(14f)).background(c.warningInk)) }
        if (stops) LegendItem("휴식") { Box(Modifier.size(d.u(14f)).clip(RoundedCornerShape(d.u(3f))).background(c.sub.copy(alpha = 0.2f))) }
    }
}

@Composable
private fun LegendItem(text: String, mark: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        mark()
        Spacer(Modifier.width(Pause.dimens.u(6f)))
        Text(text, style = Pause.type.caption, color = Pause.colors.sub, maxLines = 1)
    }
}

/**
 * 관찰 기록 칸 (v7): 아이콘 + 이름. 고르면 파랑으로 채운 면 + 흰 글자 + 체크 — 테두리만 바꾸는 [Choice]보다 멀리서도 구분된다.
 * 높이 76dp 이상.
 */
@Composable
fun SymptomTile(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    val shape = RoundedCornerShape(d.buttonRadius)
    Row(
        modifier
            .heightIn(min = d.touch)
            .clip(shape)
            .background(if (selected) c.primary else c.surface)
            .border(d.border, if (selected) c.primary else c.border, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = d.u(8f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (selected) Icons.Rounded.Check else icon, null, Modifier.size(d.u(18f)), tint = if (selected) c.onPrimary else c.sub)
        Spacer(Modifier.width(d.u(4f)))
        // 글자 15: 1/3 칸에 "괜찮았어요"까지 한 줄 (본문 16이면 잘림, v7 검수 1회차)
        Text(label, style = Pause.type.body, color = if (selected) c.onPrimary else c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────────── 흔들림 그래프 v10 (STYLE_V10 §5) ─────────────

/** 세로 0~100 단계 배경 띠: 높음(70~100) = warningSoft, 보통(40~70) = surfaceAlt, 낮음 = 바탕. 40·70 눈금 + 오른쪽 단계 이름(보조). */
internal fun DrawScope.levelBands(
    left: Float, top: Float, plotW: Float, plotH: Float,
    measurer: androidx.compose.ui.text.TextMeasurer, axis: TextStyle, soft: Color, mid: Color, grid: Color, base: Color,
    names: Boolean = true,
) {
    fun y(v: Int) = top + plotH * (1f - v / 100f)
    drawRect(soft, Offset(left, y(100)), Size(plotW, y(70) - y(100)))
    drawRect(mid, Offset(left, y(70)), Size(plotW, y(40) - y(70)))
    listOf(40, 70).forEach { g ->
        drawLine(grid, Offset(left, y(g)), Offset(left + plotW, y(g)), strokeWidth = 1.5f)
        label(measurer, "$g", axis, Offset(left - 8.dp.toPx(), y(g)), alignRight = true)
    }
    drawLine(base, Offset(left, y(0)), Offset(left + plotW, y(0)), strokeWidth = 2f)
    if (names) {
        label(measurer, "높음", axis, Offset(left + plotW - 6.dp.toPx(), (y(100) + y(70)) / 2), alignRight = true)
        label(measurer, "보통", axis, Offset(left + plotW - 6.dp.toPx(), (y(70) + y(40)) / 2), alignRight = true)
        label(measurer, "낮음", axis, Offset(left + plotW - 6.dp.toPx(), (y(40) + y(0)) / 2), alignRight = true)
    }
}

/** "오후 12:05" 대신 축·구간 레이블은 24시간 "12:05" (짧게). */
fun clockHm(minuteOfDay: Int): String {
    val m = ((minuteOfDay % 1440) + 1440) % 1440
    return "${m / 60}:${"%02d".format(m % 60)}"
}

/**
 * 주행 자극 그래프 v10: 가로 = 지나온 [pastMin]분 + 지금 → [aheadMin]분 뒤(5분 눈금, "−10분 · 지금 · 5분 …"), 세로 = 0~100 단계 띠.
 * 지나온 값 = [past](엔진 1분 기록, 마지막이 지금) primary 실선, 예측 = [ahead](엔진 1분 21점, 0번 = 지금) navy 점선, 지금 = 세로선 + 점.
 * 위험 구간 = [curveStartMin]부터 [curveLenMin]분 앰버 면 + 위쪽 "12:05~12:17 굽은 길"([clockMinute]이 있을 때 시각, 없으면 분).
 * 엔진이 준 점만 잇는다 — 모양을 만들지 않는다. 점이 모두 2개 미만이면 축과 [emptyText].
 */
@Composable
fun StimulusChart(
    past: List<Int>,
    ahead: List<Int>,
    modifier: Modifier = Modifier,
    curveStartMin: Int? = null,
    curveLenMin: Int? = null,
    clockMinute: Int? = null,
    pastMin: Int = 10,
    aheadMin: Int = 20,
    emptyText: String = "기록이 아직 없어요",
) {
    val c = Pause.colors
    val t = Pause.type
    val measurer = rememberTextMeasurer()
    val axis = t.caption.copy(color = c.sub)
    val curveStyle = t.caption.copy(color = c.warningInk)
    val emptyStyle = t.body.copy(color = c.sub)
    Canvas(modifier) {
        val left = 48.dp.toPx()
        val top = 30.dp.toPx()
        val bottom = 30.dp.toPx()
        val plotW = size.width - left
        val plotH = size.height - top - bottom
        val span = (pastMin + aheadMin).toFloat()
        fun x(m: Float) = left + plotW * ((m + pastMin) / span).coerceIn(0f, 1f)
        fun y(v: Int) = top + plotH * (1f - v.coerceIn(0, 100) / 100f)
        levelBands(left, top, plotW, plotH, measurer, axis, c.warningSoft, c.surfaceAlt, c.border, c.sub.copy(alpha = 0.5f))
        // 시간 눈금 (5분)
        var m = -pastMin
        while (m <= aheadMin) {
            val txt = when {
                m == 0 -> "지금"
                m < 0 -> "−${-m}분"
                else -> "${m}분"
            }
            label(measurer, txt, axis, Offset(x(m.toFloat()), size.height - bottom + 16.dp.toPx()), center = true)
            m += 5
        }
        // 위험 구간: 앰버 면 + 위쪽 레이블
        if (curveStartMin != null && curveLenMin != null && curveStartMin < aheadMin) {
            val x0 = x(curveStartMin.toFloat())
            val x1 = maxOf(x((curveStartMin + curveLenMin).toFloat()), x0 + 4.dp.toPx())
            drawRect(c.warning.copy(alpha = 0.22f), Offset(x0, top), Size(x1 - x0, plotH))
            drawLine(c.warning, Offset(x0, top), Offset(x0, top + plotH), strokeWidth = 2.dp.toPx())
            val txt = clockMinute?.let { "${clockHm(it + curveStartMin)}~${clockHm(it + curveStartMin + curveLenMin)} 굽은 길" } ?: "${curveStartMin}~${curveStartMin + curveLenMin}분 굽은 길"
            label(measurer, txt, curveStyle, Offset(x0, top - 14.dp.toPx()))
        }
        // 지금 세로선
        drawLine(c.navy.copy(alpha = 0.6f), Offset(x(0f), top), Offset(x(0f), top + plotH), strokeWidth = 2.dp.toPx())
        val pastTail = past.takeLast(pastMin + 1)
        val aheadPts = ahead.take(aheadMin + 1)
        if (pastTail.size < 2 && aheadPts.size < 2) {
            label(measurer, emptyText, emptyStyle, Offset(left + plotW / 2, top + plotH / 2), center = true)
            return@Canvas
        }
        if (pastTail.size >= 2) {
            val off = -(pastTail.size - 1)
            val p = Path().apply { pastTail.forEachIndexed { i, v -> val o = Offset(x((off + i).toFloat()), y(v)); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(p, c.primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (aheadPts.size >= 2) {
            val p = Path().apply { aheadPts.forEachIndexed { i, v -> val o = Offset(x(i.toFloat()), y(v)); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(p, c.navy, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            val pk = aheadPts.indices.maxBy { aheadPts[it] * 1000 - it }
            drawCircle(c.surface, 8.dp.toPx(), Offset(x(pk.toFloat()), y(aheadPts[pk])))
            drawCircle(c.warning, 5.dp.toPx(), Offset(x(pk.toFloat()), y(aheadPts[pk])))
        }
        val nowV = pastTail.lastOrNull() ?: aheadPts.first()
        drawCircle(c.surface, 9.dp.toPx(), Offset(x(0f), y(nowV)))
        drawCircle(c.navy, 6.dp.toPx(), Offset(x(0f), y(nowV)))
    }
}

/** [StimulusChart] 범례: 지나온 값(실선) · 예측(점선) · 굽은 길(앰버 면) — 있는 것만, 색과 글자를 함께. */
@Composable
fun StimulusLegend(modifier: Modifier = Modifier, past: Boolean = true, ahead: Boolean = true, curve: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(d.gap), verticalAlignment = Alignment.CenterVertically) {
        if (past) LegendItem("지나온 값") { Box(Modifier.width(d.u(18f)).height(d.u(3f)).background(c.primary)) }
        if (ahead) LegendItem("예측(점선)") {
            Row(horizontalArrangement = Arrangement.spacedBy(d.u(3f))) { repeat(3) { Box(Modifier.width(d.u(4f)).height(d.u(3f)).background(c.navy)) } }
        }
        if (curve) LegendItem("굽은 길") { Box(Modifier.size(d.u(14f)).clip(RoundedCornerShape(d.u(3f))).background(c.warning.copy(alpha = 0.35f))) }
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
