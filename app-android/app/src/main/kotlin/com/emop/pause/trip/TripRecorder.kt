package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.PauseCause
import com.emop.pause.engine.motion.MotionConstants
import com.emop.pause.engine.planner.RestStop
import kotlin.math.abs
import kotlin.math.max

/** 정차 한 번의 기록 (휴게소 단위 — 경로 좌표는 남기지 않는다, architecture §6). */
data class StopVisit(
    /** 휴게소 이름. 휴게소 목록이 아직 없으면(장면 점프 직후 다시 받는 중) null → 목록이 오면 [km]로 채운다. */
    var name: String?,
    val arrivedMinute: Int,
    /** 멈춘 곳의 경로 거리(km). 이름을 아직 모르면 다음 기록 때 다시 잰다. */
    var km: Double = 0.0,
    var stayedMin: Int = 0,
    var chargedMin: Int = 0,
    var petWalk: Boolean = false,
)

/** 주행 자극 보통(40) 이상이 이어진 구간: 출발 뒤 분(정차 포함) [startMin]~[endMin], 그 안 최고 [peak]. */
data class StimulusSegment(val startMin: Int, val endMin: Int, val peak: Int)

data class TripLog(
    val visits: List<StopVisit>,
    val fatigueSeries: List<Int>,
    /** 쉼표 위치(시리즈 비율). */
    val restMarks: List<Float>,
    val peakFatigue: Int,
    val longestDriveMin: Int,
    val distanceKm: Double,
    val startMinute: Int?,
    /** 멀미 지수 흐름(0~100, 5분 간격, F7-01). 피로 흐름과 같은 시각에 찍고, 값은 그 5분 안의 최고값 — 그래프 최고 = [motionPeak]. */
    val motionSeries: List<Int> = emptyList(),
    val motionPeak: Int = 0,
    /** 멀미 지수가 주의(40)에 처음 닿은 주행 분 (F7-02 한계 시간 추정). */
    val firstCautionMin: Int? = null,
    /** 주행 시간(분, 정차 제외). */
    val drivingMin: Int = 0,
    /** 멀미 주의(40)에 들어간 횟수 (1초 값 기준, 35 아래로 내려갔다 다시 들어와야 한 번 더 센다). */
    val cautionCount: Int = 0,
    /** 마지막 1초 멀미 지수 ("내렸어요" 때 읽으면 도착 시점 지수). */
    val motionNow: Int = 0,
    /** 주행 자극 보통 이상 구간 (기록 리포트 타임라인). 히스테리시스는 [cautionCount]와 같다. */
    val segments: List<StimulusSegment> = emptyList(),
    /** 출발 뒤 마지막 기록 분 (도착 시각 줄). */
    val elapsedMin: Int = 0,
)

/**
 * 이동 기록기 (test_plan F7-01). 1초마다 [tick]하고, 5분마다 피로·멀미 지수를 한 점씩 남긴다.
 * 멀미 지수·주의 도달 시간은 [onMotion]으로 따로 받는다(엔진이 계산).
 * 기기 안에서만 계산하므로 오프라인으로 끝난 여행도 리포트가 나온다.
 */
class TripRecorder {
    private val visits = mutableListOf<StopVisit>()
    private val series = mutableListOf<Int>()
    private val motionSeries = mutableListOf<Int>()
    private var motionNow = 0
    private var windowMax = 0
    private var cautionCount = 0
    private var inCaution = false
    private var motionPeak = 0
    private var firstCautionMin: Int? = null
    private var drivingMin = 0
    private val marks = mutableListOf<Int>()
    private var lastSampleMin: Int? = null
    private var current: StopVisit? = null
    private var peak = 0
    private var longest = 0
    private var distance = 0.0
    private var start: Int? = null
    private val segments = mutableListOf<StimulusSegment>()
    private var open: StimulusSegment? = null
    private var lastMinute: Int? = null

    fun reset() {
        visits.clear(); series.clear(); marks.clear()
        lastSampleMin = null; current = null; peak = 0; longest = 0; distance = 0.0; start = null
        motionSeries.clear(); motionNow = 0; motionPeak = 0; firstCautionMin = null; drivingMin = 0
        windowMax = 0; cautionCount = 0; inCaution = false
        segments.clear(); open = null; lastMinute = null
    }

    /** 멀미 엔진 상태 (MotionComfortEngine). [firstCautionSec]은 주의에 처음 닿은 주행 초. */
    fun onMotion(index: Int, drivingSec: Long, firstCautionSec: Long?) {
        motionNow = index
        windowMax = max(windowMax, index)
        motionPeak = max(motionPeak, index)
        if (!inCaution && index >= MotionConstants.CAUTION) {
            inCaution = true
            cautionCount++
        } else if (inCaution && index < MotionConstants.CAUTION - CAUTION_HYSTERESIS) {
            inCaution = false
        }
        drivingMin = (drivingSec / 60).toInt()
        if (firstCautionMin == null && firstCautionSec != null) firstCautionMin = (firstCautionSec / 60).toInt()
    }

    fun tick(minute: Int, mode: AppMode, cause: PauseCause?, fatigueScore: Int?, continuousMin: Int?, traveledKm: Double, stops: List<RestStop>) {
        if (mode == AppMode.PREPARE) return
        if (start == null) start = minute
        lastMinute = minute
        trackSegment(minute - start!!)
        distance = max(distance, traveledKm)
        fatigueScore?.let { peak = max(peak, it) }
        continuousMin?.let { longest = max(longest, it) }
        if (fatigueScore != null && (lastSampleMin == null || minute - lastSampleMin!! >= SAMPLE_MIN)) {
            series += fatigueScore
            motionSeries += max(windowMax, motionNow)
            windowMax = 0
            lastSampleMin = minute
        }
        // 점프로 따라잡을 때는 휴게소 목록이 아직 없어 이름이 비어 있다 → 목록이 오면 지난 정차도 채운다.
        if (stops.isNotEmpty()) visits.filter { it.name == null && it !== current }.forEach { it.name = nearestName(it.km, stops) }
        if (mode == AppMode.PAUSE) {
            val v = current ?: StopVisit(nearestName(traveledKm, stops), minute, traveledKm).also {
                current = it
                visits += it
                marks += series.size
            }
            if (v.name == null) {
                v.km = traveledKm
                v.name = nearestName(traveledKm, stops)
            }
            v.stayedMin = minute - v.arrivedMinute
            if (cause == PauseCause.CHARGING) v.chargedMin = v.stayedMin
        } else {
            current = null
        }
    }

    /** 주행 자극 보통 이상 구간: 40에 닿으면 열고, 35 아래로 내려가면 닫는다 (멀미 주의 횟수와 같은 히스테리시스). */
    private fun trackSegment(elapsed: Int) {
        val o = open
        when {
            o == null && motionNow >= MotionConstants.CAUTION -> open = StimulusSegment(elapsed, elapsed, motionNow)
            o != null && motionNow < MotionConstants.CAUTION - CAUTION_HYSTERESIS -> {
                segments += o.copy(endMin = elapsed)
                open = null
            }
            o != null -> open = o.copy(endMin = elapsed, peak = max(o.peak, motionNow))
        }
    }

    fun onPetWalk() { current?.petWalk = true }

    /** 지금 머무는 곳 이름 (멈춤 화면 앱바). */
    val currentStop: String? get() = current?.let { it.name ?: ROADSIDE }
    val currentArrived: Int? get() = current?.arrivedMinute

    fun log(): TripLog {
        val n = max(1, series.size - 1)
        return TripLog(visits.toList(), series.toList(), marks.map { it.toFloat() / n }, peak, longest, distance, start, motionSeries.toList(), motionPeak, firstCautionMin, drivingMin, cautionCount, motionNow,
            segments.toList() + listOfNotNull(open), start?.let { st -> lastMinute?.let { it - st } } ?: 0)
    }

    /** 목록이 비어 있으면 null(아직 모름), 3km 안에 휴게소가 없으면 "길가". */
    private fun nearestName(km: Double, stops: List<RestStop>): String? {
        if (stops.isEmpty()) return null
        return stops.minByOrNull { abs(it.distanceAlongRouteKm - km) }?.takeIf { abs(it.distanceAlongRouteKm - km) <= NEAR_KM }?.name ?: ROADSIDE
    }

    companion object {
        const val SAMPLE_MIN = 5
        /** 주의 횟수를 다시 세려면 이만큼 아래(40 − 5)로 내려가야 한다 — 39·40을 오가며 여러 번 세지 않게. */
        const val CAUTION_HYSTERESIS = 5
        const val NEAR_KM = 3.0
        const val ROADSIDE = "길가"

        /** 다음 여행 팁 1개 (규칙 기반, test_plan 7장 P1). */
        fun tip(log: TripLog): String = when {
            log.peakFatigue >= 70 -> "피로가 위험 단계까지 올랐어요. 다음엔 첫 휴식을 30분 앞당겨 보세요."
            log.peakFatigue >= 40 -> "첫 휴식을 20분 앞당기면 피로가 주의 단계에 닿지 않았을 거예요."
            log.longestDriveMin > 120 -> "2시간 넘게 쉬지 않은 구간이 있었어요. 중간에 짧은 쉼표를 하나 더 넣어 보세요."
            else -> "쉼표를 잘 지켰어요. 이번처럼 2시간 안에 한 번씩 쉬어 가요."
        }
    }
}
