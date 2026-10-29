package com.emop.pause.core.model

/**
 * 실차 신호 잡음 처리 (COMMERCIAL_READINESS A P1, test_plan T6-15~17).
 *
 * - 속도: 최근 [speedWindowMs] 안 값의 평균. 단, 원시 값이 0이면 바로 0 (정차 판단을 늦추지 않는다).
 * - 기어: 새 값이 [gearHoldMs] 동안 그대로여야 바꾼다 (P↔R↔N 깜빡임 무시).
 * - 충전 중: 새 값이 [chargeHoldMs] 동안 그대로여야 바꾼다 (커넥터 접촉 순간 흔들림 무시).
 * 첫 값은 바로 쓴다. null(값 없음)은 거르지 않고 그대로 넘긴다.
 *
 * 주행 안전 판단(DriveState = CarUxRestrictions)은 이 필터를 거치지 않는다 — 늦어지는 것은 AppMode뿐이다.
 * 시뮬레이터 신호는 이미 깨끗하므로 실차 경로에서만 쓴다. 시간이 지나야 확정되므로 값이 그대로여도 1초마다 [apply]를 불러야 한다.
 */
class SignalSmoother(
    private val speedWindowMs: Long = SPEED_WINDOW_MS,
    private val gearHoldMs: Long = GEAR_HOLD_MS,
    private val chargeHoldMs: Long = CHARGE_HOLD_MS,
) {
    private val speeds = ArrayDeque<Pair<Long, Float>>()
    private val gear = Hold<Gear>(gearHoldMs)
    private val charging = Hold<Boolean>(chargeHoldMs)

    fun apply(s: SignalSample): SignalSample = s.copy(
        speedKph = smoothSpeed(s.timestampMs, s.speedKph),
        gear = gear.update(s.timestampMs, s.gear),
        charging = charging.update(s.timestampMs, s.charging),
    )

    private fun smoothSpeed(now: Long, raw: Float?): Float? {
        if (raw == null) {
            speeds.clear()
            return null
        }
        speeds.addLast(now to raw)
        while (speeds.first().first <= now - speedWindowMs) speeds.removeFirst()
        if (raw == 0f) return 0f
        return speeds.map { it.second }.average().toFloat()
    }

    /** 값이 [holdMs] 동안 바뀌지 않아야 확정한다. */
    private class Hold<T : Any>(private val holdMs: Long) {
        private var stable: T? = null
        private var pending: T? = null
        private var pendingSince = 0L

        fun update(now: Long, raw: T?): T? {
            if (raw == null) {
                stable = null
                pending = null
                return null
            }
            if (stable == null || raw == stable) {
                stable = raw
                pending = null
                return raw
            }
            if (raw != pending) {
                pending = raw
                pendingSince = now
            }
            if (now - pendingSince >= holdMs) {
                stable = raw
                pending = null
            }
            return stable
        }
    }

    companion object {
        const val SPEED_WINDOW_MS = 3_000L
        const val GEAR_HOLD_MS = 2_000L
        const val CHARGE_HOLD_MS = 5_000L
    }
}
