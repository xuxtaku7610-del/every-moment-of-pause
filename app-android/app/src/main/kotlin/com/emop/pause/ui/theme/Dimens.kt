package com.emop.pause.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 간격·모양 토큰 (UI_SPEC §2.3). 그림자는 쓰지 않고 색 차이로만 구분한다. */
@Immutable
data class PauseDimens(
    val gutter: Dp = 56.dp,
    val appBar: Dp = 88.dp,
    val cardPad: Dp = 40.dp,
    val cardPadSmall: Dp = 32.dp,
    val gap: Dp = 24.dp,
    val gapLarge: Dp = 32.dp,
    val cardRadius: Dp = 28.dp,
    val sheetRadius: Dp = 36.dp,
    val chip: Dp = 60.dp,
    val button: Dp = 112.dp,
    val buttonSmall: Dp = 96.dp,
    val touch: Dp = 96.dp,
    val buttonGap: Dp = 20.dp,
)

/** 창 폭 3단계 (UI_SPEC §5). */
enum class WidthClass { WIDE, HALF, THIRD;

    companion object {
        fun of(width: Dp): WidthClass = when {
            width >= 1200.dp -> WIDE
            width >= 700.dp -> HALF
            else -> THIRD
        }
    }
}

val LocalPauseDimens = staticCompositionLocalOf { PauseDimens() }
