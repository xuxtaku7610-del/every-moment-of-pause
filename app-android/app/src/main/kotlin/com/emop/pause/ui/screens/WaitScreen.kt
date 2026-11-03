package com.emop.pause.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.emop.pause.R
import com.emop.pause.presentation.WaitLayout
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Choice
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.KeyValue
import com.emop.pause.ui.components.LinkQr
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.SectionHeader
import com.emop.pause.ui.components.StatusIcon
import com.emop.pause.ui.components.rememberAssetImage
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.WaitUi

/*
 * 잠깐 대기 (F3) — 차 밖에서 창문 너머 2m 거리에서 한눈에 읽는 화면 (11/03, `_private/WAIT_PROMPT.md`).
 * 화면 전체 한 장, 가운데 정렬: 집·반려견 그림 → "주인이 곧 돌아와요"(큰 글, 한 줄) → 실내 온도 큰 숫자(측정값이 있을 때만)
 * → 경과 시간 → 영어 한 줄. 보호자 조작은 아래 얇은 줄 하나([대기 종료하기](주 버튼, UI v9) [긴급 도움] [설정]).
 * 제한 시간·배터리·보호자 폰 QR·"폰 공유 연결 안 됨"은 설정 시트 안. 제한 시간을 넘겨도 큰 글은 그대로, 아래 줄에만 앰버 한 줄.
 * 빨강·깜박임·자동 애니메이션 없음(경과 시간 숫자만 바뀐다). 앱은 공조를 켜거나 제어하지 않는다 — "에어컨 켜짐"은 신호가 켜짐일 때만.
 */
@Composable
fun WaitScreen(ui: WaitUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    var sheet by rememberSaveable { mutableStateOf(false) }
    Page(null, on, scroll = false) { wc ->
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val tall = maxHeight
            // 큰 글 두 줄(문구·온도)은 같은 크기, 폭에 맞춰 줄바꿈 없이 (WaitLayout, 1/3 창에서도 한 줄)
            val lines = listOfNotNull(ui.headline, ui.temp)
            // 높이도 본다: 그림·작은 줄 몫(약 300)을 뺀 높이에 큰 글 줄이 다 들어가게 (스크롤 없이 영어 줄까지 한 화면)
            val byHeight = WaitLayout.fitHeight((tall - d.u(300f)).value, lines.size, Pause.type.metric.fontSize.value)
            val big = minOf(WaitLayout.bigSp((maxWidth - d.pad * 2).value, d.scale, lines), byHeight).sp
            val bigStyle = Pause.type.metric.copy(fontSize = big, lineHeight = big * 1.08f, fontWeight = FontWeight.Bold)
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = tall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(d.unit, Alignment.CenterVertically),
                ) {
                    // 집·반려견 그림(한 색 실루엣)은 글자 색으로 물들인다 — 다크에서도 배경에 묻히지 않게 (11/03)
                    val house = rememberAssetImage("wait_house_dog")
                    val iconSize = d.u(if (wc == WidthClass.THIRD) 80f else 104f)
                    if (house != null) Image(house, null, Modifier.size(iconSize), colorFilter = ColorFilter.tint(c.text))
                    else StatusIcon("wait_house_dog", R.drawable.fe_house, iconSize)
                    Text(ui.headline, style = bigStyle, color = c.text, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                    // 실내 온도: 측정값이 있을 때만 같은 크기 숫자 + 작은 이름. 없으면 줄 자체를 숨긴다(안심 문장으로 채우지 않는다)
                    ui.temp?.let { t ->
                        Text(t, style = bigStyle, color = c.text, maxLines = 1, softWrap = false)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("실내 온도", style = Pause.type.section, color = c.sub)
                            if (ui.acOn) {
                                Spacer(Modifier.width(d.gap))
                                Icon(Icons.Rounded.AcUnit, null, Modifier.size(d.u(20f)), tint = c.sub)
                                Spacer(Modifier.width(d.u(4f)))
                                Text("에어컨 켜짐", style = Pause.type.section, color = c.sub)
                            }
                        }
                        // 대기 중 온도가 오르면 한 줄 (측정값 기준, 앰버 글자만)
                        ui.heat?.let { Text(it, style = Pause.type.bodyStrong, color = c.warningInk, textAlign = TextAlign.Center) }
                    }
                    Text(ui.elapsed, style = Pause.type.title, color = c.text)
                    Text(ui.english, style = Pause.type.section.copy(fontWeight = FontWeight.Normal), color = c.sub, textAlign = TextAlign.Center)
                }
            }
        }
        if (sheet) WaitSheet(ui, on, wc) { sheet = false }
        WaitBar(ui, on, sheet, narrow = wc == WidthClass.THIRD) { sheet = !sheet }
    }
}

/** 보호자 줄: 상태 한 줄(제한 시간 넘김·켤 때 안내·여름 안내) + [대기 종료하기](주) [긴급 도움] [설정](보조). 높이 76dp. */
@Composable
private fun WaitBar(ui: WaitUi, on: On, sheetOpen: Boolean, narrow: Boolean, toggle: () -> Unit) {
    val c = Pause.colors
    val d = Pause.dimens
    Divider()
    val line = ui.overdueLine ?: ui.notice ?: ui.seasonNote
    line?.let { Text(it, style = Pause.type.bodyStrong, color = if (ui.overdueLine != null || ui.notice != null) c.warningInk else c.sub) }
    // 1/3 창은 아이콘을 빼서 글자가 낱자에서 꺾이지 않게
    Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
        PauseButton("대기 종료하기", { on(UiEvent.StopWait) }, Modifier.weight(1f), icon = Icons.Rounded.Check.takeUnless { narrow }, compact = true)
        PauseButton("긴급 도움", { on(UiEvent.OpenEmergency) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.WarningAmber.takeUnless { narrow }, compact = true)
        PauseButton(if (sheetOpen) "설정 닫기" else "설정", toggle, Modifier.weight(1f), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.MoreHoriz.takeUnless { narrow }, compact = true)
    }
}

/** 설정 시트 (보호자용): 제한 시간 5·10·15·20분, 차 안 상태(설정 시간·실내 온도·배터리), 보호자 폰 QR 또는 "폰 공유 연결 안 됨". */
@Composable
private fun WaitSheet(ui: WaitUi, on: On, wc: WidthClass, close: () -> Unit) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(Modifier.fillMaxWidth(), pad = d.u(20f), spacing = d.gapS) {
        SectionHeader("보호자 설정")
        val chips: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(d.unit)) {
                Text("제한 시간 (분)", style = Pause.type.metricLabel, color = c.sub)
                Row(horizontalArrangement = Arrangement.spacedBy(d.unit)) {
                    listOf(5, 10, 15, 20).forEach { m -> Choice("$m", ui.limitMin == m, { on(UiEvent.SetWaitLimit(m)); close() }, Modifier.weight(1f)) }
                }
            }
        }
        val status: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
                KeyValue("설정 시간", "${ui.limitMin}분")
                ui.cabin?.let { KeyValue("실내 온도", it.removePrefix("실내 "), valueColor = if (ui.heat != null) c.warningInk else c.text) }
                ui.batteryUsed?.let { KeyValue("배터리 사용", it) }
                ui.battery?.let { KeyValue("배터리", it.removePrefix("배터리 ")) }
            }
        }
        val share: @Composable () -> Unit = {
            ui.share.url?.let { url ->
                LinkQr("보호자 폰" + if (ui.share.connected) "" else " (연결 확인 중)", url, "폰 카메라로 찍으면 대기 화면이 열려요", Modifier.fillMaxWidth(), size = d.u(112f))
            } ?: Text("폰 공유 연결 안 됨 — 차 화면 안내는 계속돼요", style = Pause.type.sub, color = c.sub)
        }
        if (wc == WidthClass.FULL) {
            Row(horizontalArrangement = Arrangement.spacedBy(d.gap)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.gapS)) { chips(); status() }
                Column(Modifier.weight(1f)) { share() }
            }
        } else {
            chips(); Divider(); status(); Divider(); share()
        }
    }
}
