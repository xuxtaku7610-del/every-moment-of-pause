package com.emop.pause.engine.pet

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Gear

/** 대기 모드 판단에 쓰는 1초 입력. 없는 신호는 null. */
data class WaitInput(
    val nowMs: Long,
    val driveState: DriveState,
    val gear: Gear?,
    val parkingBrakeOn: Boolean? = null,
    val batterySocPercent: Int? = null,
    /** 실내 온도 측정값(℃). 에뮬레이터·차종에 따라 없다 → null이면 온도 표시를 숨긴다(CabinHeat). */
    val cabinTempC: Float? = null,
)

enum class WaitEndReason { USER, LEFT_PARK, DRIVING }

sealed interface WaitEvent {
    data object Started : WaitEvent
    /** 제한 시간을 넘었다. 대기 한 번에 한 번만 나온다 (F3-03). */
    data object Overdue : WaitEvent
    data class Ended(val reason: WaitEndReason) : WaitEvent
}

/** 대기 모드 상태 한 장 (차 화면·폰 공유가 같은 값을 쓴다). */
data class WaitStatus(
    val active: Boolean = false,
    val startedMs: Long? = null,
    val elapsedSec: Int = 0,
    val limitSec: Int = WaitMode.DEFAULT_LIMIT_MIN * 60,
    val overdue: Boolean = false,
    val batteryStart: Int? = null,
    val batteryNow: Int? = null,
    val cabinTempC: Float? = null,
    /** 대기 중 처음 받은 실내 온도 (오르는지 비교, UPGRADE 2). */
    val cabinTempStartC: Float? = null,
) {
    /** 대기 중 실내 온도가 오르는 중 ([CabinHeat.rising]). 온도를 모르면 false. */
    val heatRising: Boolean get() = CabinHeat.rising(cabinTempStartC, cabinTempC)

    /** 시작 대비 배터리 변화(%p). 신호가 없으면 null. */
    val batteryDelta: Int? get() = if (batteryStart != null && batteryNow != null) batteryNow - batteryStart else null
    val remainingSec: Int get() = limitSec - elapsedSec
}

/**
 * 잠깐 대기 모드 (CONCEPT F3, test_plan F3-01~F3-05).
 * - P단(DriveState PARKED + 기어 P, 기어 신호가 없으면 주차 브레이크)일 때만 시작할 수 있다.
 * - 1초마다 경과 시간·배터리 변화를 갱신하고, 제한 시간을 넘으면 [WaitEvent.Overdue]를 한 번 낸다.
 * - 기어가 P에서 벗어나거나 DriveState가 PARKED가 아니면 즉시 끝난다 — 주행 안전이 먼저다.
 */
class WaitMode(limitMin: Int = DEFAULT_LIMIT_MIN) {
    var status = WaitStatus(limitSec = limitMin.coerceIn(1, MAX_LIMIT_MIN) * 60)
        private set

    fun setLimit(minutes: Int) {
        status = status.copy(limitSec = minutes.coerceIn(1, MAX_LIMIT_MIN) * 60, overdue = status.overdue && status.elapsedSec >= minutes * 60)
    }

    fun canStart(i: WaitInput): Boolean = parked(i)

    /** "잠깐 다녀올게". P단이 아니면 시작하지 않고 빈 목록. */
    fun start(i: WaitInput): List<WaitEvent> {
        if (status.active || !canStart(i)) return emptyList()
        status = WaitStatus(true, i.nowMs, 0, status.limitSec, false, i.batterySocPercent, i.batterySocPercent, i.cabinTempC, i.cabinTempC)
        return listOf(WaitEvent.Started)
    }

    fun tick(i: WaitInput): List<WaitEvent> {
        if (!status.active) return emptyList()
        if (i.driveState != DriveState.PARKED) return end(WaitEndReason.DRIVING)
        if (!parked(i)) return end(WaitEndReason.LEFT_PARK)
        val elapsed = (((i.nowMs - (status.startedMs ?: i.nowMs)) / 1000).toInt()).coerceAtLeast(0)
        val becameOverdue = !status.overdue && elapsed >= status.limitSec
        status = status.copy(
            elapsedSec = elapsed,
            overdue = status.overdue || becameOverdue,
            batteryNow = i.batterySocPercent ?: status.batteryNow,
            cabinTempC = i.cabinTempC,
            cabinTempStartC = status.cabinTempStartC ?: i.cabinTempC,
        )
        return if (becameOverdue) listOf(WaitEvent.Overdue) else emptyList()
    }

    /** 사용자가 끝냄(돌아옴). */
    fun stop(): List<WaitEvent> = if (status.active) end(WaitEndReason.USER) else emptyList()

    private fun end(reason: WaitEndReason): List<WaitEvent> {
        status = WaitStatus(limitSec = status.limitSec)
        return listOf(WaitEvent.Ended(reason))
    }

    private fun parked(i: WaitInput): Boolean {
        if (i.driveState != DriveState.PARKED) return false
        return when (i.gear) {
            Gear.PARK -> true
            null, Gear.UNKNOWN -> i.parkingBrakeOn == true
            else -> false
        }
    }

    companion object {
        const val DEFAULT_LIMIT_MIN = 10
        const val MAX_LIMIT_MIN = 120
    }
}
