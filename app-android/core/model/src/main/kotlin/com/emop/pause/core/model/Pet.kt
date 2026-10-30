package com.emop.pause.core.model

/** 체급. 멀미 민감도와 쉼표 추천(시설)에 쓴다 (docs/plan/motion_model.md §5). */
enum class PetSize { SMALL, MEDIUM, LARGE }

/** 보호자가 답한 멀미 성향(이력) 3단계. */
enum class MotionTendency { LOW, NORMAL, HIGH }

/**
 * 반려견 프로필 (CONCEPT 6장). 기기(Room)에만 저장하고 서버로 보내지 않는다 (architecture §6).
 * [facilityPrefs]는 휴게소 편의시설 코드(`pet_zone` 등) — 쉼표 추천 가중치에 쓴다.
 */
data class PetProfile(
    val name: String,
    val size: PetSize = PetSize.MEDIUM,
    val ageYears: Int = 3,
    val motionTendency: MotionTendency = MotionTendency.NORMAL,
    val pottyIntervalMin: Int = DEFAULT_POTTY_MIN,
    val facilityPrefs: Set<String> = setOf("pet_zone"),
    /** 개인 보정 계수 (UPGRADE 4, 도착 뒤 "멀미했어요?" 답으로 맞춤). 멀미 민감도에 곱한다. 1.0 = 보정 없음. pet.db에는 저장하지 않는다(CalibrationStore). */
    val motionScale: Double = 1.0,
) {
    /** 입력 화면·저장 전에 부르는 검사. 문제가 없으면 빈 목록. */
    fun problems(): List<String> = buildList {
        if (name.isBlank() || name.length > NAME_MAX) add("이름은 1~$NAME_MAX 글자로 적어 주세요.")
        if (ageYears !in 0..AGE_MAX) add("나이는 0~${AGE_MAX}살 사이로 적어 주세요.")
        if (pottyIntervalMin !in POTTY_RANGE) add("배변 간격은 ${POTTY_RANGE.first}~${POTTY_RANGE.last}분 사이로 골라 주세요.")
    }

    companion object {
        const val DEFAULT_POTTY_MIN = 120
        const val NAME_MAX = 20
        const val AGE_MAX = 30
        val POTTY_RANGE = 30..480

        /** 프로필을 아직 만들지 않았을 때. 이름은 기본 호칭. */
        val DEFAULT = PetProfile("반려견")
    }
}
