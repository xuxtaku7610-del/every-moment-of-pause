package com.emop.pause.engine.pet

/**
 * 콩이 도우미 질문 분류 1단계 (HELPER_PROMPT 3번, 공알 `question_router` 구조를 같이가개에 맞게 새로 씀).
 * 순수 함수·오프라인·결정적. 순서가 우선순위:
 *   ① 응급 말(PetDistress 재사용) → 기존 EMERGENCY 흐름(병원 안내) — 분류·LLM보다 먼저, 막거나 늦추지 않는다.
 *   ② 앱 기능(다음 쉼표·쉬할 곳·실내 온도·대기·충전·병원) — "어디·언제·몇"처럼 앱 데이터로 답할 질문.
 *   ③ 돌봄 카드 triggers(띄어쓰기 무시, 맞은 말 길이 합이 큰 카드, 같으면 카드 순서).
 *   ④ 반려견 이상 말(PetDistress 나머지 종류) → 같은 이름의 카드.
 *   ⑤ 모름 — 위험 낱말(약·먹였·피·숨 …)이 있으면 병원 안내, 없으면 "다음 쉼터에서 살펴봐요".
 * 2단계(서버 임베딩·LLM)는 ⑤일 때만 부른다. 질문 원문은 저장하지 않는다.
 */
data class HelperCard(val id: String, val triggers: List<String>, val category: String? = null, val action: String? = null)

/** 카드 대신 앱이 가진 데이터로 답하는 질문. */
enum class HelperFeature { NEXT_REST, POTTY_SPOT, CABIN_TEMP, WAIT, CHARGING, VET }

sealed interface HelperRoute {
    /** 응급 — [cardId]는 응급 카드(breathing·seizure·bleeding·toxin, 종류를 못 고르면 emergency). */
    data class Emergency(val kind: EmergencyKind?, val cardId: String) : HelperRoute
    data class Card(val id: String) : HelperRoute
    data class App(val feature: HelperFeature) : HelperRoute
    data class Unknown(val risky: Boolean) : HelperRoute
}

object HelperRouter {
    private val FEATURES: List<Pair<HelperFeature, Regex>> = listOf(
        HelperFeature.VET to Regex("(동물\\s*)?병원\\s*(어디|찾아|알려|가까운|있어|근처)|(가까운|근처)\\s*(동물\\s*)?병원"),
        HelperFeature.POTTY_SPOT to Regex("(쉬|오줌|소변|배변|볼일|응가)\\s*(할|하|누일|눌|시킬|볼)\\s*(데|곳|만한|자리)|놀이터\\s*(있|어디)|(쉬|오줌)\\s*할\\s*데"),
        HelperFeature.NEXT_REST to Regex("다음\\s*(쉼표|휴게소|쉼터)|언제\\s*쉬|휴게소\\s*(언제|얼마|몇)|쉬는\\s*곳\\s*(언제|어디)"),
        HelperFeature.CABIN_TEMP to Regex("(실내|차\\s*안)\\s*(온도|몇\\s*도)|몇\\s*도(야|예요|에요|지)?\\??$|온도\\s*(알려|어때|몇)"),
        HelperFeature.WAIT to Regex("대기\\s*(몇|얼마)|(혼자|기다린\\s*지)\\s*(얼마|몇\\s*분)|몇\\s*분\\s*(됐|지났)"),
        HelperFeature.CHARGING to Regex("충전\\s*(언제|얼마|몇|끝|남)"),
    )

    /** 모를 때 병원 안내로 가야 하는 말 (약·처치·몸 상태). 이것만으로 응급은 아니다. */
    private val RISKY = Regex("약|먹였|먹여|진통|수면|주사|피|숨|아파|아프|다쳤|삼키|열이|토하|경련|붓|부었")

    private val QUESTION = Regex("\\?|어떻게|어떡|해도\\s*돼|돼\\s*\\?|괜찮(아|을)|뭐|어디|언제|있어|할까|하나요|알려|줘도|먹어도|얼마|몇|해야|돼요|되지|되나")

    private fun norm(s: String) = s.replace(Regex("\\s+"), "").lowercase()

    /** 질문처럼 들리는지 — 주행 중 [눌러서 말하기]에서 신고("콩이 토했어")와 도우미 질문("토하면 어떻게 해?")을 가른다. */
    fun isQuestion(text: String): Boolean = QUESTION.containsMatchIn(text)

    fun risky(text: String): Boolean = RISKY.containsMatchIn(text)

    fun route(text: String, cards: List<HelperCard>): HelperRoute {
        val t = text.trim()
        if (t.isEmpty()) return HelperRoute.Unknown(false)
        // ① 응급이 먼저 (기존 긴급 도움 흐름)
        if (PetDistress.classify(t) == DistressKind.EMERGENCY) {
            val kind = PetDistress.emergencyKind(t)
            return HelperRoute.Emergency(kind, kind?.id ?: "emergency")
        }
        // ② 앱 기능
        FEATURES.firstOrNull { (_, re) -> re.containsMatchIn(t) }?.let { return HelperRoute.App(it.first) }
        // ③ 카드 triggers
        val n = norm(t)
        val best = cards.mapIndexedNotNull { i, c ->
            val score = c.triggers.map(::norm).filter { it.isNotEmpty() && it in n }.sumOf { it.length }
            if (score > 0) Triple(c, score, i) else null
        }.maxWithOrNull(compareBy<Triple<HelperCard, Int, Int>> { it.second }.thenByDescending { it.third })
        if (best != null) {
            val c = best.first
            // 응급 카드(action VET·category emergency)를 triggers로 골랐으면 응급 흐름으로 (예: "열사병"·"장난감을 삼키면")
            return if (c.category == "emergency" && c.action == "VET" && !isQuestion(t)) HelperRoute.Emergency(null, c.id) else HelperRoute.Card(c.id)
        }
        // ④ 반려견 이상 말 → 같은 이름 카드
        PetDistress.classify(t)?.let { kind ->
            val id = when (kind) {
                DistressKind.VOMIT -> "vomit"
                DistressKind.NAUSEA -> "nausea"
                DistressKind.PANTING -> "panting_anxious"
                DistressKind.ANXIOUS -> "anxious"
                DistressKind.CLIMBING -> "climbing"
                DistressKind.UNSURE -> "unsure"
                DistressKind.EMERGENCY -> "emergency"
            }
            if (cards.any { it.id == id }) return HelperRoute.Card(id)
        }
        return HelperRoute.Unknown(risky(t))
    }

    /** "[a, b, c]" → 목록 (카드 머리말 triggers). */
    fun parseList(raw: String?): List<String> =
        raw?.trim()?.removePrefix("[")?.removeSuffix("]")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    /** 카드 머리말만 읽는다 (engine은 화면 모듈을 모른다). 형식이 틀리면 null. */
    fun parseCard(fileId: String, text: String): HelperCard? {
        val t = text.replace("\r\n", "\n")
        if (!t.startsWith("---\n")) return null
        val end = t.indexOf("\n---", 4).takeIf { it > 0 } ?: return null
        val head = t.substring(4, end).lines().mapNotNull { l ->
            val i = l.indexOf(':')
            if (i <= 0) null else l.substring(0, i).trim() to l.substring(i + 1).trim()
        }.toMap()
        return HelperCard(head["id"] ?: fileId, parseList(head["triggers"]), head["category"], head["action"])
    }
}
