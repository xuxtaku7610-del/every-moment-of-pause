package com.emop.pause.ui.components

import com.emop.pause.uistate.StopMark
import com.emop.pause.uistate.JourneyStop
import com.emop.pause.uistate.JourneyUi
import com.emop.pause.uistate.TimelineDot
import com.emop.pause.uistate.TimelinePoint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import kotlin.math.roundToInt

/**
 * 시그니처 경로선 (UI_SPEC §3 JourneyLine, mockups.html `.journey`).
 * 높이 220dp. 선 8dp는 top 120dp, 현재 위치 32dp 원(흰 테두리 6dp). 쉼표는 선 위, 라벨은 top 150dp.
 * 라벨이 겹치면 next를 우선하고 나머지는 숨긴다(출발·도착 이름 포함).
 */
@Composable
fun JourneyLine(ui: JourneyUi, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val t = Pause.type
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier.fillMaxWidth().height(220.dp).semantics {
            contentDescription = ui.stops.firstOrNull { it.mark == StopMark.NEXT }?.label ?: "경로"
        },
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val labelTop = 150.dp
        val gapPx = with(density) { 24.dp.toPx() }

        // 라벨 배치: 우선순위 next → 나머지(가까운 순) → 출발·도착. 겹치면 뺀다.
        data class Placed(val text: String, val style: TextStyle, val color: Color, val left: Float, val width: Float)
        val placed = mutableListOf<Placed>()
        fun tryPlace(text: String, style: TextStyle, color: Color, anchor: Float, align: Int) {
            val w = measurer.measure(text, style).size.width.toFloat()
            val left = when (align) {
                -1 -> 0f
                1 -> widthPx - w
                else -> (anchor - w / 2).coerceIn(0f, widthPx - w)
            }
            if (placed.none { left < it.left + it.width + gapPx && it.left < left + w + gapPx }) {
                placed += Placed(text, style, color, left, w)
            }
        }
        val nextStyle = t.bodyStrong.copy(fontWeight = FontWeight.ExtraBold)
        ui.stops.filter { it.mark == StopMark.NEXT }.forEach { tryPlace(it.label, nextStyle, c.ink, it.fraction * widthPx, 0) }
        ui.stops.filter { it.mark == StopMark.UPCOMING }.forEach { tryPlace(it.label, t.caption, c.ink2, it.fraction * widthPx, 0) }
        ui.start?.let { tryPlace(it, t.caption, c.ink2, 0f, -1) }
        ui.end?.let { tryPlace(it, t.caption, c.ink2, widthPx, 1) }

        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val top = 120.dp.toPx()
            val h = 8.dp.toPx()
            drawRoundRect(c.line, Offset(0f, top), Size(size.width, h), CornerRadius(h / 2))
            val p = ui.progress
            if (p != null) {
                val x = p.coerceIn(0f, 1f) * size.width
                if (x > 0f) drawRoundRect(c.ink, Offset(0f, top), Size(x, h), CornerRadius(h / 2))
                val cy = top + h / 2
                drawCircle(c.ink, 18.dp.toPx(), Offset(x, cy))
                drawCircle(c.surface, 16.dp.toPx(), Offset(x, cy))
                drawCircle(c.ink, 10.dp.toPx(), Offset(x, cy))
            }
        }
        ui.stops.forEach { s ->
            val (w, h, y, tint) = when (s.mark) {
                StopMark.NEXT -> Quad(60.dp, 96.dp, 14.dp, c.rest)
                StopMark.UPCOMING -> Quad(40.dp, 64.dp, 46.dp, c.ink3)
                StopMark.PAST -> Quad(40.dp, 64.dp, 46.dp, c.line)
            }
            val x = with(density) { (s.fraction.coerceIn(0f, 1f) * widthPx - w.toPx() / 2).roundToInt() }
            Icon(PauseIcons.Comma, null, Modifier.offset { IntOffset(x, y.roundToPx()) }.size(w, h), tint = tint)
        }
        placed.forEach { p ->
            Text(p.text, Modifier.offset { IntOffset(p.left.roundToInt(), labelTop.roundToPx()) }, style = p.style, color = p.color, maxLines = 1, softWrap = false)
        }
    }
}

private data class Quad(val w: Dp, val h: Dp, val y: Dp, val tint: Color)

// ───────────────────────── 정차 타임라인 ─────────────────────────

/** 정차 타임라인 (UI_SPEC §4.3, mockups.html `.tl`): 선 10dp, 점 30dp(지금 36dp), 점 아래 시각(26sp)·라벨(22sp). */
@Composable
fun StopTimeline(points: List<TimelinePoint>, nowFraction: Float, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val t = Pause.type
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth().height(130.dp)) {
        val widthPx = with(density) { maxWidth.toPx() }
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
            val top = 40.dp.toPx()
            val h = 10.dp.toPx()
            drawRoundRect(c.line, Offset(0f, top), Size(size.width, h), CornerRadius(h / 2))
            drawRoundRect(c.ink, Offset(0f, top), Size(nowFraction.coerceIn(0f, 1f) * size.width, h), CornerRadius(h / 2))
            points.forEach { p ->
                val x = p.fraction.coerceIn(0f, 1f) * size.width
                val r = (if (p.dot == TimelineDot.NOW) 18.dp else 15.dp).toPx()
                val ring = when (p.dot) {
                    TimelineDot.NOW -> c.rest
                    TimelineDot.ALARM -> c.caution
                    else -> c.ink
                }
                drawCircle(c.surface, r, Offset(x, top + h / 2))
                drawCircle(ring, r - 3.dp.toPx(), Offset(x, top + h / 2), style = Stroke(6.dp.toPx()))
            }
        }
        // 라벨: 겹치면 지금·끝을 남기고 나머지를 숨긴다.
        val timeStyle = t.label.copy(fontWeight = FontWeight.ExtraBold)
        data class Box2(val p: TimelinePoint, val left: Float, val w: Float)
        val kept = mutableListOf<Box2>()
        val order = points.sortedBy { if (it.dot == TimelineDot.NOW || it.dot == TimelineDot.END) 0 else 1 }
        order.forEach { p ->
            val w = maxOf(measurer.measure(p.time, timeStyle).size.width, measurer.measure(p.label, t.tag).size.width).toFloat() + with(density) { 12.dp.toPx() }
            val anchor = p.fraction * widthPx
            val left = when {
                p.fraction <= 0.001f -> 0f
                p.fraction >= 0.999f -> widthPx - w
                else -> (anchor - w / 2).coerceIn(0f, widthPx - w)
            }
            if (kept.none { left < it.left + it.w + 16f && it.left < left + w + 16f }) kept += Box2(p, left, w)
        }
        kept.forEach { k ->
            val align = when {
                k.p.fraction <= 0.001f -> androidx.compose.ui.Alignment.Start
                k.p.fraction >= 0.999f -> androidx.compose.ui.Alignment.End
                else -> androidx.compose.ui.Alignment.CenterHorizontally
            }
            Column(
                Modifier.offset { IntOffset(k.left.roundToInt(), 80.dp.roundToPx()) }.size(with(density) { k.w.toDp() }, 56.dp),
                horizontalAlignment = align,
            ) {
                Text(k.p.time, style = timeStyle, color = c.ink, maxLines = 1, softWrap = false)
                Text(k.p.label, style = t.tag.copy(fontWeight = FontWeight.Normal), color = c.ink2, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Preview(widthDp = 1488, heightDp = 400, showBackground = true, backgroundColor = 0xFFEEF2F4)
@Composable
private fun JourneyPreview() = PauseTheme(dark = false) {
    Column(Modifier.padding(16.dp)) {
        JourneyLine(
            JourneyUi(
                0.38f,
                listOf(JourneyStop(0.47f, "18분 뒤 안성", StopMark.NEXT), JourneyStop(0.76f, "1시간 50분 뒤 칠곡", StopMark.UPCOMING)),
                "서울",
                "부산",
            ),
        )
        StopTimeline(
            listOf(
                TimelinePoint(0f, "12:18", "도착", TimelineDot.PAST),
                TimelinePoint(0.24f, "12:45", "지금", TimelineDot.NOW),
                TimelinePoint(0.88f, "13:10", "기상 알림", TimelineDot.ALARM),
                TimelinePoint(1f, "13:12", "충전 완료", TimelineDot.END),
            ),
            0.24f,
        )
    }
}
