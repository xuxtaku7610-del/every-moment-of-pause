package com.emop.pause.voice

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** 말하기 우선순위 (작을수록 먼저). COMMERCIAL_READINESS C P0: 안전 경고 > 제안 > 안내 > 배경음. */
enum class SpeechPriority { SAFETY, SUGGESTION, INFO, BACKGROUND }

/** 오디오 포커스 종류. 제안·안내는 잠깐 다른 소리를 줄이고, 배경음은 미디어처럼 오래 잡는다. */
enum class FocusKind { TRANSIENT_DUCK, MEDIA }

/** 오디오 포커스. Android 구현은 [AndroidAudioFocus]. */
interface AudioFocus {
    /** 포커스 요청. 다른 앱(내비 음성 안내 등)이 잡고 있어 거절되면 false. */
    fun request(kind: FocusKind): Boolean
    fun abandon()

    /** 말하는 도중 포커스를 잃음(내비 안내 시작 등). 큐가 현재 문장을 멈추고 다시 기다린다. */
    var onLoss: (() -> Unit)?
}

/** 포커스를 신경 쓰지 않는 기본값 (테스트·포커스 없는 환경). */
object NoAudioFocus : AudioFocus {
    override fun request(kind: FocusKind) = true
    override fun abandon() = Unit
    override var onLoss: (() -> Unit)? = null
}

/**
 * TTS 우선순위 큐.
 * - 한 번에 한 문장. 기다리는 문장 중 우선순위가 가장 높은 것부터(같으면 먼저 온 것).
 * - 더 높은 우선순위가 들어오면 지금 문장(낮은 우선순위)을 끊는다. 끊긴 문장은 버린다.
 * - 말하기 전에 포커스를 요청하고, 거절되면(내비 안내 중) [focusRetryMs]마다 다시 시도한다. [maxWaitMs]가 지나면 버린다.
 *   안전 경고는 버리지 않고 그냥 말한다(포커스 없이 덕킹 없이라도).
 * - 말하는 중 포커스를 잃으면 그 문장을 멈추고 맨 앞에 다시 넣는다(한 번만).
 */
class SpeechQueue(
    private val output: SpeechOutput,
    private val focus: AudioFocus,
    private val scope: CoroutineScope,
    private val focusRetryMs: Long = 500,
    private val maxWaitMs: Long = 10_000,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Item(val text: String, val priority: SpeechPriority, val seq: Long, val enqueuedMs: Long, val done: CompletableDeferred<Boolean>) {
        var retried = false
    }

    private val lock = Mutex()
    private val waiting = mutableListOf<Item>()
    private var current: Item? = null
    private var currentJob: Job? = null
    private var worker: Job? = null
    private var seq = 0L

    init {
        focus.onLoss = { onFocusLost() }
    }

    /** 문장을 큐에 넣고 말해질 때까지 기다린다. 말했으면 true, 끊기거나 버려지면 false. */
    suspend fun say(text: String, priority: SpeechPriority): Boolean {
        val item = Item(text, priority, seq++, clock(), CompletableDeferred())
        lock.withLock {
            waiting += item
            val cur = current
            if (cur != null && !cur.done.isCompleted && priority < cur.priority) {
                // 더 급한 말: 지금 문장을 끊는다.
                cur.done.complete(false)
                currentJob?.cancel()
                output.stop()
            }
            ensureWorker()
        }
        return item.done.await()
    }

    /** 모두 멈추고 비운다 (여행 초기화·주행 상태 변경). */
    suspend fun clear() = lock.withLock {
        waiting.forEach { it.done.complete(false) }
        waiting.clear()
        current?.done?.complete(false)
        currentJob?.cancel()
        output.stop()
        focus.abandon()
    }

    /** 대기 중이거나 말하는 문장의 우선순위 (발표 모드 표시·테스트용). */
    suspend fun snapshot(): List<Pair<String, SpeechPriority>> = lock.withLock {
        listOfNotNull(current?.let { it.text to it.priority }) + waiting.sortedWith(order).map { it.text to it.priority }
    }

    private val order = compareBy<Item>({ it.priority.ordinal }, { it.seq })

    private fun ensureWorker() {
        if (worker?.isActive == true) return
        worker = scope.launch {
            while (true) {
                val next = lock.withLock {
                    val n = waiting.minWithOrNull(order)
                    if (n != null) waiting.remove(n)
                    current = n
                    n
                } ?: break
                if (next.done.isCompleted) continue
                val job = scope.launch { speak(next) }
                lock.withLock { currentJob = job }
                job.join()
                lock.withLock {
                    current = null
                    currentJob = null
                    // 포커스를 잃어 다시 줄 선 문장은 아직 끝난 게 아니다.
                    if (!next.done.isCompleted && next !in waiting) next.done.complete(false)
                }
            }
            focus.abandon()
        }
    }

    private suspend fun speak(item: Item) {
        val kind = if (item.priority == SpeechPriority.BACKGROUND) FocusKind.MEDIA else FocusKind.TRANSIENT_DUCK
        val got = withTimeoutOrNull((maxWaitMs - (clock() - item.enqueuedMs)).coerceAtLeast(0)) {
            while (!focus.request(kind)) delay(focusRetryMs)
            true
        } ?: false
        if (!got && item.priority != SpeechPriority.SAFETY) {
            item.done.complete(false)
            return
        }
        output.speak(item.text)
        item.done.complete(true)
    }

    private fun onFocusLost() {
        scope.launch {
            lock.withLock {
                val cur = current ?: return@withLock
                if (cur.priority == SpeechPriority.SAFETY) return@withLock // 안전 경고는 끝까지
                currentJob?.cancel()
                output.stop()
                if (!cur.retried) {
                    // 내비 안내가 끝나면 다시 말한다.
                    cur.retried = true
                    waiting += cur
                } else {
                    cur.done.complete(false)
                }
            }
        }
    }
}
