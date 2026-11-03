package com.emop.pause.trip

import com.emop.pause.voice.Reply
import com.emop.pause.voice.ReplyIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

/** 음성 차례와 그 상태. [turn]이 바뀌면 지난 차례는 상태를 바꾸지 못한다. */
data class VoiceTurn(val turn: Long, val phase: VoicePhase)

/**
 * 쉼표 제안 한 번의 응답 받기 (음성 → 버튼).
 * 2026-11-02 안정화: 장면 점프를 여러 번 한 뒤 "듣고 있어요"에 멈춘 문제(BLOCKED 11/01)를 막는다.
 * - 음성은 [voiceMs](8초) 한 번만 듣고, 그 안에 알아들은 응답이 없으면 무조건 버튼으로 넘어간다.
 * - 차례 번호: 점프·여행 초기화([invalidate]) 뒤에는 지난 차례가 상태를 바꾸지 못한다(취소가 늦게 닿아도).
 * - 받아쓰기 결과는 그 차례에서만 쓴다. 지난 차례의 받아쓰기가 다음 차례 응답 칸에 들어가지 않는다.
 * - 감시: "듣는 중"이 [watchMs](기본 8초) 넘게 이어지면(엔진 스레드가 막히는 등) [watchScope]에서 버튼으로 바꾼다.
 *   버튼 응답은 듣기 중에도 받으므로 그대로 동작한다.
 */
class ReplyGate(
    private val listen: suspend (Long) -> String?,
    private val listenScope: CoroutineScope,
    watchScope: CoroutineScope,
    private val voiceMs: Long = VOICE_MS,
    private val buttonMs: Long = BUTTON_MS,
    private val watchMs: Long = voiceMs,
    private val onStuck: (Long) -> Unit = {},
) {
    private val replies = Channel<String>(Channel.CONFLATED)
    private val counter = AtomicLong(0)
    private val _state = MutableStateFlow(VoiceTurn(0, VoicePhase.IDLE))
    val state: StateFlow<VoiceTurn> = _state.asStateFlow()

    init {
        watchScope.launch {
            _state.collectLatest { s ->
                if (s.phase != VoicePhase.LISTENING) return@collectLatest
                delay(watchMs)
                if (_state.compareAndSet(s, s.copy(phase = VoicePhase.FAILED))) onStuck(s.turn)
            }
        }
    }

    /** 화면 버튼·STT·adb 응답. */
    fun offer(text: String) {
        replies.trySend(text)
    }

    /** 새 차례. 지난 응답은 버린다. */
    fun begin(): Long {
        val t = counter.incrementAndGet()
        while (replies.tryReceive().isSuccess) Unit
        _state.value = VoiceTurn(t, VoicePhase.IDLE)
        return t
    }

    /** 장면 점프·여행 초기화: 지금 차례를 끝낸다. */
    fun invalidate() {
        _state.value = VoiceTurn(counter.incrementAndGet(), VoicePhase.IDLE)
    }

    fun isCurrent(turn: Long): Boolean = counter.get() == turn

    /** 차례 [turn]의 응답. [voice]면 [speak] 뒤 듣고, 못 들으면 버튼을 [buttonMs] 기다린다. 응답이 없으면 UNKNOWN. */
    suspend fun await(turn: Long, voice: Boolean, speak: suspend () -> Unit): Reply {
        try {
            if (voice) {
                speak()
                set(turn, VoicePhase.LISTENING)
                val heard = ReplyIntent.parse(hear())
                if (heard != Reply.UNKNOWN) return heard
            }
            set(turn, if (voice) VoicePhase.FAILED else VoicePhase.IDLE)
            return ReplyIntent.parse(withTimeoutOrNull(buttonMs) { replies.receive() })
        } finally {
            set(turn, VoicePhase.IDLE)
        }
    }

    /** 받아쓰기와 버튼 중 먼저 온 것. 받아쓰기는 이 차례 밖([listenScope])에서 돌려 취소를 기다리지 않는다. */
    private suspend fun hear(): String? {
        val dictation = listenScope.async { runCatching { listen(voiceMs) }.getOrNull() }
        try {
            return withTimeoutOrNull(voiceMs) {
                select<String?> {
                    replies.onReceive { it }
                    dictation.onAwait { it }
                }
            }
        } finally {
            dictation.cancel()
        }
    }

    private fun set(turn: Long, phase: VoicePhase) {
        _state.update { if (it.turn == turn) VoiceTurn(turn, phase) else it }
    }

    companion object {
        const val VOICE_MS = 8_000L
        const val BUTTON_MS = 15_000L
    }
}
