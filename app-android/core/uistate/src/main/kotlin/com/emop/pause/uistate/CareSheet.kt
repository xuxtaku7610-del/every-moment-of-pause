package com.emop.pause.uistate

/**
 * 정차 후 체크리스트 한 장 (UPGRADE 7) — content/care/care_ID.md를 읽은 것.
 * 머리말 `title`·`reviewed`·`sources` + `- ` 줄 3~4개. 검수 전([reviewed] = false)이면 화면에 "검수 전"을 붙인다.
 */
data class CareSheet(val id: String, val title: String, val reviewed: Boolean, val sources: String, val lines: List<String>) {
    companion object {
        /** 모든 체크리스트 화면 아래에 고정 (진단 아님). */
        const val DISCLAIMER = "일반 안내예요. 심하면 동물병원에 연락하세요"

        fun parse(id: String, text: String): CareSheet? {
            val t = text.replace("\r\n", "\n")
            if (!t.startsWith("---\n")) return null
            val end = t.indexOf("\n---", 4)
            if (end < 0) return null
            val head = t.substring(4, end).lines().mapNotNull { l ->
                val i = l.indexOf(':')
                if (i <= 0) null else l.substring(0, i).trim() to l.substring(i + 1).trim()
            }.toMap()
            val lines = t.substring(end + 4).lines().map { it.trim() }.filter { it.startsWith("- ") }.map { it.removePrefix("- ").trim() }
            val title = head["title"] ?: return null
            return CareSheet(id, title, head["reviewed"] == "true", head["sources"].orEmpty(), lines)
        }
    }
}

/** 정차 중 "콩이 돌보기" 카드 (주행 중에는 보이지 않는다). */
data class CareUi(val title: String, val lines: List<String>, val reviewed: Boolean, val disclaimer: String = CareSheet.DISCLAIMER)
