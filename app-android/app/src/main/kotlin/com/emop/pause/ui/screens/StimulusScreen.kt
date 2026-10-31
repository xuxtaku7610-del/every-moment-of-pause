package com.emop.pause.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.RecentChart
import com.emop.pause.ui.components.SectionHeader
import com.emop.pause.ui.components.toneInk
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.StimulusUi
import com.emop.pause.uistate.motionLabel
import com.emop.pause.uistate.motionTone

/*
 * 정차 중 "주행 자극 보기" (2026-10-31 재정비 원칙 2): 주행 자극 숫자·그래프를 보는 정차 화면.
 * 주행 자극 = 차량 흔들림 누적량(ISO 2631-1 단순화) — 반려견 몸을 잰 값이 아니라는 고정 문구를 항상 함께 둔다.
 */

@Composable
fun StimulusScreen(ui: StimulusUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Page("주행 자극 보기", on, scroll = true, trailing = { CloseButton(on) }) { wc ->
        val wide = wc != WidthClass.THIRD
        Panel(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.gap) {
            val now = ui.now
            if (now == null) {
                Text("아직 달린 기록이 없어요", style = Pause.type.title, color = c.text)
            } else {
                val numbers: @Composable (Modifier) -> Unit = { m ->
                    Metric("지금 주행 자극", "${now.index}", m, unit = "/ 100", note = motionLabel(now.tone), color = toneInk(now.tone), noteColor = toneInk(now.tone))
                }
                val peak: @Composable (Modifier) -> Unit = { m ->
                    ui.peak?.let { p -> Metric("이번 여행 최고", "$p", m, unit = "/ 100", note = motionLabel(motionTone(p)), small = true) }
                }
                if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.u(48f))) {
                    numbers(Modifier)
                    peak(Modifier)
                } else {
                    numbers(Modifier)
                    peak(Modifier)
                }
            }
            Text(ui.note, style = Pause.type.bodyStrong, color = c.sub)
        }
        Panel(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.gapS) {
            SectionHeader("최근 30분", trailing = "1분 간격 · 낮음 < 40 ≤ 보통 < 70 ≤ 높음")
            RecentChart(ui.recent, Modifier.fillMaxWidth().height(d.u(200f)))
        }
        ui.forecast?.let { fc -> Panel(Modifier.fillMaxWidth(), pad = d.u(24f), spacing = d.gapS) {
            SectionHeader("앞길 예보")
            Text(fc, style = Pause.type.section, color = c.text)
            ui.basis?.let {
                Divider()
                Column { Text("근거: $it", style = Pause.type.sub, color = c.sub) }
            }
        } }
    }
}

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PStimulus() = PauseTheme(false, 2f) { StimulusScreen(Fixtures.stimulus) {} }
