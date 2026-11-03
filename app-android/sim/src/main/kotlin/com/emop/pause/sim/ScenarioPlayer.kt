package com.emop.pause.sim

import com.emop.pause.core.model.DoorState
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import com.emop.pause.core.model.TripPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 시뮬레이션 시각 tSec의 전체 상태. */
data class SimFrame(
    val tSec: Double,
    val label: String?,
    val trip: TripPhase,
    val driveState: DriveState,
    val sample: SignalSample,
    val remainDistanceM: Int?,
    val remainTimeSec: Int?,
)

/**
 * 시나리오를 배속 재생한다 (test_plan §8: 1×·10×·100×, 일시정지, 점프).
 *
 * 실제 시간 [tickMs]마다 시뮬레이션 시간을 tickMs × speed만큼 진행하고 프레임을 낸다.
 */
class ScenarioPlayer(
    val scenario: Scenario,
    private val scope: CoroutineScope,
    private val tickMs: Long = 200,
) {
    private val _frame = MutableStateFlow(frameAt(scenario, 0.0))
    val frame: StateFlow<SimFrame> = _frame.asStateFlow()

    private val _speed = MutableStateFlow(1)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private var job: Job? = null

    fun play() {
        if (job?.isActive == true) return
        _playing.value = true
        job = scope.launch {
            while (isActive) {
                delay(tickMs)
                val next = (_frame.value.tSec + tickMs / 1000.0 * _speed.value).coerceAtMost(scenario.durationSec.toDouble())
                _frame.value = frameAt(scenario, next)
                if (next >= scenario.durationSec) break
            }
            _playing.value = false
        }
    }

    fun pause() {
        job?.cancel()
        job = null
        _playing.value = false
    }

    fun setSpeed(multiplier: Int) {
        require(multiplier in SPEEDS) { "배속은 $SPEEDS 중 하나" }
        _speed.value = multiplier
    }

    fun seek(tSec: Double) {
        _frame.value = frameAt(scenario, tSec.coerceIn(0.0, scenario.durationSec.toDouble()))
    }

    companion object {
        val SPEEDS = listOf(1, 10, 100)

        private fun doorsAt(driver: Boolean?, rearLeft: Boolean?, rearRight: Boolean?): DoorState? =
            if (driver == null && rearLeft == null && rearRight == null) null else DoorState(driver, rearLeft, rearRight)

        fun frameAt(scenario: Scenario, tSec: Double): SimFrame {
            val frames = scenario.timeline
            val caps = scenario.capabilities

            fun <T> held(pick: (Keyframe) -> T?): T? = frames.lastOrNull { it.t <= tSec && pick(it) != null }?.let(pick)

            fun lerp(pick: (Keyframe) -> Float?): Float? {
                val before = frames.lastOrNull { it.t <= tSec && pick(it) != null } ?: return null
                val after = frames.firstOrNull { it.t > tSec && pick(it) != null } ?: return pick(before)
                val a = pick(before)!!
                val b = pick(after)!!
                val ratio = (tSec - before.t) / (after.t - before.t)
                return (a + (b - a) * ratio).toFloat()
            }

            fun <T> cap(signal: Signal, value: T?): T? = if (signal in caps) value else null

            val sample = SignalSample(
                timestampMs = (tSec * 1000).toLong(),
                speedKph = cap(Signal.SPEED, lerp { it.speedKph }),
                gear = cap(Signal.GEAR, held { it.gear }),
                batterySocPercent = cap(Signal.BATTERY_SOC, lerp { it.socPercent }?.toInt()),
                charging = cap(Signal.CHARGING, held { it.charging }),
                hvacTempC = cap(Signal.HVAC_TEMP, lerp { it.hvacTempC }),
                hvacAcOn = cap(Signal.HVAC_AC, held { it.hvacAc }),
                recirculationOn = cap(Signal.HVAC_RECIRCULATION, held { it.recirculation }),
                doors = cap(Signal.DOOR, doorsAt(held { it.doorDriver }, held { it.doorRearLeft }, held { it.doorRearRight })),
            )
            return SimFrame(
                tSec = tSec,
                label = held { it.label },
                trip = held { it.trip } ?: TripPhase.NOT_STARTED,
                driveState = held { it.driveState } ?: DriveState.PARKED,
                sample = sample,
                remainDistanceM = lerp { it.remainDistanceM }?.toInt(),
                remainTimeSec = lerp { it.remainTimeSec }?.toInt(),
            )
        }
    }
}
