package com.emop.pause.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 움직임 허용 여부. AppMode = PAUSE 이고 DriveState = PARKED, 그리고 사용자가 움직임 줄이기를 끄지 않았을 때만 true
 * (UI_SPEC §3 Fireplace·VoiceHint, architecture §5 최우선 규칙). 주행 중 화면은 항상 false.
 */
val LocalMotionAllowed = staticCompositionLocalOf { false }

/** 화면에서 토큰을 꺼내 쓰는 곳: `Pause.colors.rest`, `Pause.type.display`. */
object Pause {
    val colors: PauseColors @Composable @ReadOnlyComposable get() = LocalPauseColors.current
    val type: PauseType @Composable @ReadOnlyComposable get() = LocalPauseType.current
    val dimens: PauseDimens @Composable @ReadOnlyComposable get() = LocalPauseDimens.current
    val motion: Boolean @Composable @ReadOnlyComposable get() = LocalMotionAllowed.current
}

/** 기본은 라이트(Pleos 런처가 밝음). 시스템 다크 모드면 다크 토큰 (UI_SPEC §1, §2.1). */
@Composable
fun PauseTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) PauseColors.Dark else PauseColors.Light
    val m3 = if (dark) {
        darkColorScheme(primary = colors.rest, background = colors.paper, surface = colors.surface, onSurface = colors.ink, onBackground = colors.ink)
    } else {
        lightColorScheme(primary = colors.rest, background = colors.paper, surface = colors.surface, onSurface = colors.ink, onBackground = colors.ink)
    }
    CompositionLocalProvider(
        LocalPauseColors provides colors,
        LocalPauseType provides PauseType(),
        LocalPauseDimens provides PauseDimens(),
    ) {
        MaterialTheme(colorScheme = m3, content = content)
    }
}
