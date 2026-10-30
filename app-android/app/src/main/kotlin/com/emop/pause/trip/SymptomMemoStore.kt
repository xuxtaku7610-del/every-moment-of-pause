package com.emop.pause.trip

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 증상 메모 (UPGRADE 3). **기기에만** 저장하고 서버로 보내지 않는다 — 공유를 누르고 체크했을 때만 도착 예정 페이지에 실린다. */
@Singleton
class SymptomMemoStore @Inject constructor(@param:ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("symptom_memo", Context.MODE_PRIVATE)

    fun load(): VetMemo = VetMemo(
        chips = prefs.getStringSet("chips", emptySet()).orEmpty().toSet(),
        text = prefs.getString("text", "").orEmpty(),
        include = prefs.getBoolean("include", false),
    )

    fun save(m: VetMemo) = prefs.edit().putStringSet("chips", m.chips).putString("text", m.text).putBoolean("include", m.include).apply()

    fun clear() = prefs.edit().clear().apply()
}
