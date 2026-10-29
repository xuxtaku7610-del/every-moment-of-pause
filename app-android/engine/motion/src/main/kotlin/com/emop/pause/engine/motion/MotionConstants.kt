package com.emop.pause.engine.motion

import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize

/**
 * 멀미 예보 모델의 계수를 모두 여기 둔다. 근거·가정은 docs/plan/motion_model.md와 같다.
 * 모두 문헌을 참고한 **가정값**이며 반려견 대상 검증은 없다 — 화면에서는 "주의 예보"로만 쓴다.
 */
object MotionConstants {
    /**
     * 대역 하한(Hz). CONCEPT 4장은 ISO 2631-1 부록 D(수직 Wf)의 0.1~0.5Hz였지만, 수평 흔들림은 0.2Hz 아래에서도
     * 가속도 기준 감수성이 거의 같다(Donohew & Griffin 2004). 고갯길 좌우 교대(10~20초 주기, 0.03~0.07Hz)를
     * 놓치지 않도록 하한을 0.02Hz로 낮췄다 (motion_model.md §3, 결정 2026-10-29). 직류(일정한 가속·곡선)는 여전히 거른다.
     */
    const val BAND_LOW_HZ = 0.02

    /** 대역 상한(Hz). 1Hz 신호에서는 나이퀴스트에 걸리므로 [MAX_LOW_PASS_RATIO]·fs로 낮춘다. */
    const val BAND_HIGH_HZ = 0.5
    const val MAX_LOW_PASS_RATIO = 0.45

    /** 수평 두 축 가중 (§4). ISO 2631-1 멀미 평가는 수직축만 정의 — 승용차 수평 흔들림에 옮긴 가정. */
    const val K_LONGITUDINAL = 1.0
    const val K_LATERAL = 1.0

    /** 주행 중 누적량 누설 시간상수(초): 같은 흔들림이 오래 이어지면 증가가 느려진다는 적응 가정 (§4). */
    const val TAU_DRIVE_SEC = 120 * 60.0

    /** 정차 중 회복 시간상수(초): 휴식하면 줄어든다 (CONCEPT 4장 "정차·휴식 시 감소"). */
    const val TAU_REST_SEC = 15 * 60.0

    /** 이 속도(m/s) 아래면 정차로 본다 (1km/h). */
    const val STOP_SPEED_MPS = 1 / 3.6

    /** 지수 척도 M₀ (m/s^1.5): 보통 프로필이 S자 고갯길 30분(MSDV ≈ 75)에 주의(≈40)에 닿도록 맞춘 값 (§4). */
    const val INDEX_SCALE_MSDV = 140.0

    const val CAUTION = 40
    const val HIGH = 70

    /** 이 간격(초)보다 신호가 끊기면 필터·이전 값을 버리고 새로 시작한다. */
    const val MAX_GAP_SEC = 3.0

    /** 예보 범위(분): 현재 속도로 이만큼 앞을 본다 (CONCEPT 4장 "앞으로 10~20분"). */
    const val FORECAST_MIN = 20

    /** 위험 구간 판정 횡가속(m/s²): v²·|κ|가 이 이상이면 멀미를 부르는 굽은 길 (§6, 가정). */
    const val A_RISK = 1.0

    /**
     * 예보에서 "안내 없이" 운전자가 굽은 길에서 실제로 내는 횡가속 상한(m/s², 가정). 고속도로 속도 그대로
     * 고갯길에 들어간다고 보면 예상 지수가 터무니없이 커지므로, 굽이마다 이 값 이하로 감속한다고 본다 (§6).
     */
    const val A_DRIVER_MAX = 3.0

    /** 권장 속도 계산용 편안한 횡가속(m/s²) (§6, 가정). */
    const val A_COMFORT = 1.0

    /** 가까운 위험 지점을 한 구간으로 합치는 간격(m). */
    const val MERGE_GAP_M = 300.0

    /** 같은 구간 예보를 다시 말하지 않는 시간(분) (F1-10). */
    const val FORECAST_COOLDOWN_MIN = 10

    /** 프로필 민감도 S (§5). 곱으로 합친다. */
    fun sensitivity(p: PetProfile): Double {
        val size = when (p.size) {
            PetSize.SMALL -> 1.2
            PetSize.MEDIUM -> 1.0
            PetSize.LARGE -> 0.9
        }
        val age = when {
            p.ageYears < 1 -> 1.3
            p.ageYears >= 8 -> 1.1
            else -> 1.0
        }
        val tendency = when (p.motionTendency) {
            MotionTendency.LOW -> 0.7
            MotionTendency.NORMAL -> 1.0
            MotionTendency.HIGH -> 1.5
        }
        return size * age * tendency
    }
}
