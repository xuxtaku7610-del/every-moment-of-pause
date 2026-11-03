package com.emop.pause.engine.pet

/** 여행 한 번의 기록 요약 (Room `local_trips`와 같은 값). */
data class PetTripRecord(
    val startedMs: Long,
    val drivingMin: Int,
    /** 주행 자극이 보통(40)에 처음 닿은 주행 분. 닿지 않았으면 null. (주행 자극 = 차량 흔들림 누적, 반려견 측정 아님) */
    val firstCautionMin: Int?,
    val motionPeak: Int,
    val stops: Int,
    val symptomNote: String? = null,
)

/** "지난 여행들은 출발 약 1시간 10분 뒤 주행 자극이 보통 이상" 추정. [basedOn]은 근거가 된 여행 수. */
data class LimitEstimate(val minutes: Int, val basedOn: Int)

/**
 * 이동 기록 패턴 (CONCEPT F7, test_plan F7-02·F7-03). 기기 안에서만 계산한다.
 * 한계 시간 = 주의에 닿은 여행들의 "처음 닿은 주행 분" 중앙값. 닿은 여행이 없으면 추정하지 않는다.
 */
object PetTripLog {
    /** 가장 최근 [RECENT]번만 본다(나이·차가 바뀌면 옛 기록이 맞지 않으므로). */
    const val RECENT = 10

    fun limit(trips: List<PetTripRecord>): LimitEstimate? {
        val mins = trips.sortedByDescending { it.startedMs }.take(RECENT).mapNotNull { it.firstCautionMin }.sorted()
        if (mins.isEmpty()) return null
        val mid = mins.size / 2
        val median = if (mins.size % 2 == 1) mins[mid] else (mins[mid - 1] + mins[mid]) / 2
        return LimitEstimate(median, mins.size)
    }

    /**
     * 템플릿 문장. 근거가 3번 미만이면 몇 번 기준인지 덧붙인다. 반려견의 한계로 말하지 않고 "주행 자극이 쌓이는 때"로 말한다
     * (2026-10-31 재정비). [name]은 예전 호출과 맞추려고 남겨 둔다.
     */
    @Suppress("UNUSED_PARAMETER")
    fun limitPhrase(name: String, e: LimitEstimate?): String {
        if (e == null) return "지난 여행에서 주행 자극이 보통 단계에 닿은 적은 없어요."
        val base = "지난 여행들은 출발 약 ${duration(e.minutes)} 뒤 주행 자극이 보통 이상이 됐어요."
        return if (e.basedOn < 3) "$base (기록 ${e.basedOn}번 기준)" else base
    }

    /**
     * 기록 리포트 한 줄 (이번 여행 + 지난 여행). **이번 여행 값을 먼저** 본다:
     * 이번에 보통에 닿았으면 그 시각, 지난 기록이 있으면 덧붙이고, 둘 다 없을 때만 "닿은 적은 없어요".
     */
    fun insight(name: String, trip: PetTripRecord, e: LimitEstimate?): String {
        val first = trip.firstCautionMin
        return when {
            first != null && e != null -> "이번엔 출발 ${duration(first)} 뒤 주행 자극이 보통 이상이 됐어요. ${limitPhrase(name, e)}"
            first != null -> "이번엔 출발 ${duration(first)} 뒤 주행 자극이 보통 이상이 됐어요."
            e != null -> "이번엔 주행 자극이 낮은 채로 도착했어요. ${limitPhrase(name, e)}"
            else -> limitPhrase(name, null)
        }
    }

    /** 동물병원에 보여 줄 요약 줄 (F7-03). 증상 메모(음성 신고)는 그대로. 주행 자극은 차량 흔들림이라는 것을 함께 쓴다. */
    fun vetSummary(name: String, trip: PetTripRecord, e: LimitEstimate?): List<String> = buildList {
        add("$name 이동 기록")
        add("주행 시간 ${duration(trip.drivingMin)}(휴식 제외) · 휴식 ${trip.stops}번")
        add("주행 자극 최고 ${trip.motionPeak} (0~100, 차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님)")
        trip.firstCautionMin?.let { add("주행 자극 보통 도달: 출발 ${duration(it)} 뒤") }
        e?.let { add("지난 여행 보통 도달: 약 ${duration(it.minutes)} (기록 ${it.basedOn}번)") }
        trip.symptomNote?.takeIf { it.isNotBlank() }?.let { add("관찰 신고: $it") }
    }

    fun duration(min: Int): String {
        val h = min / 60
        val m = min % 60
        return when {
            h == 0 -> "${m}분"
            m == 0 -> "${h}시간"
            else -> "${h}시간 ${m}분"
        }
    }
}
