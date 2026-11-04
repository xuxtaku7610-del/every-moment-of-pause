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
import com.emop.pause.ui.components.Block
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
import com.emop.pause.uistate.DrivingRules
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.GuideIconUi
import com.emop.pause.uistate.GuideUi
import com.emop.pause.uistate.NearbyKindUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.VoiceSheetState
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.components.Body
import com.emop.pause.ui.components.Caption
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize

/*
 * 주행 화면 v8 (2026-10-31 재정비, docs/states.md 상태 2~5·7): 분할 창(기본 내비 옆 1/3·1/2)이 기본이다.
 * 지도·길 그림은 그리지 않는다 — 지도는 Pleos 기본 내비가 맡고, 같이가개는 NaviHelper 실제 값(다음 회전·남은 거리·도착)과
 * 다음 쉼표, 콩이 관찰 상태(보호자 음성 신고만)를 보여 준다. 주행 자극 숫자·그래프는 주행 중에 없다(예보는 경고 한 줄).
 * 주행 중 규칙: 텍스트 2줄 · 버튼 2개 · 스크롤 없음 · 애니메이션 없음. 버튼 글자는 줄이지 않는다.
 */

@Composable
fun DriveScreen(ui: DriveUi, on: On) {
    val d = Pause.dimens
    // 앱 바 오른쪽 = 작은 한 줄 "27km · 도착 오전 11:45" (v10-1). 남은 시간·회전은 옆 기본 내비가 크게 보여 준다.
    Page(ui.barLine ?: ui.meta, on, scroll = false) { wc ->
        val compact = wc == WidthClass.THIRD
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (ui.restricted) RestrictedDrive(ui)
            // v10-1 (사용자 결정 11/04): 주인공은 기본 내비 — 평소에는 한 면(다음 쉼표 ─ 콩이 한 줄)만,
            // 필요할 때만 맨 위에 한 장(예보·쉼표 추천·주변 추천·이상해요·병원 가는 길). 1/3·1/2 창 같은 한 열, 글자 크기는 그대로.
            else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(d.gap)) {
                TopCard(ui, on, compact)
                // 버튼 카드가 뜨면 다음 쉼표 덩어리는 숨긴다 (쉼표 추천과 같은 내용 · 버튼 2개가 다 보이게). 콩이 한 줄은 남긴다
                CalmPanel(ui, showNextRest = !acting(ui), compact = compact)
                Spacer(Modifier.weight(1f))
            }
        }
        DriveFooter(ui, on)
    }
}

/** 버튼이 있는 카드가 떠 있는지 (쉼표 추천·이상해요·주변 추천). */
private fun acting(ui: DriveUi) = ui.content is DriveContent.Suggest || ui.content is DriveContent.Distress || ui.content is DriveContent.Nearby

/** 필요할 때만 맨 위 한 장. 평소(Calm·가는 중)는 아무것도 그리지 않는다 — 끝나면 사라진다. */
@Composable
private fun TopCard(ui: DriveUi, on: On, compact: Boolean) {
    val m = Modifier.fillMaxWidth()
    when (val ct = ui.content) {
        is DriveContent.Forecast -> ForecastStrip(ct, m)
        is DriveContent.Suggest -> SuggestPanel(ui, ct, on, m, buttons = ui.voice != VoiceSheetState.LISTENING, compact = compact)
        is DriveContent.Nearby -> NearbyPanel(ui, ct, on, m, compact)
        is DriveContent.Distress -> DistressPanel(ct, on, m, compact)
        is DriveContent.Vet -> VetPanel(ct, m)
        is DriveContent.Calm, is DriveContent.Heading -> Unit
    }
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

// ───────────── 평소: 다음 쉼표 ─ 콩이 한 줄 (한 면, 구분선으로만 나눈다) ─────────────

/**
 * 조용한 상태의 두 덩어리 (v10-1 1번): ① 다음 쉼표(장소 · 남은 시간/거리 · 놀이터 알약, 가는 중이면 파랑)
 * ② 콩이 한 줄(작은 사진 + "관찰 기록 없음"/"신고 n건 · 마지막"). 박스 하나에 구분선 — 카드를 겹겹이 두르지 않는다.
 */
@Composable
private fun CalmPanel(ui: DriveUi, showNextRest: Boolean, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    Block(Modifier.fillMaxWidth(), pad = d.u(20f), spacing = d.gapS) {
        if (showNextRest) {
            NextRest(ui, compact)
            Divider()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetPhoto(Modifier.size(d.u(40f)))
            Spacer(Modifier.width(d.gapS))
            Text(
                "${ui.pet.ifEmpty { "반려견" }} · ${ui.observation.line}", Modifier.weight(1f),
                style = Pause.type.body, color = if (ui.observation.count > 0) c.warningInk else c.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (ui.quiet) Text("조용히 모드 · 응급 안내와 쉼표 추천만 말해요", style = Pause.type.body, color = c.sub, maxLines = 2)
    }
}

/** 다음 쉼표: "다음 쉼표" 이름표 → 장소(큰 글, 1/3은 1줄·1/2는 2줄까지) → "1시간 2분 뒤 · 94km" + 놀이터 알약. 계획이 없으면 한 줄. */
@Composable
private fun NextRest(ui: DriveUi, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    val heading = ui.content as? DriveContent.Heading
    val minutes = heading?.minutes ?: ui.nextStopMin
    val place = heading?.place ?: ui.nextStopName
    if (minutes == null || place == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("다음 쉼표", style = Pause.type.caption, color = c.sub, maxLines = 1)
            Spacer(Modifier.width(d.gapS))
            Text("남은 길에 잡힌 쉼표가 없어요", Modifier.weight(1f), style = Pause.type.body, color = c.text, maxLines = 2)
        }
        return
    }
    // v10: 가장 중요한 한 가지 = 남은 시간(수치). 레이블·거리는 보조, 장소는 제목, 시설은 알약
    val tint = if (heading != null) c.primary else c.text
    Text(
        listOfNotNull(if (heading != null) "가는 중인 쉼표" else "다음 쉼표", ui.nextStop?.removePrefix("약 ")?.takeIf { heading == null }).joinToString(" · "),
        style = Pause.type.caption, color = if (heading != null) c.primary else c.sub, maxLines = 1,
    )
    Row(verticalAlignment = Alignment.Bottom) {
        Text(hm(minutes), style = Pause.type.metric, color = tint, maxLines = 1)
        Spacer(Modifier.width(d.unit))
        Text("뒤", style = Pause.type.unit, color = c.sub, maxLines = 1, modifier = Modifier.padding(bottom = d.u(5f)))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(place, Modifier.weight(1f, fill = false), style = Pause.type.title, color = c.text, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
        if (heading == null) ui.nextStopFacilities.firstOrNull()?.let {
            Spacer(Modifier.width(d.gapS))
            StatusChip(it)
        }
    }
}

// ───────────── 예보 (경고 한 줄) ─────────────

/** 주행 자극 예보 (상태 3): 거리·시간 + 운전 안내. 숫자·그래프 없음. */
@Composable
private fun ForecastStrip(f: DriveContent.Forecast, modifier: Modifier) {
    val c = Pause.colors
    AlertStrip(modifier, icon = R.drawable.fe_warning, iconAsset = "ic_warning") {
        Text(f.title, style = Pause.type.title, color = c.text, maxLines = 2)
        if (f.coach.isNotBlank()) Text(f.coach, style = Pause.type.body, color = c.warningInk, maxLines = 2)
    }
}

// ───────────── 맨 위 한 장 ─────────────

/** 쉼표 추천 (상태 4): 장소(2줄까지) → 남은 분·거리 → 이유 2개(확인된 시설만) → 버튼 2개 + 음성 힌트. */
@Composable
private fun SuggestPanel(ui: DriveUi, s: DriveContent.Suggest, on: On, modifier: Modifier, buttons: Boolean, compact: Boolean) {
    val c = Pause.colors
    val d = Pause.dimens
    // 맨 위 한 장은 연한 파랑 면(테두리 없음)으로 평소 면과 명도로만 나눈다 (v10-1, 피로 위험은 앰버)
    Block(modifier, tint = if (s.urgent) c.warningSoft else c.primarySoft, pad = d.u(20f), spacing = d.gapS) {
        Text(if (s.urgent) "운전자 쉬어 갈 때" else "쉼표 추천", style = Pause.type.caption, color = if (s.urgent) c.warningInk else c.primary)
        Column {
            Text(s.place, style = Pause.type.title, color = c.text, maxLines = 2)
            if (!compact) s.direction?.let { Text(it, style = Pause.type.caption, color = c.sub, maxLines = 1) }
        }
        // 1/3 창은 큰 수치 두 칸이 높이를 넘어 잘린다 → "18분 뒤 · 8km" 한 줄 (11/01)
        if (compact) {
            // v10: 남은 분 = 수치, 거리는 단위 크기 보조
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${s.minutes}분", style = Pause.type.metric, color = c.text, maxLines = 1)
                Spacer(Modifier.width(d.unit))
                Text(listOfNotNull("뒤", s.distance).joinToString(" · "), style = Pause.type.unit, color = c.sub, maxLines = 1, modifier = Modifier.padding(bottom = d.u(5f)))
            }
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
                Text(r, style = Pause.type.body, color = c.text, maxLines = 2)
            }
        }
        if (buttons) {
            // 음성 실패 안내는 버튼 바로 위 작은 글자 (UI v9 4번)
            if (ui.voice == VoiceSheetState.FAILED) Text(VOICE_FAILED, style = Pause.type.body, color = c.sub, maxLines = 2)
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
    Block(modifier, tint = c.primarySoft, pad = d.u(20f), spacing = d.gapS) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(d.u(36f)).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
                Icon(nearbyIcon(n.kind), null, Modifier.size(d.u(22f)), tint = c.primary)
            }
            Spacer(Modifier.width(d.unit))
            Text("주변 추천", style = Pause.type.caption, color = c.primary, maxLines = 1)
        }
        // 제목 = 장소 이름(2줄까지), 아래 "9.7km 앞 · 반려견 동반 쇼핑" — 거리와 이름을 한 줄에 붙여 "9.7km 앞 ·"에서 끊기지 않게 (11/03)
        Text(n.title, style = Pause.type.title.copy(lineBreak = LineBreak.Heading), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(n.line.ifBlank { nearbyWord(n.kind) }, style = Pause.type.body, color = c.text, maxLines = 1)
        if (n.reason.isNotBlank() && !ui.restricted) Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(d.u(20f)), tint = c.primary)
            Spacer(Modifier.width(d.unit))
            Text(n.reason, style = Pause.type.body, color = c.text, maxLines = 2)
        }
        if (ui.voice != VoiceSheetState.LISTENING) {
            if (ui.voice == VoiceSheetState.FAILED) Text(VOICE_FAILED, style = Pause.type.body, color = c.sub, maxLines = 2)
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
    Block(modifier, pad = d.u(20f), spacing = d.gapS) {
        AlertStrip(Modifier.fillMaxWidth(), icon = if (dc.vet) R.drawable.fe_hospital else R.drawable.fe_warning, iconAsset = if (dc.vet) "ic_hospital" else "ic_warning") {
            Text("신고를 기록했어요", style = Pause.type.caption, color = c.warningInk)
            Text(dc.line, style = Pause.type.title, color = c.text, maxLines = 2)
        }
        dc.target?.let { t ->
            Column {
                Text(if (dc.vet) "가까운 동물병원" else "가장 가까운 정차 장소", style = Pause.type.caption, color = c.sub)
                Text(t, style = Pause.type.title, color = c.text, maxLines = 2)
                dc.minutes?.let { Text("약 ${it}분", style = Pause.type.body, color = c.text) }
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
    Block(modifier, pad = d.u(20f), spacing = d.gapS) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIcon("ic_hospital", R.drawable.fe_hospital, d.u(28f))
            Spacer(Modifier.width(d.unit))
            Text("동물병원 가는 길", style = Pause.type.caption, color = c.primary)
        }
        Text(v.name, style = Pause.type.title, color = c.text, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(d.u(32f))) {
            v.minutes?.let { Metric("도착까지", "$it", unit = "분", small = true) }
            val num = v.distance.takeWhile { it.isDigit() || it == '.' }
            Metric("거리", num.ifEmpty { v.distance }, unit = v.distance.removePrefix(num).ifEmpty { null }, small = true)
        }
        v.clinic?.let { Text(it, style = Pause.type.body, color = c.primary, maxLines = 2) }
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
            style = Pause.type.body, color = c.surface, maxLines = 2, overflow = TextOverflow.Ellipsis,
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
            Text("듣고 있어요…", style = Pause.type.body, color = c.primary)
        }
        // 쉼표 추천·주변 추천은 카드 안에 실패 안내 + 버튼을 그린다 (UI v9 4번)
        VoiceSheetState.FAILED -> if (ui.content !is DriveContent.Suggest && ui.content !is DriveContent.Nearby) Block(Modifier.fillMaxWidth(), pad = d.u(16f)) {
            Text("잘 못 들었어요. 버튼으로 골라 주세요.", style = Pause.type.body, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                PauseButton("여기서 쉴게요", { on(UiEvent.AcceptSuggestion) }, Modifier.weight(1f))
                PauseButton("다음 휴게소로", { on(UiEvent.DeclineSuggestion) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }
        }
        null -> when {
            ui.restricted -> Unit
            // [눌러서 말하기] (v10-1 2번): 맨 아래 폭 전체 · 높이 76dp 이상. 버튼 2개 카드가 떠 있으면 숨기고 그 카드의 음성 응답이 대신한다
            DrivingRules.talkShown(ui) -> PauseButton(TALK_LABEL, { on(UiEvent.PushToTalk) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Mic)
            ui.talk && ui.micDenied && !acting(ui) -> VoiceHint(MIC_DENIED)
            ui.offline -> Text("인터넷 없이 내장 데이터로 안내 중", style = Pause.type.body, color = c.sub)
        }
    }
}

private const val TALK_LABEL = "눌러서 말하기"
private const val MIC_DENIED = "마이크 권한이 없어요 · 설정에서 허용하면 말로 기록해요"

// ───────────── 미리보기 (2560×1440dp, 배율 2) — 1/3 창이 기본 ─────────────

@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PDriveThird() = PauseTheme(false, 2f) { DriveScreen(Fixtures.drive) {} }
@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PForecastThird() = PauseTheme(false, 2f) { DriveScreen(Fixtures.forecast) {} }
@Preview(widthDp = 1264, heightDp = 1210) @Composable private fun PSuggestHalf() = PauseTheme(false, 2f) { DriveScreen(Fixtures.suggest) {} }
@Preview(widthDp = 841, heightDp = 1210) @Composable private fun PNearbyThird() = PauseTheme(false, 2f) { DriveScreen(Fixtures.nearby) {} }
@Preview(widthDp = 1680, heightDp = 1210) @Composable private fun PDrive() = PauseTheme(false, 2f) { DriveScreen(Fixtures.drive) {} }
