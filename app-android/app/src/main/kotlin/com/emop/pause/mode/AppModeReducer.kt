package com.emop.pause.mode

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.TripPhase

/** AppModeReducer 입력. gear·speed가 null이면 그 신호를 지원하지 않는 차종이다. */
data class ModeInput(
    val nowMs: Long,
    val driveState: DriveState,
    val trip: TripPhase,
    val gear: Gear?,
    val speedKph: Float?,
    val charging: Boolean?,
)

data class ModeState(
    val mode: AppMode = AppMode.PREPARE,
    val pauseCause: PauseCause? = null,
    /** 기어 신호 미지원 차종에서 PARKED·속도 0이 시작된 시각. */
    val stillSinceMs: Long? = null,
)

/**
 * DriveState + 기어 + 충전 + 여행 진행 → AppMode (architecture §5.2).
 *
 * "DRIVING/RESTRICTED면 즉시 주행 UI"라는 최우선 규칙은 화면 쪽(PauseRoot)에서도 한 번 더 지킨다.
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

    private fun fromDriving(prev: ModeState, input: ModeInput): ModeState {
        if (input.gear != null) {
            // 기어 P가 아니면(신호 대기 등) DRIVING 유지 — 안전 쪽.
            return if (input.gear == Gear.PARK) pause(input) else ModeState(AppMode.DRIVING)
        }
        val stopped = input.speedKph != null && input.speedKph == 0f
        if (!stopped) return ModeState(AppMode.DRIVING)
        val since = prev.stillSinceMs ?: input.nowMs
        return if (input.nowMs - since >= STILL_PAUSE_MS) pause(input) else ModeState(AppMode.DRIVING, stillSinceMs = since)
    }

    private fun fromPause(prev: ModeState, input: ModeInput): ModeState {
        val leaving = if (input.gear != null) input.gear != Gear.PARK else input.speedKph != null && input.speedKph != 0f
        return if (leaving) ModeState(AppMode.DRIVING) else pause(input).copy(stillSinceMs = prev.stillSinceMs)
    }

    private fun pause(input: ModeInput) =
        ModeState(AppMode.PAUSE, pauseCause = if (input.charging == true) PauseCause.CHARGING else PauseCause.REST)
}
