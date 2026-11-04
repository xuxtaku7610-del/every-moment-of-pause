package com.emop.pause.trip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반려견 사진 (v10-3 2번, QR 업로드만): 서버에서 받은 사진을 **기기에만** 저장한다(`files/pets/<프로필 id>.jpg`, 긴 변 [MAX_PX]px JPEG).
 * 서버 쪽은 차가 받아 가면 지운다. 사진이 없으면 화면은 기본 실루엣(시연 데이터일 때만 콩이 사진).
 */
@Singleton
class PhotoStore @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val dir = File(context.filesDir, "pets")
    private val _photos = MutableStateFlow(load())
    val photos: StateFlow<Map<Long, Bitmap>> = _photos.asStateFlow()

    /** 받은 바이트를 열어 줄여서 저장. 이미지가 아니면 false. */
    fun save(petId: Long, bytes: ByteArray): Boolean {
        val src = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull() ?: return false
        val k = MAX_PX.toFloat() / maxOf(src.width, src.height)
        val scaled = if (k < 1f) Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true) else src
        // 투명 PNG는 JPEG로 바꾸면 검게 된다 → 흰 바탕에 얹는다
        val bmp = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888).also { out ->
            android.graphics.Canvas(out).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(scaled, 0f, 0f, null) }
        }
        dir.mkdirs()
        runCatching { File(dir, "$petId.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) } }.getOrElse { return false }
        _photos.value = _photos.value + (petId to bmp)
        return true
    }

    fun delete(petId: Long) {
        File(dir, "$petId.jpg").delete()
        _photos.value = _photos.value - petId
    }

    /** "내 데이터 삭제". */
    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
        _photos.value = emptyMap()
    }

    private fun load(): Map<Long, Bitmap> = dir.listFiles().orEmpty().mapNotNull { f ->
        val id = f.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null
        BitmapFactory.decodeFile(f.path)?.let { id to it }
    }.toMap()

    companion object {
        const val MAX_PX = 512
    }
}
