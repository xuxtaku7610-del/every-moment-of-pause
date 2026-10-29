package com.emop.pause.trip

import com.emop.pause.engine.content.ContentLibrary
import com.emop.pause.engine.content.Episode
import com.emop.pause.engine.content.Playlist
import com.emop.pause.engine.content.PlaylistPlanner
import com.emop.pause.voice.SpeechPriority
import com.emop.pause.voice.SpeechQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 지금 라디오 상태 (발표 모드·화면 계약 `DriveUi.radio`). */
data class RadioState(
    val playing: Boolean = false,
    val title: String? = null,
    val index: Int = 0,
    val count: Int = 0,
    /** 검수 전 초안 — 디버그 빌드에서만 재생되고 "DRAFT"로 표시한다. */
    val draft: Boolean = false,
) {
    val episodesLeft: Int get() = if (playing) (count - index).coerceAtLeast(0) else 0
}

/**
 * 도착 맞춤 라디오 (todo M3, COMMERCIAL_READINESS F P0).
 * - 도착까지 남은 시간 이하로 0/1 배낭 편성, 같은 여행에서 들은 편은 빼고.
 * - 문장마다 [SpeechPriority.RADIO]로 읽고 0.6초 쉰다 (F P1). 더 급한 말(제안·안전·아이 답)이 끼면 그 문장부터 다시.
 * - 에피소드 끝 퀴즈는 뒷좌석 대상 — 답을 기다리지 않고 "생각해 봐" 뒤 정답을 알려 준다(운전자 참여 유도 없음, test_plan T10-3).
 */
class RadioDirector(private val library: () -> ContentLibrary, private val speech: SpeechQueue, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()
    private val played = mutableSetOf<String>()
    private var job: Job? = null

    /** 남은 시간에 맞춰 편성하고 재생을 시작한다. 들을 게 없으면 false. */
    fun start(remainSec: Int, childAge: Int?): Boolean {
        stop()
        val plan = plan(remainSec, childAge)
        if (plan.items.isEmpty()) return false
        val lib = library()
        _state.value = RadioState(true, plan.items.first().title, 0, plan.items.size, lib.isDraft(plan.items.first()))
        job = scope.launch {
            plan.items.forEachIndexed { i, ep ->
                _state.value = RadioState(true, ep.title, i, plan.items.size, lib.isDraft(ep))
                play(ep)
                played += ep.id
            }
            _state.value = RadioState()
        }
        return true
    }

    fun plan(remainSec: Int, childAge: Int?): Playlist = PlaylistPlanner.plan(library().episodes, remainSec, childAge, played)

    fun stop() {
        job?.cancel()
        job = null
        _state.value = RadioState()
    }

    fun resetTrip() {
        stop()
        played.clear()
    }

    private suspend fun play(ep: Episode) {
        val lines = ep.lines + listOfNotNull(
            ep.quiz?.let { "퀴즈! ${it.question}" },
            ep.quiz?.let { "생각해 봤나요? 정답은, ${it.choices[it.answerIndex]}." },
        )
        for (line in lines) {
            // 더 급한 말에 끊기면 잠깐 뒤 같은 문장부터 다시 (최대 3번).
            var tries = 0
            while (!speech.say(line, SpeechPriority.RADIO) && tries++ < 3) delay(RETRY_MS)
            delay(PAUSE_MS)
        }
    }

    private companion object {
        const val PAUSE_MS = 600L
        const val RETRY_MS = 1_500L
    }
}
