package com.emop.pause.trip

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 설정 (UI_SPEC §4.12). 기기에만 저장한다. */
@Singleton
class SettingsStore @Inject constructor(@param:ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** 제안 시점: 0 빠르게, 1 보통, 2 여유 있게. */
    var timing: Int
        get() = prefs.getInt("timing", 1)
        set(v) = prefs.edit().putInt("timing", v).apply()
    var voiceSuggest: Boolean
        get() = prefs.getBoolean("voice", true)
        set(v) = prefs.edit().putBoolean("voice", v).apply()
    var reduceMotion: Boolean
        get() = prefs.getBoolean("reduceMotion", false)
        set(v) = prefs.edit().putBoolean("reduceMotion", v).apply()
    var proactive: Boolean
        get() = prefs.getBoolean("proactive", true)
        set(v) = prefs.edit().putBoolean("proactive", v).apply()

    /** 안내 빈도 (v10-1): GuidanceLevel 순서 0 많이 · 1 보통 · 2 적게. */
    var guidanceLevel: Int
        get() = prefs.getInt("guidanceLevel", GuidanceLevel.NORMAL.ordinal)
        set(v) = prefs.edit().putInt("guidanceLevel", v).apply()

    /** "오늘은 안 타요"를 고른 날 (ISO 날짜). 날짜만 남기고 위치·시각은 저장하지 않는다. */
    var dayOff: String?
        get() = prefs.getString("dayOff", null)
        set(v) = prefs.edit().putString("dayOff", v).apply()

    /** 출발 체크 기억 (v10-3, CheckMemory.encode — 최근 3번 체크 항목만). */
    var checkMemory: String?
        get() = prefs.getString("checkMemory", null)
        set(v) = prefs.edit().putString("checkMemory", v).apply()

    /** 오늘 타는 아이 (v10-3) — 지난번 선택 프로필 id들 ("1,3"). 자동 시작은 이 선택 그대로. */
    var riders: String?
        get() = prefs.getString("riders", null)
        set(v) = prefs.edit().putString("riders", v).apply()

    /** 온보딩을 마친 단계(0~3). 3이면 끝. 주행으로 중단되면 다음 정차 때 이어서 (UI_SPEC §4.11). */
    var onboardDone: Int
        get() = prefs.getInt("onboard", 0)
        set(v) = prefs.edit().putInt("onboard", v).apply()

    fun clear() = prefs.edit().clear().apply()
}
