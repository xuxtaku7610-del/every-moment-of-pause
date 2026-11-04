package com.emop.pause.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.emop.pause.R
import com.emop.pause.ui.components.AlertStrip
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Choice
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.Block
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.QrCode
import com.emop.pause.ui.components.StatusChip
import com.emop.pause.ui.components.SymptomTile
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.EmergencyCardUi
import com.emop.pause.uistate.EmergencyUi
import com.emop.pause.uistate.MotionTone
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.VetUi
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.components.Body
import com.emop.pause.ui.components.Caption
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import com.emop.pause.ui.components.Metric

/*
 * 긴급 도움 (2026-11-03, `_private/EMERGENCY_PROMPT.md` 3번) — 정차 화면 위 겹침, 주행 중에는 열리지 않는다(화면 선택이 막는다).
 * 단계: 정차 화면 "긴급 도움" → (1) 상황 타일 또는 "바로 동물병원 찾기" → 응급 카드 + 가장 가까운 병원 [경로 안내][전화].
 * 자동 전화·자동 경로 변경 없음 — 경로는 [경로 안내]를 누를 때만 경유지로 들어간다. 앱은 전화를 걸지 못하므로 [전화]는 번호를 크게 + tel: QR.
 * 기존 부품만: Panel·AlertStrip·SymptomTile·PauseButton·StatusChip, 색은 warningSoft·warning·warningInk·primary.
 */

/** 정차 화면의 "긴급 도움" 버튼 (연한 앰버 면 + 앰버 테두리 + 경고 아이콘, 높이 76dp 이상). 주행 화면에는 넣지 않는다. */
@Composable
internal fun EmergencyButton(on: On, modifier: Modifier = Modifier, compact: Boolean = false) {
    PauseButton("긴급 도움", { on(UiEvent.OpenEmergency) }, modifier, kind = ButtonKind.WARNING, icon = Icons.Rounded.WarningAmber, compact = compact)
}

private fun optionIcon(id: String): ImageVector = when (id) {
    "breathing" -> Icons.Rounded.Air
    "seizure" -> Icons.Rounded.Bolt
    "bleeding" -> Icons.Rounded.Bloodtype
    else -> Icons.Rounded.NoFood
}

@Composable
fun EmergencyScreen(ui: EmergencyUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page(null, on, scroll = true, title = "긴급 도움", trailing = { CloseButton(on) }) { wc ->
        // UI v9 5번: 상황 타일은 처음에만 크게. 고른 뒤에는 위쪽 작은 칩 줄로 접고,
        // 왼쪽 큰 카드(띠 → 본문 → [가까운 병원 경로 안내][전화] → 제휴면 [사전 접수] → 출처) | 오른쪽 좁은 칸 "관련 안내". 1/2·1/3은 위아래.
        val wide = wc == WidthClass.FULL
        val card = ui.card
        if (card == null) PickPanel(ui, on, Modifier.fillMaxWidth(), wc)
        else {
            ChipRow(ui, on, wc == WidthClass.THIRD)
            SideBySide(wide, 0.66f, { m -> CardAndVet(ui, card, on, m) }, { m -> RelatedPanel(ui, on, m) })
        }
    }
}

@Composable
private fun PickPanel(ui: EmergencyUi, on: On, m: Modifier, wc: WidthClass) {
    val c = Pause.colors
    val d = Pause.dimens
    Block(m, pad = d.u(24f), spacing = d.gap) {
        Text("어떤 상황인가요?", style = Pause.type.title, color = c.text)
        // 타일 2×2 (전체·1/2 창), 1/3 창은 1열
        val perRow = if (wc == WidthClass.THIRD) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(d.gapS)) {
            ui.options.chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                    row.forEach { o ->
                        SymptomTile(o.label, optionIcon(o.id), ui.selected == o.id, { on(UiEvent.PickEmergency(o.id)) }, Modifier.weight(1f))
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        // 타일을 고르지 않아도 바로 병원으로 (한 번 누르기)
        PauseButton("바로 동물병원 찾기", { on(UiEvent.OpenVets) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.LocalHospital)
        Text(ui.disclaimer, style = Pause.type.caption, color = c.sub)
    }
}

/** 고른 뒤 위쪽 작은 칩 줄: 상황 4개(고른 것 표시). 같은 것을 다시 누르면 그대로. */
@Composable
private fun ChipRow(ui: EmergencyUi, on: On, narrow: Boolean) {
    val d = Pause.dimens
    // 1/3 창은 2×2 — 상황 이름이 잘리지 않게
    Column(verticalArrangement = Arrangement.spacedBy(d.gapS)) {
        ui.options.chunked(if (narrow) 2 else 4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                row.forEach { o -> Choice(o.label, ui.selected == o.id, { on(UiEvent.PickEmergency(o.id)) }, Modifier.weight(1f)) }
            }
        }
    }
}

/** 오른쪽 좁은 칸 "관련 안내": 다른 응급 상황 3개 바로가기 + "동물병원 찾는 방법"(기존 병원 목록) + 고정 안내. */
@Composable
private fun RelatedPanel(ui: EmergencyUi, on: On, m: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Block(m, pad = d.u(24f), spacing = d.gapS) {
        Caption("관련 안내")
        ui.options.filter { it.id != ui.selected }.forEach { o ->
            PauseButton(o.label, { on(UiEvent.PickEmergency(o.id)) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = optionIcon(o.id), compact = true)
        }
        Divider()
        PauseButton("동물병원 찾는 방법", { on(UiEvent.OpenVets) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.LocalHospital, compact = true)
        Text(ui.disclaimer, style = Pause.type.caption, color = c.sub)
    }
}

/**
 * 왼쪽 큰 카드 하나: 앰버 띠 "바로 동물병원에 연락해요" → 본문 2~3줄 → 가장 가까운 병원 [가까운 병원 경로 안내](주)[전화](보조)
 * → 제휴면 [사전 접수] → 출처 한 줄. 카드와 병원을 한 장에 붙여 왼쪽 아래가 비지 않게 (UI v9).
 */
@Composable
private fun CardAndVet(ui: EmergencyUi, card: EmergencyCardUi, on: On, m: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Block(m, pad = d.u(24f), spacing = d.gapS) {
        AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_warning, iconAsset = "ic_warning") {
            // 경고 띠 첫 줄 = 제목 크기(이보다 크게 쓰지 않는다), 상황 이름 = 설명
            Text(EmergencyCardUi.LEAD, style = Pause.type.title, color = c.warningInk)
            Text(card.title, style = Pause.type.body, color = c.text)
        }
        card.lines.forEach { line -> Text("· $line", style = Pause.type.body, color = c.text) }
        Divider()
        Text("가장 가까운 동물병원", style = Pause.type.caption, color = c.sub)
        val vet = ui.vet
        when {
            vet != null -> NearestVet(vet, on, intake = true, routeLabel = "가까운 병원 경로 안내", noPhoneLine = false)
            ui.vetLoading -> Text("찾는 중…", style = Pause.type.body, color = c.sub)
            else -> Text("근처에 찾은 곳이 없어요.", style = Pause.type.body, color = c.sub)
        }
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                card.source?.let { Text(it, style = Pause.type.caption, color = c.sub) }
                Text(EmergencyCardUi.NOTE, style = Pause.type.caption, color = c.sub)
            }
            if (!card.reviewed && !card.hidden) StatusChip("검수 전", neutral = true)
        }
    }
}

/**
 * 가장 가까운 병원 한 곳 (긴급 도움·동물병원 화면이 같이 쓴다): 이름 · 거리 · 도착 예정(값이 있을 때만) → [경로 안내](주) [전화](보조).
 * [전화]를 누르면 번호를 크게 + tel: QR(폰 카메라로 찍으면 전화 앱) — 차 화면이 거는 것처럼 보이지 않게. 번호가 없으면 버튼 없이 "전화번호 정보 없음".
 * 진료 가능 여부·영업시간·24시는 데이터에 없어 쓰지 않는다. [intake]면(긴급 도움) 제휴 병원에 [사전 접수] — 동물병원 화면의 기존 사전 접수로 간다.
 */
@Composable
internal fun NearestVet(v: VetUi, on: On, modifier: Modifier = Modifier, intake: Boolean = false, routeLabel: String = "경로 안내", noPhoneLine: Boolean = true) {
    val c = Pause.colors
    val d = Pause.dimens
    var showPhone by rememberSaveable(v.id) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
        Text(if (v.sample) v.name.removePrefix("[샘플] ") else v.name, style = Pause.type.title, color = c.text, maxLines = 2)
        // v10: 가장 중요한 한 가지 = 거리·도착까지(수치), 상태 알약은 그 옆
        Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f)), verticalAlignment = Alignment.Bottom) {
            v.distance.takeIf { it.isNotBlank() }?.let { dist ->
                val num = dist.takeWhile { it.isDigit() || it == '.' }
                Metric("거리", num.ifEmpty { dist }, unit = dist.removePrefix(num).ifEmpty { null })
            }
            v.minutes?.let { Metric("도착까지 약", "$it", unit = "분") }
            Row(horizontalArrangement = Arrangement.spacedBy(d.unit), modifier = Modifier.padding(bottom = d.u(8f))) {
                v.status?.let { StatusChip(it, MotionTone.CALM) }
                if (v.partner) StatusChip("시연용 제휴") else if (v.sample) StatusChip("샘플", neutral = true)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
            PauseButton(routeLabel, { on(UiEvent.GoVet(v.id)) }, Modifier.weight(if (v.phone != null) 1.4f else 1f), icon = Icons.Rounded.Navigation)
            if (v.phone != null) PauseButton(if (showPhone) "번호 접기" else "전화", { showPhone = !showPhone }, Modifier.weight(1f), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Call)
        }
        // 긴급 도움에서 가장 가까운 곳이 제휴 병원이면 기존 사전 접수(동물병원 화면의 IntakePanel)로 — 흐름 재사용 (11/03)
        if (intake && v.intakeButton) PauseButton("사전 접수", { on(UiEvent.OpenVets) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Send)
        // 긴급 도움에서는 번호가 없으면 줄 자체를 숨긴다 (11/03) — 동물병원 목록은 그대로 "전화번호 정보 없음"
        if (v.phone == null && noPhoneLine) Text("전화번호 정보 없음", style = Pause.type.caption, color = c.sub)
        val phone = v.phone
        if (showPhone && phone != null) {
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                QrCode("tel:" + phone.filter { it.isDigit() }, d.u(112f))
                Spacer(Modifier.width(d.gap))
                Column(Modifier.weight(1f)) {
                    Text(phone, style = Pause.type.metric, color = c.primary, maxLines = 1, softWrap = false)
                    Text("폰으로 걸어 주세요 (폰 카메라로 찍으면 전화 앱이 열려요)", style = Pause.type.caption, color = c.sub)
                }
            }
        }
        // 전화번호가 없으면 "전화로 확인" 안내는 뺀다 (11/03)
        v.callNote?.let { Text(it, style = Pause.type.caption, color = c.sub) }
    }
}
