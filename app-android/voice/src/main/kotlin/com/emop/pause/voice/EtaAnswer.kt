package com.emop.pause.voice

import kotlin.math.roundToInt

/** "아직 멀었어?" 답에 쓰는 사실. 모두 엔진·내비에서 온 값이라 지어낸 숫자가 없다. */
data class EtaFacts(
    val destination: String,
    val remainMin: Int,
    val remainKm: Double,
    /** 도착 전 다음 쉼표 (수락했거나 계획한 곳). */
    val nextStopName: String? = null,
    val nextStopMin: Int? = null,
    /** 도착까지 남은 라디오 편수 (플레이리스트가 있을 때만). */
    val episodesLeft: Int? = null,
)

/**
 * "아직 멀었어?" 응답기 — 템플릿만 쓴다.
 * 아이용 답변은 실시간 LLM으로 만들지 않는다(CLAUDE.md 규칙 4): 아래 문장 틀이 검수된 고정 문구다.
 * 어른 답변은 시간·거리 중심 (test_plan T5-6).
 */
object EtaAnswer {
    fun answer(f: EtaFacts, tone: Tone): String = when (tone) {
        Tone.KID -> kid(f)
        Tone.ADULT -> adult(f)
    }

    private fun kid(f: EtaFacts): String {
        val dest = f.destination
        val stopBefore = f.nextStopName != null && f.nextStopMin != null && f.nextStopMin < f.remainMin
        return when {
            f.remainMin <= 0 -> "다 왔어! ${dest}${eda(dest)}."
            f.remainMin <= 10 -> "거의 다 왔어! ${f.remainMin}분만 기다리면 ${dest}${eda(dest)}."
            f.episodesLeft != null && f.episodesLeft > 0 -> "라디오 ${f.episodesLeft}편만 들으면 $dest 도착!"
            stopBefore -> "${f.nextStopMin}분 뒤에 ${short(f.nextStopName!!)}에서 한 번 쉬고 가자. 조금만 기다려 줘!"
            else -> "${dest}까지 ${duration(f.remainMin)} 남았어. 조금만 더 힘내자!"
        }
    }

    private fun adult(f: EtaFacts): String {
        if (f.remainMin <= 0) return "${f.destination}에 도착했어요."
        val km = if (f.remainKm >= 10) f.remainKm.roundToInt().toString() else "%.1f".format(f.remainKm).removeSuffix(".0")
        val head = "${f.destination}까지 ${duration(f.remainMin)}, ${km}km 남았어요."
        val stopBefore = f.nextStopName != null && f.nextStopMin != null && f.nextStopMin < f.remainMin
        return if (stopBefore) "$head 다음 쉼표는 ${f.nextStopMin}분 뒤 ${f.nextStopName}${ieyo(f.nextStopName!!)}." else head
    }

    fun duration(min: Int): String {
        val h = min / 60
        val m = min % 60
        return when {
            h == 0 -> "${m}분"
            m == 0 -> "${h}시간"
            else -> "${h}시간 ${m}분"
        }
    }

    /** "죽암휴게소" → "죽암휴게소" 그대로 두되 졸음쉼터는 "쉼터"로 줄인다(아이에게 낯선 말). */
    private fun short(name: String) = name.replace("졸음쉼터", "쉼터")

    private fun hasBatchim(word: String): Boolean {
        val c = word.trim().lastOrNull() ?: return false
        if (c !in '가'..'힣') return false
        return (c - '가') % 28 != 0
    }

    /** "부산이야" / "서울이야"… 받침 있으면 "이야", 없으면 "야". */
    private fun eda(word: String) = if (hasBatchim(word)) "이야" else "야"

    /** "죽암휴게소예요" / "칠곡휴게소예요" / "졸음쉼터예요"… 받침 있으면 "이에요". */
    private fun ieyo(word: String) = if (hasBatchim(word)) "이에요" else "예요"
}
