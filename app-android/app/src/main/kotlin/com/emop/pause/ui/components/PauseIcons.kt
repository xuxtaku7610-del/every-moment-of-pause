package com.emop.pause.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 시안(mockups.html `<symbol>`)의 아이콘을 그대로 옮긴 벡터. 색은 Icon(tint)로 정한다.
 * 원은 SVG arc 두 개로 바꿨다.
 */
object PauseIcons {
    /** 쉼표 기호 — 이 앱에서 "휴식 지점"을 뜻하는 유일한 기호 (UI_SPEC §1, §3 CommaMark). */
    const val COMMA_PATH = "M20 2a17 17 0 0 1 17 17c0 21-11 36-28 44l-4-7c9-5 15-12 17-20A17 17 0 0 1 20 2z"

    val Comma: ImageVector = icon("comma", 40f, 64f) { fill(COMMA_PATH) }

    val Kid = icon("kid") {
        stroke(circle(12f, 7f, 3.4f))
        stroke("M5.5 21v-3.5a6.5 6.5 0 0 1 13 0V21")
    }
    val Bolt = icon("bolt") { fill("M13 2 5 14h6l-1 8 8-12h-6z") }
    val Paw = icon("paw") {
        fill(circle(6f, 10f, 2.2f)); fill(circle(10f, 5.5f, 2.2f)); fill(circle(14f, 5.5f, 2.2f)); fill(circle(18f, 10f, 2.2f))
        fill("M12 11c-3.5 0-6 4.5-6 7 0 2 2 3 3.5 2.4 1.6-.6 3.4-.6 5 0C16 21 18 20 18 18c0-2.5-2.5-7-6-7z")
    }
    val Moon = icon("moon") { fill("M20 15A8 8 0 1 1 9 4a6.5 6.5 0 0 0 11 11z") }
    val Flag = icon("flag") { stroke("M6 21V4h10l-2 4 2 4H6") }
    val Warn = icon("warn") {
        stroke("M12 3 2 20h20z")
        stroke("M12 9v5M12 17v.5", width = 2.4f)
    }
    val CloudOff = icon("cloud") {
        stroke("M7 18h10a4 4 0 0 0 .6-7.96A6 6 0 0 0 6.1 9.5 4.3 4.3 0 0 0 7 18z")
        stroke("M4 4l16 16")
    }
    val Mic = icon("mic") {
        stroke("M12 3a3 3 0 0 1 3 3v5a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3z")
        stroke("M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21")
    }
    val User = icon("user") {
        fill(circle(12f, 8f, 4f))
        fill("M4 21a8 8 0 0 1 16 0z")
    }
    val Bell = icon("bell") {
        stroke("M6 16V11a6 6 0 0 1 12 0v5l2 2H4z")
        stroke("M10 21h4")
    }
    val Car = icon("car") {
        stroke("M4 16v-4l2-5h12l2 5v4z")
        fill(circle(7.5f, 16.5f, 1.8f)); fill(circle(16.5f, 16.5f, 1.8f))
    }
    val Lock = icon("lock") {
        stroke("M7 10h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z")
        stroke("M8 10V7a4 4 0 0 1 8 0v3")
    }
    val Info = icon("info") {
        stroke(circle(12f, 12f, 9f))
        stroke("M12 11v6M12 7.5v.5", width = 2.4f)
    }
    val Check = icon("check") { stroke("M6 12.5l4 4 8-9", width = 3f) }
    val Plus = icon("plus") { stroke("M12 5v14M5 12h14", width = 2.4f) }
    val Toilet = icon("toilet") {
        stroke(circle(8f, 5f, 2f)); stroke(circle(16f, 5f, 2f))
        stroke("M8 9v12M5.5 9h5l-.5 6M16 9l-3 7h6l-3-7zM16 16v5")
    }
    val Baby = icon("baby") {
        stroke(circle(12f, 9f, 5f))
        stroke("M10 9h.1M14 9h.1M10 12a3 3 0 0 0 4 0M8 21a4 4 0 0 1 8 0")
    }
    val Bed = icon("bed") { stroke("M3 18V7M3 14h18v4M21 14v-2a3 3 0 0 0-3-3h-7v5M7 11.5h.1") }
    val Briefcase = icon("briefcase") {
        stroke("M5 8h14a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z")
        stroke("M9 8V6a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2")
    }
    val Stretch = icon("stretch") {
        stroke(circle(12f, 4.5f, 2f))
        stroke("M4 9l8 1 8-1M12 10v5l-4 6M12 15l4 6")
    }
    val Radio = icon("radio") {
        stroke("M5 9h14a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1z")
        stroke("M7 9l10-5"); stroke(circle(15.5f, 14.5f, 2.2f)); stroke("M7 13h3M7 16h3")
    }
}

private class IconScope(val builder: ImageVector.Builder) {
    fun fill(d: String) {
        builder.addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))
    }

    fun stroke(d: String, width: Float = 2.2f) {
        builder.addPath(
            PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun icon(name: String, w: Float = 24f, h: Float = 24f, block: IconScope.() -> Unit): ImageVector {
    val b = ImageVector.Builder(name, w.dp, h.dp, w, h)
    IconScope(b).block()
    return b.build()
}
