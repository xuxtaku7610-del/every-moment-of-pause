package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.PauseCause
import com.emop.pause.engine.planner.RestStop
import kotlin.math.abs
import kotlin.math.max

/** 정차 한 번의 기록 (휴게소 단위 — 경로 좌표는 남기지 않는다, architecture §6). */
data class StopVisit(
    val name: String,
    val arrivedMinute: Int,
    var stayedMin: Int = 0,
    var chargedMin: Int = 0,
    var napMin: Int = 0,
    var expedition: Boolean = false,
    var petWalk: Boolean = false,
)

data class TripLog(
    val visits: List<StopVisit>,
    val fatigueSeries: List<Int>,
    /** 쉼표 위치(시리즈 비율). */
    val restMarks: List<Float>,
    val peakFatigue: Int,
    val longestDriveMin: Int,
    val distanceKm: Double,
    val startMinute: Int?,
)

/**
 * 여행 리포트용 기록기 (test_plan 7장 F7). 1초마다 [tick]하고, 5분마다 피로 점수를 한 점 남긴다.
 * 기기 안에서만 계산하므로 오프라인으로 끝난 여행도 리포트가 나온다.
 */
class TripRecorder {
    private val visits = mutableListOf<StopVisit>()
    private val series = mutableListOf<Int>()
    private val marks = mutableListOf<Int>()
    private var lastSampleMin: Int? = null
    private var current: StopVisit? = null
    private var peak = 0
    private var longest = 0
    private var distance = 0.0
    private var start: Int? = null

    fun reset() {
        visits.clear(); series.clear(); marks.clear()
        lastSampleMin = null; current = null; peak = 0; longest = 0; distance = 0.0; start = null
    }

    fun tick(minute: Int, mode: AppMode, cause: PauseCause?, fatigueScore: Int?, continuousMin: Int?, traveledKm: Double, stops: List<RestStop>) {
        if (mode == AppMode.PREPARE) return
        if (start == null) start = minute
        distance = max(distance, traveledKm)
        fatigueScore?.let { peak = max(peak, it) }
        continuousMin?.let { longest = max(longest, it) }
        if (fatigueScore != null && (lastSampleMin == null || minute - lastSampleMin!! >= SAMPLE_MIN)) {
            series += fatigueScore
            lastSampleMin = minute
        }
        if (mode == AppMode.PAUSE) {
            val v = current ?: StopVisit(nearestName(traveledKm, stops), minute).also {
                current = it
                visits += it
                marks += series.size
            }
            v.stayedMin = minute - v.arrivedMinute
            if (cause == PauseCause.CHARGING) v.chargedMin = v.stayedMin
        } else {
            current = null
        }
    }

    fun onNap(minutes: Int) { current?.let { it.napMin += minutes } }
    fun onExpedition() { current?.expedition = true }
    fun onPetWalk() { current?.petWalk = true }

    /** 지금 머무는 곳 이름 (멈춤 화면 앱바). */
    val currentStop: String? get() = current?.name
    val currentArrived: Int? get() = current?.arrivedMinute

    fun log(): TripLog {
        val n = max(1, series.size - 1)
        return TripLog(visits.toList(), series.toList(), marks.map { it.toFloat() / n }, peak, longest, distance, start)
    }

    private fun nearestName(km: Double, stops: List<RestStop>): String =
        stops.minByOrNull { abs(it.distanceAlongRouteKm - km) }?.takeIf { abs(it.distanceAlongRouteKm - km) <= NEAR_KM }?.name ?: "길가"

    companion object {
        const val SAMPLE_MIN = 5
        const val NEAR_KM = 3.0

        /** 다음 여행 팁 1개 (규칙 기반, test_plan 7장 P1). */
        fun tip(log: TripLog): String = when {
            log.peakFatigue >= 70 -> "피로가 위험 단계까지 올랐어요. 다음엔 첫 휴식을 30분 앞당겨 보세요."
            log.peakFatigue >= 40 -> "첫 휴식을 20분 앞당기면 피로가 주의 단계에 닿지 않았을 거예요."
            log.longestDriveMin > 120 -> "2시간 넘게 쉬지 않은 구간이 있었어요. 중간에 짧은 쉼표를 하나 더 넣어 보세요."
            else -> "쉼표를 잘 지켰어요. 이번처럼 2시간 안에 한 번씩 쉬어 가요."
        }
    }
}
