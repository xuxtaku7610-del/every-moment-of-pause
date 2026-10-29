package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.data.PetPlace
import com.emop.pause.engine.pet.Josa
import com.emop.pause.uistate.DrivingRules

/**
 * 도착 10분 전 산책 제안 (CONCEPT F4, test_plan F4-03). 한 여행에 한 번, 주행 중 남은 시간이 [LEAD_SEC] 안에 들어올 때.
 * 장소는 목적지 반경 [RADIUS_M] 안의 산책로 → 반려견 동반 카페 순, 가까운 곳. 없으면 말하지 않는다(지어내지 않음).
 */
object ArrivalWalk {
    const val LEAD_SEC = 600
    const val RADIUS_M = 3_000
    val CATEGORIES = setOf("walk", "cafe")

    fun due(mode: AppMode, remainTimeSec: Int?, done: Boolean): Boolean =
        !done && mode == AppMode.DRIVING && remainTimeSec != null && remainTimeSec in 1..LEAD_SEC

    fun pick(places: List<PetPlace>): PetPlace? =
        places.filter { it.category == "walk" }.minByOrNull { it.offKm ?: Double.MAX_VALUE }
            ?: places.filter { it.category == "cafe" }.minByOrNull { it.offKm ?: Double.MAX_VALUE }

    /** 주행 중 한 줄(48자 이하). 장소 이름의 "[샘플] " 표시는 소리 내지 않는다. 길면 이름을 뺀다. */
    fun phrase(petName: String, place: PetPlace): String {
        val name = place.name.removePrefix("[샘플] ").trim()
        val walk = place.category == "walk"
        val full = "곧 도착해요. 근처 ${name}에서 ${Josa.waGwa(petName)} ${if (walk) "산책" else "쉬어 가기"} 어때요?"
        return if (full.length <= DrivingRules.MAX_BLOCK_CHARS) full
        else "곧 도착해요. 목적지 근처 ${if (walk) "산책로" else "반려견 동반 카페"}를 찾아 뒀어요."
    }
}
