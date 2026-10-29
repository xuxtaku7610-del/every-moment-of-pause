package com.emop.pause.engine.motion

import com.emop.pause.core.model.PetProfile
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** 비교 실험 한 줄 (10초마다). */
data class CompareRow(val tSec: Int, val unguidedM: Double, val unguidedKph: Double, val unguidedIndex: Int, val guidedM: Double, val guidedKph: Double, val guidedIndex: Int)

data class CompareResult(
    val rows: List<CompareRow>,
    val unguidedFinal: Int,
    val guidedFinal: Int,
    val unguidedPeak: Int,
    val guidedPeak: Int,
    val unguidedSec: Int,
    val guidedSec: Int,
    /** 안내대로 운전의 가장 낮은 속도(km/h, 가장 급한 굽이). */
    val guidedMinKph: Int,
    /** 안내대로 운전의 평균 속도(km/h). */
    val guidedAvgKph: Int,
)

/**
 * 시연 비교 실험 (CONCEPT 4장 "안내 없이 vs 안내대로", test_plan F1-07, docs/plan/demo_compare.md).
 * 같은 경로 형상·같은 프로필로 두 운전을 1초씩 돌린다.
 * - 안내 없이: [cruiseKph] 그대로 굽은 길에 들어간다.
 * - 안내대로: 굽이마다 편안한 횡가속([MotionConstants.A_COMFORT])이 되는 속도 아래로, 굽이 앞에서 [DECEL_MPS2]로
 *   미리 감속하고(조금 일찍 감속) 굽이를 지나면 [ACCEL_MPS2]로 천천히 돌아간다. 속도 계획은 경로 형상 10m 격자에서
 *   뒤로 한 번(제동), 앞으로 한 번(가속) 훑어 만든다.
 * 횡가속은 경로 곡률(v²κ), 종방향은 속도 변화로 계산하므로 감속·가속 자체의 흔들림도 지수에 들어간다.
 */
object MotionCompare {
    const val DECEL_MPS2 = 0.5
    const val ACCEL_MPS2 = 0.4
    const val ROW_EVERY_SEC = 10

    fun run(shape: RouteShape, profile: PetProfile, cruiseKph: Double = 45.0): CompareResult {
        val cruise = cruiseKph / 3.6
        val plan = speedPlan(shape, cruise)
        val a = MotionComfortEngine(profile, shape)
        val b = MotionComfortEngine(profile, shape)
        var sa = 0.0
        var sb = 0.0
        var t = 0
        var doneA: Int? = null
        var doneB: Int? = null
        var minB = cruise
        val rows = mutableListOf<CompareRow>()
        while (doneA == null || doneB == null) {
            if (doneA == null) {
                sa += cruise
                a.update(MotionInput(t * 1000L, (cruise * 3.6).toFloat(), routeM = min(sa, shape.lengthM)))
                if (sa >= shape.lengthM) doneA = t
            }
            val vb = plan(sb)
            if (doneB == null) {
                minB = min(minB, vb)
                sb += vb
                b.update(MotionInput(t * 1000L, (vb * 3.6).toFloat(), routeM = min(sb, shape.lengthM)))
                if (sb >= shape.lengthM) doneB = t
            }
            if (t % ROW_EVERY_SEC == 0) {
                rows += CompareRow(t, min(sa, shape.lengthM), if (doneA == null) cruise * 3.6 else 0.0, a.state.index, min(sb, shape.lengthM), if (doneB == null) vb * 3.6 else 0.0, b.state.index)
            }
            t++
            check(t < 24 * 3600) { "비교 실험이 끝나지 않음" }
        }
        val secB = doneB!! + 1
        return CompareResult(
            rows, a.state.index, b.state.index, a.state.peak, b.state.peak, doneA!! + 1, secB,
            (minB * 3.6).toInt(), (shape.lengthM / secB * 3.6).toInt(),
        )
    }

    /** 위치(m) → 안내대로 속도(m/s). 굽이 제한 속도를 뒤로(제동)·앞으로(가속) 훑어 이어 붙인다. */
    fun speedPlan(shape: RouteShape, cruise: Double): (Double) -> Double {
        val step = RouteShape.STEP_M
        val n = (shape.lengthM / step).toInt() + 2
        val v = DoubleArray(n) { i ->
            val k = abs(shape.curvatureAt(i * step))
            if (k < 1e-6) cruise else min(cruise, sqrt(MotionConstants.A_COMFORT / k))
        }
        for (i in n - 2 downTo 0) v[i] = min(v[i], sqrt(v[i + 1] * v[i + 1] + 2 * DECEL_MPS2 * step))
        for (i in 1 until n) v[i] = min(v[i], sqrt(v[i - 1] * v[i - 1] + 2 * ACCEL_MPS2 * step))
        return { pos ->
            val x = (pos / step).coerceIn(0.0, (n - 1).toDouble())
            val i = x.toInt().coerceAtMost(n - 2)
            max(1.0, v[i] + (v[i + 1] - v[i]) * (x - i))
        }
    }

    fun csv(r: CompareResult): String = buildString {
        appendLine("t_sec,unguided_m,unguided_kph,unguided_index,guided_m,guided_kph,guided_index")
        r.rows.forEach {
            appendLine(String.format(Locale.US, "%d,%.0f,%.1f,%d,%.0f,%.1f,%d", it.tSec, it.unguidedM, it.unguidedKph, it.unguidedIndex, it.guidedM, it.guidedKph, it.guidedIndex))
        }
    }
}
