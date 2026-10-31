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
 * 글자 토큰 v6 (docs/design/v6/STYLE_V6.md §3 — v5 대체). 기준값(sp) × [scale] (2560dp 화면에서 2).
 * 주요 수치 36 Bold · 화면 제목 24 SemiBold · 섹션 18 SemiBold · 본문 16 · 보조 14 · 캡션 12.
 * 수치는 크기만 키우지 않고 [metricLabel](레이블)·[unit](단위)과 함께 쓴다.
 */
@Immutable
data class PauseType(val scale: Float = 2f) {
    /** 주요 수치 (멀미 지수·남은 시간). */
    val metric = style(36 * scale, 44 * scale, FontWeight.Bold)
    /** 보조 수치 (KPI 줄, 그래프 옆 값). */
    val metricSmall = style(26 * scale, 32 * scale, FontWeight.Bold)
    /** 단위·수치 옆 작은 글자 ("/100", "분"). */
    val unit = style(16 * scale, 24 * scale, FontWeight.SemiBold)
    val title = style(24 * scale, 32 * scale, FontWeight.SemiBold)
    val section = style(18 * scale, 26 * scale, FontWeight.SemiBold)
    val body = style(16 * scale, 24 * scale, FontWeight.Normal)
    val bodyStrong = style(16 * scale, 24 * scale, FontWeight.SemiBold)
    val button = style(16 * scale, 24 * scale, FontWeight.SemiBold)
    /** 보조 설명 14. */
    val sub = style(14 * scale, 20 * scale, FontWeight.Normal)
    /** 수치 레이블 ("멀미 지수") 14 Medium. */
    val metricLabel = style(14 * scale, 20 * scale, FontWeight.Medium)
    val caption = style(12 * scale, 16 * scale, FontWeight.Medium)

    // v5 이름 (발표 모드 패널·비교 화면이 쓰는 것) → v6 값
    val display get() = title
    val number get() = metric
    val h1 get() = section.copy(fontSize = (20 * scale).sp, lineHeight = (28 * scale).sp)
    val h2 get() = section
    val label get() = metricLabel
}

val LocalPauseType = staticCompositionLocalOf { PauseType() }
