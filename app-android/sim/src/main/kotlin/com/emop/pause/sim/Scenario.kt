package com.emop.pause.sim

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.TripPhase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 시나리오 JSON (저장소 루트 scenarios 폴더).
 *
 * timeline은 시각 t(초)의 키프레임 목록이다. 키프레임에 없는 항목은 이전 값을 유지하고,
 * 숫자 항목은 앞뒤 키프레임이 모두 값을 가지면 선형 보간한다. 지원하지 않는 신호는
 * capabilities에서 빼면 해당 값이 null로 나간다.
 */
@Serializable
data class Scenario(
    val name: String,
    val description: String = "",
    /** 시나리오 시작 시각(HH:mm). 피로 엔진의 시간대 요인에 쓴다. */
    val startClock: String = "09:00",
    val capabilities: Set<Signal>,
    val route: ScenarioRoute,
    val timeline: List<Keyframe>,
) {
    val durationSec: Int get() = timeline.maxOf { it.t }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): Scenario {
            val s = json.decodeFromString(serializer(), text)
            require(s.timeline.isNotEmpty()) { "timeline이 비어 있음" }
            require(s.timeline.zipWithNext().all { (a, b) -> a.t < b.t }) { "timeline의 t는 증가해야 함" }
            return s
        }
    }
}

@Serializable
data class ScenarioRoute(
    val destinationName: String,
    val destination: SLatLng,
    val totalDistanceM: Int,
    val points: List<SLatLng> = emptyList(),
)

@Serializable
data class SLatLng(val lat: Double, val lng: Double) {
    fun toModel() = LatLng(lat, lng)
}

@Serializable
data class Keyframe(
    val t: Int,
    val label: String? = null,
    val trip: TripPhase? = null,
    val driveState: DriveState? = null,
    val gear: Gear? = null,
    val speedKph: Float? = null,
    val socPercent: Float? = null,
    val charging: Boolean? = null,
    val hvacTempC: Float? = null,
    val hvacAc: Boolean? = null,
    val recirculation: Boolean? = null,
    val remainDistanceM: Float? = null,
    val remainTimeSec: Float? = null,
)
