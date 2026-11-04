package com.emop.pause.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.components.Block
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Caption
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.components.levelBands
import com.emop.pause.ui.theme.Pause
import com.emop.pause.uistate.CompareChart
import com.emop.pause.uistate.CompareChartUi
import com.emop.pause.uistate.ComparePoint
import com.emop.pause.ui.components.Body
import androidx.compose.foundation.layout.IntrinsicSize

/**
 * 발표 모드 "고갯길 비교" v10 (STYLE_V10 §5): 위 수치 3개(최고값 차이 · 평균 차이 · 더 걸리는 시간) → 같은 축 두 선 → 보조.
 * 가로 거리 0~15km, 세로 주행 자극 0~100 단계 띠, "안내 없이"(sub)·"안내대로"(primary). 정지된 그림 — 애니메이션 없음. 카드 1장.
 * 정차 중에만 연다(MainActivity가 drivingLocked면 그리지 않는다).
 */
@Composable
fun CompareScreen(ui: CompareChartUi, onClose: () -> Unit) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler(onBack = onClose)
    val uPeak = CompareChart.peak(ui.unguided)
    val gPeak = CompareChart.peak(ui.guided)
    val uMean = CompareChart.mean(ui.unguided)
    val gMean = CompareChart.mean(ui.guided)
    Column(Modifier.fillMaxSize().background(c.bg).padding(d.margin), verticalArrangement = Arrangement.spacedBy(d.gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title("고갯길 비교 — 안내 없이 vs 안내대로", Modifier.weight(1f), maxLines = 1)
            PauseButton("닫기", onClose, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Close, compact = true)
        }
        Block(Modifier.fillMaxWidth().weight(1f), pad = d.u(24f), spacing = d.gap) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.u(48f))) {
                Metric("최고값", "$uPeak → $gPeak", Modifier.weight(1f), note = "${gPeak - uPeak} (안내대로 − 안내 없이)", color = c.primary)
                Metric("평균", "$uMean → $gMean", Modifier.weight(1f), note = "${gMean - uMean} · 끝 지점 ${ui.unguidedFinal} → ${ui.guidedFinal} (${ui.dropPct}%)")
                Metric("소요 시간", CompareChart.extraLabel(ui.extraSec), Modifier.weight(1f), note = "대관령 옛길 ${CompareChart.kmLabel(ui.roadKm)} · OpenStreetMap")
            }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(c.sub, "안내 없이 (45km/h, 굽이 횡가속 3m/s²)")
                Spacer(Modifier.width(d.u(32f)))
                Legend(c.primary, "안내대로 (굽이 앞 감속, 편안한 횡가속)")
            }
            Box(Modifier.fillMaxWidth().weight(1f)) { Chart(ui, Modifier.fillMaxSize()) }
            Caption("가로: 고갯길 시작에서의 거리 · 세로: 주행 자극 0~100 (낮음 < 40 ≤ 보통 < 70 ≤ 높음)")
        }
        Caption("시뮬레이션 결과 · 차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님 · 지도 © OpenStreetMap contributors (ODbL) · docs/plan/demo_compare.md")
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    val d = Pause.dimens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(d.u(32f)).height(d.u(6f)).background(color))
        Spacer(Modifier.width(d.unit))
        Caption(label, color = Pause.colors.text, maxLines = 1)
    }
}

@Composable
private fun Chart(ui: CompareChartUi, modifier: Modifier) {
    val c = Pause.colors
    val measurer = rememberTextMeasurer()
    val endStyle = Pause.type.title
    val axisStyle = Pause.type.caption.copy(color = c.sub)
    val maxKm = maxOf(ui.unguided.last().km, ui.guided.last().km)
    Canvas(modifier) {
        val left = 48.dp.toPx()
        val axisH = 30.dp.toPx()
        val plotH = size.height - axisH
        val labelW = measurer.measure("55", endStyle).size.width + 32f
        val plotW = size.width - left - labelW
        fun pt(p: ComparePoint) = Offset(left + plotW * (p.km / maxKm), plotH * (1f - p.index / 100f))
        // 끝점 값 라벨과 겹치지 않게 단계 이름은 빼고 띠·눈금만 (아래 보조 글에 단계 기준)
        levelBands(left, 0f, plotW, plotH, measurer, axisStyle, c.warningSoft, c.surfaceAlt, c.border, c.sub.copy(alpha = 0.5f), names = false)
        // 가로축 눈금·라벨: 실제 거리 위치에
        listOf(0f, 5f, 10f, 15f).filter { it <= maxKm + 0.05f }.forEach { km ->
            val x = left + plotW * (minOf(km, maxKm) / maxKm)
            if (km > 0f) drawLine(c.border, Offset(x, 0f), Offset(x, plotH), 1.5f)
            val t = measurer.measure(if (km == 15f) "15km" else "${km.toInt()}", axisStyle)
            drawText(t, topLeft = Offset((x - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), plotH + 8.dp.toPx()))
        }
        fun line(points: List<ComparePoint>, color: Color) {
            val path = Path().apply { points.forEachIndexed { i, p -> val o = pt(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(path, color, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(color, 7.dp.toPx(), pt(points.last()))
        }
        line(ui.unguided, c.sub)
        line(ui.guided, c.primary)
        // 끝점 값 라벨: 끝점 오른쪽, 점 높이에 세로 가운데
        listOf(Triple(ui.unguidedFinal, ui.unguided.last(), c.sub), Triple(ui.guidedFinal, ui.guided.last(), c.primary)).forEach { (v, p, color) ->
            val t = measurer.measure("$v", endStyle.copy(color = color))
            val o = pt(p)
            drawText(t, topLeft = Offset(o.x + 16.dp.toPx(), (o.y - t.size.height / 2f).coerceIn(0f, plotH - t.size.height / 2f)))
        }
    }
}
