package com.emop.pause.trip

import android.content.Context
import com.emop.pause.engine.needs.EvProfile
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.sim.ScenarioSetup
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동승자 프리셋 (test_plan 1장). 기기에만 저장하고 서버로 보내지 않는다 (architecture §6).
 * 수면 시간은 여행마다 묻는 값이라 저장하지 않는다.
 */
@Singleton
class PresetStore @Inject constructor(@param:ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("preset", Context.MODE_PRIVATE)

    var childAges: List<Int>
        get() = prefs.getString("children", "").orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }
        set(v) = prefs.edit().putString("children", v.joinToString(",")).apply()

    var dog: Boolean
        get() = prefs.getBoolean("dog", false)
        set(v) = prefs.edit().putBoolean("dog", v).apply()

    var ev: Boolean
        get() = prefs.getBoolean("ev", false)
        set(v) = prefs.edit().putBoolean("ev", v).apply()

    /** 교대 운전자 동승. */
    var relief: Boolean
        get() = prefs.getBoolean("relief", false)
        set(v) = prefs.edit().putBoolean("relief", v).apply()

    /** 사용자가 입력한 이름. 없으면 기본 호칭 (UI_SPEC §6). */
    var childName: String
        get() = prefs.getString("childName", null) ?: "아이"
        set(v) = prefs.edit().putString("childName", v).apply()
    var dogName: String
        get() = prefs.getString("dogName", null) ?: "반려견"
        set(v) = prefs.edit().putString("dogName", v).apply()

    fun clear() {
        prefs.edit().clear().apply()
        sleepHours = null
    }

    /** 이번 여행의 수면 한마디 (메모리만). */
    var sleepHours: Float? = null

    val hasPreset: Boolean get() = prefs.contains("children") || prefs.contains("dog")

    fun setup() = TripSetup(passengers(childAges, dog), if (ev) EvProfile() else null, sleepHours)

    companion object {
        fun passengers(childAges: List<Int>, dog: Boolean): List<Passenger> =
            childAges.map { Passenger.Child(it) } + listOfNotNull(Passenger.Dog.takeIf { dog })

        fun fromScenario(s: ScenarioSetup) = TripSetup(passengers(s.childAges, s.dog), if (s.ev) EvProfile() else null, s.sleepHours)
    }
}
