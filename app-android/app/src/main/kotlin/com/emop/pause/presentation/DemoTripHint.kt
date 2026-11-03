package com.emop.pause.presentation

/**
 * 심사위원 첫 실행 안내 띠 (2026-11-03, test_plan DM-10): 실차 모드 출발 전 화면에서 10초가 지나도
 * 차량 신호(속도·기어)가 하나도 없거나, 에뮬레이터 기기면 "시연 주행으로 둘러볼까요?"를 띄운다.
 * Pleos 에뮬레이터는 실차 모드에서도 속도 0·기어 P를 주기 때문에 기기 판별을 같이 쓴다(사용자 결정).
 * 실차(에뮬레이터가 아니고 속도·기어가 오는 차)에는 뜨지 않는다. 한 번 닫으면 그 실행 동안 다시 안 뜬다.
 * 순수 로직 — 시계는 바깥에서 받는다.
 */
class DemoTripHint(private val emulator: Boolean, private val waitMs: Long = WAIT_MS) {
    private var since: Long? = null
    private var signalSeen = false
    var dismissed = false
        private set

    /**
     * @param vehicle 실차 SDK 모드인지 (시뮬레이터면 늘 false)
     * @param onDepart 출발 전 화면(정차·PREPARE)인지 — 주행 화면에는 뜨지 않는다
     * @param hasSpeedOrGear 이번 신호에 속도나 기어 값이 있는지
     * @return 띠를 보여 줄지
     */
    fun update(nowMs: Long, vehicle: Boolean, onDepart: Boolean, hasSpeedOrGear: Boolean): Boolean {
        if (!vehicle) {
            since = null
            return false
        }
        if (hasSpeedOrGear && !emulator) signalSeen = true
        if (signalSeen || dismissed) return false
        if (!onDepart) {
            since = null
            return false
        }
        val start = since ?: nowMs.also { since = it }
        return nowMs - start >= waitMs
    }

    fun dismiss() {
        dismissed = true
    }

    companion object {
        const val WAIT_MS = 10_000L

        /** 에뮬레이터 기기인지 (Build 값만 본다 — Pleos 에뮬레이터: HARDWARE ranchu, MODEL "… emulator"). */
        fun isEmulator(hardware: String, model: String, fingerprint: String): Boolean =
            hardware in setOf("ranchu", "goldfish") ||
                model.contains("emulator", ignoreCase = true) ||
                fingerprint.startsWith("generic") || fingerprint.contains("emulator", ignoreCase = true)
    }
}
