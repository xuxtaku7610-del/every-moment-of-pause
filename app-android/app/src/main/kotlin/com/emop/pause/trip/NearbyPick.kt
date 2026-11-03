package com.emop.pause.trip

import com.emop.pause.engine.pet.Josa
import com.emop.pause.uistate.DrivingRules
import kotlin.math.roundToInt

/**
 * 고속도로인지 (UI v9, 사용자 결정 11/03): 최근 5분 평균 속도가 80km/h 이상이면 고속도로로 본다.
 * NaviHelper `TBTInfo`에는 도로 이름이 없고(type·distance·description=방면뿐, javap 3.0.4) 도로 이름은 정체 구간
 * (`TrafficCongestionInfo.roadName`)에만 있어 쓰지 않는다. 속도 표본이 [MIN_SAMPLES_SEC]초보다 적으면 모름 → null.
 * 순수 로직 — 1초마다 [add]로 속도를 넣는다(없으면 넣지 않는다).
 */
class HighwayGuess(private val windowSec: Int = WINDOW_SEC) {
    private val samples = ArrayDeque<Pair<Long, Float>>()

    fun add(nowMs: Long, speedKph: Float?) {
        if (speedKph != null) samples.addLast(nowMs to speedKph)
        while (samples.isNotEmpty() && nowMs - samples.first().first > windowSec * 1000L) samples.removeFirst()
    }

    /** true 고속도로 · false 일반 도로 · null 아직 모름(표본 부족). */
    fun highway(): Boolean? {
        if (samples.size < MIN_SAMPLES_SEC) return null
        return samples.map { it.second }.average() >= HIGHWAY_KPH
    }

    fun reset() = samples.clear()

    companion object {
        const val WINDOW_SEC = 300
        const val MIN_SAMPLES_SEC = 60
        const val HIGHWAY_KPH = 80.0
    }
}

/** 주변 추천 종류: 산책로·공원(TourAPI walk) · 반려견 동반 쇼핑(TourAPI 쇼핑 = shop) · 동물병원(인허가). 음식점·카페는 넣지 않는다. */
enum class NearbyKind(val label: String) { WALK("산책로·공원"), SHOP("반려견 동반 쇼핑"), VET("동물병원") }

/** 왜 띄우나: 오늘 관찰 신고 + 근처 병원 / 목적지 10km 안. (배변 임박은 쉼표 추천에 합쳤다 — PetRestPlanner F2-16) */
enum class NearbyWhy { REPORT_VET, DESTINATION }

/** 후보 한 곳. [aheadKm]은 지금 위치에서 경로를 따라 앞쪽 거리, [offKm]은 경로에서 떨어진 거리, [fromDestKm]은 목적지 거리(목적지 추천만). */
data class NearbyPlace(
    val id: String,
    val name: String,
    val kind: NearbyKind,
    val lat: Double,
    val lng: Double,
    val aheadKm: Double,
    val offKm: Double,
    val fromDestKm: Double? = null,
    /** 앱 내장 샘플("[샘플] …")이면 true — 실제 이름이 아니라 추천하지 않는다. */
    val sample: Boolean = false,
)

/** 지금 띄울 추천 하나. */
data class NearbyOffer(val place: NearbyPlace, val why: NearbyWhy) {
    /** 카드 제목 = 장소 이름 ("야옹아멍멍해봐 강릉내곡점"). */
    val title: String get() = place.name

    /** 제목 아래 한 줄 "9.7km 앞 · 반려견 동반 쇼핑". */
    val line: String get() = "${km(place.aheadKm)} 앞 · ${place.kind.label}"

    /** 이유 한 줄 (사실만: 신고가 있었는지, 목적지에서 얼마나 떨어졌는지). */
    val reason: String get() = when (why) {
        NearbyWhy.REPORT_VET -> "오늘 관찰 신고가 있었어요"
        NearbyWhy.DESTINATION -> "목적지에서 ${km(place.fromDestKm ?: place.aheadKm)}"
    }

    /** 음성 한 줄 (주행 중 48자 이하, 넘으면 이름을 뺀다). */
    fun phrase(petName: String): String {
        val full = when (why) {
            NearbyWhy.REPORT_VET -> "${km(place.aheadKm)} 앞에 ${place.name}이 있어요. 들를까요?"
            NearbyWhy.DESTINATION -> "곧 도착해요. 근처 ${place.name}에 ${Josa.waGwa(petName)} 들를까요?"
        }
        return if (full.length <= DrivingRules.MAX_BLOCK_CHARS) full
        else when (why) {
            NearbyWhy.REPORT_VET -> "${km(place.aheadKm)} 앞에 동물병원이 있어요. 들를까요?"
            NearbyWhy.DESTINATION -> "곧 도착해요. 목적지 근처 ${place.kind.label} 장소에 들를까요?"
        }
    }

    companion object {
        fun km(v: Double): String = if (v < 10) "${(v * 10).roundToInt() / 10.0}km" else "${v.roundToInt()}km"
    }
}

/** 고르기 입력 (1초마다, 주행 중). */
data class NearbyInput(
    val nowMs: Long,
    /** [HighwayGuess.highway] — true(고속도로)나 null(모름)이면 띄우지 않는다. */
    val highway: Boolean?,
    /** 예보·쉼표 추천·"이상해요"·응급·병원 가는 중·다른 질문이 떠 있으면 true → 미룬다. */
    val busy: Boolean,
    /** 오늘 관찰 신고 수 (보호자 음성 신고만). */
    val reports: Int,
    /** 응급 신고가 있었으면 기존 응급 흐름이 먼저 — 이 추천은 병원을 띄우지 않는다. */
    val emergency: Boolean,
    /** 목적지까지 남은 거리(km, NaviHelper remainDistance). */
    val remainKm: Double?,
)

/**
 * 주행 중 주변 추천 (UI v9 1번, test_plan F4-10~15). 운전자는 주행 중 거의 기본 내비만 보므로 같이가개 분할 창은 평소 조용하고,
 * **일반 도로에서만**(고속도로는 휴게소 쉼표 추천만, 사용자 결정 11/03) 필요한 순간에 경로 근처 반려견 장소 **하나**를 띄운다.
 * - 오늘 관찰 신고가 있고 앞 5km·경로 1km 안에 영업 중 병원 → 동물병원 (응급이면 응급 흐름이 먼저라 띄우지 않음)
 * - 목적지 10km 안 → 목적지 반경 3km 산책로·공원, 없으면 반려견 동반 쇼핑 (기존 "도착 전 산책" 대체, 한 여행에 한 번)
 * 같은 장소는 다시 안 뜨고, 추천 사이 최소 15분. 실제 데이터 이름만 — 샘플 행은 버린다(지어내지 않음). 데이터가 없는 종류는 안 뜬다.
 */
class NearbyPick {
    private val shown = mutableSetOf<String>()
    private var lastMs: Long? = null
    /** 병원 후보가 없었을 때 다시 찾을 시각 (그 사이에는 목적지 추천이 먼저 볼 수 있다). */
    private var vetRetryMs: Long? = null
    var destinationDone = false
        private set

    /** 지금 무엇을 찾아야 하나. null이면 찾지 않는다(고속도로·모름·미룸·쿨다운). */
    fun due(i: NearbyInput): NearbyWhy? {
        if (i.highway != false || i.busy) return null
        if (lastMs?.let { i.nowMs - it < COOLDOWN_MS } == true) return null
        if (i.reports > 0 && !i.emergency && vetRetryMs.let { it == null || i.nowMs >= it }) return NearbyWhy.REPORT_VET
        if (!destinationDone && i.remainKm != null && i.remainKm <= DEST_KM) return NearbyWhy.DESTINATION
        return null
    }

    /** 후보에서 하나 고른다. 병원은 앞 5km·경로 1km, 목적지는 산책로 → 쇼핑 순 가까운 곳. 없으면 null. */
    fun pick(why: NearbyWhy, candidates: List<NearbyPlace>): NearbyOffer? {
        val real = candidates.filter { !it.sample && it.id !in shown }
        val place = when (why) {
            NearbyWhy.REPORT_VET -> real.filter { it.kind == NearbyKind.VET && it.aheadKm > 0 && it.aheadKm <= AHEAD_KM && it.offKm <= OFF_ROUTE_KM }
                .minByOrNull { it.aheadKm }
            NearbyWhy.DESTINATION -> {
                val near = real.filter { (it.fromDestKm ?: Double.MAX_VALUE) <= DEST_RADIUS_KM }
                near.filter { it.kind == NearbyKind.WALK }.minByOrNull { it.fromDestKm!! }
                    ?: near.filter { it.kind == NearbyKind.SHOP }.minByOrNull { it.fromDestKm!! }
            }
        }
        return place?.let { NearbyOffer(it, why) }
    }

    /** 찾아봤다 (찾았든 못 찾았든). 목적지 추천은 한 여행에 한 번만 찾는다. */
    fun onSearched(why: NearbyWhy) {
        if (why == NearbyWhy.DESTINATION) destinationDone = true
    }

    /** 화면에 띄웠다 → 같은 장소는 다시 안 뜨고, 다음 추천까지 15분. */
    fun onShown(offer: NearbyOffer, nowMs: Long) {
        shown += offer.place.id
        lastMs = nowMs
    }

    /** 병원 후보를 찾았지만 없을 때: 1분 뒤 다시 찾는다(그 사이 목적지 추천은 볼 수 있다). */
    fun onEmpty(nowMs: Long) {
        vetRetryMs = nowMs + RETRY_EMPTY_MS
    }

    fun reset() {
        shown.clear()
        lastMs = null
        vetRetryMs = null
        destinationDone = false
    }

    companion object {
        const val AHEAD_KM = 5.0
        const val OFF_ROUTE_KM = 1.0
        const val DEST_KM = 10.0
        const val DEST_RADIUS_KM = 3.0
        const val COOLDOWN_MS = 15 * 60_000L
        const val RETRY_EMPTY_MS = 60_000L
        val PLACE_CATEGORIES = setOf("walk", "shop")
    }
}
