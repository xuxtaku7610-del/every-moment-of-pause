package com.emop.pause.engine.pause

import com.emop.pause.core.model.PauseCause
import kotlin.math.max
import kotlin.math.min

/** 쉼표에서 할 일. 반려견 산책·물과 배변 챙기기·운전자 스트레칭 (CONCEPT F2). */
enum class PauseActivity { PET_WALK, PET_CARE, STRETCH }

enum class Audience { DRIVER, PET }

data class CuratedActivity(
    val kind: PauseActivity,
    val audience: Audience,
    val minutes: Int,
    val recommended: Boolean,
)

data class PauseInput(
    val cause: PauseCause,
    /** 충전 완료까지 남은 분 (충전 중일 때). 모르면 null → [chosenRestMin]. */
    val chargeRemainingMin: Int?,
    /** 사용자가 고른 휴식 시간(10/15/20분). 충전이 아닌 정차나 충전 속도를 모를 때. */
    val chosenRestMin: Int = DEFAULT_REST_MIN,
    val fatigueScore: Int,
    val dog: Boolean,
    /** 멀미 지수 0~100 (MotionComfortEngine). 높으면 산책보다 물·안정이 먼저. */
    val motionIndex: Int = 0,
) {
    companion object {
        const val DEFAULT_REST_MIN = 15
    }
}

data class PausePlan(val totalMin: Int, val activities: List<CuratedActivity>)

/**
 * 쉼표 큐레이터 (architecture §3.1 `engine:pause`).
 * - 정차 시간 = 충전 완료까지(충전 중) 또는 사용자가 고른 시간.
 * - 반려견이 있으면 산책 10분 → 물·배변 5분, 운전자 스트레칭 5분.
 * - 추천 1개: 멀미 지수가 주의(40) 이상이면 물·배변(안정 먼저), 반려견이 있으면 산책, 아니면 스트레칭.
 * - 남은 시간보다 긴 활동은 뺀다.
 */
object PauseCurator {
    const val PET_WALK_MIN = 10
    const val PET_CARE_MIN = 5
    const val STRETCH_MIN = 5
    const val MOTION_CAUTION = 40

    fun curate(input: PauseInput): PausePlan {
        val total = max(1, input.chargeRemainingMin?.takeIf { input.cause == PauseCause.CHARGING } ?: input.chosenRestMin)
        val list = mutableListOf<CuratedActivity>()
        if (input.dog && total >= PET_WALK_MIN) list += CuratedActivity(PauseActivity.PET_WALK, Audience.PET, PET_WALK_MIN, false)
        if (input.dog) list += CuratedActivity(PauseActivity.PET_CARE, Audience.PET, min(PET_CARE_MIN, total), false)
        list += CuratedActivity(PauseActivity.STRETCH, Audience.DRIVER, min(STRETCH_MIN, total), false)

        val pick = when {
            input.dog && input.motionIndex >= MOTION_CAUTION -> PauseActivity.PET_CARE
            else -> listOf(PauseActivity.PET_WALK, PauseActivity.PET_CARE, PauseActivity.STRETCH).first { k -> list.any { it.kind == k } }
        }
        return PausePlan(total, list.map { it.copy(recommended = it.kind == pick) })
    }
}
