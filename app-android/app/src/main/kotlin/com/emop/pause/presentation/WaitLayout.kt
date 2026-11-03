package com.emop.pause.presentation

import kotlin.math.min

/**
 * 잠깐 대기 화면 큰 글 크기 (11/03 "한눈에 크게", `_private/WAIT_PROMPT.md`) — 순수 함수라 창 폭별로 테스트한다.
 * 큰 글("주인이 곧 돌아와요"·"24℃")은 같은 크기로 **줄바꿈 없이** 한 줄: 기준 72(배율 1, metric 36의 2배)에서 시작해
 * 가장 긴 줄의 글자 폭(어림)이 쓸 수 있는 폭을 넘으면 줄인다.
 */
object WaitLayout {
    /** 큰 글 기준 크기 (배율 1, sp). */
    const val BIG = 72f

    /** 글자 폭 어림 (em): 한글 1.0, 숫자·영문 0.62, ℃ 0.9, 띄어쓰기 0.3, 그 밖 0.6. 실제 글꼴(Pretendard)보다 조금 넉넉하게. */
    fun em(text: String): Float = text.sumOf { ch ->
        when {
            ch == ' ' -> 0.3
            ch == '℃' -> 0.9
            ch in '가'..'힣' -> 1.0
            ch.isDigit() || ch in 'a'..'z' || ch in 'A'..'Z' -> 0.62
            else -> 0.6
        }
    }.toFloat()

    /** [availDp] 폭에 [lines]가 모두 한 줄로 들어가는 큰 글 크기(sp, 글꼴 배율 1에서 1sp = 1dp). 기준 [BIG]×[scale]을 넘지 않는다. */
    fun bigSp(availDp: Float, scale: Float, lines: List<String>): Float {
        val widest = lines.maxOfOrNull { em(it) }?.takeIf { it > 0f } ?: return BIG * scale
        return min(BIG * scale, availDp / widest)
    }

    /** 큰 글 [n]줄이 [availDp] 높이에 들어가는 크기(줄 높이 1.1배). 그래도 [floorSp](기존 metric 크기)보다 작게는 줄이지 않는다. */
    fun fitHeight(availDp: Float, n: Int, floorSp: Float): Float =
        if (n <= 0) Float.MAX_VALUE else maxOf(floorSp, availDp / (n * 1.1f))
}
