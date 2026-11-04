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
    /** 개인 보정 계수 (UPGRADE 4, 2026-10-31부터 음성 신고에서 자동 — AutoCalibration). 주행 자극 민감도에 곱한다. 1.0 = 보정 없음. pet.db에는 저장하지 않는다(CalibrationStore). */
    val motionScale: Double = 1.0,
    /** 기기 저장 행 id (v10-3 여러 마리, 최대 3). 0 = 아직 저장 안 함 또는 합친 설정. */
    val id: Long = 0,
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

/**
 * 여러 마리 (v10-3 1번, 최대 [MAX]): 오늘 타는 아이들을 엔진 설정 하나로 합친다 — **가장 예민한 아이 기준**.
 * 배변·산책 간격은 가장 짧은 아이, 흔들림에 예민한 정도는 가장 높은 아이, 체급·나이는 가장 큰 값, 시설 선호는 합집합.
 * 개인 보정 계수는 앱이 아이별 기록에서 가장 큰 값을 따로 곱한다. 이름은 "콩이·보리"(문장·기록에 그대로).
 */
object PetGroup {
    const val MAX = 3
    const val SEP = "·"

    fun combine(riders: List<PetProfile>): PetProfile {
        if (riders.size <= 1) return riders.firstOrNull() ?: PetProfile.DEFAULT
        return PetProfile(
            name = riders.joinToString(SEP) { it.name },
            size = riders.maxOf { it.size },
            ageYears = riders.maxOf { it.ageYears },
            motionTendency = riders.maxOf { it.motionTendency },
            pottyIntervalMin = riders.minOf { it.pottyIntervalMin },
            facilityPrefs = riders.flatMap { it.facilityPrefs }.toSet(),
            motionScale = riders.maxOf { it.motionScale },
        )
    }

    /** 말에 들어 있는 아이 이름 (긴 이름 먼저 — "보리"와 "보리차"가 있으면 "보리차"). 없으면 null. */
    fun findName(text: String, names: List<String>): String? =
        names.filter { it.isNotBlank() }.sortedByDescending { it.length }.firstOrNull { it in text }

    /**
     * "콩이만 타" · "콩이랑 보리만 탔어" → 그 아이들만 오늘 타는 아이로. "만 타/만 탔"이 없거나 아는 이름이 없으면 null.
     */
    fun onlyRiders(text: String, pets: List<PetProfile>): List<PetProfile>? {
        if (!Regex("""만\s*(타|탔)""").containsMatchIn(text)) return null
        val picked = pets.filter { p -> p.name.isNotBlank() && p.name in text }
        return picked.takeIf { it.isNotEmpty() }
    }
}
