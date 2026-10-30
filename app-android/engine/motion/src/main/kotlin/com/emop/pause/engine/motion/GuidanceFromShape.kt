package com.emop.pause.engine.motion

import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.TurnKind
import kotlin.math.abs

/**
 * 가짜 NaviHelper 회전 안내 (UPGRADE 5): 도로 형상(OSM GPX)에서 내비가 할 법한 회전 안내를 만든다 — 종류와 거리만.
 * 굽이(|κ| ≥ [KAPPA_MIN]가 이어진 구간)마다 방위 변화 합 Δψ로 종류를 정하고 구간 가운데를 안내 지점으로 둔다:
 * |Δψ| ≥ 110° 급회전, ≥ 45° 좌·우회전, ≥ 15° 차로 유지(왼쪽 +). 그보다 작은 흔들림은 안내하지 않는다.
 *
 * 주의: 에뮬레이터 실제 안내(성남→강릉, 2026-10-30)에는 교차로·나들목·톨게이트·휴게소만 있었고 "굽은 길" 안내는 없었다.
 * 그래서 이 목록은 **내비가 고갯길 굽이까지 알려 준다고 가정한 낙관적인 경우**다 (docs/plan/shape_compare.md).
 */
class GuidanceFromShape(shape: RouteShapeProvider) {
    /** (경로 거리 m, 종류) — 시작에서 끝까지. */
    val points: List<Pair<Double, TurnKind>>

    init {
        val out = mutableListOf<Pair<Double, TurnKind>>()
        var s = 0.0
        var segStart: Double? = null
        var dPsi = 0.0
        while (s <= shape.lengthM) {
            val k = shape.curvatureAt(s)
            if (abs(k) >= KAPPA_MIN) {
                if (segStart == null) {
                    segStart = s
                    dPsi = 0.0
                }
                dPsi += k * STEP_M
            } else if (segStart != null) {
                kindOf(Math.toDegrees(dPsi))?.let { out += ((segStart + s) / 2) to it }
                segStart = null
            }
            s += STEP_M
        }
        if (segStart != null) kindOf(Math.toDegrees(dPsi))?.let { out += ((segStart + s) / 2) to it }
        points = out
    }

    /** NaviHelper처럼: 지금 위치 [atM]에서 앞 [horizonM] 안의 안내를 "남은 거리"로 (RouteTurn.fromSegments를 거친 뒤와 같은 꼴). */
    fun upcoming(atM: Double, horizonM: Double = HORIZON_M): List<RouteTurn> =
        points.filter { (m, _) -> m > atM && m - atM <= horizonM }.map { (m, k) -> RouteTurn(k, m - atM) }

    companion object {
        const val STEP_M = 10.0
        /** 반경 150m보다 급한 곳만 굽이로 본다. */
        const val KAPPA_MIN = 1.0 / 150
        const val HORIZON_M = 3_000.0

        fun kindOf(deg: Double): TurnKind? {
            val left = deg > 0
            return when {
                abs(deg) >= 110 -> if (left) TurnKind.SHARP_LEFT else TurnKind.SHARP_RIGHT
                abs(deg) >= 45 -> if (left) TurnKind.LEFT else TurnKind.RIGHT
                abs(deg) >= 15 -> if (left) TurnKind.KEEP_LEFT else TurnKind.KEEP_RIGHT
                else -> null
            }
        }
    }
}
