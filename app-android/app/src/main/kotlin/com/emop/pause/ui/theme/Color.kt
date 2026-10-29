package com.emop.pause.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 색 토큰 v5 (docs/design/v5/STYLE_V5.md §2, DESIGNER_NOTES.md). 역할 고정:
 * 파랑(primary) = 주 버튼·경로·체크, 앰버(warning) = 멀미 주의·온도 경고에만, 민트(comfort) = 편안 상태에만.
 * 상태는 색만으로 구분하지 않고 글자를 함께 쓴다.
 */
@Immutable
data class PauseColors(
    val primary: Color,
    val primaryPressed: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    val text: Color,
    val sub: Color,
    val border: Color,
    val disabled: Color,
    /** 그래프·아이콘 채움. 글자는 [warningInk]. */
    val warning: Color,
    val warningInk: Color,
    val warningSoft: Color,
    val comfort: Color,
    val comfortSoft: Color,
    /** "내 데이터 삭제"만. */
    val danger: Color,
    val road: Color,
    val roadEdge: Color,
    val land: Color,
    val sky: Color,
    val scrim: Color,
    val isDark: Boolean,
) {
    companion object {
        val Light = PauseColors(
            primary = Color(0xFF286FE5),
            primaryPressed = Color(0xFF1F5BC0),
            onPrimary = Color(0xFFFFFFFF),
            primarySoft = Color(0xFFE6EEFC),
            bg = Color(0xFFF4F5F7),
            surface = Color(0xFFFFFFFF),
            elevated = Color(0xFFFFFFFF),
            text = Color(0xFF1A2430),
            sub = Color(0xFF667384),
            border = Color(0xFFD6DBE2),
            disabled = Color(0xFFAEB6C2),
            warning = Color(0xFFF59E0B),
            warningInk = Color(0xFFA85C10),
            warningSoft = Color(0xFFFDE8CC),
            comfort = Color(0xFF137A60),
            comfortSoft = Color(0xFFDDF3EC),
            danger = Color(0xFFB3261E),
            road = Color(0xFF5B6676),
            roadEdge = Color(0xFFC9D2DC),
            land = Color(0xFFDCE9DF),
            sky = Color(0xFFE8F0FA),
            scrim = Color(0x471A2430),
            isDark = false,
        )

        val Dark = PauseColors(
            primary = Color(0xFF69A9FF),
            primaryPressed = Color(0xFF8BBDFF),
            onPrimary = Color(0xFF10151D),
            primarySoft = Color(0xFF1C2B44),
            bg = Color(0xFF10151D),
            surface = Color(0xFF19212D),
            elevated = Color(0xFF222D3B),
            text = Color(0xFFF4F6F8),
            sub = Color(0xFFAAB5C3),
            border = Color(0xFF3B4757),
            disabled = Color(0xFF5A6677),
            warning = Color(0xFFF4A344),
            warningInk = Color(0xFFF4A344),
            warningSoft = Color(0xFF3A2A17),
            comfort = Color(0xFF59C9A5),
            comfortSoft = Color(0xFF173A33),
            danger = Color(0xFFF2B8B5),
            road = Color(0xFF2A3442),
            roadEdge = Color(0xFF3B4757),
            land = Color(0xFF16241F),
            sky = Color(0xFF141D2A),
            scrim = Color(0x8C000000),
            isDark = true,
        )
    }
}

/** 앱 아이콘(12번) 색. 라이트·다크와 상관없이 같다. */
object IconColors {
    val bgTop = Color(0xFF286FE5)
    val bgBottom = Color(0xFF1C53A6)
    val car = Color(0xFFFAA627)
}

val LocalPauseColors = staticCompositionLocalOf { PauseColors.Light }
