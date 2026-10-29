package com.emop.pause.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 색 토큰 (UI_SPEC §2.1, mockups.html :root). 강조색(rest)은 한 화면에 한 곳만 쓴다. */
@Immutable
data class PauseColors(
    val paper: Color,
    val sunk: Color,
    val surface: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val rest: Color,
    val restPressed: Color,
    val restSoft: Color,
    val restInk: Color,
    val caution: Color,
    val cautionSoft: Color,
    val cautionInk: Color,
    val alert: Color,
    val alertSoft: Color,
    val alertInk: Color,
    val kidSoft: Color,
    val kidInk: Color,
    val petSoft: Color,
    val petInk: Color,
    val sky: Color,
    val hill: Color,
    val hill2: Color,
    val sun: Color,
    val road: Color,
    val roadDash: Color,
    val scrim: Color,
    val isDark: Boolean,
) {
    companion object {
        val Light = PauseColors(
            paper = Color(0xFFEEF2F4),
            sunk = Color(0xFFE3E9EC),
            surface = Color(0xFFFFFFFF),
            ink = Color(0xFF13191D),
            ink2 = Color(0xFF55616A),
            ink3 = Color(0xFF8A959D),
            line = Color(0xFFD3DBE0),
            rest = Color(0xFF0B7A6B),
            restPressed = Color(0xFF0A6A5D),
            restSoft = Color(0xFFDCEFEB),
            restInk = Color(0xFF064A40),
            caution = Color(0xFFB97F05),
            cautionSoft = Color(0xFFFBF1D9),
            cautionInk = Color(0xFF7A5300),
            alert = Color(0xFFC2410C),
            alertSoft = Color(0xFFFDE7DC),
            alertInk = Color(0xFF5B3A2A),
            kidSoft = Color(0xFFE6EEFA),
            kidInk = Color(0xFF2D4E8A),
            petSoft = Color(0xFFF6E9D7),
            petInk = Color(0xFF8A5A1E),
            sky = Color(0xFFDDEBF3),
            hill = Color(0xFFC9DFD9),
            hill2 = Color(0xFFA9CDC2),
            sun = Color(0xFFF7F3E3),
            road = Color(0xFFF4F6F7),
            roadDash = Color(0xFFC7D3D9),
            scrim = Color(0x4713191D), // 28% (UI_SPEC §4.9)
            isDark = false,
        )

        val Dark = PauseColors(
            paper = Color(0xFF111417),
            sunk = Color(0xFF252B31),
            surface = Color(0xFF1C2126),
            ink = Color(0xFFEEF1F4),
            ink2 = Color(0xFFA3ADB8),
            ink3 = Color(0xFF76818A),
            line = Color(0xFF2E353C),
            rest = Color(0xFF3CC2AD),
            restPressed = Color(0xFF33A895),
            restSoft = Color(0xFF173A35),
            restInk = Color(0xFFBFEDE5),
            caution = Color(0xFFE8B23A),
            cautionSoft = Color(0xFF3A2E12),
            cautionInk = Color(0xFFF3D58F),
            alert = Color(0xFFF2804A),
            alertSoft = Color(0xFF3E2116),
            alertInk = Color(0xFFF5C9B4),
            kidSoft = Color(0xFF1F2A3D),
            kidInk = Color(0xFFA9C2EC),
            petSoft = Color(0xFF392C1C),
            petInk = Color(0xFFE6C193),
            sky = Color(0xFF1B2A33),
            hill = Color(0xFF1F3833),
            hill2 = Color(0xFF26473F),
            sun = Color(0xFF3B3828),
            road = Color(0xFF2A3036),
            roadDash = Color(0xFF46505A),
            scrim = Color(0x8C000000),
            isDark = true,
        )
    }
}

/** 파워냅 전용 어두운 난로 테마 (UI_SPEC §4.4). 라이트·다크와 상관없이 항상 같다. */
object NapColors {
    val night = Color(0xFF1C1713)
    val night2 = Color(0xFF2A221C)
    val glow = Color(0xFF5A2D12)
    val ember = Color(0xFFF0A04B)
    val emberInk = Color(0xFFF6E7D6)
    val emberOn = Color(0xFF2A1607)
    val clock = Color(0xFFFBEEDF)
    val muted = Color(0xFFC9B6A2)
    val brand = Color(0xFFB8A797)
    val dimButton = Color(0x14FFFFFF)
    val dimButtonInk = Color(0xFFE9D9C8)
    val track = Color(0x1AFFFFFF)
}

val LocalPauseColors = staticCompositionLocalOf { PauseColors.Light }
