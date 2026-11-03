package com.emop.pause.engine.pet

import com.emop.pause.core.model.DoorState
import com.emop.pause.core.model.Gear

/** 문 이벤트로 띄울 기존 안내 (차량 이벤트 연결, 2026-11-02). 화면·문구는 기존 것을 쓰고 사실("문이 열렸어요")만 덧붙인다. */
enum class DoorEvent {
    /** a) 도착 후 P단 + 뒷문 열림 → "콩이 내렸나요?"를 60초 기다리지 않고 바로. */
    ARRIVAL_CHECK,
    /** b) 휴게소 정차 P단 + 운전석 문 열림 → 대기 모드를 켜지 않고 "잠깐 다녀올게" 제안 한 줄만. */
    WAIT_OFFER,
    /** c) 도착 후 P단 + 운전석 문 열림인데 "내렸나요?"에 답이 없거나 뒷문이 한 번도 안 열림 → 기존 안전 안내 한 번. */
    LEFT_BEHIND,
}

data class DoorInput(
    val gear: Gear?,
    val parkingBrakeOn: Boolean? = null,
    /** 문 신호. null이면(미지원) 문 이벤트는 모두 꺼진다. */
    val doors: DoorState?,
    /** 여행 중(출발 체크 뒤). 출발 전·여행 밖에서는 아무것도 하지 않는다. */
    val tripActive: Boolean,
    /** 목적지 도착·여행 끝(BoardingCheck와 같은 기준). 휴게소 정차는 false. */
    val tripEnding: Boolean,
    val waitActive: Boolean = false,
    /** "내렸어요"를 눌렀다. */
    val leftConfirmed: Boolean = false,
)

/**
 * 문 열림 → 기존 안내 (차량 이벤트 연결, 2026-11-02). 문이 **닫힘 → 열림**으로 바뀐 순간만 본다.
 * - 셋 다 P단(기어 신호가 없으면 주차 브레이크)일 때만. P단을 벗어나면 정차마다의 기록(제안·뒷문)을 지운다.
 * - a)는 정차마다 한 번, b)는 정차마다 한 번, c)는 여행마다 한 번. 반복 알림·경적·외부 전송은 하지 않는다.
 * - c)는 도착(여행 끝)에서만 본다. 휴게소 정차의 운전석 문은 b)(대기 모드 제안)가 맡는다.
 * - 뒷문 신호가 없으면 c)는 "내렸나요?" 답 여부만 본다.
 * 감지·진단이 아니라 문 신호 사실만 쓴다.
 */
class DoorWatch {
    private var lastDriver: Boolean? = null
    private var lastRear: Boolean? = null
    private var rearOpenedSinceParked = false
    private var arrivalAsked = false
    private var offered = false
    private var warned = false

    /** 새 여행. */
    fun reset() {
        lastDriver = null
        lastRear = null
        clearStop()
        warned = false
    }

    fun tick(i: DoorInput): List<DoorEvent> {
        val parked = when (i.gear) {
            Gear.PARK -> true
            null, Gear.UNKNOWN -> i.parkingBrakeOn == true
            else -> false
        }
        val d = i.doors
        val driver = d?.driverOpen
        val rear = d?.rearOpen
        val driverOpened = driver == true && lastDriver == false
        val rearOpened = rear == true && lastRear == false
        lastDriver = driver
        lastRear = rear
        if (!parked) {
            clearStop()
            return emptyList()
        }
        if (d == null || !i.tripActive) return emptyList()
        if (rear == true) rearOpenedSinceParked = true

        val out = mutableListOf<DoorEvent>()
        if (i.tripEnding) {
            if (rearOpened && !arrivalAsked && !i.leftConfirmed) {
                arrivalAsked = true
                out += DoorEvent.ARRIVAL_CHECK
            }
            val rearNever = rear != null && !rearOpenedSinceParked
            if (driverOpened && !warned && (!i.leftConfirmed || rearNever)) {
                warned = true
                out += DoorEvent.LEFT_BEHIND
            }
        } else if (driverOpened && !offered && !i.waitActive) {
            offered = true
            out += DoorEvent.WAIT_OFFER
        }
        return out
    }

    private fun clearStop() {
        rearOpenedSinceParked = false
        arrivalAsked = false
        offered = false
    }
}
