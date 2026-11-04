package com.emop.pause.trip

import com.emop.pause.engine.pet.HelperCard
import com.emop.pause.engine.pet.HelperFeature
import com.emop.pause.engine.pet.HelperRouter
import com.emop.pause.engine.pet.HelperRoute
import com.emop.pause.uistate.CareSheet
import com.emop.pause.uistate.EmergencyCardUi
import com.emop.pause.uistate.HelperAnswerUi
import com.emop.pause.uistate.HelperUi

/**
 * 콩이 도우미 답 만들기 (HELPER_PROMPT 1·3번) — 순수 함수. 답은 **검수된 돌봄 카드**와 **앱이 이미 가진 데이터**에서만.
 * - 릴리스([showDrafts] = false)는 검수 전 카드(reviewed: false)를 답으로 쓰지 않는다 → 카드 행동이 VET이면 병원 안내, 아니면 쉼터 안내.
 * - 응급 카드도 검수 전이면 릴리스에서 본문 없이 "바로 동물병원에 연락해요"만(긴급 도움과 같은 규칙).
 * - 주행 중 음성 = 카드 voice 줄 또는 첫 문장 40자 이하([HelperReply.voice]).
 */
data class HelperFacts(
    /** 다음 쉼표 (기존 쉼표 계획). */
    val nextRest: Pair<String, Int>? = null,
    /** 쉬할 곳: 앞길 반려견 놀이터 휴게소 하나 (이름, "12km 앞") — 공원 데이터는 [PottySpotProvider]로 나중에. */
    val pottySpot: PottySpot? = null,
    /** 실내 온도 측정값 (없으면 null — 지어내지 않는다). */
    val cabinTempC: Float? = null,
    /** 대기 중이면 (경과 분, 제한 분). */
    val wait: Pair<Int, Int>? = null,
    /** 충전 중이면 남은 분(모르면 -1). */
    val chargingMin: Int? = null,
)

/** 쉬할 곳 후보 하나 — 경유지로 넣을 수 있게 좌표까지. */
data class PottySpot(val id: String, val name: String, val distance: String, val lat: Double, val lng: Double, val note: String)

/** 쉬할 곳 공급자 (PT 2에서 공원 데이터를 붙인다). 지금 구현은 앞길 휴게소 중 반려견 놀이터가 확인된 곳. */
fun interface PottySpotProvider {
    fun nearest(): PottySpot?
}

data class HelperReply(val ui: HelperAnswerUi, val voice: String, val cardId: String?)

object HelperAnswers {
    const val VOICE_MAX = 40
    private val ACTION_LABEL = mapOf(
        "VET" to "동물병원 찾기", "NEAREST_STOP" to "가까운 정차 장소", "POTTY_SPOT" to "경유지로 넣기", "WAIT_MODE" to "잠깐 대기 켜기", "NEXT_REST" to "다음 쉼표 보기",
    )
    const val DRAFT_HIDDEN = "이 안내는 아직 검수 중이에요."

    fun voiceOf(lines: List<String>, voice: String? = null): String =
        (voice?.takeIf { it.length <= VOICE_MAX } ?: lines.firstOrNull { it.length <= VOICE_MAX } ?: lines.firstOrNull()?.take(VOICE_MAX)).orEmpty()

    fun unknown(question: String, risky: Boolean): HelperReply {
        val line = if (risky) HelperUi.UNKNOWN_VET else HelperUi.UNKNOWN_REST
        return HelperReply(
            HelperAnswerUi(question, HelperUi.UNKNOWN, listOf(line), action = if (risky) "VET" else "NEXT_REST", actionLabel = ACTION_LABEL[if (risky) "VET" else "NEXT_REST"]),
            "${HelperUi.UNKNOWN} $line", null,
        )
    }

    /** 카드 답. [explained] = 서버가 카드만 근거로 풀어 쓰고 근거 검사를 통과한 문장(없으면 카드 줄 그대로). */
    fun card(question: String, sheet: CareSheet?, showDrafts: Boolean, explained: String? = null, emergency: Boolean = false): HelperReply {
        if (sheet == null) return unknown(question, emergency)
        if (!sheet.reviewed && !showDrafts) {
            // 릴리스: 검수 전 카드 본문을 답으로 쓰지 않는다 → 병원/쉼터 안내로
            val vet = sheet.action == "VET" || emergency
            val line = if (vet) EmergencyCardUi.LEAD else HelperUi.UNKNOWN_REST
            return HelperReply(
                HelperAnswerUi(question, if (vet) sheet.title else HelperUi.UNKNOWN, listOf(DRAFT_HIDDEN, line).let { if (vet) listOf(line) else it },
                    action = if (vet) "VET" else "NEXT_REST", actionLabel = ACTION_LABEL[if (vet) "VET" else "NEXT_REST"], reviewed = false, emergency = vet),
                if (vet) EmergencyCardUi.LEAD else "$DRAFT_HIDDEN $line", sheet.id,
            )
        }
        val lines = explained?.split(Regex("(?<=[.!?])\\s+"))?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: sheet.lines
        val action = sheet.action?.takeIf { it != "NONE" && it in ACTION_LABEL }
        return HelperReply(
            HelperAnswerUi(question, sheet.title, lines.take(4), action, action?.let { ACTION_LABEL[it] }, source = sheet.sources.substringBefore(" (").takeIf { it.isNotBlank() },
                reviewed = sheet.reviewed, explained = explained != null, emergency = emergency || sheet.category == "emergency"),
            voiceOf(sheet.lines, sheet.voice), sheet.id,
        )
    }

    /** 앱 데이터 답 (카드 대신). [card]는 같이 보여 줄 안전 카드(실내 온도·충전 중 산책). */
    fun app(question: String, f: HelperFeature, facts: HelperFacts, card: (String) -> CareSheet?, showDrafts: Boolean): HelperReply = when (f) {
        HelperFeature.NEXT_REST -> facts.nextRest?.let { (name, min) ->
            HelperReply(HelperAnswerUi(question, "다음 쉼표", listOf("$name · ${hm(min)} 뒤")), "다음 쉼표는 $name, ${hm(min)} 뒤예요.", null)
        } ?: HelperReply(HelperAnswerUi(question, "다음 쉼표", listOf("남은 길에 잡힌 쉼표가 없어요")), "남은 길에 잡힌 쉼표가 없어요.", null)
        HelperFeature.POTTY_SPOT -> facts.pottySpot?.let { p ->
            HelperReply(HelperAnswerUi(question, "쉬할 곳", listOf("${p.name} · ${p.distance}", p.note), action = "POTTY_SPOT", actionLabel = ACTION_LABEL["POTTY_SPOT"]), "${p.name}, ${p.distance}에 있어요.".take(VOICE_MAX), null)
        } ?: HelperReply(HelperAnswerUi(question, "쉬할 곳", listOf("앞길에 반려견 놀이터가 확인된 쉼터가 없어요", HelperUi.UNKNOWN_REST)), "앞길에 확인된 놀이터가 없어요.", null)
        HelperFeature.CABIN_TEMP -> {
            val safe = visible(card("wait_heat"), showDrafts)
            val first = facts.cabinTempC?.let { "지금 실내 ${Math.round(it)}℃예요" } ?: "이 차는 실내 온도를 알려 주지 않아요"
            HelperReply(HelperAnswerUi(question, "실내 온도", listOf(first) + safe?.lines.orEmpty().take(2), source = safe?.sources?.substringBefore(" ("), reviewed = safe?.reviewed ?: true), "$first.".take(VOICE_MAX), safe?.id)
        }
        HelperFeature.WAIT -> facts.wait?.let { (el, lim) ->
            HelperReply(HelperAnswerUi(question, "잠깐 대기", listOf("${el}분째 · 제한 ${lim}분")), "대기 ${el}분째예요.", null)
        } ?: HelperReply(HelperAnswerUi(question, "잠깐 대기", listOf("지금은 대기 중이 아니에요"), action = "WAIT_MODE", actionLabel = ACTION_LABEL["WAIT_MODE"]), "지금은 대기 중이 아니에요.", null)
        HelperFeature.CHARGING -> {
            val walk = visible(card("charging_walk"), showDrafts)
            val first = facts.chargingMin?.let { if (it >= 0) "충전 중 · 약 ${it}분 남았어요" else "충전 중이에요" } ?: "지금은 충전 중이 아니에요"
            HelperReply(HelperAnswerUi(question, "충전", listOf(first) + walk?.lines.orEmpty().take(2), source = walk?.sources?.substringBefore(" ("), reviewed = walk?.reviewed ?: true), "$first.".take(VOICE_MAX), walk?.id)
        }
        HelperFeature.VET -> HelperReply(HelperAnswerUi(question, "가까운 동물병원", listOf("가까운 동물병원을 거리순으로 보여 드려요"), action = "VET", actionLabel = ACTION_LABEL["VET"]), "가까운 동물병원을 찾을게요.", null)
    }

    /** 라우팅 결과 → 답 (서버 설명 없이). */
    fun build(question: String, route: HelperRoute, facts: HelperFacts, card: (String) -> CareSheet?, showDrafts: Boolean): HelperReply = when (route) {
        is HelperRoute.Emergency -> card(question, card(route.cardId), showDrafts, emergency = true)
        is HelperRoute.Card -> card(question, card(route.id), showDrafts)
        is HelperRoute.App -> app(question, route.feature, facts, card, showDrafts)
        is HelperRoute.Unknown -> unknown(question, route.risky)
    }

    /**
     * 도우미가 고를 수 있는 카드 목록 (응급 카드 먼저). **릴리스([showDrafts] = false)는 reviewed: false 카드를 아예 빼서** 규칙 분류도
     * 서버 허용 목록(allowed)도 검수 전 카드를 고르지 못한다 (HelperAnswersTest F9-30).
     */
    fun catalog(texts: Map<String, String>, showDrafts: Boolean): List<HelperCard> = texts.entries.sortedBy { it.key }.mapNotNull { (id, text) ->
        val sheet = CareSheet.parse(id, text) ?: return@mapNotNull null
        if (!sheet.reviewed && !showDrafts) null else HelperRouter.parseCard(id, text)
    }.sortedBy { if (it.category == "emergency") 0 else 1 }

    private fun visible(s: CareSheet?, showDrafts: Boolean) = s?.takeIf { it.reviewed || showDrafts }

    private fun hm(min: Int) = if (min < 60) "${min}분" else if (min % 60 == 0) "${min / 60}시간" else "${min / 60}시간 ${min % 60}분"
}
