package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.emop.pause.R
import com.emop.pause.ui.components.AlertStrip
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.ForecastChart
import com.emop.pause.ui.components.GaugeScale
import com.emop.pause.ui.components.IndexGauge
import com.emop.pause.ui.components.KeyValue
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PetPhoto
import com.emop.pause.ui.components.RoadIllustration
import com.emop.pause.ui.components.StatusChip
import com.emop.pause.ui.components.StatusIcon
import com.emop.pause.ui.components.forecastAt
import com.emop.pause.ui.components.toneInk
import com.emop.pause.ui.components.toneWord
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.CareSheet
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.MotionTone
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.VoiceSheetState
import com.emop.pause.uistate.motionTone

/*
 * 주행 화면 v6 (F1·F2, docs/design/v6/STYLE_V6.md §7.1~7.2): 텍스트 2줄 · 버튼 2개 · 음성 힌트 · 스크롤 없음.
 * 모든 주행 상태가 같은 뼈대를 쓴다 — 왼쪽 "경로 보기"(도로 일러스트, 화면의 주 영역) + 오른쪽 "상태 패널".
 * 상태(평소·예보·추천·이상·병원)에 따라 오른쪽 패널 내용만 바뀌고 위치·크기는 그대로라 2초 안에 같은 자리를 본다.
 */

@Composable
fun DriveScreen(ui: DriveUi, on: On) {
    Page(ui.meta, on, scroll = false) { wc ->
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                ui.restricted -> RestrictedDrive(ui)
                wc == WidthClass.THIRD -> ThirdDrive(ui, on)
                else -> {
                    val forecast = ui.content as? DriveContent.Forecast
                    // 예보일 때는 그래프가 읽히도록 상태 패널을 넓힌다(경로 보기 45%)
                    val routeWeight = when {
                        wc == WidthClass.HALF -> 0.48f
                        forecast != null -> 0.45f
                        else -> 0.58f
                    }
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Pause.dimens.gap)) {
                        RouteView(ui, Modifier.weight(routeWeight).fillMaxHeight())
                        Box(Modifier.weight(1f - routeWeight).fillMaxHeight()) { SidePanel(ui, on, forecast) }
                    }
                }
            }
        }
        DriveFooter(ui, on)
    }
}

// ───────────── 경로 보기 (지도 대신 경로 일러스트) ─────────────

@Composable
private fun RouteView(ui: DriveUi, modifier: Modifier) {
    val d = Pause.dimens
    val c = Pause.colors
    val f = ui.content as? DriveContent.Forecast
    Box(modifier.clip(RoundedCornerShape(d.cardRadius)).background(c.land)) {
        RoadIllustration(ui.roadAhead, Modifier.fillMaxSize(), cautionFromM = f?.startM?.toFloat())
        Column(Modifier.fillMaxWidth().padding(d.gap), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
            if (f == null) Text("앞 1km 경로", style = Pause.type.caption, color = c.sub)
            if (f != null) {
                // 1) 어디서·얼마나 2) 누가·얼마나 3) 운전 안내 — 띠는 내용 높이만, 글자는 20/16
                AlertStrip(Modifier.fillMaxWidth(), icon = R.drawable.fe_warning, iconAsset = "ic_warning", soft = c.surface) {
                    Text(f.title ?: f.line, style = Pause.type.h1, color = c.text, maxLines = if (f.title != null) 1 else 2, overflow = TextOverflow.Ellipsis)
                    // 문자열 제한(LIMIT_STRING_LENGTH)이면 코칭이 빠진다 → 누가·얼마나만 짧게
                    val second = f.coach ?: f.peak?.let { "${ui.pet.ifEmpty { "반려견" }} ${toneWord(motionTone(it))} 예보" }
                    second?.let { Text(it, style = Pause.type.bodyStrong, color = c.warningInk, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

// ───────────── 오른쪽 상태 패널 ─────────────

@Composable
private fun SidePanel(ui: DriveUi, on: On, forecast: DriveContent.Forecast?) {
    when (val ct = ui.content) {
        is DriveContent.Distress -> DistressPanel(ui, ct, on)
        is DriveContent.Suggest -> SuggestPanel(ui, ct, on, buttons = ui.voice == null)
        is DriveContent.Vet -> VetPanel(ui, ct)
        else -> StatusPanel(ui, forecast)
    }
}

/** 반려견 한 줄: 원형 사진 · 이름 · 상태 알약. */
@Composable
private fun PetHeader(ui: DriveUi, photo: Float = 64f) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(verticalAlignment = Alignment.CenterVertically) {
        PetPhoto(Modifier.size(d.u(photo)))
        Spacer(Modifier.width(d.gap))
        Column(verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
            Text(ui.pet.ifEmpty { "반려견" }, style = Pause.type.h1, color = c.text, maxLines = 1)
            ui.motion?.let { m -> StatusChip(toneWord(m.tone), m.tone) } ?: StatusChip("기록 중", neutral = true)
        }
    }
}

/**
 * 반려견 상태 (평소·쉼표로 가는 중·멀미 예보). 위계: 멀미 지수(36) → 지금 단계 → 마지막 휴식·다음 쉼표.
 * 예보면 아래가 20분 그래프로 바뀐다: 지금 지수 · 20분 뒤 · 예상 최고와 위험 구간.
 */
@Composable
private fun StatusPanel(ui: DriveUi, forecast: DriveContent.Forecast?) {
    val c = Pause.colors
    val d = Pause.dimens
    val m = ui.motion
    val index = m?.index ?: forecast?.now ?: 0
    val tone = m?.tone ?: MotionTone.CALM
    Panel(Modifier.fillMaxSize(), pad = d.u(24f), spacing = d.gap) {
        PetHeader(ui)
        Divider()
        Row(verticalAlignment = Alignment.Bottom) {
            Metric("멀미 지수", "$index", unit = "/ 100", color = toneInk(tone), modifier = Modifier.weight(1f))
            if (forecast?.now != null && forecast.peak != null) {
                val at20 = forecastAt(20, forecast.now!!, forecast.peak!!, forecast.startMin, forecast.durationMin)
                Metric("20분 뒤 예상", "$at20", color = if (at20 >= 40) c.warningInk else c.comfort, small = true, align = Alignment.End)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(d.u(6f))) {
            IndexGauge(index, tone, Modifier.fillMaxWidth().height(d.u(8f)))
            GaugeScale()
        }
        if (forecast == null) Spacer(Modifier.weight(1f))
        if (forecast?.now != null && forecast.peak != null) {
            ForecastBlock(forecast)
        } else {
            Divider()
            // 마지막 휴식 후 · 다음 쉼표까지: 둘 다 시간 수치로 나란히 (같은 단위라 비교가 된다)
            val heading = ui.content as? DriveContent.Heading
            Row(Modifier.fillMaxWidth()) {
                ui.sinceRest?.let { Metric("마지막 휴식 후", it.removePrefix("마지막 휴식 후 "), small = true, modifier = Modifier.weight(1f)) }
                    ?: Metric("마지막 휴식 후", "–", small = true, color = c.sub, modifier = Modifier.weight(1f))
                when {
                    heading != null -> Metric("쉼표까지", hm(heading.minutes), small = true, color = c.primary, note = heading.place, modifier = Modifier.weight(1f))
                    ui.nextStopMin != null -> Metric("다음 쉼표까지", hm(ui.nextStopMin!!), small = true, note = listOfNotNull(ui.nextStopName, ui.nextStop?.removePrefix("약 ")).joinToString(" · "), modifier = Modifier.weight(1f))
                    else -> Metric("다음 쉼표", "계획 없음", small = true, color = c.sub, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.weight(1f))
            // 다음 쉼표 문장은 위 수치와 같으므로 쉼표 계획이 없을 때의 한 줄만
            (ui.content as? DriveContent.Calm)?.takeIf { ui.nextStopMin == null }?.let {
                Divider()
                Text(it.line, style = Pause.type.body, color = c.sub, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ColumnScope.ForecastBlock(f: DriveContent.Forecast) {
    val c = Pause.colors
    val d = Pause.dimens
    val peakAt = f.startMin?.let { s -> f.durationMin?.let { s + it } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("앞으로 20분", style = Pause.type.section, color = c.text)
        Spacer(Modifier.weight(1f))
        Text(
            "예상 최고 ${f.peak}" + (peakAt?.let { if (it <= 20) " · ${it}분 뒤" else " · 20분 뒤 이후" } ?: ""),
            style = Pause.type.bodyStrong, color = c.warningInk, maxLines = 1,
        )
    }
    ForecastChart(f.now!!, f.peak!!, f.startMin, f.durationMin, Modifier.fillMaxWidth().weight(1f))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(d.u(10f)).clip(RoundedCornerShape(d.u(2f))).background(c.warning.copy(alpha = 0.35f)))
        Spacer(Modifier.width(d.u(6f)))
        Text("굽은 길 구간", style = Pause.type.caption, color = c.sub)
        Spacer(Modifier.width(d.gap))
        Box(Modifier.size(d.u(10f)).clip(RoundedCornerShape(d.u(2f))).background(c.warningSoft))
        Spacer(Modifier.width(d.u(6f)))
        Text("멀미 주의(40 이상)", style = Pause.type.caption, color = c.sub)
        Spacer(Modifier.weight(1f))
        Text("진단 아님", style = Pause.type.caption, color = c.sub)
    }
}

/** 쉼표 추천: 장소(24) → 남은 분·거리(수치) → 이유 2개 → 버튼 2개 + 음성 힌트. */
@Composable
private fun SuggestPanel(ui: DriveUi, s: DriveContent.Suggest, on: On, buttons: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(Modifier.fillMaxSize(), pad = d.u(24f), spacing = d.gap) {
        PetHeader(ui, photo = 48f)
        Divider()
        Text(if (s.urgent) "운전자 쉬어 갈 때" else "쉼표 추천", style = Pause.type.metricLabel, color = if (s.urgent) c.warningInk else c.primary)
        Column {
            Text(s.place, style = Pause.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            s.direction?.let { Text(it, style = Pause.type.sub, color = c.sub, maxLines = 1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            Metric("도착까지", "${s.minutes}", unit = "분", small = true)
            s.distance?.let { dist ->
                val num = dist.takeWhile { it.isDigit() || it == '.' }
                Metric("거리", num.ifEmpty { dist }, unit = dist.removePrefix(num).ifEmpty { null }, small = true)
            }
        }
        if (!ui.restricted) s.reasons.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(d.u(22f)), tint = c.primary)
                Spacer(Modifier.width(d.gapS))
                Text(r, style = Pause.type.bodyStrong, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(1f))
        if (buttons) {
            Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.weight(1f))
                if (!s.urgent) PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }
            VoiceHint(if (s.urgent) "“여기서 쉴게요”라고 말해도 돼요" else "“여기서 쉴게요” 또는 “나중에”라고 말해도 돼요")
        }
    }
}

/** "콩이가 이상해요" (UPGRADE 7): 안내 2줄 + 장소·분 + 버튼 2개 + 하단 고정 문구(진단 아님). 애니메이션 없음. */
@Composable
private fun DistressPanel(ui: DriveUi, dc: DriveContent.Distress, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(Modifier.fillMaxSize(), pad = d.u(24f), spacing = d.gap) {
        PetHeader(ui, photo = 48f)
        AlertStrip(Modifier.fillMaxWidth(), icon = if (dc.vet) R.drawable.fe_hospital else R.drawable.fe_warning, iconAsset = if (dc.vet) "ic_hospital" else "ic_warning") {
            Text("반려견 안내", style = Pause.type.metricLabel, color = c.warningInk)
            Text(dc.line, style = Pause.type.h1, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        dc.target?.let { t ->
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (dc.vet) "가까운 동물병원" else "가장 가까운 정차 장소", style = Pause.type.metricLabel, color = c.sub)
                    Text(t, style = Pause.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                dc.minutes?.let { Metric("약", "$it", unit = "분", small = true, align = Alignment.End) }
            }
        }
        Spacer(Modifier.weight(1f))
        DistressButtons(dc, on, vertical = false)
        Text(CareSheet.DISCLAIMER, style = Pause.type.caption, color = c.sub)
    }
}

@Composable
private fun DistressButtons(dc: DriveContent.Distress, on: On, vertical: Boolean) {
    val d = Pause.dimens
    val primary = when {
        dc.vet -> "동물병원으로"
        dc.target != null -> "${dc.target}로"
        else -> "알겠어요"
    }
    if (vertical) {
        PauseButton(primary, { on(UiEvent.AcceptDistress) }, Modifier.fillMaxWidth())
        PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
            PauseButton(primary, { on(UiEvent.AcceptDistress) }, Modifier.weight(1f))
            PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
        }
    }
}

/** 동물병원 가는 길 (주행 중 1순위 한 곳) + 사전 접수 상태 한 줄. 버튼 없음. */
@Composable
private fun VetPanel(ui: DriveUi, v: DriveContent.Vet) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(Modifier.fillMaxSize(), pad = d.u(24f), spacing = d.gap) {
        PetHeader(ui, photo = 48f)
        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIcon("ic_hospital", R.drawable.fe_hospital, d.u(32f))
            Spacer(Modifier.width(d.gapS))
            Text("동물병원 가는 길", style = Pause.type.metricLabel, color = c.primary)
        }
        Text(v.name, style = Pause.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            v.minutes?.let { Metric("도착까지", "$it", unit = "분") }
            val num = v.distance.takeWhile { it.isDigit() || it == '.' }
            Metric("거리", num.ifEmpty { v.distance }, unit = v.distance.removePrefix(num).ifEmpty { null }, small = true)
        }
        v.clinic?.let { StatusChip(it) }
        Spacer(Modifier.weight(1f))
        Text("흔들림을 줄여 천천히 안내할게요", style = Pause.type.body, color = c.sub, maxLines = 1)
    }
}

/** RESTRICTED: 가장 급한 한 줄만 (DS-03). */
@Composable
private fun RestrictedDrive(ui: DriveUi) {
    val c = Pause.colors
    val text = when (val ct = ui.content) {
        is DriveContent.Calm -> ct.line
        is DriveContent.Forecast -> ct.title ?: ct.line
        is DriveContent.Suggest -> "${ct.minutes}분 뒤 ${ct.place}"
        is DriveContent.Heading -> "${ct.minutes}분 뒤 ${ct.place}"
        is DriveContent.Vet -> "${ct.name} ${ct.distance}"
        is DriveContent.Distress -> ct.line
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = Pause.type.title, color = c.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 1/3 창: 상태와 다음 행동만 (그래프·사진·다중 버튼 없음). */
@Composable
private fun ThirdDrive(ui: DriveUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(Modifier.fillMaxSize(), pad = d.u(20f), spacing = d.gapS) {
        ui.motion?.let { m ->
            Text(ui.pet.ifEmpty { "반려견" }, style = Pause.type.h1, color = c.text, maxLines = 1)
            Metric("멀미 지수", "${m.index}", unit = "/ 100", note = toneWord(m.tone), color = toneInk(m.tone), noteColor = toneInk(m.tone))
        }
        ui.sinceRest?.let { KeyValue("마지막 휴식 후", it.removePrefix("마지막 휴식 후 ")) }
        ui.nextStop?.let { KeyValue("다음 쉼표", it.removePrefix("약 ")) }
        val line = when (val ct = ui.content) {
            is DriveContent.Calm -> null
            is DriveContent.Forecast -> ct.title ?: ct.line
            is DriveContent.Suggest -> "${ct.minutes}분 뒤 ${ct.place}"
            is DriveContent.Heading -> "${ct.minutes}분 뒤 ${ct.place}로 가는 중"
            is DriveContent.Vet -> "${ct.name} · ${ct.distance}"
            is DriveContent.Distress -> ct.line
        }
        line?.let { Text(it, style = Pause.type.h1, color = if (ui.content is DriveContent.Forecast) c.warningInk else c.text, maxLines = 2) }
        Spacer(Modifier.weight(1f))
        (ui.content as? DriveContent.Distress)?.let { dc -> DistressButtons(dc, on, vertical = true) }
        val s = ui.content as? DriveContent.Suggest
        if (s != null && ui.voice == null) {
            PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.fillMaxWidth())
            if (!s.urgent) PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
        }
    }
}

/** 결과 한 줄 · 음성 시트(듣는 중 / 실패하면 버튼 2개) · 오프라인 한 줄. */
@Composable
private fun DriveFooter(ui: DriveUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    if (!ui.restricted) ui.toast?.let { t ->
        Text(
            t, Modifier.fillMaxWidth().clip(RoundedCornerShape(d.buttonRadius)).background(c.navy).padding(horizontal = d.u(20f), vertical = d.gapS),
            style = Pause.type.bodyStrong, color = c.surface, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
    when (ui.voice) {
        VoiceSheetState.LISTENING -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(d.buttonRadius)).background(c.primarySoft).padding(horizontal = d.u(20f), vertical = d.gapS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(d.u(36f)).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Mic, null, Modifier.size(d.u(22f)), tint = c.onPrimary)
            }
            Spacer(Modifier.width(d.gapS))
            Text("듣고 있어요…", style = Pause.type.section, color = c.primary)
        }
        VoiceSheetState.FAILED -> Panel(Modifier.fillMaxWidth(), pad = d.u(16f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                Text("잘 못 들었어요. 버튼으로 골라 주세요.", style = Pause.type.section, color = c.text, modifier = Modifier.weight(1f))
                PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.width(d.u(200f)))
                PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.width(d.u(200f)), kind = ButtonKind.SECONDARY)
            }
        }
        null -> if (ui.offline) Text("인터넷 없이 내장 데이터로 안내 중", style = Pause.type.sub, color = c.sub)
    }
}

// ───────────── 미리보기 (2560×1440dp, 배율 2) ─────────────

@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PDrive() = PauseTheme(false, 2f) { DriveScreen(Fixtures.drive) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PForecast() = PauseTheme(false, 2f) { DriveScreen(Fixtures.forecast) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PSuggest() = PauseTheme(false, 2f) { DriveScreen(Fixtures.suggest) {} }
