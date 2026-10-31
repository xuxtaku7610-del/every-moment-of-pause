package com.emop.pause.trip

import com.emop.pause.engine.motion.CalibrationSample
import com.emop.pause.engine.motion.MotionAnswer
import com.emop.pause.engine.motion.MotionCalibration
import com.emop.pause.engine.motion.MotionConstants
import com.emop.pause.engine.pet.DistressKind

/**
 * 개인 보정 자동 표본 (2026-10-31 재정비 — 도착 뒤 "오늘 멀미했어요?" 질문 대신). 보호자 음성 신고와 주행 자극만 본다:
 * - 신고: 구토 = 예, 침 흘림·헐떡임 = 조금 (가중치 1). 한 여행에서 더 강한 답이 나올 때만 표본을 더한다(헐떡임을 세 번 말해도 한 번).
 * - 신고 없음: 주행 자극 높음(70) 구간을 지난 뒤 [MotionCalibration.QUIET_WINDOW_SEC] 동안 신고가 없으면 "아니요"를
 *   [MotionCalibration.QUIET_WEIGHT]로 한 번(한 여행에 한 번). 높음 구간이 없던 여행·신고 없는 평범한 날은 아무것도 남기지 않는다(정보 없음).
 * 시각은 신호 시계(초, 시뮬레이션은 시나리오 시계)라 장면 점프로 따라잡아도 같은 결과다. 순수 로직 — 단위 테스트(test_plan F7-05b).
 */
class AutoCalibration {
    private var best: MotionAnswer? = null
    private var inHigh = false
    private var highPeak = 0
    private var quietFromSec: Long? = null
    private var quietUsed = false

    fun reset() {
        best = null
        inHigh = false
        highPeak = 0
        quietFromSec = null
        quietUsed = false
    }

    /** 1초마다: 주행 자극 [index]. 조용한 구간이 [MotionCalibration.QUIET_WINDOW_SEC]를 채우면 약한 "아니요" 표본. */
    fun onTick(sec: Long, index: Int): CalibrationSample? {
        if (index >= MotionConstants.HIGH) {
            inHigh = true
            highPeak = maxOf(highPeak, index)
            quietFromSec = null
        } else if (inHigh && index < MotionConstants.HIGH - HYSTERESIS) {
            inHigh = false
            if (!quietUsed && best == null) quietFromSec = sec
        }
        val from = quietFromSec ?: return null
        if (sec - from < MotionCalibration.QUIET_WINDOW_SEC) return null
        quietFromSec = null
        quietUsed = true
        return CalibrationSample(highPeak, MotionAnswer.NO, MotionCalibration.QUIET_WEIGHT)
    }

    /** 신고 한 건. [peak]는 그때까지 주행 자극 최고(최근 30분). 보정에 쓰지 않는 신고(불안·앞좌석 등)면 null. */
    fun onReport(kind: DistressKind, peak: Int): CalibrationSample? {
        quietFromSec = null
        val answer = answerOf(kind) ?: return null
        if (best != null && strength(answer) <= strength(best!!)) return null
        best = answer
        return CalibrationSample(peak, answer)
    }

    companion object {
        const val HYSTERESIS = 5

        fun answerOf(kind: DistressKind): MotionAnswer? = when (kind) {
            DistressKind.VOMIT -> MotionAnswer.YES
            DistressKind.NAUSEA, DistressKind.PANTING -> MotionAnswer.LITTLE
            else -> null
        }

        private fun strength(a: MotionAnswer) = when (a) {
            MotionAnswer.YES -> 2
            MotionAnswer.LITTLE -> 1
            MotionAnswer.NO -> 0
        }
    }
}
