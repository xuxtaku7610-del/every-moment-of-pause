package com.emop.pause.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.theme.NapColors
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import kotlin.math.max

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

/** SVG `preserveAspectRatio="xMidYMax slice"`: 영역을 꽉 채우고 아래·가운데 기준으로 맞춘다. */
private fun DrawScope.slice(vw: Float, vh: Float, block: DrawScope.() -> Unit) {
    val s = max(size.width / vw, size.height / vh)
    val dx = (size.width - vw * s) / 2
    val dy = size.height - vh * s
    translate(dx, dy) { scale(s, s, pivot = Offset.Zero) { block() } }
}

enum class LandscapeKind {
    /** 제안 없음 화면 카드(해 + 언덕 2겹 + 길). */
    CALM,
    /** 리포트 상단 띠(언덕 2겹). */
    REPORT,
    /** 온보딩 1단계(언덕 + 경로선 + 쉼표 2개). */
    ONBOARD,
}

/** 벡터 풍경 (UI_SPEC §3 Landscape). 단색 면만, 움직임 없음. */
@Composable
fun Landscape(kind: LandscapeKind, modifier: Modifier = Modifier) {
    val c = Pause.colors
    val paths = remember(kind) {
        when (kind) {
            LandscapeKind.CALM -> listOf(
                "M0 470 C220 380 380 420 560 360 C760 290 920 380 1110 330 C1260 290 1380 330 1488 300 V740 H0z",
                "M0 560 C260 480 470 540 700 480 C930 420 1110 500 1488 440 V740 H0z",
                "M820 740 C880 640 960 560 1060 500 L1100 500 C1030 570 990 650 980 740z",
                "M900 740 C940 660 1000 590 1080 500",
            ).map(::path)
            LandscapeKind.REPORT -> listOf(
                "M0 200 C150 150 260 170 380 140 C520 104 640 160 780 130 C880 110 950 128 1000 118 V300 H0z",
                "M0 250 C180 210 330 240 480 214 C640 186 780 230 1000 200 V300 H0z",
            ).map(::path)
            LandscapeKind.ONBOARD -> listOf(
                "M0 640 C160 560 300 600 440 540 C560 490 620 520 680 500 V980 H0z",
                "M0 760 C200 690 380 730 680 660 V980 H0z",
                "M120 980 C180 820 330 700 560 430",
            ).map(::path)
        }
    }
    val comma = remember { path(PauseIcons.COMMA_PATH) }
    Canvas(modifier) {
        drawRect(c.sky)
        when (kind) {
            LandscapeKind.CALM -> slice(1488f, 740f) {
                drawCircle(c.sun, 90f, Offset(1180f, 210f))
                drawPath(paths[0], c.hill)
                drawPath(paths[1], c.hill2)
                drawPath(paths[2], c.road)
                drawPath(paths[3], c.roadDash, style = Stroke(6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(26f, 22f))))
            }
            LandscapeKind.REPORT -> slice(1000f, 300f) {
                drawPath(paths[0], c.hill)
                drawPath(paths[1], c.hill2)
            }
            LandscapeKind.ONBOARD -> slice(680f, 980f) {
                drawPath(paths[0], c.hill)
                drawPath(paths[1], c.hill2)
                drawPath(paths[2], c.ink, style = Stroke(10f, cap = StrokeCap.Round))
                translate(236f, 650f) { scale(1.5f, 1.5f, pivot = Offset.Zero) { drawPath(comma, c.rest) } }
                translate(420f, 470f) { scale(1.1f, 1.1f, pivot = Offset.Zero) { drawPath(comma, c.ink2) } }
            }
        }
    }
}

/**
 * 파워냅 원형 링 (UI_SPEC §3 NapRing): 지름 560dp, 두께 16dp, 바탕 흰색 10%, 진행 ember.
 * [remaining]은 남은 비율(1 → 0). 1분마다 갱신되므로 애니메이션 없음.
 */
@Composable
fun NapRing(remaining: Float, label: String, time: String, modifier: Modifier = Modifier) {
    Box(modifier.size(560.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = 30.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(NapColors.track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(NapColors.ember, -90f, 360f * remaining.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = Pause.type.body, color = NapColors.muted)
            Text(time, style = Pause.type.clock, color = NapColors.clock, maxLines = 1)
        }
    }
}

/**
 * 벽난로 (UI_SPEC §3 Fireplace): 불꽃 2겹 + 장작 2개 + 바닥 빛.
 * 불꽃은 [animate]이고 움직임이 허용될 때(PAUSE·PARKED·움직임 줄이기 꺼짐)만 4~6초 주기로 아주 느리게 흔들린다.
 */
@Composable
fun Fireplace(modifier: Modifier = Modifier, animate: Boolean = true) {
    val outer = remember { path("M300 20c32 56 88 88 88 152a88 88 0 0 1-176 0c0-40 32-64 48-104 8 32 24 48 40 56-8-40-8-72 0-104z") }
    val inner = remember { path("M304 84c20 36 56 56 56 100a56 56 0 0 1-112 0c0-28 20-44 30-70 6 20 16 32 26 36-5-24-5-44 0-66z") }
    val moving = animate && Pause.motion
    var sway = 0f
    var breathe = 1f
    if (moving) {
        val t = rememberInfiniteTransition(label = "fire")
        sway = t.animateFloat(-1.5f, 1.5f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse), label = "sway").value
        breathe = t.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse), label = "breathe").value
    }
    Canvas(modifier.size(600.dp, 300.dp)) {
        // viewBox -80 0 760 300
        val s = size.width / 760f
        translate(80f * s, 0f) {
            scale(s, s, pivot = Offset.Zero) {
                drawOval(
                    Brush.radialGradient(listOf(Color(0x80FFB45E), Color(0x00FFB45E)), center = Offset(300f, 250f), radius = 340f),
                    topLeft = Offset(-40f, 180f),
                    size = androidx.compose.ui.geometry.Size(680f, 140f),
                )
                rotate(sway, pivot = Offset(300f, 250f)) {
                    scale(1f, breathe, pivot = Offset(300f, 250f)) {
                        drawPath(outer, Brush.verticalGradient(listOf(Color(0xFFF0A04B), Color(0xFFC2410C)), 20f, 260f), alpha = 0.9f)
                    }
                }
                rotate(-sway * 0.7f, pivot = Offset(300f, 250f)) {
                    scale(1f, 2f - breathe, pivot = Offset(300f, 250f)) {
                        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFE2A3), Color(0xFFF7A440), Color(0xFFF26A1B)), 84f, 240f))
                    }
                }
                rotate(-7f, pivot = Offset(300f, 244f)) {
                    drawRoundRect(Color(0xFF4A2E1C), Offset(160f, 226f), androidx.compose.ui.geometry.Size(280f, 36f), androidx.compose.ui.geometry.CornerRadius(18f))
                }
                rotate(7f, pivot = Offset(300f, 244f)) {
                    drawRoundRect(Color(0xFF5C3A23), Offset(160f, 226f), androidx.compose.ui.geometry.Size(280f, 36f), androidx.compose.ui.geometry.CornerRadius(18f))
                }
            }
        }
    }
}

@Preview(widthDp = 1200, heightDp = 700, showBackground = true, backgroundColor = 0xFF1C1713)
@Composable
private fun SceneryPreview() = PauseTheme(dark = false) {
    Column {
        Landscape(LandscapeKind.CALM, Modifier.size(740.dp, 370.dp))
        Box { NapRing(0.65f, "남은 시간", "13:00", Modifier.size(300.dp)) }
        Fireplace(animate = false)
    }
}
