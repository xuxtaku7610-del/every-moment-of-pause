package com.emop.pause.engine.pet

import com.emop.pause.core.model.Gear

/** 출발 전 체크 항목 (CONCEPT F6). */
enum class CheckItem { HARNESS, WATER, POTTY_BAGS }

/** "콩이 내렸나요?" 알림. [strong]이면 더우니 더 강하게(소리·붉은 화면). */
data class ArrivalPrompt(val strong: Boolean, val cabinTempC: Float?)

data class ArrivalInput(
    val nowMs: Long,
    val gear: Gear?,
    val parkingBrakeOn: Boolean? = null,
    /** 목적지에 도착했거나(ARRIVED·목적지 300m 안) 사용자가 여행을 끝냈다. 휴게소 정차는 false. */
    val tripEnding: Boolean,
    val cabinTempC: Float? = null,
    /** 1~12. 온도 신호가 없을 때 여름철(6~9월)이면 강한 알림. */
    val month: Int,
)

/**
 * 타고·내릴 때 체크 (CONCEPT F6, test_plan F6-01~F6-04).
 * - 출발 전: 하네스/켄넬·물·배변봉투를 모두 확인해야 출발할 수 있다.
 * - 도착: P단(기어 신호가 없으면 주차 브레이크)이 [END_HOLD_SEC] 이상 이어지고 여행이 끝나는 중이면 "내렸나요?"를 한 번 낸다.
 *   26℃ 이상이거나, 온도를 모르는데 여름철이면 강한 알림. "내렸어요"를 누를 때까지 다시 묻지 않고 기다린다.
 */
class BoardingCheck {
    private val checked = mutableSetOf<CheckItem>()
    private var parkedSinceMs: Long? = null
    private var prompted = false

    var pending: ArrivalPrompt? = null
        private set

    var confirmedLeft = false
        private set

    val checkedItems: Set<CheckItem> get() = checked.toSet()
    val readyToDepart: Boolean get() = checked.containsAll(CheckItem.entries)

    fun toggle(item: CheckItem) {
        if (!checked.remove(item)) checked += item
    }

    /** 새 여행 준비: 체크와 도착 상태를 지운다. */
    fun reset() {
        checked.clear()
        parkedSinceMs = null
        prompted = false
        pending = null
        confirmedLeft = false
    }

    /** 1초마다. 새로 알림을 띄워야 하면 돌려준다(한 번만). */
    fun tick(i: ArrivalInput): ArrivalPrompt? {
        val parked = when (i.gear) {
            Gear.PARK -> true
            null, Gear.UNKNOWN -> i.parkingBrakeOn == true
            else -> false
        }
        if (!parked) {
            parkedSinceMs = null
            return null
        }
        val since = parkedSinceMs ?: i.nowMs.also { parkedSinceMs = it }
        if (prompted || !i.tripEnding || i.nowMs - since < END_HOLD_SEC * 1000) return null
        prompted = true
        val strong = i.cabinTempC?.let { it >= HOT_C } ?: (i.month in SUMMER)
        return ArrivalPrompt(strong, i.cabinTempC).also { pending = it }
    }

    /**
     * 문 이벤트(DoorWatch a·c)로 지금 바로 "내렸나요?" — 60초를 기다리지 않는다. P단·여행 끝일 때만, 여행마다 한 번(이미 물었으면 null).
     */
    fun promptNow(i: ArrivalInput): ArrivalPrompt? {
        val parked = when (i.gear) {
            Gear.PARK -> true
            null, Gear.UNKNOWN -> i.parkingBrakeOn == true
            else -> false
        }
        if (prompted || confirmedLeft || !parked || !i.tripEnding) return null
        prompted = true
        val strong = i.cabinTempC?.let { it >= HOT_C } ?: (i.month in SUMMER)
        return ArrivalPrompt(strong, i.cabinTempC).also { pending = it }
    }

    /** "내렸어요". 이때부터 이동 기록 화면으로 넘어갈 수 있다 (F6-04). */
    fun confirmLeft() {
        pending = null
        confirmedLeft = true
    }

    companion object {
        const val END_HOLD_SEC = 60
        const val HOT_C = 26f
        val SUMMER = 6..9
    }
}
