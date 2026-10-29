package com.emop.pause.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.emop.pause.R

/** Pretendard (SIL OFL 1.1, licenses/Pretendard-OFL.txt). Medium은 SemiBold 파일로 대신한다(묶은 굵기가 5개). */
val Pretendard = FontFamily(
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/** 숫자는 항상 고정폭. */
private const val TNUM = "tnum"

private fun style(size: Float, line: Float, weight: FontWeight) = TextStyle(
    fontFamily = Pretendard,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = line.sp,
    fontFeatureSettings = TNUM,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/**
 * 글자 토큰 v5 (STYLE_V5 §3). 디자이너 메모 기준값(Body 16/24 …) × [scale].
 * 2560dp 화면(Pleos Connect 에뮬레이터)에서 scale = 2. 주행 화면 최소 글자는 [label].
 */
@Immutable
data class PauseType(val scale: Float = 2f) {
    val display = style(32 * scale, 40 * scale, FontWeight.Bold)
    val number = style(44 * scale, 52 * scale, FontWeight.Bold)
    val h1 = style(28 * scale, 36 * scale, FontWeight.Bold)
    val h2 = style(20 * scale, 28 * scale, FontWeight.SemiBold)
    val body = style(16 * scale, 24 * scale, FontWeight.Normal)
    val bodyStrong = style(16 * scale, 24 * scale, FontWeight.SemiBold)
    val button = style(16 * scale, 24 * scale, FontWeight.Medium)
    val label = style(14 * scale, 20 * scale, FontWeight.Medium)
    val caption = style(12 * scale, 16 * scale, FontWeight.Medium)
}

val LocalPauseType = staticCompositionLocalOf { PauseType() }
