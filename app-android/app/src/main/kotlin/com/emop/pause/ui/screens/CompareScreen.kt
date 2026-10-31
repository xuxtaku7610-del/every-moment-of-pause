package com.emop.pause.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.V5Card
import com.emop.pause.ui.theme.Pause
import com.emop.pause.uistate.CompareChart
import com.emop.pause.uistate.CompareChartUi
import com.emop.pause.uistate.ComparePoint

/**
 * 발표 모드 "고갯길 비교" 전체 화면 (UPGRADE 1). 정지된 그림 — 애니메이션 없음.
 * 가로 거리 0~15km, 세로 주행 자극 0~100, "안내 없이"(회색 굵은 선)·"안내대로"(primary 굵은 선), 끝점 값 라벨.
 * 정차 중에만 연다(MainActivity가 drivingLocked면 그리지 않는다).
 */
@Composable
fun CompareScreen(ui: CompareChartUi, onClose: () -> Unit) {
    val c = Pause.colors
    val d = Pause.dimens
    val grey = Color(0xFF8A94A3)
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(c.bg).padding(d.margin), verticalArrangement = Arrangement.spacedBy(d.gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("고갯길 비교 — 안내 없이 vs 안내대로", Modifier.weight(1f), style = Pause.type.display, color = c.text)
            PauseButton("닫기", onClose, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Close, compact = true)
        }
        V5Card(Modifier.fillMaxWidth().weight(1f), pad = d.u(24f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(grey, "안내 없이 (45km/h, 굽이 횡가속 3m/s²)")
                Spacer(Modifier.width(d.u(32f)))
                Legend(c.primary, "안내대로 (굽이 앞 감속, 편안한 횡가속)")
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.fillMaxHeight().padding(end = d.unit, bottom = d.u(28f)), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    listOf("100", "50", "0").forEach { Text(it, style = Pause.type.label, color = c.sub) }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Chart(ui, grey, Modifier.fillMaxSize())
                }
            }
            Text("가로: 고갯길 시작에서의 거리 · 세로: 주행 자극(0~100, 차량 흔들림 기준)", style = Pause.type.caption, color = c.sub)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
            Fact("주행 자극", "${ui.dropPct}%", Modifier.weight(1f), c.primary)
            Fact("소요 시간", CompareChart.extraLabel(ui.extraSec), Modifier.weight(1f), c.text)
            Fact("실제 도로", "대관령 옛길 ${CompareChart.kmLabel(ui.roadKm)}", Modifier.weight(1f), c.text, "OpenStreetMap")
        }
        Text("시뮬레이션 결과 · 차량 흔들림 기준, 반려견 측정 아님 · 지도 © OpenStreetMap contributors (ODbL) · docs/plan/demo_compare.md", style = Pause.type.caption, color = c.sub)
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    val d = Pause.dimens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(d.u(32f)).height(d.u(6f)).background(color))
        Spacer(Modifier.width(d.unit))
        Text(label, style = Pause.type.label, color = Pause.colors.text)
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier, color: Color, note: String? = null) {
    V5Card(modifier, pad = Pause.dimens.u(20f)) {
        Text(label, style = Pause.type.label, color = Pause.colors.sub)
        Text(value, style = Pause.type.h1, color = color)
        note?.let { Text(it, style = Pause.type.caption, color = Pause.colors.sub) }
    }
}

@Composable
private fun Chart(ui: CompareChartUi, grey: Color, modifier: Modifier) {
    val c = Pause.colors
    val primary = c.primary
    val grid = c.border
    val sub = c.sub
    val measurer = rememberTextMeasurer()
    val endStyle = Pause.type.number.copy(fontSize = (Pause.type.h1.fontSize.value * 1.3f).sp)
    val axisStyle = Pause.type.label.copy(color = sub)
    val maxKm = maxOf(ui.unguided.last().km, ui.guided.last().km)
    Canvas(modifier) {
        val axisH = measurer.measure("0", axisStyle).size.height + 16f
        val plotH = size.height - axisH
        val labelW = measurer.measure("55", endStyle).size.width + 32f
        val plotW = size.width - labelW
        fun pt(p: ComparePoint) = Offset(plotW * (p.km / maxKm), plotH * (1f - p.index / 100f))
        listOf(0, 25, 50, 75, 100).forEach { g ->
            val y = plotH * (1f - g / 100f)
            drawLine(grid, Offset(0f, y), Offset(plotW, y), strokeWidth = if (g % 50 == 0) 3f else 1.5f)
        }
        // 가로축 눈금·라벨: 실제 거리 위치에
        listOf(0f, 5f, 10f, 15f).filter { it <= maxKm + 0.05f }.forEach { km ->
            val x = plotW * (minOf(km, maxKm) / maxKm)
            if (km > 0f) drawLine(grid, Offset(x, 0f), Offset(x, plotH), 1.5f)
            val t = measurer.measure(if (km == 15f) "15km" else "${km.toInt()}", axisStyle)
            drawText(t, topLeft = Offset((x - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), plotH + 12f))
        }
        fun line(points: List<ComparePoint>, color: Color) {
            val path = Path().apply { points.forEachIndexed { i, p -> val o = pt(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(path, color, style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(color, 18f, pt(points.last()))
        }
        line(ui.unguided, grey)
        line(ui.guided, primary)
        // 끝점 값 라벨: 끝점 오른쪽, 점 높이에 세로 가운데
        listOf(ui.unguidedFinal to ui.unguided.last() to grey, ui.guidedFinal to ui.guided.last() to primary).forEach { (vp, color) ->
            val (v, p) = vp
            val t = measurer.measure("$v", endStyle.copy(color = color))
            val o = pt(p)
            drawText(t, topLeft = Offset(o.x + 24f, (o.y - t.size.height / 2f).coerceIn(0f, plotH - t.size.height / 2f)))
        }
    }
}
