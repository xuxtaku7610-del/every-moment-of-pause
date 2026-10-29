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

    /** 온보딩을 마친 단계(0~3). 3이면 끝. 주행으로 중단되면 다음 정차 때 이어서 (UI_SPEC §4.11). */
    var onboardDone: Int
        get() = prefs.getInt("onboard", 0)
        set(v) = prefs.edit().putInt("onboard", v).apply()

    fun clear() = prefs.edit().clear().apply()
}
