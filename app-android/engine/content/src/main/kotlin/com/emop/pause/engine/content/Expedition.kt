package com.emop.pause.engine.content

/**
 * 휴게소 원정대 (COMMERCIAL_READINESS E P0, test_plan T6-3).
 * - 정차 원인(휴게소 휴식/충전)과 아이 나이에 맞는 미션 3개, 이번 여행에서 한 미션은 다시 내지 않는다.
 * - 3개를 다 하면 다음 구간 라디오가 열린다.
 * - 정차 중에만 진행: 주행이 시작되면 [onDrivingStarted]로 남은 미션을 접는다(완료한 것은 유지).
 */
class ExpeditionTracker(private val missions: List<Mission>) {
    var current: List<Mission> = emptyList()
        private set
    private val done = mutableSetOf<String>()
    private val usedThisTrip = mutableSetOf<String>()

    /** 지금 원정대가 끝나서 다음 구간 라디오가 열렸는가. */
    var radioUnlocked = false
        private set

    /** 정차가 시작되면 미션 3개를 고른다. 나이에 맞는 미션이 모자라면 있는 만큼. */
    fun start(charging: Boolean, childAge: Int, count: Int = 3): List<Mission> {
        val ctx = if (charging) "charging" else "rest_stop"
        val fits = missions.filter { it.minAge <= childAge && it.id !in usedThisTrip }
        // 이 정차 원인에 맞는 미션 먼저, 모자라면 다른 원인 미션으로 채운다(충전 중에도 휴게소 미션은 할 수 있다).
        val picked = (fits.filter { it.context == ctx } + fits.filter { it.context != ctx }).take(count)
        current = picked
        done.clear()
        radioUnlocked = false
        usedThisTrip += picked.map { it.id }
        return picked
    }

    fun complete(missionId: String): Boolean {
        if (current.none { it.id == missionId }) return false
        done += missionId
        if (current.isNotEmpty() && current.all { it.id in done }) radioUnlocked = true
        return true
    }

    fun isDone(missionId: String) = missionId in done

    /** 주행 시작: 화면에서 내리고, 열린 라디오는 다음 구간에 쓴다. */
    fun onDrivingStarted() {
        current = emptyList()
        done.clear()
    }

    /** 다음 구간 라디오를 썼다. */
    fun consumeRadio() {
        radioUnlocked = false
    }

    fun resetTrip() {
        onDrivingStarted()
        usedThisTrip.clear()
        radioUnlocked = false
    }
}
