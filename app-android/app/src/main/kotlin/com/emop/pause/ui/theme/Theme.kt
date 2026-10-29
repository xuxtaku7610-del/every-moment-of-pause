package com.emop.pause.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 움직임 허용 여부. AppMode = PAUSE 이고 DriveState = PARKED, 그리고 사용자가 움직임 줄이기를 끄지 않았을 때만 true
 * (architecture §5 최우선 규칙). 주행 중 화면은 항상 false.
 */
val LocalMotionAllowed = staticCompositionLocalOf { false }

/** 화면에서 토큰을 꺼내 쓰는 곳: `Pause.colors.primary`, `Pause.type.h1`, `Pause.dimens.margin`. */
object Pause {
    val colors: PauseColors @Composable @ReadOnlyComposable get() = LocalPauseColors.current
    val type: PauseType @Composable @ReadOnlyComposable get() = LocalPauseType.current
    val dimens: PauseDimens @Composable @ReadOnlyComposable get() = LocalPauseDimens.current
    val motion: Boolean @Composable @ReadOnlyComposable get() = LocalMotionAllowed.current
}

/**
 * 디자인 기준값 배율 (STYLE_V5 §0): 화면 전체 폭(dp) / 1280, 1~2. 분할 창이어도 화면 전체 폭으로 정하므로 글자 크기는 그대로다.
 * [screenWidthDp]는 창 폭이 아니라 화면 폭이어야 한다 — 호출하는 쪽(Activity)이 WindowMetrics로 넘긴다.
 */
fun designScale(screenWidthDp: Int): Float = (screenWidthDp / 1280f).coerceIn(1f, 2f)

/** 기본은 라이트. 시스템 다크 모드면 다크 토큰. */
@Composable
fun PauseTheme(dark: Boolean = isSystemInDarkTheme(), scale: Float? = null, content: @Composable () -> Unit) {
    val colors = if (dark) PauseColors.Dark else PauseColors.Light
    val s = scale ?: designScale(LocalConfiguration.current.screenWidthDp)
    val m3 = if (dark) {
        darkColorScheme(primary = colors.primary, onPrimary = colors.onPrimary, background = colors.bg, surface = colors.surface, onSurface = colors.text, onBackground = colors.text)
    } else {
        lightColorScheme(primary = colors.primary, onPrimary = colors.onPrimary, background = colors.bg, surface = colors.surface, onSurface = colors.text, onBackground = colors.text)
    }
    CompositionLocalProvider(
        LocalPauseColors provides colors,
        LocalPauseType provides PauseType(s),
        LocalPauseDimens provides PauseDimens(s),
    ) {
        MaterialTheme(colorScheme = m3, content = content)
    }
}
