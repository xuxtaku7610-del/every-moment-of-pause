package com.emop.pause.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emop.pause.R

/** Pretendard (SIL OFL 1.1, licenses/Pretendard-OFL.txt). */
val Pretendard = FontFamily(
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/** 숫자는 항상 고정폭 (UI_SPEC §2.2). */
private const val TNUM = "tnum"

private fun style(size: Int, weight: FontWeight, tracking: TextUnit = 0.em, lineHeight: Float = 1.2f) = TextStyle(
    fontFamily = Pretendard,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = tracking,
    lineHeight = (size * lineHeight).sp,
    fontFeatureSettings = TNUM,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/**
 * 글꼴 토큰 (UI_SPEC §2.2 표). Pleos Connect는 160dpi라 시안 px = dp/sp.
 * 28sp 미만(caption·label)은 정차 화면에서만, 주행 화면 최소는 24sp(경로 라벨).
 */
@Immutable
data class PauseType(
    val display: TextStyle = style(200, FontWeight.ExtraBold, (-0.05).em, 0.88f),
    val displayUnit: TextStyle = style(72, FontWeight.Bold, (-0.02).em, 1f),
    val clock: TextStyle = style(150, FontWeight.Light, (-0.04).em, 1f),
    val hero: TextStyle = style(96, FontWeight.ExtraBold, (-0.04).em, 1.15f),
    val calm: TextStyle = style(88, FontWeight.ExtraBold, (-0.03).em, 1.15f),
    val onboard: TextStyle = style(72, FontWeight.ExtraBold, (-0.03).em, 1.15f),
    val headline: TextStyle = style(64, FontWeight.ExtraBold, (-0.03).em, 1.1f),
    val sentence: TextStyle = style(50, FontWeight.ExtraBold, (-0.03).em, 1.3f),
    val number: TextStyle = style(56, FontWeight.ExtraBold, 0.em, 1.1f),
    val sheet: TextStyle = style(48, FontWeight.ExtraBold, (-0.02).em, 1.2f),
    val level: TextStyle = style(48, FontWeight.ExtraBold, 0.em, 1.1f),
    val title: TextStyle = style(40, FontWeight.Bold, (-0.02).em, 1.25f),
    val activity: TextStyle = style(40, FontWeight.ExtraBold, (-0.02).em, 1.2f),
    val button: TextStyle = style(36, FontWeight.Bold, 0.em, 1.2f),
    val lead: TextStyle = style(32, FontWeight.SemiBold, 0.em, 1.3f),
    val item: TextStyle = style(30, FontWeight.Bold, 0.em, 1.3f),
    val body: TextStyle = style(28, FontWeight.Normal, 0.em, 1.45f),
    val bodyStrong: TextStyle = style(28, FontWeight.Bold, 0.em, 1.45f),
    val label: TextStyle = style(26, FontWeight.SemiBold, 0.em, 1.3f),
    val caption: TextStyle = style(24, FontWeight.Normal, 0.em, 1.35f),
    val captionStrong: TextStyle = style(24, FontWeight.SemiBold, 0.em, 1.35f),
    val tag: TextStyle = style(22, FontWeight.Bold, 0.em, 1.2f),
    val small: TextStyle = style(20, FontWeight.SemiBold, 0.em, 1.2f),
)

val LocalPauseType = staticCompositionLocalOf { PauseType() }
