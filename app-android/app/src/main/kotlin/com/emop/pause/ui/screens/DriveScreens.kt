package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.RoundaboutRight
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.TurnSlightLeft
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.emop.pause.R
import com.emop.pause.ui.components.AlertStrip
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.Metric
import com.emop.pause.ui.components.Panel
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PetPhoto
import com.emop.pause.ui.components.StatusChip
import com.emop.pause.ui.components.StatusIcon
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.CareSheet
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.GuideIconUi
import com.emop.pause.uistate.GuideUi
import com.emop.pause.uistate.NearbyKindUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.VoiceSheetState

/*
 * 주행 화면 v8 (2026-10-31 재정비, docs/states.md 상태 2~5·7): 분할 창(기본 내비 옆 1/3·1/2)이 기본이다.
 * 지도·길 그림은 그리지 않는다 — 지도는 Pleos 기본 내비가 맡고, 같이가개는 NaviHelper 실제 값(다음 회전·남은 거리·도착)과
 * 다음 쉼표, 콩이 관찰 상태(보호자 음성 신고만)를 보여 준다. 주행 자극 숫자·그래프는 주행 중에 없다(예보는 경고 한 줄).
 * 주행 중 규칙: 텍스트 2줄 · 버튼 2개 · 스크롤 없음 · 애니메이션 없음. 버튼 글자는 줄이지 않는다.
 */

@Composable
fun DriveScreen(ui: DriveUi, on: On) {
    val d = Pause.dimens
    Page(ui.meta, on, scroll = false) { wc ->
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                ui.restricted -> RestrictedDrive(ui)
                // 1/3 창(내비 옆 기본, UI v9): 예보 띠 → 다음 회전 → 주변 추천 또는 다음 쉼표 → 관찰 상태 한 줄 → 음성 힌트.
                // 카드 높이는 내용에 맞추고 위로 붙인다 — 아래 빈 큰 카드를 두지 않는다
                wc == WidthClass.THIRD -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(d.gapS)) {
                    (ui.content as? DriveContent.Forecast)?.let { ForecastStrip(it, Modifier.fillMaxWidth()) }
                    // 버튼이 있는 카드(쉼표 추천·이상해요·주변 추천)는 버튼 2개가 다 보여야 하므로 길 안내를 도착 한 줄로 줄인다
                    if (acting(ui) || ui.guide == null) ArriveLine(ui) else GuideCard(ui, Modifier.fillMaxWidth(), compact = true)
                    ContextPanel(ui, on, Modifier.fillMaxWidth(), compact = true)
                    ObservationCard(ui, Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                }
                // 1/2 창·전체 창: 왼쪽 길 안내·관찰 상태, 오른쪽 지금 할 일 (같은 자리에 같은 종류 — 2초 안에 찾는다). 둘 다 위로 붙임
                else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                    (ui.content as? DriveContent.Forecast)?.let { ForecastStrip(it, Modifier.fillMaxWidth()) }
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(d.gap)) {
                        Column(Modifier.weight(0.46f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                            if (ui.guide != null) GuideCard(ui, Modifier.fillMaxWidth(), compact = false)
                            else ArriveLine(ui)
                            ObservationCard(ui, Modifier.fillMaxWidth())
                            Spacer(Modifier.weight(1f))
                        }
                        Column(Modifier.weight(0.54f).fillMaxHeight()) {
                            ContextPanel(ui, on, Modifier.fillMaxWidth(), compact = false)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        DriveFooter(ui, on)
    }
}

/** 버튼이 있는 카드가 떠 있는지 (쉼표 추천·이상해요·주변 추천). */
private fun acting(ui: DriveUi) = ui.content is DriveContent.Suggest || ui.content is DriveContent.Distress || ui.content is DriveContent.Nearby

// ───────────── 다음 회전 (NaviHelper 실제 값) ─────────────

private fun guideIcon(i: GuideIconUi): ImageVector = when (i) {
    GuideIconUi.LEFT -> Icons.Rounded.TurnLeft
    GuideIconUi.RIGHT -> Icons.Rounded.TurnRight
    GuideIconUi.KEEP_LEFT -> Icons.Rounded.TurnSlightLeft
    GuideIconUi.KEEP_RIGHT -> Icons.Rounded.TurnSlightRight
    GuideIconUi.UTURN -> Icons.Rounded.UTurnLeft
    GuideIconUi.STRAIGHT -> Icons.Rounded.Straight
    GuideIconUi.ROUNDABOUT -> Icons.Rounded.RoundaboutRight
    GuideIconUi.TOLL -> Icons.Rounded.Toll
    GuideIconUi.REST_AREA -> Icons.Rounded.LocalCafe
    GuideIconUi.ARRIVE -> Icons.Rounded.Flag
    GuideIconUi.OTHER -> Icons.Rounded.Navigation
}

private fun guideWord(i: GuideIconUi): String = when (i) {
    GuideIconUi.LEFT -> "좌회전"
    GuideIconUi.RIGHT -> "우회전"
    GuideIconUi.KEEP_LEFT -> "왼쪽 길"
    GuideIconUi.KEEP_RIGHT -> "오른쪽 길"
    GuideIconUi.UTURN -> "유턴"
    GuideIconUi.STRAIGHT -> "직진"
    GuideIconUi.ROUNDABOUT -> "회전 교차로"
    GuideIconUi.TOLL -> "톨게이트"
    GuideIconUi.REST_AREA -> "휴게소"
    GuideIconUi.ARRIVE -> "도착"
    GuideIconUi.OTHER -> "안내"
}

/**
 * 다음 회전 카드: 방향 아이콘 + 거리(큰 수치) + 방면, 아래 줄 도착 예정·남은 거리.
 * 안내가 없으면(내비 안내 전·고속도로 직진) 도착 줄만, 그것도 없으면 "내비에서 목적지를 정하면 보여요".
 */
@Composable
private fun GuideCard(ui: DriveUi, modifier: Modifier, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val g = ui.guide
    Panel(modifier, pad = d.u(if (compact) 16f else 20f), spacing = d.gapS) {
        if (g != null) GuideRow(g, compact)
        if (ui.arrive != null || ui.remain != null) {
            Divider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ui.arrive?.let {
                    Text("도착", style = Pause.type.metricLabel, color = c.sub)
                    Spacer(Modifier.width(d.unit))
                    Text(it, style = Pause.type.section, color = c.text, maxLines = 1)
                }
                Spacer(Modifier.weight(1f).width(d.unit))
                ui.remain?.let { Text(it, style = Pause.type.bodyStrong, color = c.sub, maxLines = 1) }
            }
        }
    }
}

/**
 * 회전 안내가 없을 때(고속도로 직진·안내 전)와 1/3 창에서 버튼이 있는 상태일 때: 목적지 · 남은 거리 · 도착 한 줄.
 * 빈 카드("안내를 기다리는 중")를 두지 않는다. 목적지도 없으면 "기본 내비에서 목적지를 정하면 보여요".
 */
@Composable
private fun ArriveLine(ui: DriveUi) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(Modifier.fillMaxWidth(), pad = d.u(16f)) {
        if (ui.arrive == null && ui.remain == null) {
            Text("기본 내비에서 목적지를 정하면 보여요", style = Pause.type.body, color = c.sub, maxLines = 2)
            return@Panel
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Flag, null, Modifier.size(d.u(24f)), tint = c.navy)
            Spacer(Modifier.width(d.unit))
            Text(
                listOfNotNull(ui.destination, ui.remain).joinToString(" · "),
                Modifier.weight(1f), style = Pause.type.bodyStrong.copy(lineBreak = LineBreak.Heading), color = c.text, maxLines = 2,
            )
            ui.arrive?.let {
                Spacer(Modifier.width(d.gapS))
                Text("도착 $it", style = Pause.type.bodyStrong, color = c.primary, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun GuideRow(g: GuideUi, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(d.u(if (compact) 64f else 80f)).clip(RoundedCornerShape(d.cardRadius)).background(c.navy), contentAlignment = Alignment.Center) {
            Icon(guideIcon(g.icon), guideWord(g.icon), Modifier.size(d.u(if (compact) 44f else 56f)), tint = c.surface)
        }
        Spacer(Modifier.width(d.gap))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(guideWord(g.icon), style = Pause.type.metricLabel, color = c.sub, maxLines = 1)
                if (g.demo) {
                    Spacer(Modifier.width(d.unit))
                    StatusChip("시연", neutral = true)
                }
            }
            Text(g.distance, style = Pause.type.metric, color = c.text, maxLines = 1)
            g.towards?.let { Text(it, style = Pause.type.bodyStrong, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

// ───────────── 콩이 관찰 상태 (보호자 음성 신고만) ─────────────

/** 관찰 상태 한 줄: 원형 사진 + "콩이 관찰 상태" + "관찰 기록 없음"/"신고 1건 · 헐떡임(출발 39분 뒤)". 주행 자극과 섞지 않는다. */
@Composable
private fun ObservationCard(ui: DriveUi, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(16f), spacing = d.u(4f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetPhoto(Modifier.size(d.u(48f)))
            Spacer(Modifier.width(d.gapS))
            Column(Modifier.weight(1f)) {
                Text("${ui.pet.ifEmpty { "반려견" }} 관찰 상태", style = Pause.type.metricLabel, color = c.sub, maxLines = 1)
                Text(
                    ui.observation.line, style = Pause.type.bodyStrong, color = if (ui.observation.count > 0) c.warningInk else c.text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ───────────── 예보 (경고 한 줄) ─────────────

/** 주행 자극 예보 (상태 3): 거리·시간 + 운전 안내. 숫자·그래프 없음. */
@Composable
private fun ForecastStrip(f: DriveContent.Forecast, modifier: Modifier) {
    val c = Pause.colors
    AlertStrip(modifier, icon = R.drawable.fe_warning, iconAsset = "ic_warning") {
        Text(f.title, style = Pause.type.h1, color = c.text, maxLines = 2)
        if (f.coach.isNotBlank()) Text(f.coach, style = Pause.type.bodyStrong, color = c.warningInk, maxLines = 2)
    }
}

// ───────────── 지금 할 일 패널 ─────────────

@Composable
private fun ContextPanel(ui: DriveUi, on: On, modifier: Modifier, compact: Boolean) {
    when (val ct = ui.content) {
        is DriveContent.Distress -> DistressPanel(ct, on, modifier, compact)
        is DriveContent.Suggest -> SuggestPanel(ui, ct, on, modifier, buttons = ui.voice != VoiceSheetState.LISTENING, compact = compact)
        is DriveContent.Nearby -> NearbyPanel(ui, ct, on, modifier, compact)
        is DriveContent.Vet -> VetPanel(ct, modifier)
        else -> NextRestPanel(ui, modifier, compact)
    }
}

/** 다음 쉼표: 남은 시간(수치) · 장소(2줄까지) · 확인된 반려견 시설 · 마지막 휴식 후. 계획이 없으면 한 줄 안내. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NextRestPanel(ui: DriveUi, modifier: Modifier, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val heading = ui.content as? DriveContent.Heading
    val minutes = heading?.minutes ?: ui.nextStopMin
    val place = heading?.place ?: ui.nextStopName
    // 쉼표 계획이 없으면 큰 빈 카드 대신 한 줄 (UI v9) — 그 자리는 주변 추천 카드가 쓴다
    if (minutes == null || place == null) {
        Panel(modifier, pad = d.u(16f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("다음 쉼표", style = Pause.type.metricLabel, color = c.sub, maxLines = 1)
                Spacer(Modifier.width(d.gapS))
                Text("남은 길에 잡힌 쉼표가 없어요", Modifier.weight(1f), style = Pause.type.bodyStrong, color = c.text, maxLines = 2)
            }
        }
        return
    }
    // 1/3 창 (UI v9): 내용 높이 카드라 관찰 상태가 아래로 밀리지 않게 한 줄씩 — "1시간 2분 뒤 · 94km" / 장소·시설 / 마지막 휴식 후
    if (compact) {
        Panel(modifier, pad = d.u(16f), spacing = d.u(4f)) {
            Text(if (heading != null) "가는 중인 쉼표" else "다음 쉼표", style = Pause.type.metricLabel, color = if (heading != null) c.primary else c.sub)
            Text(
                listOfNotNull("${hm(minutes)} 뒤", ui.nextStop?.removePrefix("약 ")?.takeIf { heading == null }).joinToString(" · "),
                style = Pause.type.section, color = if (heading != null) c.primary else c.text, maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(place, Modifier.weight(1f, fill = false), style = Pause.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (heading == null) ui.nextStopFacilities.firstOrNull()?.let {
                    Spacer(Modifier.width(d.unit))
                    StatusChip(it)
                }
            }
            ui.sinceRest?.let { Text(it, style = Pause.type.sub, color = c.sub, maxLines = 1) }
        }
        return
    }
    Panel(modifier, pad = d.u(20f), spacing = d.gapS) {
        Text(if (heading != null) "가는 중인 쉼표" else "다음 쉼표", style = Pause.type.metricLabel, color = if (heading != null) c.primary else c.sub)
        run {
            // 남은 시간(큰 수치) + 남은 거리 한 줄 → 장소(2줄까지)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(hm(minutes), style = Pause.type.metric, color = if (heading != null) c.primary else c.text, maxLines = 1)
                if (heading == null) ui.nextStop?.let {
                    Spacer(Modifier.width(d.gapS))
                    Text(it.removePrefix("약 "), style = Pause.type.section, color = c.sub, maxLines = 1, modifier = Modifier.padding(bottom = d.u(6f)))
                }
            }
            Text(place, style = Pause.type.title, color = c.text, maxLines = 2)
            if (ui.nextStopFacilities.isNotEmpty() && heading == null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                    ui.nextStopFacilities.forEach { StatusChip(it) }
                }
            }
        }
        ui.sinceRest?.let {
            Divider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("마지막 휴식 후", style = Pause.type.metricLabel, color = c.sub)
                Spacer(Modifier.weight(1f))
                Text(it.removePrefix("마지막 휴식 후 "), style = Pause.type.section, color = c.text, maxLines = 1)
            }
        }
    }
}

/** 쉼표 추천 (상태 4): 장소(2줄까지) → 남은 분·거리 → 이유 2개(확인된 시설만) → 버튼 2개 + 음성 힌트. */
@Composable
private fun SuggestPanel(ui: DriveUi, s: DriveContent.Suggest, on: On, modifier: Modifier, buttons: Boolean, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(20f), spacing = d.gapS) {
        Text(if (s.urgent) "운전자 쉬어 갈 때" else "쉼표 추천", style = Pause.type.metricLabel, color = if (s.urgent) c.warningInk else c.primary)
        Column {
            Text(s.place, style = Pause.type.title, color = c.text, maxLines = 2)
            if (!compact) s.direction?.let { Text(it, style = Pause.type.sub, color = c.sub, maxLines = 1) }
        }
        // 1/3 창은 큰 수치 두 칸이 높이를 넘어 잘린다 → "18분 뒤 · 8km" 한 줄 (11/01)
        if (compact) {
            Text(listOfNotNull("${s.minutes}분 뒤", s.distance).joinToString(" · "), style = Pause.type.section, color = c.text, maxLines = 1)
        } else Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            Metric("도착까지", "${s.minutes}", unit = "분", small = true)
            s.distance?.let { dist ->
                val num = dist.takeWhile { it.isDigit() || it == '.' }
                Metric("거리", num.ifEmpty { dist }, unit = dist.removePrefix(num).ifEmpty { null }, small = true)
            }
        }
        if (!ui.restricted) s.reasons.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(d.u(20f)), tint = c.primary)
                Spacer(Modifier.width(d.unit))
                Text(r, style = Pause.type.bodyStrong, color = c.text, maxLines = 2)
            }
        }
        if (buttons) {
            // 음성 실패 안내는 버튼 바로 위 작은 글자 (UI v9 4번)
            if (ui.voice == VoiceSheetState.FAILED) Text(VOICE_FAILED, style = Pause.type.sub, color = c.sub, maxLines = 1)
            // 좁은 창은 위아래로 (버튼 글자를 줄이지 않는다)
            if (compact) {
                PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.fillMaxWidth())
                if (!s.urgent) PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
            } else Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.weight(1f))
                if (!s.urgent) PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }
            if (ui.voice == null) VoiceHint(if (s.urgent) "“여기서 쉴게요”라고 말해도 돼요" else "“여기서 쉴게요”·“나중에”라고 말해도 돼요")
        }
    }
}

private const val VOICE_FAILED = "잘 못 들었어요. 버튼으로 골라 주세요."

private fun nearbyIcon(k: NearbyKindUi): ImageVector = when (k) {
    NearbyKindUi.WALK -> Icons.Rounded.Park
    NearbyKindUi.SHOP -> Icons.Rounded.ShoppingBag
    NearbyKindUi.VET -> Icons.Rounded.LocalHospital
}

private fun nearbyWord(k: NearbyKindUi): String = when (k) {
    NearbyKindUi.WALK -> "산책로·공원"
    NearbyKindUi.SHOP -> "반려견 동반 쇼핑"
    NearbyKindUi.VET -> "동물병원"
}

/**
 * 주변 추천 (UI v9 1번, 일반 도로에서만): 아이콘 + 종류 → "9.7km 앞 · 장소"(2줄까지) → 이유 한 줄 → 버튼 2개(들를게요·괜찮아요).
 * 음성은 쉼표 추천과 같은 응답 창 — 듣는 동안은 버튼을 숨기고, 실패하면 작은 안내 + 버튼.
 */
@Composable
private fun NearbyPanel(ui: DriveUi, n: DriveContent.Nearby, on: On, modifier: Modifier, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(20f), spacing = d.gapS) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(d.u(36f)).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
                Icon(nearbyIcon(n.kind), null, Modifier.size(d.u(22f)), tint = c.primary)
            }
            Spacer(Modifier.width(d.unit))
            Text("주변 추천", style = Pause.type.metricLabel, color = c.primary, maxLines = 1)
        }
        // 제목 = 장소 이름(2줄까지), 아래 "9.7km 앞 · 반려견 동반 쇼핑" — 거리와 이름을 한 줄에 붙여 "9.7km 앞 ·"에서 끊기지 않게 (11/03)
        Text(n.title, style = Pause.type.title.copy(lineBreak = LineBreak.Heading), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(n.line.ifBlank { nearbyWord(n.kind) }, style = Pause.type.section, color = c.text, maxLines = 1)
        if (n.reason.isNotBlank() && !ui.restricted) Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(d.u(20f)), tint = c.primary)
            Spacer(Modifier.width(d.unit))
            Text(n.reason, style = Pause.type.bodyStrong, color = c.text, maxLines = 2)
        }
        if (ui.voice != VoiceSheetState.LISTENING) {
            if (ui.voice == VoiceSheetState.FAILED) Text(VOICE_FAILED, style = Pause.type.sub, color = c.sub, maxLines = 1)
            if (compact) {
                PauseButton("들를게요", { on(UiEvent.AcceptNearby) }, Modifier.fillMaxWidth())
                PauseButton("괜찮아요", { on(UiEvent.DeclineNearby) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
            } else Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                PauseButton("들를게요", { on(UiEvent.AcceptNearby) }, Modifier.weight(1f))
                PauseButton("괜찮아요", { on(UiEvent.DeclineNearby) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }
            if (ui.voice == null) VoiceHint("“들를게요”·“괜찮아요”라고 말해도 돼요")
        }
    }
}

/** "콩이가 이상해요" (상태 5): 안내 2줄 + 장소·분 + 버튼 2개 + 하단 고정 문구. 애니메이션 없음. */
@Composable
private fun DistressPanel(dc: DriveContent.Distress, on: On, modifier: Modifier, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(20f), spacing = d.gapS) {
        AlertStrip(Modifier.fillMaxWidth(), icon = if (dc.vet) R.drawable.fe_hospital else R.drawable.fe_warning, iconAsset = if (dc.vet) "ic_hospital" else "ic_warning") {
            Text("신고를 기록했어요", style = Pause.type.metricLabel, color = c.warningInk)
            Text(dc.line, style = Pause.type.h1, color = c.text, maxLines = 2)
        }
        dc.target?.let { t ->
            Column {
                Text(if (dc.vet) "가까운 동물병원" else "가장 가까운 정차 장소", style = Pause.type.metricLabel, color = c.sub)
                Text(t, style = Pause.type.title, color = c.text, maxLines = 2)
                dc.minutes?.let { Text("약 ${it}분", style = Pause.type.section, color = c.text) }
            }
        }
        val primary = when {
            dc.vet -> "동물병원으로"
            dc.target != null -> "그곳으로"
            else -> "알겠어요"
        }
        if (compact) {
            PauseButton(primary, { on(UiEvent.AcceptDistress) }, Modifier.fillMaxWidth())
            PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
        } else Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
            PauseButton(primary, { on(UiEvent.AcceptDistress) }, Modifier.weight(1f))
            PauseButton("괜찮아졌어요", { on(UiEvent.DismissDistress) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
        }
        Text(CareSheet.DISCLAIMER, style = Pause.type.caption, color = c.sub)
    }
}

/** 동물병원 가는 길 (상태 7, 주행 중 1순위 한 곳) + 사전 접수 상태 한 줄. 버튼 없음. */
@Composable
private fun VetPanel(v: DriveContent.Vet, modifier: Modifier) {
    val c = Pause.colors
    val d = Pause.dimens
    Panel(modifier, pad = d.u(20f), spacing = d.gapS) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIcon("ic_hospital", R.drawable.fe_hospital, d.u(28f))
            Spacer(Modifier.width(d.unit))
            Text("동물병원 가는 길", style = Pause.type.metricLabel, color = c.primary)
        }
        Text(v.name, style = Pause.type.title, color = c.text, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            v.minutes?.let { Metric("도착까지", "$it", unit = "분", small = true) }
            val num = v.distance.takeWhile { it.isDigit() || it == '.' }
            Metric("거리", num.ifEmpty { v.distance }, unit = v.distance.removePrefix(num).ifEmpty { null }, small = true)
        }
        v.clinic?.let { Text(it, style = Pause.type.bodyStrong, color = c.primary, maxLines = 2) }
        Text("흔들림을 줄여 천천히 가요", style = Pause.type.body, color = c.sub, maxLines = 1)
    }
}

/** RESTRICTED: 가장 급한 한 줄만 (DS-03). */
@Composable
private fun RestrictedDrive(ui: DriveUi) {
    val c = Pause.colors
    val text = when (val ct = ui.content) {
        is DriveContent.Calm -> ui.guide?.let { "${it.distance} 뒤 ${guideWord(it.icon)}" } ?: ct.line
        is DriveContent.Forecast -> ct.title
        is DriveContent.Suggest -> "${ct.minutes}분 뒤 ${ct.place}"
        is DriveContent.Heading -> "${ct.minutes}분 뒤 ${ct.place}"
        is DriveContent.Vet -> "${ct.name} ${ct.distance}"
        is DriveContent.Distress -> ct.line
        is DriveContent.Nearby -> ct.title
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = Pause.type.title, color = c.text, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** 결과 한 줄 · 음성 시트(듣는 중 / 실패하면 버튼 2개) · 음성 힌트 · 오프라인 한 줄. */
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
        // 쉼표 추천·주변 추천은 카드 안에 실패 안내 + 버튼을 그린다 (UI v9 4번)
        VoiceSheetState.FAILED -> if (ui.content !is DriveContent.Suggest && ui.content !is DriveContent.Nearby) Panel(Modifier.fillMaxWidth(), pad = d.u(16f)) {
            Text("잘 못 들었어요. 버튼으로 골라 주세요.", style = Pause.type.section, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.weight(1f))
                PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }
        }
        null -> when {
            ui.restricted -> Unit
            !acting(ui) -> ui.voiceHint?.let { VoiceHint(it) }
            ui.offline -> Text("인터넷 없이 내장 데이터로 안내 중", style = Pause.type.sub, color = c.sub)
        }
    }
}

// ───────────── 미리보기 (2560×1440dp, 배율 2) — 1/3 창이 기본 ─────────────

@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PDriveThird() = PauseTheme(false, 2f) { DriveScreen(Fixtures.drive) {} }
@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PForecastThird() = PauseTheme(false, 2f) { DriveScreen(Fixtures.forecast) {} }
@Preview(widthDp = 1264, heightDp = 1210) @Composable private fun PSuggestHalf() = PauseTheme(false, 2f) { DriveScreen(Fixtures.suggest) {} }
@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PNearbyThird() = PauseTheme(false, 2f) { DriveScreen(Fixtures.nearby) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PDrive() = PauseTheme(false, 2f) { DriveScreen(Fixtures.drive) {} }
