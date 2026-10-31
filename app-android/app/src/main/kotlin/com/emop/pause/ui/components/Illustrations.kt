package com.emop.pause.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.emop.pause.ui.theme.IconColors
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseColors
import kotlin.math.min
import kotlin.math.sin

/*
 * v5 일러스트 — 모두 직접 그린 단색 도형이다(사진·지도 타일·남의 캐릭터 없음, STYLE_V5 §1).
 * 사용자가 docs/design/v5/assets/에 그림을 넣으면 화면이 [ArtSlot]으로 그것을 먼저 쓴다.
 */

private val Cream = Color(0xFFFFF7EC)
private val Fur = Color(0xFFF1DFC8)
private val FurDark = Color(0xFFE2C6A4)
private val Ink = Color(0xFF1A2430)
private val Tongue = Color(0xFFF08A8A)

/** 반려견 기본 일러스트 (프로필 사진이 없을 때). 둥근 얼굴·늘어진 귀의 소형견, 연한 하늘 바탕. */
@Composable
fun DogIllustration(modifier: Modifier = Modifier) {
    val c = Pause.colors
    Canvas(modifier) {
        drawRect(if (c.isDark) Color(0xFF223047) else Color(0xFFDDEBFA))
        val s = min(size.width, size.height)
        val cx = size.width / 2
        val cy = size.height * 0.56f
        // 귀
        listOf(-1f, 1f).forEach { side ->
            rotate(-side * 14f, Offset(cx + side * s * 0.26f, cy - s * 0.02f)) {
                drawOval(FurDark, Offset(cx + side * s * 0.26f - s * 0.11f, cy - s * 0.22f), Size(s * 0.22f, s * 0.42f))
            }
        }
        // 머리·볼
        drawCircle(Fur, s * 0.3f, Offset(cx, cy - s * 0.04f))
        drawOval(Cream, Offset(cx - s * 0.2f, cy - s * 0.02f), Size(s * 0.4f, s * 0.28f))
        // 눈
        listOf(-1f, 1f).forEach { side ->
            val e = Offset(cx + side * s * 0.11f, cy - s * 0.08f)
            drawCircle(Ink, s * 0.04f, e)
            drawCircle(Color.White, s * 0.013f, e + Offset(s * 0.012f, -s * 0.012f))
        }
        // 코·입·혀
        drawOval(Ink, Offset(cx - s * 0.05f, cy + s * 0.02f), Size(s * 0.1f, s * 0.07f))
        drawOval(Tongue, Offset(cx - s * 0.04f, cy + s * 0.12f), Size(s * 0.08f, s * 0.08f))
        val m = Path().apply {
            moveTo(cx - s * 0.08f, cy + s * 0.1f)
            quadraticTo(cx - s * 0.04f, cy + s * 0.15f, cx, cy + s * 0.09f)
            quadraticTo(cx + s * 0.04f, cy + s * 0.15f, cx + s * 0.08f, cy + s * 0.1f)
        }
        drawPath(m, Ink, style = Stroke(s * 0.012f, cap = StrokeCap.Round))
    }
}

/**
 * 원근 도로 + 파란 경로선 (2·3번). [curvature]는 앞 1km 곡률(1/m, 50m 간격, 왼쪽 +) — 경로 굽이대로 길이 휜다.
 * 비면 곧은 길. 현재 위치 = 아래 가운데 삼각형.
 */
@Composable
fun RoadIllustration(curvature: List<Float>, modifier: Modifier = Modifier, showCar: Boolean = true, cautionFromM: Float? = null) {
    val c = Pause.colors
    Canvas(modifier) { drawRoad(c, curvature, showCar, cautionFromM) }
}

/**
 * 경로 보기 (v6, 지도 타일 없음): 앞 1km 경로 곡률을 원근으로 그린 도로 + 파란 경로선 + 내 차 표시.
 * 바탕은 무채색(쿨그레이)이라 정보 색(파랑 경로·앰버 굽은 길)만 눈에 띈다. [cautionFromM]이 1km 안이면 그 지점부터 경로선을 앰버로.
 * 폭·높이 비율이 달라도 도로 폭이 늘어나지 않게 짧은 변 기준으로 축척을 잡는다.
 */
private fun DrawScope.drawRoad(c: PauseColors, curvature: List<Float>, showCar: Boolean, cautionFromM: Float?) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.16f
    drawRect(Brush.verticalGradient(listOf(c.sky, c.land), 0f, horizon * 1.6f), size = Size(w, h))
    drawRect(c.land, Offset(0f, horizon), Size(w, h - horizon))
    // 원근 격자 (지면 느낌, 아주 옅게)
    val grid = c.roadEdge.copy(alpha = if (c.isDark) 0.25f else 0.45f)
    for (i in 1..6) {
        val yy = horizon + (h - horizon) * (i / 6f) * (i / 6f)
        drawLine(grid, Offset(0f, yy), Offset(w, yy), strokeWidth = 1.5f)
    }

    // 경로 중심선: 곡률 적분 → 옆 거리(m). s = 0..1000m
    val step = 50f
    val samples = if (curvature.isEmpty()) List(21) { 0f } else curvature
    val lateral = FloatArray(samples.size)
    var heading = 0f
    for (i in 1 until samples.size) {
        heading += samples[i - 1] * step
        lateral[i] = lateral[i - 1] - sin(heading) * step // 왼쪽(+곡률)이면 화면 왼쪽으로
    }
    val d0 = 25f // 원근: 화면 y = horizon + (h - horizon)·d0/(d0+s)
    val pxPerM = minOf(w, h * 1.3f) * 0.045f
    fun proj(sM: Float, xM: Float): Offset {
        val k = d0 / (d0 + sM)
        return Offset(w / 2 + (xM - lateral.first()) * k * pxPerM, horizon + (h - horizon) * k)
    }
    fun latAt(sM: Float): Float {
        val i = (sM / step).toInt().coerceIn(0, samples.size - 2)
        val f = ((sM - i * step) / step).coerceIn(0f, 1f)
        return lateral[i] * (1 - f) + lateral[i + 1] * f
    }
    val ss = (0..60).map { 1000f * (it / 60f) * (it / 60f) } // 가까운 곳을 촘촘히
    val half = 7.5f
    fun ribbon(fromM: Float, toM: Float, s0: Float = 0f) = Path().apply {
        val sel = ss.filter { it >= s0 }.let { if (s0 > 0f) listOf(s0) + it else it }
        val l = sel.map { proj(it, latAt(it) + fromM) }
        val r = sel.map { proj(it, latAt(it) + toM) }
        if (s0 > 0f) moveTo(l.first().x, l.first().y) else moveTo(l.first().x, h)
        l.forEach { lineTo(it.x, it.y) }
        r.asReversed().forEach { lineTo(it.x, it.y) }
        if (s0 > 0f) lineTo(r.first().x, r.first().y) else lineTo(r.first().x, h)
        close()
    }
    drawPath(ribbon(-half - 0.8f, half + 0.8f), c.roadEdge)
    drawPath(ribbon(-half, half), c.road)
    fun line(xM: Float, color: Color, width: Float, dash: Boolean = false) {
        val p = Path().apply { ss.forEachIndexed { i, s -> val o = proj(s, latAt(s) + xM); if (i == 0) moveTo(o.x, h) else lineTo(o.x, o.y) } }
        drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = if (dash) PathEffect.dashPathEffect(floatArrayOf(w * 0.025f, w * 0.03f)) else null))
    }
    line(0f, Color.White.copy(alpha = 0.55f), w * 0.0035f, dash = true)
    // 파란 경로선 (오른쪽 차로 가운데). 굽은 길 구간은 앰버.
    drawPath(ribbon(2.4f, 4.8f), c.primary)
    cautionFromM?.takeIf { it in 0f..950f }?.let { from -> drawPath(ribbon(2.4f, 4.8f, from), c.warning) }
    if (showCar) {
        val p = proj(4f, latAt(4f) + 3.6f)
        val r = minOf(w, h) * 0.045f
        val tri = Path().apply { moveTo(p.x, p.y - r * 1.15f); lineTo(p.x + r * 0.9f, p.y + r * 0.8f); lineTo(p.x, p.y + r * 0.35f); lineTo(p.x - r * 0.9f, p.y + r * 0.8f); close() }
        drawCircle(c.primary.copy(alpha = 0.18f), r * 2.1f, p)
        drawCircle(Color.White, r * 1.45f, p)
        drawPath(tri, c.navy)
    }
}

/** SUV 측면 평면 그림 (5번 대기 화면). */
@Composable
fun SuvIllustration(modifier: Modifier = Modifier) {
    val c = Pause.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = if (c.isDark) Color(0xFF8E9AAB) else Color(0xFFB9C3CF)
        val glass = if (c.isDark) Color(0xFF2C3A4E) else Color(0xFF51637A)
        drawOval(Color.Black.copy(alpha = 0.08f), Offset(w * 0.06f, h * 0.84f), Size(w * 0.88f, h * 0.1f))
        val p = Path().apply {
            moveTo(w * 0.06f, h * 0.72f)
            lineTo(w * 0.07f, h * 0.52f)
            quadraticTo(w * 0.1f, h * 0.45f, w * 0.2f, h * 0.43f)
            lineTo(w * 0.32f, h * 0.24f)
            quadraticTo(w * 0.35f, h * 0.2f, w * 0.42f, h * 0.2f)
            lineTo(w * 0.78f, h * 0.2f)
            quadraticTo(w * 0.84f, h * 0.2f, w * 0.88f, h * 0.3f)
            lineTo(w * 0.94f, h * 0.45f)
            lineTo(w * 0.95f, h * 0.72f)
            close()
        }
        drawPath(p, body)
        val win = Path().apply {
            moveTo(w * 0.24f, h * 0.43f); lineTo(w * 0.35f, h * 0.26f); lineTo(w * 0.58f, h * 0.26f); lineTo(w * 0.58f, h * 0.43f); close()
            moveTo(w * 0.61f, h * 0.43f); lineTo(w * 0.61f, h * 0.26f); lineTo(w * 0.8f, h * 0.26f); lineTo(w * 0.88f, h * 0.43f); close()
        }
        drawPath(win, glass)
        drawLine(body.copy(alpha = 0.6f), Offset(w * 0.1f, h * 0.55f), Offset(w * 0.92f, h * 0.55f), w * 0.004f)
        listOf(0.25f, 0.76f).forEach { x ->
            drawCircle(Ink, h * 0.15f, Offset(w * x, h * 0.74f))
            drawCircle(Color(0xFFD5DBE3), h * 0.07f, Offset(w * x, h * 0.74f))
        }
        drawRoundRect(IconColors.car, Offset(w * 0.9f, h * 0.48f), Size(w * 0.04f, h * 0.05f), CornerRadius(h * 0.02f))
    }
}

/** 휴게소 단색 일러스트 (4번). 실제 휴게소 사진처럼 보이지 않게 단순한 도형. */
@Composable
fun RestStopIllustration(modifier: Modifier = Modifier) {
    val c = Pause.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(c.sky)
        drawRect(c.land, Offset(0f, h * 0.7f), Size(w, h * 0.3f))
        trees(w * 0.08f, h * 0.7f, h * 0.22f, c)
        trees(w * 0.9f, h * 0.7f, h * 0.26f, c)
        val roof = Path().apply { moveTo(w * 0.12f, h * 0.38f); lineTo(w * 0.5f, h * 0.22f); lineTo(w * 0.88f, h * 0.38f); close() }
        drawRect(c.surface, Offset(w * 0.18f, h * 0.38f), Size(w * 0.64f, h * 0.32f))
        drawPath(roof, c.road)
        for (i in 0..4) drawRect(c.primarySoft, Offset(w * (0.22f + i * 0.12f), h * 0.45f), Size(w * 0.08f, h * 0.18f))
        drawRoundRect(c.primary, Offset(w * 0.42f, h * 0.27f), Size(w * 0.16f, h * 0.07f), CornerRadius(h * 0.02f))
    }
}

/** 동물병원 단색 일러스트 (8번). */
@Composable
fun HospitalIllustration(modifier: Modifier = Modifier) {
    val c = Pause.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(c.sky)
        drawRect(c.land, Offset(0f, h * 0.78f), Size(w, h * 0.22f))
        drawRect(c.surface, Offset(w * 0.2f, h * 0.3f), Size(w * 0.6f, h * 0.48f))
        drawRect(c.border, Offset(w * 0.2f, h * 0.3f), Size(w * 0.6f, h * 0.48f), style = Stroke(w * 0.01f))
        for (r in 0..1) for (i in 0..2) drawRect(c.primarySoft, Offset(w * (0.26f + i * 0.18f), h * (0.42f + r * 0.16f)), Size(w * 0.12f, h * 0.1f))
        val cx = w * 0.5f
        val cy = h * 0.2f
        val a = w * 0.07f
        drawCircle(c.surface, a * 1.6f, Offset(cx, cy))
        drawRect(c.primary, Offset(cx - a / 3, cy - a), Size(a * 2 / 3, a * 2))
        drawRect(c.primary, Offset(cx - a, cy - a / 3), Size(a * 2, a * 2 / 3))
        trees(w * 0.1f, h * 0.78f, h * 0.2f, c)
    }
}

/** 온보딩 그림 (11번): 길 위 차 두 대. */
@Composable
fun CarsIllustration(modifier: Modifier = Modifier) {
    val c = Pause.colors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(c.sky)
        drawRect(c.road.copy(alpha = 0.25f), Offset(0f, h * 0.72f), Size(w, h * 0.12f))
        drawLine(c.surface, Offset(0f, h * 0.78f), Offset(w, h * 0.78f), h * 0.01f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.04f, w * 0.03f)))
        car(Offset(w * 0.22f, h * 0.48f), w * 0.26f, c.surface, c)
        car(Offset(w * 0.54f, h * 0.48f), w * 0.26f, IconColors.car, c)
    }
}

private fun DrawScope.car(at: Offset, len: Float, body: Color, c: PauseColors) {
    val hgt = len * 0.42f
    val p = Path().apply {
        moveTo(at.x, at.y + hgt * 0.8f)
        lineTo(at.x, at.y + hgt * 0.45f)
        lineTo(at.x + len * 0.2f, at.y + hgt * 0.35f)
        lineTo(at.x + len * 0.33f, at.y)
        lineTo(at.x + len * 0.72f, at.y)
        lineTo(at.x + len * 0.86f, at.y + hgt * 0.35f)
        lineTo(at.x + len, at.y + hgt * 0.45f)
        lineTo(at.x + len, at.y + hgt * 0.8f)
        close()
    }
    drawPath(p, body)
    drawPath(p, c.road, style = Stroke(len * 0.015f, join = StrokeJoin.Round))
    drawRect(c.primarySoft, Offset(at.x + len * 0.36f, at.y + hgt * 0.1f), Size(len * 0.33f, hgt * 0.24f))
    listOf(0.22f, 0.78f).forEach { drawCircle(Ink, hgt * 0.17f, Offset(at.x + len * it, at.y + hgt * 0.82f)) }
}

private fun DrawScope.trees(x: Float, ground: Float, height: Float, c: PauseColors) {
    val green = if (c.isDark) Color(0xFF2E5A47) else Color(0xFF8FC3A3)
    drawRect(c.road.copy(alpha = 0.6f), Offset(x - height * 0.04f, ground - height * 0.35f), Size(height * 0.08f, height * 0.35f))
    drawCircle(green, height * 0.32f, Offset(x, ground - height * 0.6f))
}

/** 앱 바·온보딩의 작은 앱 표시 (12번 아이콘을 단순화). */
@Composable
fun AppMark(modifier: Modifier = Modifier) {
    Canvas(modifier) { drawAppIcon(this) }
}

/**
 * 앱 아이콘 그림 (12번, mipmap 벡터와 같은 모양): 파란 둥근 사각 + 흰 반려견 얼굴 + 노란 차가 달리는 길.
 * 특정 회사 로고·캐릭터를 본뜨지 않은 단순 도형.
 */
fun drawAppIcon(scope: DrawScope) = with(scope) {
    val s = min(size.width, size.height)
    drawRoundRect(Brush.verticalGradient(listOf(IconColors.bgTop, IconColors.bgBottom)), size = Size(s, s), cornerRadius = CornerRadius(s * 0.22f))
    // 길 (아래 곡선)
    val road = Path().apply {
        moveTo(0f, s * 0.86f); quadraticTo(s * 0.5f, s * 0.6f, s, s * 0.7f); lineTo(s, s); lineTo(0f, s); close()
    }
    drawPath(road, Color(0xFF15418A))
    // 얼굴
    val face = Color.White
    // 늘어진 귀: 위쪽이 얼굴 안쪽으로 기운다 (왼쪽 귀 시계 방향, 오른쪽 반대)
    listOf(-1f, 1f).forEach { side ->
        val ex = s * (0.47f + side * 0.21f)
        rotate(-side * 16f, Offset(ex, s * 0.47f)) {
            drawOval(Color(0xFFE6ECF7), Offset(ex - s * 0.065f, s * 0.33f), Size(s * 0.13f, s * 0.27f))
        }
    }
    drawCircle(face, s * 0.24f, Offset(s * 0.47f, s * 0.42f))
    listOf(-1f, 1f).forEach { drawCircle(Ink, s * 0.027f, Offset(s * (0.47f + it * 0.08f), s * 0.39f)) }
    drawOval(Ink, Offset(s * 0.43f, s * 0.45f), Size(s * 0.08f, s * 0.055f))
    // 노란 차
    val car = IconColors.car
    val cx = s * 0.66f
    val cy = s * 0.74f
    drawRoundRect(car, Offset(cx - s * 0.13f, cy - s * 0.03f), Size(s * 0.26f, s * 0.09f), CornerRadius(s * 0.03f))
    drawRoundRect(car, Offset(cx - s * 0.07f, cy - s * 0.09f), Size(s * 0.14f, s * 0.08f), CornerRadius(s * 0.03f))
    listOf(-1f, 1f).forEach { drawCircle(Ink, s * 0.03f, Offset(cx + it * s * 0.08f, cy + s * 0.065f)) }
}
