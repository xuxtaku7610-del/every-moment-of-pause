package com.emop.pause.trip

import com.emop.pause.core.model.LatLng
import com.emop.pause.data.Vet

/**
 * 시연용 제휴 동물병원 (사용자 결정 10/31): 병원 사전 접수함은 **가상의 시연 병원 한 곳**만 쓴다. 실제 병원(공공 인허가 데이터)과는
 * 제휴가 없으므로 접수·"접수 완료"를 보이지 않고 전화·도착 예정 링크 공유만 한다. 시뮬레이터에서만 목록 맨 앞에 넣는다.
 * 위치는 지금 자리에서 약 1km 떨어진 가상 지점이다 — 실제 병원이 아니라는 것을 이름·주소에 쓴다.
 */
object DemoPartner {
    const val ID = "demo-partner"
    const val NAME = "시연용 제휴 동물병원"
    const val ADDRESS = "시연용 가상 병원 — 실제 병원이 아니에요"

    fun vet(at: LatLng): Vet = Vet(ID, NAME, at.lat + 0.007, at.lng + 0.006, 1.0, ADDRESS, null, sample = true, partner = true)

    /** 시뮬레이터면 시연 병원을 맨 앞에, 실차면 그대로. */
    fun prepend(items: List<Vet>, at: LatLng?, simulation: Boolean): List<Vet> =
        if (!simulation || at == null) items else listOf(vet(at)) + items.filter { it.id != ID }

    /** 주행 중 음성 동의: "네·응·보내" = 예, "아니·안·싫·말아" = 아니요, 모르면 null. */
    fun consent(heard: String?): Boolean? {
        val t = heard?.replace(" ", "") ?: return null
        return when {
            listOf("아니", "안돼", "안보내", "싫", "말아", "하지마").any { it in t } -> false
            listOf("네", "예", "응", "보내", "좋아", "그래").any { it in t } -> true
            else -> null
        }
    }
}
