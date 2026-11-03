package com.emop.pause.uistate

/**
 * 정차 후 체크리스트 한 장 (UPGRADE 7) — content/care/care_ID.md를 읽은 것.
 * 머리말 `title`·`reviewed`·`sources` + `- ` 줄 3~4개. 검수 전([reviewed] = false)이면 화면에 "검수 전"을 붙인다.
 */
data class CareSheet(
    val id: String,
    val title: String,
    val reviewed: Boolean,
    val sources: String,
    val lines: List<String>,
    /** 주행 중 음성 한 줄 (40자 이하, 긴급 도움 카드부터). 없으면 null. */
    val voice: String? = null,
    /** 다음 행동 (VET·NEAREST_STOP 등, HELPER_PROMPT 2번 형식). 없으면 null. */
    val action: String? = null,
    val category: String? = null,
) {
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
            return CareSheet(
                id, title, head["reviewed"] == "true", head["sources"].orEmpty(), lines,
                voice = head["voice"]?.takeIf { it.isNotBlank() },
                action = head["action"]?.takeIf { it.isNotBlank() },
                category = head["category"]?.takeIf { it.isNotBlank() },
            )
        }
    }
}

/** 정차 중 "콩이 돌보기" 카드 (주행 중에는 보이지 않는다). */
data class CareUi(val title: String, val lines: List<String>, val reviewed: Boolean, val disclaimer: String = CareSheet.DISCLAIMER)

/**
 * 긴급 도움 응급 카드 (2026-11-03, `_private/EMERGENCY_PROMPT.md` 2번). 화면 위: 경고 띠 [LEAD] + 본문 2~3줄 + 출처 한 줄 + [NOTE].
 * **검수 전 카드(reviewed=false)는 [showDrafts]가 아니면(릴리스, 발표 모드 아님) 본문·출처를 보이지 않는다** — 경고 띠와 병원 버튼만.
 */
data class EmergencyCardUi(
    val id: String,
    val title: String,
    /** 경고 띠 다음 줄 (첫 줄 [LEAD]는 띠에 있으니 뺀다). 숨김이면 빈 목록. */
    val lines: List<String>,
    /** 출처 한 줄 (기관명만). 숨김이면 null. */
    val source: String?,
    val reviewed: Boolean,
    /** 검수 전이라 본문을 숨겼다 (릴리스). */
    val hidden: Boolean,
) {
    companion object {
        const val LEAD = "바로 동물병원에 연락해요"
        const val NOTE = "일반 안내예요, 진단이 아니에요"

        /** 디버그 빌드 또는 발표 모드일 때만 검수 전 본문을 보여 준다 (릴리스 일반 사용자는 검수된 카드만). */
        fun showDrafts(debugBuild: Boolean, demoMode: Boolean): Boolean = debugBuild || demoMode

        /** 출처 칸 → 기관명만 ("Merck Veterinary Manual · RSPCA"). 확인된 기관이 없으면 null. */
        fun sourceLine(sources: String): String? =
            listOf("Merck Veterinary Manual", "AVMA", "RSPCA").filter { it in sources }.takeIf { it.isNotEmpty() }?.joinToString(" · ", prefix = "출처: ")

        fun from(sheet: CareSheet, showDrafts: Boolean): EmergencyCardUi {
            val hidden = !sheet.reviewed && !showDrafts
            return EmergencyCardUi(
                id = sheet.id,
                title = sheet.title,
                lines = if (hidden) emptyList() else sheet.lines.filterNot { it.trimEnd('.') == LEAD },
                source = if (hidden) null else sourceLine(sheet.sources),
                reviewed = sheet.reviewed,
                hidden = hidden,
            )
        }
    }
}
