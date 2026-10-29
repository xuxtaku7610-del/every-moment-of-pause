package com.emop.pause.engine.motion

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.tan

/** 2차 버터워스 필터 한 단 (쌍선형 변환, 직접형 I). */
internal class Biquad private constructor(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double,
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun step(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }

    fun copy() = Biquad(b0, b1, b2, a1, a2).also { it.x1 = x1; it.x2 = x2; it.y1 = y1; it.y2 = y2 }

    companion object {
        private val Q = 1 / sqrt(2.0)

        fun lowPass(cutHz: Double, sampleHz: Double): Biquad {
            val k = tan(PI * cutHz / sampleHz)
            val n = 1 / (1 + k / Q + k * k)
            val b0 = k * k * n
            return Biquad(b0, 2 * b0, b0, 2 * (k * k - 1) * n, (1 - k / Q + k * k) * n)
        }

        fun highPass(cutHz: Double, sampleHz: Double): Biquad {
            val k = tan(PI * cutHz / sampleHz)
            val n = 1 / (1 + k / Q + k * k)
            return Biquad(n, -2 * n, n, 2 * (k * k - 1) * n, (1 - k / Q + k * k) * n)
        }
    }
}

/**
 * 멀미 대역 필터: 고역통과 [MotionConstants.BAND_LOW_HZ] + 저역통과 [MotionConstants.BAND_HIGH_HZ] (docs/plan/motion_model.md §3).
 * 저역통과 차단 주파수는 나이퀴스트에 걸리지 않게 `min(0.5Hz, 0.45·fs)`.
 */
class BandPass private constructor(private val hp: Biquad, private val lp: Biquad) {
    constructor(sampleHz: Double) : this(
        Biquad.highPass(MotionConstants.BAND_LOW_HZ, sampleHz),
        Biquad.lowPass(min(MotionConstants.BAND_HIGH_HZ, MotionConstants.MAX_LOW_PASS_RATIO * sampleHz), sampleHz),
    )

    fun step(x: Double): Double = lp.step(hp.step(x))

    fun copy() = BandPass(hp.copy(), lp.copy())
}
