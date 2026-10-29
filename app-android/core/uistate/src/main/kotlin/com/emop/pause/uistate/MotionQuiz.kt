package com.emop.pause.uistate

/**
 * 온보딩 3단계 "멀미 민감도 진단" (11번 화면). 질문 3개 → 멀미 성향 3단계.
 * 의학적 진단이 아니라 멀미 지수 계수(MotionTendency)를 고르는 도움말이다 — 결과는 프로필에서 언제든 바꿀 수 있다.
 * 답 점수: 0(없음) · 1(가끔) · 2(자주). 합 0~1 → LOW, 2~3 → NORMAL, 4 이상 → HIGH.
 */
object MotionQuiz {
    val questions = listOf(
        "차에서 침을 흘리거나 토한 적이 있나요?",
        "차에 타면 헐떡이거나 낑낑대나요?",
        "30분쯤 타고 나면 기운이 없어 보이나요?",
    )
    val options = listOf("없어요", "가끔", "자주")

    /** 답이 모두 있으면 결과, 아니면 null. */
    fun result(answers: List<Int?>): TendencyUi? {
        if (answers.size < questions.size || answers.any { it == null }) return null
        val sum = answers.sumOf { it!!.coerceIn(0, 2) }
        return when {
            sum >= 4 -> TendencyUi.HIGH
            sum >= 2 -> TendencyUi.NORMAL
            else -> TendencyUi.LOW
        }
    }

    fun label(t: TendencyUi) = when (t) {
        TendencyUi.LOW -> "멀미 거의 안 해요"
        TendencyUi.NORMAL -> "가끔 해요"
        TendencyUi.HIGH -> "멀미에 예민해요"
    }

    fun ui(answers: List<Int?>): List<QuizUi> = questions.mapIndexed { i, q -> QuizUi(q, options, answers.getOrNull(i)) }
}
