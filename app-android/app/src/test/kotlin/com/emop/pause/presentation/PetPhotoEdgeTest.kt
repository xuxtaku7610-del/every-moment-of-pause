package com.emop.pause.presentation

import com.emop.pause.ui.components.PHOTO_EDGE_SCALE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.util.zip.InflaterInputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * test_plan V10-04: 원형 반려견 그림(PetPhoto)의 잘림선 안쪽은 모두 불투명이다.
 * `pet_kongi_avatar.png`는 그림 안에 원이 그려져 있고 테두리가 반투명이라, 그대로 원형으로 자르면 가장자리에 뒤 배경(다크면 검정)이 비친다.
 * PetPhoto는 원형일 때 [PHOTO_EDGE_SCALE]만큼 키워 자른다 → 그 배율로 보이는 원 안에 alpha < 250 픽셀이 없어야 한다.
 * (Android 단위 테스트 클래스패스에는 javax.imageio가 없어 RGBA 8비트 PNG만 직접 푼다.)
 */
class PetPhotoEdgeTest {
    @Test
    fun `V10-04 원형 반려견 그림 잘림선 안쪽에 반투명 픽셀이 없다`() {
        val (w, h, alpha) = pngAlpha(File("../../docs/design/v5/assets/pet_kongi_avatar.png"))
        // ContentScale.Crop: 정사각 틀에 짧은 변을 맞춘다 → 보이는 원 반지름 = 짧은 변/2 ÷ 확대 배율
        val r = min(w, h) / 2.0 / PHOTO_EDGE_SCALE
        var translucent = 0
        for (y in 0 until h) for (x in 0 until w) {
            if (hypot(x + 0.5 - w / 2.0, y + 0.5 - h / 2.0) <= r && alpha[y * w + x] < 250) translucent++
        }
        assertEquals(0, translucent)
    }

    /** 비인터레이스 RGBA 8비트 PNG의 (너비, 높이, alpha 배열). */
    private fun pngAlpha(f: File): Triple<Int, Int, IntArray> {
        val inp = DataInputStream(f.inputStream().buffered())
        inp.skipBytes(8)
        var w = 0
        var h = 0
        val idat = ByteArrayOutputStream()
        while (true) {
            val len = inp.readInt()
            val type = String(ByteArray(4).also { inp.readFully(it) }, Charsets.US_ASCII)
            val data = ByteArray(len).also { inp.readFully(it) }
            inp.skipBytes(4) // CRC
            when (type) {
                "IHDR" -> {
                    w = java.nio.ByteBuffer.wrap(data, 0, 4).int
                    h = java.nio.ByteBuffer.wrap(data, 4, 4).int
                    check(data[8].toInt() == 8 && data[9].toInt() == 6 && data[12].toInt() == 0) { "RGBA 8비트 비인터레이스만" }
                }
                "IDAT" -> idat.write(data)
                "IEND" -> break
            }
        }
        val raw = InflaterInputStream(idat.toByteArray().inputStream()).readBytes()
        val bpp = 4
        val stride = w * bpp
        val prev = IntArray(stride)
        val cur = IntArray(stride)
        val alpha = IntArray(w * h)
        var p = 0
        for (y in 0 until h) {
            val filter = raw[p++].toInt()
            for (i in 0 until stride) {
                val x = raw[p++].toInt() and 0xFF
                val a = if (i >= bpp) cur[i - bpp] else 0
                val b = prev[i]
                val c = if (i >= bpp) prev[i - bpp] else 0
                cur[i] = (x + when (filter) {
                    0 -> 0
                    1 -> a
                    2 -> b
                    3 -> (a + b) / 2
                    else -> {
                        val pa = abs(b - c); val pb = abs(a - c); val pc = abs(a + b - 2 * c)
                        if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
                    }
                }) and 0xFF
            }
            for (x in 0 until w) alpha[y * w + x] = cur[x * bpp + 3]
            cur.copyInto(prev)
        }
        return Triple(w, h, alpha)
    }
}
