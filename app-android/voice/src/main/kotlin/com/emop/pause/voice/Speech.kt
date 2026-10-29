package com.emop.pause.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

/** 음성 출력. Pleos TTS가 열리면 이 인터페이스 뒤에서 교체한다 (architecture §3.3). */
interface SpeechOutput {
    suspend fun speak(text: String)
    fun stop()
}

/** 음성 입력 한 번. 인식 실패·권한 없음·시간 초과면 null. */
interface SpeechInput {
    suspend fun listen(timeoutMs: Long = 6_000): String?
}

/** Android 기본 TTS (한국어). 엔진이 없거나 초기화에 실패하면 조용히 건너뛴다. */
class AndroidTts(context: Context) : SpeechOutput {
    private val ready = CompletableDeferred<Boolean>()
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready.complete(status == TextToSpeech.SUCCESS)
    }

    override suspend fun speak(text: String) {
        if (withTimeoutOrNull(3_000) { ready.await() } != true) return
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        tts.language = Locale.KOREAN
        // 오디오 포커스와 같은 용도로 낸다 (AndroidAudioFocus.SPEECH_ATTRIBUTES).
        tts.setAudioAttributes(AndroidAudioFocus.SPEECH_ATTRIBUTES)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { if (utteranceId == id) done.complete(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { if (utteranceId == id) done.complete(Unit) }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        // 문장 길이에 비례한 상한: 끝 콜백이 오지 않아도 흐름이 멈추지 않게.
        withTimeoutOrNull(2_000L + text.length * 250L) { done.await() }
    }

    override fun stop() {
        tts.stop()
    }

    fun shutdown() = tts.shutdown()
}

/** Android 기본 STT. RECORD_AUDIO 권한이 없거나 인식기가 없으면 null을 돌려 버튼 응답으로 넘어간다. */
class AndroidStt(private val context: Context) : SpeechInput {
    override suspend fun listen(timeoutMs: Long): String? = withContext(Dispatchers.Main) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return@withContext null
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return@withContext null
        val result = CompletableDeferred<String?>()
        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull() ?: return@withContext null
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                result.complete(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())
            }
            override fun onError(error: Int) { result.complete(null) }
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        try {
            runCatching { recognizer.startListening(intent) }.onFailure { return@withContext null }
            withTimeoutOrNull(timeoutMs) { result.await() }
        } finally {
            recognizer.destroy()
        }
    }
}
