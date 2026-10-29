package com.emop.pause.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Android 오디오 포커스 (COMMERCIAL_READINESS C P0).
 * - 제안·답변: `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` — 음악은 잠깐 작아지고, 말이 끝나면 돌려준다.
 * - 라디오: `AUDIOFOCUS_GAIN` — 다른 앱이 포커스를 가져가면(내비 안내·전화) [onLoss]로 알린다.
 * 내비 음성 안내가 포커스를 잡고 있으면 요청이 거절되어 [SpeechQueue]가 기다린다.
 */
class AndroidAudioFocus(context: Context) : AudioFocus {
    private val am = context.applicationContext.getSystemService(AudioManager::class.java)
    private var active: AudioFocusRequest? = null
    override var onLoss: (() -> Unit)? = null

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onLoss?.invoke()
            else -> Unit // LOSS_TRANSIENT_CAN_DUCK: 시스템이 대신 줄여 준다
        }
    }

    override fun request(kind: FocusKind): Boolean {
        abandon()
        val (gain, attrs) = when (kind) {
            FocusKind.TRANSIENT_DUCK -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK to SPEECH_ATTRIBUTES
            FocusKind.MEDIA -> AudioManager.AUDIOFOCUS_GAIN to MEDIA_ATTRIBUTES
        }
        val req = AudioFocusRequest.Builder(gain)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(listener)
            .setWillPauseWhenDucked(kind == FocusKind.MEDIA)
            .build()
        val granted = am?.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) active = req
        return granted
    }

    override fun abandon() {
        active?.let { am?.abandonAudioFocusRequest(it) }
        active = null
    }

    companion object {
        /** 안내 음성: 내비 안내보다 낮은 '어시스턴트' 용도. TTS도 같은 속성으로 낸다. */
        val SPEECH_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val MEDIA_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}
