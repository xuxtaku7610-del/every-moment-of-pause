package com.emop.pause.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.emop.pause.ui.theme.Pause
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** 폰 카메라로 열 수 있는 주소인지 (http·https 전체 주소). 서버 주소 앞부분이 없으면 글자로만 보인다. */
fun scannable(url: String): Boolean = url.startsWith("http://") || url.startsWith("https://")

/** QR 비트 (zxing-core 인코더만). 여백 1모듈, 오류 정정 M. 실패하면 null. */
fun qrMatrix(text: String): BitMatrix? = runCatching {
    QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
}.getOrNull()

/**
 * 링크 QR: 흰 바탕 검은 칸(다크 모드에서도 흰 바탕 — 카메라가 읽게). 애니메이션 없음.
 * 주행 중 화면에는 쓰지 않는다(정차 화면만).
 */
@Composable
fun QrCode(url: String, size: Dp, modifier: Modifier = Modifier) {
    val m = remember(url) { qrMatrix(url) } ?: return
    Canvas(modifier.size(size).clip(RoundedCornerShape(Pause.dimens.u(8f))).background(Color.White)) {
        val cell = this.size.width / m.width
        for (y in 0 until m.height) for (x in 0 until m.width) {
            if (m[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

/** QR + 이름 + 안내 한 줄 + 주소(작게). 주소가 상대 경로면(서버 주소 앞부분 없음) QR 없이 주소만. */
@Composable
fun LinkQr(label: String, url: String, hint: String, modifier: Modifier = Modifier, size: Dp = Pause.dimens.u(140f)) {
    val c = Pause.colors
    val d = Pause.dimens
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (scannable(url)) {
            QrCode(url, size)
            Spacer(Modifier.width(d.gap))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.u(4f))) {
            Text(label, style = Pause.type.bodyStrong, color = c.text)
            Text(if (scannable(url)) hint else "서버 주소가 없어 QR을 만들 수 없어요", style = Pause.type.sub, color = c.sub)
            Text(url, style = Pause.type.caption, color = c.sub, modifier = Modifier.padding(top = d.u(2f)))
        }
    }
}
