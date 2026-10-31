package com.emop.pause.uistate

import java.util.Locale
import kotlin.math.roundToInt

/** 큰 비교 그래프의 한 점: 고갯길 시작에서 [km], 그때 주행 자극. */
data class ComparePoint(val km: Float, val index: Int)

/**
 * 발표 모드 "고갯길 비교" 전체 화면 (UPGRADE 1). 정지된 그림: 가로 거리 0~15km, 세로 지수 0~100, 선 2개.
 * 값은 `DemoCompareTest`가 만든 docs/plan/demo_compare.csv(10초 간격)·demo_compare_summary.txt를 그대로 쓴다.
 */
data class CompareChartUi(
    val unguided: List<ComparePoint>,
    val guided: List<ComparePoint>,
    val unguidedFinal: Int,
    val guidedFinal: Int,
    /** 지수 변화(%) — 음수면 줄어듦. */
    val dropPct: Int,
    /** 안내대로 가면 더 걸리는 시간(초). */
    val extraSec: Int,
    val roadKm: Double,
)

object CompareChart {
    fun parse(csv: String, summary: String): CompareChartUi {
        val rows = csv.lineSequence().drop(1).filter { it.isNotBlank() }.map { it.split(',') }.toList()

        fun line(mCol: Int, iCol: Int): List<ComparePoint> {
            val out = mutableListOf<ComparePoint>()
            rows.forEach { r ->
                val km = r[mCol].toFloat() / 1000f
                if (out.isEmpty() || km > out.last().km) out += ComparePoint(km, r[iCol].toInt())
            }
            return listOf(ComparePoint(0f, 0)) + out
        }

        fun num(key: String) = Regex("(?<![A-Za-z])$key=(\\d+)").find(summary)?.groupValues?.get(1)?.toInt()
            ?: error("요약에 $key 없음")
        fun sec(key: String) = Regex("(?<![A-Za-z])$key=\\d+\\(peak \\d+, (\\d+)s\\)").find(summary)?.groupValues?.get(1)?.toInt()
            ?: error("요약에 $key 시간 없음")

        val u = num("unguided")
        val g = num("guided")
        return CompareChartUi(
            unguided = line(1, 3),
            guided = line(4, 6),
            unguidedFinal = u,
            guidedFinal = g,
            dropPct = ((g - u) * 100.0 / u).roundToInt(),
            extraSec = sec("guided") - sec("unguided"),
            roadKm = num("length") / 1000.0,
        )
    }

    fun extraLabel(sec: Int): String = "+${sec / 60}분 ${sec % 60}초"

    fun kmLabel(km: Double): String = String.format(Locale.US, "%.1fkm", km)
}
