package com.emop.pause.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 디자인 토큰. 차량 화면은 멀리서 보므로 글자·터치 타깃을 크게 잡는다. 다크가 기본. */
object PauseColors {
    val Night = Color(0xFF0E1116)
    val Surface = Color(0xFF1A1F27)
    val OnNight = Color(0xFFF2F4F7)
    val Muted = Color(0xFF9AA4B2)
    val Pause = Color(0xFFFFB547)      // 쉼표 포인트 색 (따뜻한 주황)
    val Calm = Color(0xFF5CC8B0)       // 정상·여유
    val Alert = Color(0xFFFF6B5E)      // 피로 높음

    val Day = Color(0xFFF7F8FA)
    val DaySurface = Color(0xFFFFFFFF)
    val OnDay = Color(0xFF12161C)
}

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 40.dp,
    /** 최소 터치 타깃. 주행 중 큰 버튼 1개 기준. */
    val touchTarget: Dp = 76.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

private val PauseTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 64.sp),
    headlineLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold, lineHeight = 44.sp),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    bodyLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, lineHeight = 32.sp),
    labelLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = PauseColors.Pause,
    onPrimary = PauseColors.Night,
    secondary = PauseColors.Calm,
    error = PauseColors.Alert,
    background = PauseColors.Night,
    onBackground = PauseColors.OnNight,
    surface = PauseColors.Surface,
    onSurface = PauseColors.OnNight,
    onSurfaceVariant = PauseColors.Muted,
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFB86E00),
    onPrimary = Color.White,
    secondary = Color(0xFF1E8C74),
    error = Color(0xFFC6392D),
    background = PauseColors.Day,
    onBackground = PauseColors.OnDay,
    surface = PauseColors.DaySurface,
    onSurface = PauseColors.OnDay,
    onSurfaceVariant = Color(0xFF5B6573),
)

@Composable
fun PauseTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = PauseTypography,
            content = content,
        )
    }
}
