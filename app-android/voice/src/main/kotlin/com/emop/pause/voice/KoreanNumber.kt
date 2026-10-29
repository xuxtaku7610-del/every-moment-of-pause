package com.emop.pause.voice

/**
 * 한국어 숫자 정규화 (COMMERCIAL_READINESS C P1). STT는 "5시간", "다섯 시간", "오 시간", "여섯시간 반"처럼 섞어서 준다.
 * 고유어(하나~스물넷)·한자어(일~이십사)·아라비아 숫자를 모두 0~99 정수로 바꾼다.
 */
object KoreanNumber {
    private val native = mapOf(
        "한" to 1, "하나" to 1, "두" to 2, "둘" to 2, "세" to 3, "셋" to 3, "석" to 3, "네" to 4, "넷" to 4, "넉" to 4,
        "다섯" to 5, "여섯" to 6, "일곱" to 7, "여덟" to 8, "아홉" to 9, "열" to 10, "스무" to 20, "스물" to 20,
    )
    private val sino = mapOf(
        "영" to 0, "공" to 0, "일" to 1, "이" to 2, "삼" to 3, "사" to 4, "오" to 5, "육" to 6, "륙" to 6,
        "칠" to 7, "팔" to 8, "구" to 9,
    )
    private val digits = Regex("""(\d{1,2})""")

    /** 첫 숫자 하나를 정수로. 못 찾으면 null. */
    fun first(text: String): Int? {
        val t = text.replace(" ", "")
        digits.find(t)?.let { return it.value.toInt() }
        return nativeAt(t) ?: sinoAt(t)
    }

    /** 고유어: "열한", "스물네", "다섯". 긴 단어부터 맞춘다. */
    private fun nativeAt(t: String): Int? {
        for (i in t.indices) {
            val tens = listOf("스물", "스무", "열").firstOrNull { t.startsWith(it, i) }
            if (tens != null) {
                val base = native.getValue(tens)
                val rest = t.substring(i + tens.length)
                val unit = native.keys.filter { it !in setOf("열", "스무", "스물") }.sortedByDescending { it.length }.firstOrNull { rest.startsWith(it) }
                return base + (unit?.let { native.getValue(it) } ?: 0)
            }
            val unit = native.keys.sortedByDescending { it.length }.firstOrNull { t.startsWith(it, i) }
            // "세"·"네"·"두"·"한"은 다른 낱말 속에도 흔하다 → 바로 뒤에 단위(시·분·살·번·개)가 올 때만 숫자로 본다.
            if (unit != null && (unit.length > 1 || t.getOrNull(i + 1) in UNIT_START)) return native.getValue(unit)
        }
        return null
    }

    /** 한자어: "이십사", "십", "오". 단위 바로 앞의 숫자열만 본다(예: "오 시간" → 5, "오늘"은 아님). */
    private fun sinoAt(t: String): Int? {
        for (i in t.indices) {
            var j = i
            var value = 0
            var cur = -1
            while (j < t.length) {
                val c = t[j].toString()
                when {
                    c == "십" -> {
                        value += (if (cur < 0) 1 else cur) * 10
                        cur = -1
                    }
                    c in sino && cur < 0 -> cur = sino.getValue(c)
                    else -> break
                }
                j++
            }
            if (j > i && t.getOrNull(j) in UNIT_START) return value + maxOf(cur, 0)
        }
        return null
    }

    private val UNIT_START = setOf('시', '분', '살', '번', '개')

    /** "몇 시간 잤는지" 답 → 시간(0.5 단위). "반"이 붙으면 +0.5. 범위(0~24 미만) 밖이면 null (test_plan T1-5 재질문). */
    fun sleepHours(text: String?): Float? {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return null
        val h = first(t) ?: return null
        val v = h + if ("반" in t) 0.5f else 0f
        return v.takeIf { it > 0f && it < 24f }
    }
}
