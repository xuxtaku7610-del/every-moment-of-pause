package com.emop.pause.ui.components

import com.emop.pause.uistate.VoiceSheetState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme

/**
 * 멈춤 큐레이터 행 (UI_SPEC §3 ActivityRow): 높이 172dp, 아이콘 원 96dp, 대상(24sp)·활동명(40sp)·설명(24sp 한 줄), 오른쪽 버튼 96dp.
 * 추천 행은 4dp rest 안쪽 테두리 + "지금 가장 필요해요" 태그. 추천 행만 주 버튼, 나머지는 paper 배경 보조 버튼.
 */
@Composable
fun ActivityRow(
    who: String,
    title: String,
    description: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    action: String,
    onAction: () -> Unit,
    recommended: Boolean,
    modifier: Modifier = Modifier,
    badge: String? = null,
    enabled: Boolean = true,
) {
    val c = Pause.colors
    val shape = RoundedCornerShape(Pause.dimens.cardRadius)
    Row(
        modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(shape)
            .background(c.surface)
            .then(if (recommended) Modifier.border(4.dp, c.rest, shape) else Modifier)
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(48.dp), tint = iconTint)
        }
        Spacer(Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(who, style = Pause.type.captionStrong, color = c.ink2, maxLines = 1)
                if (recommended) {
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(c.rest).padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("지금 가장 필요해요", style = Pause.type.tag, color = Color.White, maxLines = 1)
                    }
                }
                if (badge != null) {
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(c.cautionSoft).padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(badge, style = Pause.type.tag, color = c.cautionInk, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(title, style = Pause.type.activity, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(description, style = Pause.type.caption, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(28.dp))
        PauseButton(
            action,
            onAction,
            kind = if (recommended) ButtonKind.PRIMARY else ButtonKind.QUIET,
            small = true,
            onCard = true,
            enabled = enabled,
        )
    }
}

/**
 * 음성 하단 시트 (UI_SPEC §4.9): 높이 360dp, 모서리 36dp, 2dp line 테두리.
 * 듣는 중: 파형 막대 8개(14dp) + "듣고 있어요". 실패: 마이크 원 + "잘 듣지 못했어요" + 버튼 2개(같은 동작).
 * 파형은 [level](0~1 마이크 음량)에 반응한다. 주행 중이라도 사용자 음성에 직접 반응하는 표시라 자동 애니메이션이 아니다.
 */
@Composable
fun VoiceSheet(
    state: VoiceSheetState,
    modifier: Modifier = Modifier,
    hint: String = "\"응\" 또는 \"나중에\"라고 말해 주세요",
    level: Float = 0.6f,
    primary: String = "여기서 쉴게요",
    secondary: String = "나중에",
    onPrimary: () -> Unit = {},
    onSecondary: () -> Unit = {},
    compact: Boolean = false,
) {
    val c = Pause.colors
    val shape = RoundedCornerShape(Pause.dimens.sheetRadius)
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        when (state) {
            VoiceSheetState.LISTENING -> {
                Waveform(level)
                Spacer(Modifier.width(56.dp))
                Column {
                    Text("듣고 있어요", style = Pause.type.sheet, color = c.ink)
                    Spacer(Modifier.height(14.dp))
                    Text(hint, style = Pause.type.body, color = c.ink2, maxLines = 2)
                }
            }
            VoiceSheetState.FAILED -> {
                Box(Modifier.size(120.dp).clip(CircleShape).background(c.sunk), contentAlignment = Alignment.Center) {
                    Icon(PauseIcons.Mic, null, Modifier.size(60.dp), tint = c.ink2)
                }
                Spacer(Modifier.width(40.dp))
                Column(Modifier.weight(1f)) {
                    Text("잘 듣지 못했어요", style = Pause.type.sheet, color = c.ink, maxLines = 1)
                    Spacer(Modifier.height(14.dp))
                    Text("버튼으로 골라 주세요", style = Pause.type.body, color = c.ink2, maxLines = 1)
                }
                if (!compact) {
                    Spacer(Modifier.width(24.dp))
                    PauseButton(primary, onPrimary)
                    Spacer(Modifier.width(16.dp))
                    PauseButton(secondary, onSecondary, kind = ButtonKind.QUIET, onCard = true)
                }
            }
        }
    }
    if (compact && state == VoiceSheetState.FAILED) {
        Column(
            modifier.fillMaxWidth().clip(shape).background(c.surface).border(2.dp, c.line, shape).padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) { content() }
            PauseButton(primary, onPrimary, Modifier.fillMaxWidth())
            PauseButton(secondary, onSecondary, Modifier.fillMaxWidth(), kind = ButtonKind.QUIET, onCard = true)
        }
    } else {
        Row(
            modifier.fillMaxWidth().height(360.dp).clip(shape).background(c.surface).border(2.dp, c.line, shape).padding(horizontal = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

private val BAR_HEIGHTS = floatArrayOf(40f, 84f, 120f, 70f, 104f, 50f, 90f, 36f)

@Composable
private fun Waveform(level: Float) {
    val c = Pause.colors
    // 실제 음량 값이 없을 때(에뮬레이터)는 정차 중에만 천천히 숨쉬듯 움직인다.
    val pulse = if (Pause.motion) {
        rememberInfiniteTransition(label = "wave").animateFloat(0.75f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "w").value
    } else {
        1f
    }
    val k = (0.4f + 0.6f * level.coerceIn(0f, 1f)) * pulse
    Row(Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BAR_HEIGHTS.forEach { h ->
            Box(Modifier.width(14.dp).height((h * k).coerceAtLeast(14f).dp).clip(RoundedCornerShape(7.dp)).background(c.rest))
        }
    }
}

@Preview(widthDp = 1500, heightDp = 1300, showBackground = true, backgroundColor = 0xFFEEF2F4)
@Composable
private fun SheetsPreview() = PauseTheme(dark = false) {
    Column(Modifier.padding(40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ActivityRow("운전자", "20분 파워냅", "충전이 끝나기 2분 전에 조명과 소리로 깨워 드릴게요.", PauseIcons.Moon, Pause.colors.restSoft, Pause.colors.rest, "눈 붙이기", {}, true)
        ActivityRow("지우", "휴게소 원정대 15분", "화장실, 물 한 컵, 기지개 3번. 다 하면 다음 라디오가 열려요.", PauseIcons.Flag, Pause.colors.kidSoft, Pause.colors.kidInk, "시작", {}, false)
        VoiceSheet(VoiceSheetState.LISTENING)
        VoiceSheet(VoiceSheetState.FAILED)
    }
}
