package com.emop.pause.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 간격·모양 토큰 v6 (STYLE_V6 §4). 기준값 × [scale]. 화면 외곽 24 · 패널 안 20 · 요소 사이 16/12/8. 모서리 14(패널)·12(버튼). 그림자 없음. */
@Immutable
data class PauseDimens(val scale: Float = 2f) {
    fun u(base: Float): Dp = (base * scale).dp
    val margin = u(24f)
    val gap = u(16f)
    val pad = u(20f)
    val unit = u(8f)
    val cardRadius = u(14f)
    val chipRadius = u(8f)
    val buttonRadius = u(12f)
    val buttonHeight = u(56f)
    val border = u(1f)
    val appBar = u(48f)
    val gapS = u(12f)

    /** 최소 터치 영역 — 배율과 상관없는 하한 (DS, 76dp). */
    val touch = 76.dp
}

/**
 * 창 폭 3단계 (STYLE_V5 §5). Pleos Connect 런처는 왼쪽에 차량 패널을 두어 앱 기본 영역이 약 1680dp다 → 이것이 FULL.
 * 화면 1/2 분할 ≈ 1280dp → HALF, 1/3 ≈ 853dp → THIRD.
 */
enum class WidthClass {
    FULL, HALF, THIRD;

    companion object {
        fun of(width: Dp): WidthClass = when {
            width >= 1500.dp -> FULL
            width >= 1000.dp -> HALF
            else -> THIRD
        }
    }
}

val LocalPauseDimens = staticCompositionLocalOf { PauseDimens() }
