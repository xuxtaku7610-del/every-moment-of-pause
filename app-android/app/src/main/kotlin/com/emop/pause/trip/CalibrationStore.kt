package com.emop.pause.trip

import android.content.Context
import com.emop.pause.engine.motion.CalibrationSample
import com.emop.pause.engine.motion.MotionAnswer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 개인 보정 기록 (UPGRADE 4): 반려견 이름별 "최고 지수:답" 목록. 기기에만 저장. 최근 [KEEP]개만. */
@Singleton
class CalibrationStore @Inject constructor(@param:ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("motion_calibration", Context.MODE_PRIVATE)

    fun load(pet: String): List<CalibrationSample> = prefs.getString(key(pet), "").orEmpty().split(';').mapNotNull { e ->
        val (p, a) = e.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        val answer = runCatching { MotionAnswer.valueOf(a) }.getOrNull() ?: return@mapNotNull null
        p.toIntOrNull()?.let { CalibrationSample(it, answer) }
    }

    fun add(pet: String, s: CalibrationSample): List<CalibrationSample> {
        val all = (load(pet) + s).takeLast(KEEP)
        prefs.edit().putString(key(pet), all.joinToString(";") { "${it.peak}:${it.answer.name}" }).apply()
        return all
    }

    fun reset(pet: String) = prefs.edit().remove(key(pet)).apply()

    fun clear() = prefs.edit().clear().apply()

    private fun key(pet: String) = "pet:" + pet.trim()

    private companion object {
        const val KEEP = 30
    }
}
