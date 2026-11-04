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
 * 글자 토큰 v10 (docs/design/v10/STYLE_V10.md §1) — **4단계만**: 제목 · 수치 · 설명 · 보조. 기준값(sp) × [scale] (2560dp 화면에서 2).
 * 강조는 크기를 키우지 않고 단계를 올린다. 주행 중 최소 크기 = 설명. [unit]·[button]은 수치·버튼 부품 안에서만 쓴다.
 */
@Immutable
data class PauseType(val scale: Float = 2f) {
    /** 제목 24 SemiBold — 화면·카드 제목, 장소 이름, 경고 띠 첫 줄(이보다 크게 쓰지 않는다). */
    val title = style(24 * scale, 32 * scale, FontWeight.SemiBold)
    /** 수치 36 Bold — 가장 중요한 숫자 한두 개. 단위는 [unit]. */
    val metric = style(36 * scale, 44 * scale, FontWeight.Bold)
    /** 설명 16 Medium — 본문·값 한 줄·목록. */
    val body = style(16 * scale, 24 * scale, FontWeight.Medium)
    /** 보조 14 Regular — 레이블·출처·힌트·축. 색은 sub. */
    val caption = style(14 * scale, 20 * scale, FontWeight.Normal)
    /** 수치 옆 단위 (설명 크기). */
    val unit = style(16 * scale, 24 * scale, FontWeight.Medium)
    /** 버튼 글자 (설명 크기, SemiBold). */
    val button = style(16 * scale, 24 * scale, FontWeight.SemiBold)
}

val LocalPauseType = staticCompositionLocalOf { PauseType() }
