package com.emop.pause.engine.pet

/** 운전자가 말한 반려견의 모습 (UPGRADE 7). */
enum class DistressKind { PANTING, NAUSEA, VOMIT, ANXIOUS, CLIMBING, EMERGENCY, UNSURE }

/** 다음 행동. */
enum class DistressAction {
    /** 가장 가까운 휴게소·졸음쉼터. */
    NEAREST_STOP,
    /** 부드러운 주행 안내(멀미 예보 민감도 잠시 올림). */
    GENTLE_DRIVE,
    /** 다음 쉼표(휴게소) 앞당김. */
    EARLIER_REST,
    /** 동물병원 가는 길 + 도착 예정 공유(증상 첨부). */
    VET,
}

/** 분류할 때 함께 보는 지금 신호. 실내 온도는 측정값만(없으면 null — 그때는 계절만 본다). */
data class DistressSignals(
    val cabinTempC: Float?,
    val month: Int,
    /** 이번 여행 주행 분 (출발 직후 판단). */
    val drivingMin: Int,
    val motionIndex: Int,
    /** 최근 멀미 지수가 오르는 중. */
    val motionRising: Boolean,
)

data class DistressAdvice(
    val kind: DistressKind,
    /** 운전자에게 한 줄 (주행 중 화면 1줄 + 음성). */
    val line: String,
    val action: DistressAction,
    /** 정차 후 체크리스트 파일 이름(content/care/care_<care>.md). */
    val care: String,
    /** 클수록 먼저 (여러 말이 겹치면 높은 쪽). */
    val priority: Int,
    /** 멀미 예보 민감도에 잠시 곱할 값 (부드러운 주행). 1.0 = 그대로. */
    val boost: Double = 1.0,
)

/**
 * "콩이가 이상해요" — 주행 중 반려견 이상 대처 (UPGRADE 7). **진단이 아니라 일반 안내 + 가장 가까운 안전한 정차 장소.**
 * 규칙 기반: 말(키워드) → [DistressKind], 그 종류와 지금 신호(실내 온도·계절·출발 뒤 시간·멀미 지수) → [DistressAdvice].
 * 운전자는 뒤돌아보거나 손을 뻗지 않고 말로만 알리고, 화면은 2줄 + 버튼 2개(주행 중 제한).
 * 문구 근거와 정차 후 체크리스트는 content/care 폴더의 md 파일 (검수 전 reviewed=false). 약 이름·용량은 쓰지 않는다.
 */
object PetDistress {
    /** 주행 중 안내 최대 글자 = 2줄 (DrivingRules.MAX_BLOCK_CHARS와 같다). 정차 장소 이름은 버튼에 싣는다. */
    const val LINE_MAX = 48

    /** 출발 직후로 보는 주행 분 (불안 헐떡임 판단). */
    const val EARLY_MIN = 20

    /** 순서가 우선순위: 응급 → 앞좌석 → 구토 → 헐떡임 → 침·하품 → 낑낑·떨림 → 막연히 이상함. */
    private val RULES: List<Pair<DistressKind, Regex>> = listOf(
        DistressKind.EMERGENCY to Regex("축\\s*처|쓰러|기절|경련|발작|숨(을|이)?\\s*(못|너무|가빠|가쁘|차)|계속\\s*토|또\\s*토|자꾸\\s*토|반복|의식"),
        DistressKind.CLIMBING to Regex("앞\\s*(좌석|자리)|운전석|앞으로\\s*넘어|넘어오|넘어와|기어\\s*나와"),
        DistressKind.VOMIT to Regex("토했|토해|토하|토할|구토|게워"),
        DistressKind.PANTING to Regex("헥헥|헉헉|헐떡|숨을\\s*몰아|혀를\\s*길게"),
        DistressKind.NAUSEA to Regex("침\\s*(을|흘)|침이|입맛|하품|느끼|멀미|입술을\\s*핥"),
        DistressKind.ANXIOUS to Regex("낑낑|끙끙|떨|서성|불안|안절부절|짖"),
        DistressKind.UNSURE to Regex("이상해|이상한|괜찮은지|아픈\\s*것|아파"),
    )

    /** 말 → 종류. 관련 없는 말(응답 "응"·"나중에" 등)은 null. */
    fun classify(text: String): DistressKind? {
        val t = text.trim()
        if (t.isEmpty()) return null
        return RULES.firstOrNull { (_, re) -> re.containsMatchIn(t) }?.first
    }

    fun hot(s: DistressSignals): Boolean = s.cabinTempC?.let { it >= CabinHeat.CAUTION_C } ?: (s.month in CabinHeat.SUMMER)

    fun advise(kind: DistressKind, s: DistressSignals): DistressAdvice = when (kind) {
        DistressKind.EMERGENCY -> DistressAdvice(kind, "가까운 동물병원으로 안내할게요", DistressAction.VET, "emergency", 100)
        DistressKind.CLIMBING -> DistressAdvice(kind, "안전한 곳에 먼저 멈춰요", DistressAction.NEAREST_STOP, "climbing", 90)
        DistressKind.VOMIT -> DistressAdvice(kind, "다음 쉼터에서 멈춰요. 지금은 운전에 집중해요", DistressAction.NEAREST_STOP, "vomit", 70)
        DistressKind.PANTING -> when {
            hot(s) -> DistressAdvice(kind, "에어컨을 낮추고, 다음 안전한 곳에서 멈춰요", DistressAction.NEAREST_STOP, "panting_hot", 80)
            s.drivingMin <= EARLY_MIN -> DistressAdvice(kind, "불안해서 그럴 수 있어요. 부드럽게 달릴게요", DistressAction.GENTLE_DRIVE, "panting_anxious", 40, boost = 1.3)
            // 표에 없는 경우(온도 정상, 출발한 지 오래): 이어지면 쉬도록 다음 쉼표를 앞당긴다
            else -> DistressAdvice(kind, "헐떡임이 이어지면 다음 쉼터에서 쉬어요", DistressAction.EARLIER_REST, "panting_anxious", 50)
        }
        DistressKind.NAUSEA -> DistressAdvice(kind, "속이 불편할 수 있어요. 창문을 조금 열어요", DistressAction.EARLIER_REST, "nausea", 60, boost = 1.2)
        DistressKind.ANXIOUS -> DistressAdvice(kind, "정차 후 켄넬과 벨트를 확인해요", DistressAction.EARLIER_REST, "anxious", 30)
        DistressKind.UNSURE -> DistressAdvice(kind, "다음 쉼터에서 멈춰 살펴봐요", DistressAction.NEAREST_STOP, "unsure", 20)
    }

    /** 이동 기록·도착 예정 공유에 남길 이름. */
    fun label(kind: DistressKind): String = when (kind) {
        DistressKind.PANTING -> "헐떡임"
        DistressKind.NAUSEA -> "침 흘림·하품"
        DistressKind.VOMIT -> "구토"
        DistressKind.ANXIOUS -> "낑낑·떨림"
        DistressKind.CLIMBING -> "앞좌석으로 넘어옴"
        DistressKind.EMERGENCY -> "축 처짐·호흡 곤란"
        DistressKind.UNSURE -> "이상함"
    }
}
