package com.emop.pause.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 색 토큰 v6 (docs/design/v6/STYLE_V6.md §2 — v5 대체). 역할 고정, 강조색은 한 화면에 하나만 앞에 둔다:
 * 파랑(primary) = 주 버튼·경로, 앰버(warning) = 주의·위험 예보, 초록(comfort) = 편안·정상, 네이비 = 깊은 강조(제목 아이콘·차트 기준선),
 * 차분한 빨강(attention) = 이동 기록 "관찰 필요" 상태에만(v7, 아이콘·글자와 함께).
 * 상태는 색만으로 구분하지 않고 글자를 함께 쓴다. 그림자는 쓰지 않고 바탕 명도·얇은 테두리로 나눈다.
 */
@Immutable
data class PauseColors(
    val primary: Color,
    val primaryPressed: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    /** 깊은 강조 (#1C3150). 넓은 면적에는 쓰지 않는다. */
    val navy: Color,
    val bg: Color,
    val surface: Color,
    val elevated: Color,
    /** 패널 안 한 단계 낮은 면 (그래프 바탕·입력칸). */
    val surfaceAlt: Color,
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
    /** 이동 기록 "관찰 필요" 상태에만 (STYLE_V6 §2.1). 면은 [attentionSoft], 글자·아이콘은 [attentionInk]. */
    val attention: Color,
    val attentionInk: Color,
    val attentionSoft: Color,
    val road: Color,
    val roadEdge: Color,
    val land: Color,
    val sky: Color,
    val scrim: Color,
    val isDark: Boolean,
) {
    companion object {
        val Light = PauseColors(
            primary = Color(0xFF3478E5),
            primaryPressed = Color(0xFF2560C2),
            onPrimary = Color(0xFFFFFFFF),
            primarySoft = Color(0xFFEAF1FC),
            navy = Color(0xFF1C3150),
            bg = Color(0xFFF5F6F8),
            surface = Color(0xFFFFFFFF),
            elevated = Color(0xFFFFFFFF),
            surfaceAlt = Color(0xFFF0F2F5),
            text = Color(0xFF182230),
            sub = Color(0xFF687586),
            border = Color(0xFFDFE4EA),
            disabled = Color(0xFFB9C2CD),
            warning = Color(0xFFE99532),
            // 앰버 글자: 흰 바탕 대비 4.5:1 이상이 되게 진하게 (#E99532 글자는 2.4:1)
            warningInk = Color(0xFF9A5A10),
            warningSoft = Color(0xFFFCF1E3),
            comfort = Color(0xFF16866C),
            comfortSoft = Color(0xFFE4F2EE),
            danger = Color(0xFFB3261E),
            // 경고등 빨강보다 채도를 낮춘 벽돌색. 글자 대비 흰 바탕 6.4:1, 연한 면 5.6:1
            attention = Color(0xFFC4473D),
            attentionInk = Color(0xFFA63A31),
            attentionSoft = Color(0xFFFBECEA),
            road = Color(0xFF465366),
            roadEdge = Color(0xFFCDD4DD),
            land = Color(0xFFE8ECF0),
            sky = Color(0xFFF2F4F7),
            scrim = Color(0x47182230),
            isDark = false,
        )

        val Dark = PauseColors(
            primary = Color(0xFF5E97F0),
            primaryPressed = Color(0xFF82AEF4),
            onPrimary = Color(0xFF0E1420),
            primarySoft = Color(0xFF1B2A42),
            navy = Color(0xFFA9BCD8),
            bg = Color(0xFF0F141B),
            surface = Color(0xFF171E27),
            elevated = Color(0xFF1E2631),
            surfaceAlt = Color(0xFF1C242E),
            text = Color(0xFFEEF2F6),
            sub = Color(0xFF9AA7B6),
            border = Color(0xFF2B3542),
            disabled = Color(0xFF4F5B6A),
            warning = Color(0xFFF0A54B),
            warningInk = Color(0xFFF0A54B),
            warningSoft = Color(0xFF33261A),
            comfort = Color(0xFF3FB394),
            comfortSoft = Color(0xFF15302A),
            danger = Color(0xFFF2B8B5),
            attention = Color(0xFFE8857B),
            attentionInk = Color(0xFFF09B92),
            attentionSoft = Color(0xFF3A2220),
            road = Color(0xFF2C3746),
            roadEdge = Color(0xFF3A4555),
            land = Color(0xFF141B24),
            sky = Color(0xFF121821),
            scrim = Color(0x8C000000),
            isDark = true,
        )
    }
}

/** 앱 아이콘(12번) 색. 라이트·다크와 상관없이 같다. */
object IconColors {
    val bgTop = Color(0xFF3478E5)
    val bgBottom = Color(0xFF1C53A6)
    val car = Color(0xFFFAA627)
}

val LocalPauseColors = staticCompositionLocalOf { PauseColors.Light }
