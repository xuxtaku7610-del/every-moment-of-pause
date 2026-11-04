package com.emop.pause.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emop.pause.ui.components.Block
import com.emop.pause.ui.components.Body
import com.emop.pause.ui.components.Caption
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.StimulusChart
import com.emop.pause.ui.components.StimulusLegend
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.components.clockHm
import com.emop.pause.ui.components.toneInk
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.StimulusUi
import com.emop.pause.uistate.motionLabel
import com.emop.pause.uistate.motionTone
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize

/*
 * 정차 중 "주행 자극 보기" v10 (STYLE_V10 §4·§5): 위 수치 3개(현재 지수 · 예측 최고(시각) · 위험 구간) → 그래프(높이 ≤ 40%) → 예보 한 줄·근거.
 * 주행 자극 = 차량 흔들림 누적량(ISO 2631-1 단순화) — 반려견 몸을 잰 값이 아니라는 고정 문구를 항상 함께 둔다. 카드 1장.
 */

@Composable
fun StimulusScreen(ui: StimulusUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page("주행 자극 보기", on, scroll = true, trailing = { CloseButton(on) }) { wc ->
        Block(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.gap) {
            val now = ui.now
            if (now == null) {
                Title("아직 달린 기록이 없어요")
                Caption(ui.note)
                return@Block
            }
            // 수치 3개 — 값이 없는 칸은 숨긴다 (예보는 주행 중에만 있다)
            val peakMin = ui.ahead.take(21).indices.maxByOrNull { ui.ahead[it] * 1000 - it }
            val cells = listOfNotNull<@Composable (Modifier) -> Unit>(
                { m -> Metric("현재 지수", "${now.index}", m, unit = "/ 100", note = motionLabel(now.tone), color = toneInk(now.tone), noteColor = toneInk(now.tone)) },
                peakMin?.takeIf { ui.ahead.size >= 2 }?.let { pk ->
                    { m: Modifier ->
                        val v = ui.ahead[pk]
                        Metric("예측 최고", "$v", m, unit = "/ 100", note = ui.clockMinute?.let { "${clockHm(it + pk)} (${pk}분 뒤)" } ?: "${pk}분 뒤", color = toneInk(motionTone(v)))
                    }
                } ?: ui.peak?.let { p -> { m: Modifier -> Metric("이번 여행 최고", "$p", m, unit = "/ 100", note = motionLabel(motionTone(p))) } },
                ui.curveStartMin?.let { s0 ->
                    { m: Modifier ->
                        // 수치 = 시작 시각(한 줄에 들어가게), 끝 시각·거리·길이는 아래 설명 줄
                        val len = ui.curveLenMin ?: 0
                        val start = ui.clockMinute?.let { clockHm(it + s0) } ?: "${s0}분 뒤"
                        val end = ui.clockMinute?.let { "~${clockHm(it + s0 + len)}" } ?: "~${s0 + len}분 뒤"
                        Metric("위험 구간", start, m, note = listOfNotNull(end, ui.curveKm, "${len}분").joinToString(" · "), color = c.warningInk)
                    }
                },
            )
            if (wc != WidthClass.THIRD) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.u(48f))) { cells.forEach { it(Modifier.weight(1f)) } }
            else Column(verticalArrangement = Arrangement.spacedBy(d.gap)) { cells.forEach { it(Modifier) } }
            Divider()
            StimulusChart(
                past = ui.recent, ahead = ui.ahead, modifier = Modifier.fillMaxWidth().height(d.u(220f)),
                curveStartMin = ui.curveStartMin, curveLenMin = ui.curveLenMin, clockMinute = ui.clockMinute,
            )
            StimulusLegend(past = ui.recent.size >= 2, ahead = ui.ahead.size >= 2, curve = ui.curveStartMin != null)
            ui.forecast?.let { Body(it) }
            ui.basis?.let { Caption("근거: $it") }
            Caption("${ui.note} · 1분 간격 엔진 값 · 낮음 < 40 ≤ 보통 < 70 ≤ 높음")
        }
    }
}

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PStimulus() = PauseTheme(false, 2f) { StimulusScreen(Fixtures.stimulus) {} }
