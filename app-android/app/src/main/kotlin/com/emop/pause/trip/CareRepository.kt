package com.emop.pause.trip

import android.content.Context
import com.emop.pause.engine.pet.HelperCard
import com.emop.pause.uistate.CareSheet
import com.emop.pause.uistate.CareUi
import com.emop.pause.uistate.EmergencyCardUi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 정차 후 체크리스트 (UPGRADE 7): content/care/care_ID.md를 앱 assets로 묶은 것. 없거나 깨지면 null(화면에서 카드를 숨긴다). */
@Singleton
class CareRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val cache = mutableMapOf<String, CareSheet?>()

    fun sheet(id: String): CareSheet? = synchronized(cache) {
        cache.getOrPut(id) {
            runCatching { context.assets.open("care_$id.md").bufferedReader().use { CareSheet.parse(id, it.readText()) } }.getOrNull()
        }
    }

    /**
     * 정차 중 "콩이 돌보기". 응급 카드(`action: VET`)가 검수 전이고 [showDrafts]가 아니면(릴리스) 본문 대신
     * "바로 동물병원에 연락해요" 한 줄만 (긴급 도움 11/03). 그 밖의 기존 체크리스트는 이전과 같다.
     */
    fun ui(id: String, showDrafts: Boolean = true): CareUi? = sheet(id)?.let {
        if (it.action == "VET" && !it.reviewed && !showDrafts) CareUi(it.title, listOf(EmergencyCardUi.LEAD), reviewed = false)
        else CareUi(it.title, it.lines, it.reviewed)
    }

    /**
     * 콩이 도우미 분류용 카드 (HELPER_PROMPT): assets의 care_*.md 전부(디버그 빌드는 content/care/_drafts 초안도 묶는다).
     * 응급 카드가 앞(같은 점수면 앞 카드). [showDrafts]가 아니면(릴리스) 검수 끝난 카드만 — 미검수 카드는 분류·답에 쓰지 않는다.
     */
    fun helperCards(showDrafts: Boolean): List<HelperCard> = synchronized(cache) {
        val ids = runCatching { context.assets.list("").orEmpty() }.getOrDefault(emptyArray())
            .filter { it.startsWith("care_") && it.endsWith(".md") }.map { it.removePrefix("care_").removeSuffix(".md") }.sorted()
        val texts = ids.mapNotNull { id -> runCatching { context.assets.open("care_$id.md").bufferedReader().use { id to it.readText() } }.getOrNull() }.toMap()
        HelperAnswers.catalog(texts, showDrafts)
    }

    /** 긴급 도움 응급 카드 (릴리스 미검수 차단은 [EmergencyCardUi.from]). */
    fun emergency(id: String, showDrafts: Boolean): EmergencyCardUi? = sheet(id)?.let { EmergencyCardUi.from(it, showDrafts) }
}
