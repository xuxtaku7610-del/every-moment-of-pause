package com.emop.pause.engine.pet

/** 운전자가 말한 반려견의 모습 (UPGRADE 7). */
enum class DistressKind { PANTING, NAUSEA, VOMIT, ANXIOUS, CLIMBING, EMERGENCY, UNSURE }

/**
 * 응급(EMERGENCY)의 상황 종류 (긴급 도움, 2026-11-03). [id]는 응급 카드 content/care/care_<id>.md.
 * 분류는 [PetDistress.emergencyKind] — 종류를 못 고르면(예: "계속 토해") null, 그때는 기존 응급 카드(emergency).
 */
enum class EmergencyKind(val id: String, val label: String) {
    BREATHING("breathing", "호흡 곤란"),
    SEIZURE("seizure", "경련·의식 이상"),
    BLEEDING("bleeding", "외상·출혈"),
    TOXIN("toxin", "위험 물질 섭취");

    companion object {
        fun of(id: String?): EmergencyKind? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

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

    /**
     * 응급 상황 종류별 말 (긴급 도움). 순서가 우선순위: 호흡 → 경련·의식 → 외상·출혈 → 위험 물질 (여러 말이 겹치면 앞쪽).
     * "멀미약 먹였어"처럼 보호자가 준 것은 위험 물질로 보지 않는다("먹었"·"삼켰"만).
     */
    private val EMERGENCY_KINDS: List<Pair<EmergencyKind, Regex>> = listOf(
        EmergencyKind.BREATHING to Regex("숨(을|이)?\\s*(못|안\\s*쉬|너무|가빠|가쁘|차|막)|숨\\s*쉬기\\s*(가\\s*)?(힘들|어려)|호흡|잇몸(이|색이)?\\s*(파래|파랗|퍼래|보라|하얘|하얗)|혀(가|색이)?\\s*(파래|파랗|보라)|목에\\s*(뭐가\\s*)?걸|켁켁"),
        EmergencyKind.SEIZURE to Regex("경련|발작|쓰러|기절|정신(을|이)?\\s*(잃|없|못\\s*차)|의식|축\\s*처|눈이\\s*풀|반응(이|을)?\\s*(없|안\\s*해)"),
        EmergencyKind.BLEEDING to Regex("피(가|를)|피\\s*(나|흘)|출혈|다쳤|다친|상처|물렸|물려서|찢어|절뚝|못\\s*걸어|부러졌|부러진|차에\\s*치|치였"),
        // 음식 이름은 먹는 말과 함께일 때만, 주격 조사("포도가 밥 먹었어")는 빼고 ("초코"·"포도"는 흔한 반려견 이름이라 "초코"는 쓰지 않는다).
        // 쥐약·살충제·부동액은 말만 나와도.
        EmergencyKind.TOXIN to Regex("(초콜(릿|렛)|포도|양파|마늘|자일리톨|껌|세제|담배|꽁초|사람\\s*약|약)[을를도]?[^,.?!]{0,10}?(먹었|먹은|먹어\\s*버|삼켰|삼킨|주워\\s*먹|씹어\\s*먹|핥아\\s*먹)|쥐약|살충제|부동액|이상한\\s*(걸|것을?)\\s*(먹|삼)|뭘\\s*(주워\\s*)?(먹|삼)었"),
    )

    /** 순서가 우선순위: 응급 → 앞좌석 → 구토 → 헐떡임 → 침·하품 → 낑낑·떨림 → 막연히 이상함. */
    private val RULES: List<Pair<DistressKind, Regex>> = listOf(
        DistressKind.EMERGENCY to Regex("축\\s*처|쓰러|기절|경련|발작|숨(을|이)?\\s*(못|너무|가빠|가쁘|차)|계속\\s*토|또\\s*토|자꾸\\s*토|반복|의식"),
        // 긴급 도움 4가지 말 (기존 말 뒤에 더한 것 — 기존 말의 분류 결과는 그대로다)
        DistressKind.EMERGENCY to Regex(EMERGENCY_KINDS.joinToString("|") { "(?:${it.second.pattern})" }),
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

    /** 응급 말이면 상황 종류 (긴급 도움 카드). 응급이 아니거나 종류를 못 고르면 null. */
    fun emergencyKind(text: String): EmergencyKind? {
        val t = text.trim()
        if (classify(t) != DistressKind.EMERGENCY) return null
        return EMERGENCY_KINDS.firstOrNull { (_, re) -> re.containsMatchIn(t) }?.first
    }

    fun hot(s: DistressSignals): Boolean = s.cabinTempC?.let { it >= CabinHeat.CAUTION_C } ?: (s.month in CabinHeat.SUMMER)

    /** [emergency]는 응급일 때만 본다: 문구·행동은 같고 정차 후 카드만 상황별 응급 카드(care_<id>). */
    fun advise(kind: DistressKind, s: DistressSignals, emergency: EmergencyKind? = null): DistressAdvice = when (kind) {
        DistressKind.EMERGENCY -> DistressAdvice(kind, "가까운 동물병원으로 안내할게요", DistressAction.VET, emergency?.id ?: "emergency", 100)
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

    /** 이동 기록·도착 예정 공유에 남길 이름. 응급은 상황 종류가 있으면 그 이름("위험 물질 섭취"). */
    fun label(kind: DistressKind, emergency: EmergencyKind?): String =
        if (kind == DistressKind.EMERGENCY && emergency != null) emergency.label else label(kind)

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
