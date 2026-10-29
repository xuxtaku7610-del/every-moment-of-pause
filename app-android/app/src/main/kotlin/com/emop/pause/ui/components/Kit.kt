package com.emop.pause.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.Pause
import com.emop.pause.uistate.MotionTone

/**
 * 최소 부품 (PIVOT 4단계: 디자인 v5가 오기 전까지 동작 확인용). 화면은 UiState만 보고 이 부품으로 그린다.
 * 움직이는 효과는 쓰지 않는다 — 주행 중 자동 애니메이션 금지(CONCEPT 5장)를 부품 단계에서 지킨다.
 */

enum class ButtonKind { PRIMARY, QUIET, GHOST, ALERT }

/** 큰 버튼 (터치 영역 76dp 이상, 주행 중 버튼과 같은 크기). [small]은 정차 화면 보조 버튼. */
@Composable
fun PauseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Pause.colors
    val (bg, fg) = when (kind) {
        ButtonKind.PRIMARY -> c.rest to Color.White
        ButtonKind.QUIET -> c.sunk to c.ink
        ButtonKind.GHOST -> Color.Transparent to c.ink2
        ButtonKind.ALERT -> c.alert to Color.White
    }
    val shape = RoundedCornerShape(if (small) 36.dp else 48.dp)
    Box(
        modifier
            .heightIn(min = if (small) 76.dp else 104.dp)
            .clip(shape)
            .background(if (enabled) bg else c.sunk)
            .then(if (kind == ButtonKind.GHOST) Modifier.border(2.dp, c.line, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (small) 28.dp else 44.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = if (small) Pause.type.label else Pause.type.button, color = if (enabled) fg else c.ink3, maxLines = 1)
    }
}

/** 앱바: 왼쪽 앱 이름(로고 = 발표 모드 진입점, 5번 탭), 오른쪽 메타 한 줄. */
@Composable
fun AppBar(meta: String?, onLogoTap: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = Pause.colors
    Row(modifier.fillMaxWidth().height(88.dp).padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClick = onLogoTap),
        ) {
            Text("같이가개", style = Pause.type.item, color = c.rest)
        }
        Spacer(Modifier.width(24.dp))
        meta?.let { Text(it, Modifier.weight(1f), style = Pause.type.label, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            ?: Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** 흰 카드. */
@Composable
fun PauseCard(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Pause.colors
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(tint ?: c.surface).padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** 작은 알약 (이유·시설·샘플 표시). */
@Composable
fun Pill(text: String, tone: MotionTone? = null, sample: Boolean = false) {
    val c = Pause.colors
    val (bg, fg) = when {
        sample -> c.cautionSoft to c.cautionInk
        tone == MotionTone.HIGH -> c.alertSoft to c.alertInk
        tone == MotionTone.CAUTION -> c.cautionSoft to c.cautionInk
        else -> c.restSoft to c.restInk
    }
    Box(Modifier.clip(RoundedCornerShape(30.dp)).background(bg).padding(horizontal = 22.dp, vertical = 10.dp)) {
        Text(text, style = Pause.type.label, color = fg, maxLines = 1)
    }
}

/** 멀미 단계 점 + 한 줄 ("콩이 멀미 주의 45"). */
@Composable
fun MotionLine(label: String, tone: MotionTone, modifier: Modifier = Modifier) {
    val c = Pause.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(toneColor(tone)))
        Spacer(Modifier.width(16.dp))
        Text(label, style = Pause.type.title, color = c.ink, maxLines = 1)
    }
}

@Composable
fun toneColor(t: MotionTone): Color = when (t) {
    MotionTone.CALM -> Pause.colors.rest
    MotionTone.CAUTION -> Pause.colors.caution
    MotionTone.HIGH -> Pause.colors.alert
}

/** 체크 줄 (출발 체크·설정 토글). */
@Composable
fun CheckRow(label: String, checked: Boolean, onClick: () -> Unit, sub: String? = null) {
    val c = Pause.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Checkbox, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (checked) c.rest else c.sunk),
            contentAlignment = Alignment.Center,
        ) { if (checked) Text("✓", style = Pause.type.item, color = Color.White) }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = Pause.type.item, color = c.ink)
            sub?.let { Text(it, style = Pause.type.label, color = c.ink2) }
        }
    }
}

/** 고르기 (체급·성향 등). */
@Composable
fun Choice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(if (selected) c.restSoft else c.paper)
            .border(2.dp, if (selected) c.rest else c.line, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Pause.type.label, color = if (selected) c.restInk else c.ink, maxLines = 1) }
}

/** 멀미 지수 흐름 (정차 화면 전용, 정적 그림). 40·70 기준선을 옅게 긋는다. */
@Composable
fun MotionChart(series: List<Int>, modifier: Modifier = Modifier, height: Dp = 220.dp) {
    val c = Pause.colors
    val line = c.rest
    val grid = c.line
    val caution = c.caution
    Canvas(modifier.fillMaxWidth().height(height)) {
        fun y(v: Int) = size.height * (1f - v.coerceIn(0, 100) / 100f)
        listOf(40, 70).forEach { g -> drawLine(grid, Offset(0f, y(g)), Offset(size.width, y(g)), strokeWidth = 2f) }
        if (series.size < 2) return@Canvas
        val step = size.width / (series.size - 1)
        val p = Path().apply {
            series.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * step, y(v)) }
        }
        drawPath(p, line, style = Stroke(width = 6f))
        series.forEachIndexed { i, v -> if (v >= 40) drawCircle(caution, 7f, Offset(i * step, y(v))) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 8.dp), style = Pause.type.captionStrong, color = Pause.colors.ink2)
}
