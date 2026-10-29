package com.emop.pause.engine.pause

import com.emop.pause.core.model.PauseCause
import kotlin.math.max
import kotlin.math.min

enum class PauseActivity { NAP, EXPEDITION, PET_WALK, FOCUS, STRETCH }

enum class Audience { DRIVER, CHILD, PET }

/** 멈춤 활동 하나. [climateKeep]=false면 파워냅을 공조 없이 권한다(배터리 낮음). */
data class CuratedActivity(
    val kind: PauseActivity,
    val audience: Audience,
    val minutes: Int,
    val recommended: Boolean,
    val climateKeep: Boolean = true,
    /** 미션 대상 아이 나이 (원정대). */
    val childAge: Int? = null,
)

data class PauseInput(
    val cause: PauseCause,
    /** 충전 완료까지 남은 분 (충전 중일 때). 모르면 null → [chosenRestMin]. */
    val chargeRemainingMin: Int?,
    /** 사용자가 고른 휴식 시간(10/15/20분). 충전이 아닌 정차나 충전 속도를 모를 때. */
    val chosenRestMin: Int = DEFAULT_REST_MIN,
    val socPercent: Int?,
    val fatigueScore: Int,
    val sleepHours: Float?,
    val childAges: List<Int>,
    val dog: Boolean,
) {
    companion object {
        const val DEFAULT_REST_MIN = 15
    }
}

data class PausePlan(val totalMin: Int, val activities: List<CuratedActivity>, val napMinutes: Int?, val wakeBeforeEndMin: Int)

/**
 * 멈춤 큐레이터 (architecture §3.1 `engine:pause`, UI_SPEC §4.3, test_plan 6장).
 * - 정차 시간 = 충전 완료까지(충전 중) 또는 사용자가 고른 시간.
 * - 순서는 운전자 → 아이 → 반려동물. 동승자가 없으면 "업무 집중 15분"·"스트레칭 5분".
 * - 파워냅: 15분 이상 남았을 때, 최대 20분, 끝나기 2분 전에 깨운다. 배터리 20% 이하면 공조 유지형으로 권하지 않는다.
 * - 추천 1개: 피로 주의(40) 이상이거나 수면 6시간 미만이면 파워냅, 아니면 아이 원정대, 반려견 산책, 스트레칭 순.
 * - 남은 시간보다 긴 활동은 뺀다.
 */
object PauseCurator {
    const val NAP_MAX_MIN = 20
    const val NAP_MIN_TOTAL = 15
    const val WAKE_BEFORE_END_MIN = 2
    const val LOW_BATTERY_PERCENT = 20
    const val EXPEDITION_MIN = 15
    const val PET_WALK_MIN = 10
    const val FOCUS_MIN = 15
    const val STRETCH_MIN = 5
    const val EXPEDITION_MIN_AGE = 3

    fun curate(input: PauseInput): PausePlan {
        val total = max(1, input.chargeRemainingMin?.takeIf { input.cause == PauseCause.CHARGING } ?: input.chosenRestMin)
        val list = mutableListOf<CuratedActivity>()
        val napMin = if (total >= NAP_MIN_TOTAL) min(NAP_MAX_MIN, total - WAKE_BEFORE_END_MIN) else null
        val lowBattery = input.socPercent != null && input.socPercent <= LOW_BATTERY_PERCENT
        if (napMin != null) list += CuratedActivity(PauseActivity.NAP, Audience.DRIVER, napMin, false, climateKeep = !lowBattery)

        val kid = input.childAges.filter { it >= EXPEDITION_MIN_AGE }.minOrNull()
        if (kid != null && total >= EXPEDITION_MIN) list += CuratedActivity(PauseActivity.EXPEDITION, Audience.CHILD, EXPEDITION_MIN, false, childAge = kid)
        if (input.dog && total >= PET_WALK_MIN) list += CuratedActivity(PauseActivity.PET_WALK, Audience.PET, PET_WALK_MIN, false)
        if (input.childAges.isEmpty() && !input.dog) {
            if (total >= FOCUS_MIN) list += CuratedActivity(PauseActivity.FOCUS, Audience.DRIVER, FOCUS_MIN, false)
            list += CuratedActivity(PauseActivity.STRETCH, Audience.DRIVER, min(STRETCH_MIN, total), false)
        }

        val tired = input.fatigueScore >= 40 || (input.sleepHours != null && input.sleepHours < 6f)
        val pick = when {
            tired && napMin != null -> PauseActivity.NAP
            else -> listOf(PauseActivity.EXPEDITION, PauseActivity.PET_WALK, PauseActivity.STRETCH, PauseActivity.NAP, PauseActivity.FOCUS)
                .firstOrNull { k -> list.any { it.kind == k } }
        }
        val marked = list.map { it.copy(recommended = it.kind == pick) }
        return PausePlan(total, marked, napMin, WAKE_BEFORE_END_MIN)
    }
}
