package com.emop.pause.mode

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.TripPhase

/** AppModeReducer 입력. gear·speed·parkingBrakeOn이 null이면 그 신호를 지원하지 않는 차종이다. Gear.UNKNOWN은 null과 같다. */
data class ModeInput(
    val nowMs: Long,
    val driveState: DriveState,
    val trip: TripPhase,
    val gear: Gear?,
    val speedKph: Float?,
    val charging: Boolean?,
    val parkingBrakeOn: Boolean? = null,
)

data class ModeState(
    val mode: AppMode = AppMode.PREPARE,
    val pauseCause: PauseCause? = null,
    /** 기어 신호 미지원 차종에서 PARKED·속도 0이 시작된 시각. */
    val stillSinceMs: Long? = null,
    /** 기어 미지원 차종에서 주차 브레이크로 PAUSE에 들어왔음. */
    val viaBrake: Boolean = false,
)

/**
 * DriveState + 기어 + 충전 + 여행 진행 → AppMode (architecture §5.2).
 *
 * "DRIVING/RESTRICTED면 즉시 주행 UI"라는 최우선 규칙은 화면 쪽(ScreenRouter)에서도 한 번 더 지킨다.
 * 기어 미지원 차종의 주차 브레이크 규칙은 2026-10-29 추가 (architecture §5.2).
 */
object AppModeReducer {
    const val STILL_PAUSE_MS = 60_000L

    fun reduce(prev: ModeState, input: ModeInput): ModeState {
        when (input.trip) {
            TripPhase.NOT_STARTED -> return ModeState(AppMode.PREPARE)
            TripPhase.ENDED -> return ModeState(AppMode.ARRIVED)
            TripPhase.ACTIVE -> Unit
        }
        if (input.driveState.forcesDrivingUi) return ModeState(AppMode.DRIVING)

        // 여기부터 여행 중이고 DriveState = PARKED.
        return when (prev.mode) {
            // 출발 준비 화면에서는 실제로 주행이 시작될 때까지 머문다.
            AppMode.PREPARE, AppMode.ARRIVED -> ModeState(AppMode.PREPARE)
            AppMode.DRIVING -> fromDriving(prev, input)
            AppMode.PAUSE -> fromPause(prev, input)
        }
    }

    private fun ModeInput.knownGear(): Gear? = gear?.takeIf { it != Gear.UNKNOWN }

    private fun fromDriving(prev: ModeState, input: ModeInput): ModeState {
        val gear = input.knownGear()
        if (gear != null) {
            // 기어 P가 아니면(신호 대기 등) DRIVING 유지 — 안전 쪽.
            return if (gear == Gear.PARK) pause(input) else ModeState(AppMode.DRIVING)
        }
        // 기어 미지원: 주차 브레이크가 걸리고 움직이지 않으면 바로 멈춤 (60초 타이머보다 강한 신호).
        if (input.parkingBrakeOn == true && (input.speedKph == null || input.speedKph == 0f)) return pause(input)
        val stopped = input.speedKph != null && input.speedKph == 0f
        if (!stopped) return ModeState(AppMode.DRIVING)
        val since = prev.stillSinceMs ?: input.nowMs
        return if (input.nowMs - since >= STILL_PAUSE_MS) pause(input) else ModeState(AppMode.DRIVING, stillSinceMs = since)
    }

    private fun fromPause(prev: ModeState, input: ModeInput): ModeState {
        val gear = input.knownGear()
        val leaving = when {
            gear != null -> gear != Gear.PARK
            input.speedKph != null && input.speedKph != 0f -> true
            // 브레이크로 멈춤에 들어온 경우 브레이크를 풀면 출발로 본다.
            else -> input.parkingBrakeOn == false && prev.viaBrake
        }
        return if (leaving) ModeState(AppMode.DRIVING) else pause(input).copy(stillSinceMs = prev.stillSinceMs, viaBrake = prev.viaBrake || input.parkingBrakeOn == true)
    }

    private fun pause(input: ModeInput) =
        ModeState(
            AppMode.PAUSE,
            pauseCause = if (input.charging == true) PauseCause.CHARGING else PauseCause.REST,
            viaBrake = input.knownGear() == null && input.parkingBrakeOn == true,
        )
}
