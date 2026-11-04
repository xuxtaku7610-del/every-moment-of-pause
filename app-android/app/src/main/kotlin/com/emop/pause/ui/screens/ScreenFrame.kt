package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.emop.pause.ui.components.AppBar
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.WidthClass
import com.emop.pause.uistate.UiEvent
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.components.Body
import com.emop.pause.ui.components.Caption
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize

/*
 * 같이가개 화면 v6 공통 틀 (docs/design/v6/STYLE_V6.md §5).
 * 모든 화면 = 앱 바(48) + 본문(바깥 여백 24, 영역 사이 16). 창 폭(WidthClass)에 따라 배치만 바꾸고 글자 크기는 그대로.
 */

internal typealias On = (UiEvent) -> Unit

/** 시뮬레이터·장면 고정 값을 그리는 중 → 앱 바에 "시연 데이터" (재정비 원칙 5, AppUiState.demoData). */
val LocalDemoData = staticCompositionLocalOf { false }

/** 바깥 틀. 주행 화면은 scroll = false (DS-02). [title]이 있으면 앱 바에 화면 이름(위로 연 화면). */
@Composable
internal fun Page(
    meta: String?,
    on: On,
    scroll: Boolean,
    background: Color? = null,
    title: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.(WidthClass) -> Unit,
) {
    val c = Pause.colors
    val d = Pause.dimens
    BoxWithConstraints(Modifier.fillMaxSize().background(background ?: c.bg)) {
        // 창 단계는 배율 2 기준 dp로 잰다 — 1920×1080(배율 1.5)에서도 전체 창이 FULL (v7 검수)
        val wc = WidthClass.of(maxWidth * (2f / d.scale))
        Column(Modifier.fillMaxSize()) {
            AppBar(meta, { on(UiEvent.LogoTap) }, title = title, trailing = trailing, demo = LocalDemoData.current)
            val body = Modifier.fillMaxWidth().weight(1f).padding(start = d.margin, end = d.margin, bottom = d.margin, top = d.u(4f))
            Column(if (scroll) body.verticalScroll(rememberScrollState()) else body, verticalArrangement = Arrangement.spacedBy(d.gap)) { content(wc) }
        }
    }
}

@Composable
internal fun CloseButton(on: On) {
    Spacer(Modifier.width(Pause.dimens.gap))
    PauseButton("닫기", { on(UiEvent.Close) }, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Close, compact = true)
}

/** 아이콘 + 한 줄 (보조 정보). */
@Composable
internal fun IconLine(icon: ImageVector, text: String, color: Color = Pause.colors.sub, maxLines: Int = 1, modifier: Modifier = Modifier) {
    val d = Pause.dimens
    Row(modifier, verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top) {
        Icon(icon, null, Modifier.size(d.u(20f)), tint = color)
        Spacer(Modifier.width(d.unit))
        Text(text, style = Pause.type.body, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun VoiceHint(text: String) {
    val d = Pause.dimens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Mic, null, Modifier.size(d.u(18f)), tint = Pause.colors.sub)
        Spacer(Modifier.width(d.u(6f)))
        Text(text, style = Pause.type.caption, color = Pause.colors.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "2시간 58분". */
internal fun hm(min: Int): String = when {
    min < 60 -> "${min}분"
    min % 60 == 0 -> "${min / 60}시간"
    else -> "${min / 60}시간 ${min % 60}분"
}

/** 넓으면 [first]:나머지 비율로 나란히, 좁으면(1/2·1/3 창) 위아래로. 각 칸은 받은 Modifier를 쓴다. */
@Composable
internal fun SideBySide(wide: Boolean, first: Float, a: @Composable (Modifier) -> Unit, b: @Composable (Modifier) -> Unit) {
    val gap = Pause.dimens.gap
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            a(Modifier.weight(first))
            b(Modifier.weight(1f - first))
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
            a(Modifier.fillMaxWidth())
            b(Modifier.fillMaxWidth())
        }
    }
}
