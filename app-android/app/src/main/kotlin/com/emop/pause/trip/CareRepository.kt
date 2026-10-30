package com.emop.pause.trip

import android.content.Context
import com.emop.pause.uistate.CareSheet
import com.emop.pause.uistate.CareUi
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

    fun ui(id: String): CareUi? = sheet(id)?.let { CareUi(it.title, it.lines, it.reviewed) }
}
