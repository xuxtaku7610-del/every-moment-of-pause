package com.emop.pause.trip

import com.emop.pause.core.model.TripPhase
import com.emop.pause.data.RestStopRepository
import com.emop.pause.data.StopSource
import com.emop.pause.engine.fatigue.FatigueResult
import com.emop.pause.engine.needs.NeedKind
import com.emop.pause.engine.needs.NeedsResult
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.RestPhrases
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.TripPlan
import com.emop.pause.engine.planner.TripPlanPreview
import com.emop.pause.runtime.SignalRuntime
import com.emop.pause.runtime.Snapshot
import com.emop.pause.runtime.SourceMode
import com.emop.pause.voice.PhraseProvider
import com.emop.pause.voice.Reply
import com.emop.pause.voice.ReplyIntent
import com.emop.pause.voice.SpeechInput
import com.emop.pause.voice.SpeechOutput
import com.emop.pause.voice.AudioFocus
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.AppMode
import com.emop.pause.engine.content.ExpeditionTracker
import com.emop.pause.engine.content.Mission
import com.emop.pause.voice.EtaAnswer
import com.emop.pause.voice.EtaFacts
import com.emop.pause.voice.SpeechPriority
import com.emop.pause.voice.SpeechQueue
import com.emop.pause.voice.Tone
import com.emop.pause.voice.Utterance
import com.emop.pause.voice.VoiceIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** 음성 하단 시트 상태 (UI_SPEC §4.9). */
enum class VoicePhase { IDLE, LISTENING, FAILED }

/** 화면이 보는 여행 상태. 화면 모델 변환은 UiMapper가 한다. */
data class TripUi(
    val fatigue: FatigueResult? = null,
    val needs: NeedsResult? = null,
    /** 지금 묻고 있는 제안 (버튼·음성 힌트를 보인다). */
    val asking: PlanDecision.Suggest? = null,
    /** 지금 띄울 제안 문장 (TTS로 읽은 것과 같다). */
    val suggestion: String? = null,
    val heading: RestStop? = null,
    val voice: VoicePhase = VoicePhase.IDLE,
    val lastSpoken: String? = null,
    /** 결과 한 줄 ("경로에 안성휴게소를 넣었어요"). */
    val toast: String? = null,
    val stopSource: StopSource? = null,
    val stopCount: Int = 0,
    val stops: List<RestStop> = emptyList(),
    val plan: TripPlan? = null,
    val traveledKm: Double = 0.0,
    val kmPerMin: Double = 0.0,
    val passengers: List<Passenger> = emptyList(),
    val sleepHours: Float? = null,
    val chargeInfoAvailable: Boolean = false,
    /** 도착 맞춤 라디오. */
    val radio: RadioState = RadioState(),
    /** 이번 정차의 원정대 미션과 완료한 미션 ID. */
    val missions: List<Mission> = emptyList(),
    val missionsDone: Set<String> = emptySet(),
    /** 원정대를 다 해서 다음 구간 라디오가 열림. */
    val radioUnlocked: Boolean = false,
)

/**
 * 신호 → 엔진(TripBrain) → 제안 문장 → TTS → "응/나중에" → 경유지 추가 (flowchart §2).
 * 응답은 STT 또는 화면 버튼·디버그 adb(`--es say 응`)로 들어온다.
 * 인식 실패: 한 번 더 묻고, 그래도 모르면 버튼만 보여 준다 (UI_SPEC §4.9, COMMERCIAL_READINESS C P0).
 */
@Singleton
class TripCoordinator @Inject constructor(
    private val runtime: SignalRuntime,
    private val restStops: RestStopRepository,
    private val phrases: PhraseProvider,
    private val speechOut: SpeechOutput,
    private val speechIn: SpeechInput,
    private val presets: PresetStore,
    focus: AudioFocus,
    private val content: ContentRepository,
    private val resumeStore: TripResumeStore,
) {
    // 엔진 상태는 한 스레드에서만 만진다(신호 수집과 음성 응답이 같은 큐를 쓴다).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val _ui = MutableStateFlow(TripUi())
    val ui: StateFlow<TripUi> = _ui.asStateFlow()

    private val replies = Channel<String>(Channel.CONFLATED)
    private var brain: TripBrain? = null
    private var setup = TripSetup()
    private var tripKey: Pair<SourceMode, String?>? = null
    @Volatile private var stops: List<RestStop> = emptyList()
    private var voiceJob: Job? = null
    private var toastJob: Job? = null
    /** 안전 경고 > 제안 > 아이 답변 > 라디오, 오디오 포커스 (COMMERCIAL_READINESS C P0). */
    private val speech = SpeechQueue(speechOut, focus, scope)
    private val radio = RadioDirector({ content.library }, speech, scope)
    private val expedition by lazy { ExpeditionTracker(content.library.missions) }
    private var lastMode: AppMode? = null
    /** 앱 시작 뒤 저장된 여행을 한 번만 확인한다. */
    private var resumeChecked = false
    private var pendingResume: TripResume? = null
    private var lastSaveMs: Long? = null

    /** 설정 "아이 질문에 답하기". 끄면 아이 말투 질문에도 어른 답(시간·거리)으로 답한다. */
    @Volatile var kidAnswerEnabled: Boolean = true

    /** 음성 제안을 켤지 (설정 "음성으로 제안"). 꺼지면 화면 카드와 버튼만. */
    @Volatile var voiceEnabled: Boolean = true

    init {
        scope.launch { runtime.snapshot.collect(::onSnapshot) }
        scope.launch { radio.state.collect { r -> _ui.update { it.copy(radio = r) } } }
        scope.launch { runtime.replays.collect(::replay) }
    }

    /**
     * 들은 말 (STT 결과, 카드 버튼, 디버그 주입). "아직 멀었어?" 계열이면 언제든 답하고,
     * 그 밖의 말은 제안에 대한 응답으로 듣기 창에 넘긴다.
     */
    fun reply(text: String) {
        when (val u = VoiceIntent.parse(text)) {
            is Utterance.AskEta -> scope.launch { answerEta(u.tone) }
            is Utterance.Radio -> scope.launch { if (u.on) startRadio() else radio.stop() }
            is Utterance.Answer -> replies.trySend(text)
        }
    }

    /** "아직 멀었어?" 응답기 (test_plan T5-1·T5-5·T5-6). 템플릿만 — 아이용 문장은 LLM으로 만들지 않는다. */
    private suspend fun answerEta(asked: Tone) {
        val s = runtime.snapshot.value
        val eta = s.eta ?: return
        val t = _ui.value
        val next = (t.heading ?: t.plan?.stops?.map { it.stop }?.firstOrNull { it.distanceAlongRouteKm > t.traveledKm + 1 })
        val speed = t.kmPerMin.takeIf { it > 0 }
        val facts = EtaFacts(
            destination = s.route?.destinationName ?: "목적지",
            remainMin = (eta.remainTimeSec + 59) / 60,
            remainKm = eta.remainDistanceM / 1000.0,
            nextStopName = next?.name,
            nextStopMin = if (next != null && speed != null) maxOf(1, ((next.distanceAlongRouteKm - t.traveledKm) / speed).toInt()) else null,
            episodesLeft = radio.state.value.episodesLeft.takeIf { it > 0 },
        )
        val tone = if (asked == Tone.KID && !kidAnswerEnabled) Tone.ADULT else asked
        val text = EtaAnswer.answer(facts, tone)
        _ui.update { it.copy(lastSpoken = text) }
        speech.say(text, if (tone == Tone.KID) SpeechPriority.KID_ANSWER else SpeechPriority.SUGGESTION)
    }

    /** 발표 모드 점프: 건너뛴 구간을 음성 없이 따라잡는다. 그 사이 제안은 없었던 것으로 본다. */
    private fun replay(frames: List<Snapshot>) {
        voiceJob?.cancel()
        scope.launch { speech.clear() }
        reset()
        frames.forEach { s ->
            onSnapshot(s, silent = true)
            brain?.let { b ->
                if (b.state.pending != null) {
                    b.onShown(s.nowMs)
                    b.onDeclined(s.nowMs)
                }
            }
        }
        brain?.policy?.onAccepted()
    }

    private fun onSnapshot(s: Snapshot) = onSnapshot(s, silent = false)

    private fun onSnapshot(s: Snapshot, silent: Boolean) {
        if (s.source == SourceMode.VEHICLE && checkResume(s)) return
        val key = s.source to s.scenarioName
        if (key != tripKey || s.trip == TripPhase.NOT_STARTED) {
            if (brain != null || key != tripKey) reset()
            tripKey = key
        }
        if (s.trip == TripPhase.NOT_STARTED) {
            if (previewFor != key) loadPreview(s, key)
            return
        }
        if (s.trip != TripPhase.ACTIVE) {
            if (s.source == SourceMode.VEHICLE) resumeStore.clear()
            return
        }
        val b = brain ?: startTrip(s)
        // 경로가 바뀌면(목적지 변경·경로 이탈 후 재탐색) 휴게소 후보를 다시 받는다 (COMMERCIAL_READINESS B P0).
        val rk = routeKey(s.route)
        if (!silent && rk != null && stopsRouteKey != null && rk != stopsRouteKey && !reloading) {
            reloading = true
            scope.launch {
                try {
                    loadStops(s)
                    replan(s)
                } finally {
                    reloading = false
                }
            }
        }
        val state = b.tick(BrainInput(s.nowMs, s.clockMinute, s.mode, s.driveState, s.sample, s.capabilities, s.eta, s.route?.totalDistanceM))
        if (!silent && s.mode != lastMode) onModeChanged(s)
        // 주행 중 쉼표 계획을 지금 위치·니즈로 다시 잡는다 (출발 전 계획이 낡아 "77분 뒤"가 보이지 않게).
        if (s.mode == AppMode.DRIVING && (s.mode != lastMode || lastReplanMs.let { it == null || s.nowMs - it >= REPLAN_MS })) {
            lastReplanMs = s.nowMs
            replanDriving(s, state)
        }
        lastMode = s.mode
        if (s.source == SourceMode.VEHICLE && lastSaveMs.let { it == null || s.nowMs - it >= TripResume.SAVE_INTERVAL_MS }) {
            lastSaveMs = s.nowMs
            resumeStore.save(b.save(s.nowMs, resumeKey(s.route)))
        }
        val kmPerMin = s.eta?.takeIf { it.remainTimeSec > 0 }?.let { it.remainDistanceM / 1000.0 / (it.remainTimeSec / 60.0) } ?: _ui.value.kmPerMin
        _ui.update {
            it.copy(
                fatigue = state.fatigue,
                needs = state.needs,
                heading = state.heading,
                traveledKm = state.traveledKm,
                kmPerMin = kmPerMin,
                chargeInfoAvailable = state.needs?.chargeInfoAvailable == true,
            )
        }
        val d = state.pending ?: return
        if (silent || voiceJob?.isActive == true) return
        b.onShown(s.nowMs)
        voiceJob = scope.launch { handle(b, d) }
    }

    private fun startTrip(s: Snapshot): TripBrain {
        setup = setupFor(s)
        val b = TripBrain(setup) { stops }
        brain = b
        _ui.update { it.copy(passengers = setup.passengers, sleepHours = setup.sleepHours) }
        // 출발 준비에서 받아 둔 휴게소가 있으면 그대로 쓰고, 계획만 이번 설정으로 다시 잡는다.
        val resumed = pendingResume
        pendingResume = null
        scope.launch {
            if (stops.isEmpty()) loadStops(s)
            if (resumed != null) {
                b.restore(resumed)
                say(RESUME_PHRASE)
            }
            replan(s)
            // 출발 기준 계획이 끝났으니 주행 중이면 다음 프레임에서 지금 위치 기준으로 다시 잡는다.
            lastReplanMs = null
        }
        return b
    }

    private var previewFor: Pair<SourceMode, String?>? = null
    private var lastSnapshot: Snapshot? = null

    private fun setupFor(s: Snapshot) = s.simSetup?.let(PresetStore::fromScenario) ?: presets.setup()

    /** 출발 준비 화면의 "오늘의 쉼표" (경로를 알면 출발 전에 미리 받는다). */
    private fun loadPreview(s: Snapshot, key: Pair<SourceMode, String?>) {
        if (s.route == null) return
        previewFor = key
        lastSnapshot = s
        scope.launch {
            loadStops(s)
            setup = setupFor(s)
            _ui.update { it.copy(passengers = setup.passengers, sleepHours = setup.sleepHours) }
            replan(s)
        }
    }

    /** 프리셋·수면 시간이 바뀌면 계획을 다시 잡는다 (출발 준비 화면). */
    fun refreshPreview() {
        val s = lastSnapshot ?: return
        scope.launch {
            if (brain == null) {
                setup = setupFor(s)
                _ui.update { it.copy(passengers = setup.passengers, sleepHours = setup.sleepHours) }
            }
            replan(s)
        }
    }

    @Volatile private var stopsRouteKey: String? = null
    @Volatile private var reloading = false

    private suspend fun loadStops(s: Snapshot) {
        val r = restStops.loadForRoute(s.route?.points.orEmpty(), s.route?.totalDistanceM?.toDouble())
        stopsRouteKey = routeKey(s.route)
        stops = r.stops
        _ui.update { it.copy(stopSource = r.source, stopCount = r.stops.size, stops = r.stops) }
    }

    private fun replan(s: Snapshot) {
        val totalKm = (s.route?.totalDistanceM ?: 0) / 1000.0
        val speed = s.eta?.takeIf { it.remainTimeSec > 0 }?.let { it.remainDistanceM / 1000.0 / (it.remainTimeSec / 60.0) } ?: 0.0
        val plan = TripPlanPreview.plan(stops, totalKm, speed, setup.passengers, setup.ev != null, setup.sleepHours, eager = eager)
        _ui.update { it.copy(plan = plan, kmPerMin = if (it.kmPerMin > 0) it.kmPerMin else speed) }
    }

    /** 설정 "제안 시점: 빠르게" 또는 출발 준비 "먼저 알려 주기". */
    @Volatile var eager: Boolean = false

    /**
     * 멈춤이 시작되면 아이가 있을 때 원정대 미션 3개를 고르고 라디오를 멈춘다.
     * 다시 주행하면 미션을 접고, 원정대를 다 했으면 다음 구간 라디오를 튼다 (COMMERCIAL_READINESS E P0).
     */
    private fun onModeChanged(s: Snapshot) {
        when (s.mode) {
            AppMode.PAUSE -> {
                radio.stop()
                val age = setup.passengers.filterIsInstance<Passenger.Child>().maxOfOrNull { it.age }
                if (age != null && age >= 3) expedition.start(s.pauseCause == PauseCause.CHARGING, age)
            }
            AppMode.DRIVING -> {
                val unlocked = expedition.radioUnlocked
                expedition.onDrivingStarted()
                if (unlocked && startRadio()) expedition.consumeRadio()
            }
            else -> radio.stop()
        }
        publishContent()
    }

    private var lastReplanMs: Long? = null

    private fun replanDriving(s: Snapshot, st: BrainState) {
        val f = st.fatigue ?: return
        val interval = _ui.value.plan?.intervalMin ?: TripPlanPreview.DRIVE_LIMIT_MIN
        val due = st.needs?.needs?.filter { it.kind != NeedKind.CHARGE }?.minOfOrNull { it.dueInMin }
        val gap = listOfNotNull(interval - f.continuousDriveMinutes, due).min()
        val totalKm = (s.route?.totalDistanceM ?: return) / 1000.0
        val speed = _ui.value.kmPerMin.takeIf { it > 0 } ?: (100.0 / 60)
        val plan = TripPlanPreview.plan(stops, totalKm, speed, setup.passengers, setup.ev != null, setup.sleepHours, eager, fromKm = st.traveledKm, firstGapMin = gap)
        _ui.update { it.copy(plan = plan.copy(intervalMin = interval)) }
    }

    /** 원정대 미션 완료 (멈춤 화면). */
    fun completeMission(id: String) {
        scope.launch {
            expedition.complete(id)
            publishContent()
        }
    }

    /** 도착까지 남은 시간에 맞춰 라디오를 튼다. 아이 나이를 모르면 나이 제한 없이. */
    private fun startRadio(): Boolean {
        val remain = runtime.snapshot.value.eta?.remainTimeSec ?: return false
        val age = setup.passengers.filterIsInstance<Passenger.Child>().maxOfOrNull { it.age }
        val ok = radio.start(remain, age)
        publishContent()
        return ok
    }

    private fun publishContent() {
        _ui.update {
            it.copy(
                radio = radio.state.value,
                missions = expedition.current,
                missionsDone = expedition.current.filter { m -> expedition.isDone(m.id) }.map { m -> m.id }.toSet(),
                radioUnlocked = expedition.radioUnlocked,
            )
        }
    }

    /**
     * 앱이 여행 중에 죽었다 살아났으면 이어서 안내한다 (COMMERCIAL_READINESS A P1).
     * 실차 모드 첫 화면(NOT_STARTED)에서 한 번만 본다. 이어 갈 수 있으면 여행을 ACTIVE로 되돌리고 true.
     */
    private fun checkResume(s: Snapshot): Boolean {
        // 시작 직후 기본값 Snapshot(출처 VEHICLE, 신호 출처 없음, 시각 0)은 실제 차량 프레임이 아니다.
        if (resumeChecked || s.sources.isEmpty()) return false
        resumeChecked = true
        if (s.trip != TripPhase.NOT_STARTED) return false
        val r = resumeStore.load() ?: return false
        if (!r.usable(s.nowMs, resumeKey(s.route))) {
            resumeStore.clear()
            return false
        }
        pendingResume = r
        runtime.setVehicleTrip(TripPhase.ACTIVE)
        return true
    }

    /** 사용자가 여행을 끝내거나 "내 데이터 삭제". */
    fun clearResume() {
        pendingResume = null
        resumeStore.clear()
    }

    private fun reset() {
        voiceJob?.cancel()
        radio.resetTrip()
        expedition.resetTrip()
        lastMode = null
        lastReplanMs = null
        lastSaveMs = null
        scope.launch { speech.clear() }
        brain = null
        stops = emptyList()
        previewFor = null
        _ui.value = TripUi()
    }

    private suspend fun handle(b: TripBrain, d: PlanDecision) {
        when (d) {
            is PlanDecision.Suggest -> suggest(b, d)
            is PlanDecision.NoCandidate -> say(RestPhrases.noCandidate(d))
            PlanDecision.NoNeed -> Unit
        }
    }

    private suspend fun suggest(b: TripBrain, d: PlanDecision.Suggest) {
        val level = b.state.fatigue?.level ?: return
        val phrase = phrases.restSuggestion(d, level, setup.passengers)
        while (replies.tryReceive().isSuccess) Unit // 지난 응답은 버린다
        _ui.update { it.copy(asking = d, suggestion = phrase.text, voice = VoicePhase.IDLE) }
        var answer = Reply.UNKNOWN
        // 피로 위험 제안은 안전 경고로 낸다 (큐에서 가장 먼저, 포커스가 없어도 말한다).
        val priority = if (d.fatigueFirst) SpeechPriority.SAFETY else SpeechPriority.SUGGESTION
        if (voiceEnabled) {
            say(phrase.text, priority)
            answer = listenOnce()
            if (answer == Reply.UNKNOWN) {
                // 한 번 더 묻고 끝 (C P0).
                say(RestPhrases.askAgain(), priority)
                answer = listenOnce()
            }
        }
        if (answer == Reply.UNKNOWN) {
            // 인식 실패(또는 음성 꺼짐): 버튼으로 고르게 한다. 응답이 없으면 "나중에".
            _ui.update { it.copy(voice = if (voiceEnabled) VoicePhase.FAILED else VoicePhase.IDLE) }
            answer = ReplyIntent.parse(withTimeoutOrNull(BUTTON_WAIT_MS) { replies.receive() })
        }
        _ui.update { it.copy(asking = null, suggestion = null, voice = VoicePhase.IDLE) }
        val now = runtime.snapshot.value.nowMs
        when (answer) {
            Reply.YES -> {
                val added = runtime.naviSource?.addWaypoint(d.stop.lat, d.stop.lng, d.stop.name)
                b.onAccepted(d.stop)
                val line = if (added?.isSuccess == true) RestPhrases.accepted(d.stop) else RestPhrases.waypointFailed(d)
                showToast(line)
                say(line)
            }
            Reply.LATER -> {
                b.onDeclined(now)
                say(RestPhrases.later())
            }
            Reply.UNKNOWN -> b.onDeclined(now) // 응답 없음은 "나중에"로 본다
        }
    }

    /** 8초 듣기 창. 그 사이 버튼 응답이 오면 그걸 쓴다. */
    private suspend fun listenOnce(): Reply {
        _ui.update { it.copy(voice = VoicePhase.LISTENING) }
        val text = withTimeoutOrNull(REPLY_WAIT_MS) {
            val listening = scope.launch { speechIn.listen(REPLY_WAIT_MS)?.let(replies::trySend) ?: replies.trySend("") }
            replies.receive().also { listening.cancel() }
        }
        _ui.update { it.copy(voice = VoicePhase.IDLE) }
        return ReplyIntent.parse(text)
    }

    private fun showToast(text: String) {
        toastJob?.cancel()
        _ui.update { it.copy(toast = text) }
        toastJob = scope.launch {
            delay(TOAST_MS)
            _ui.update { it.copy(toast = null) }
        }
    }

    private suspend fun say(text: String, priority: SpeechPriority = SpeechPriority.SUGGESTION) {
        _ui.update { it.copy(lastSpoken = text) }
        if (voiceEnabled) speech.say(text, priority)
    }

    private companion object {
        const val REPLY_WAIT_MS = 8_000L
        const val REPLAN_MS = 5 * 60_000L
        const val BUTTON_WAIT_MS = 15_000L
        const val TOAST_MS = 8_000L
        const val RESUME_PHRASE = "이어서 안내할게요."
    }
}

/** 경로가 바뀌었는지 알아보는 키: 목적지와 좌표열. 남은 거리·시간은 계속 바뀌므로 넣지 않는다. */
internal fun routeKey(r: com.emop.pause.core.model.RouteInfo?): String? =
    r?.takeIf { it.points.size >= 2 }?.let { "${it.destinationName}|${it.destination}|${it.points.joinToString(";") { p -> "%.4f,%.4f".format(java.util.Locale.US, p.lat, p.lng) }}" }

/**
 * 이어서 안내할 여행인지 가르는 키: 목적지만 본다. 경로 좌표에는 현재 위치가 들어 있어
 * 재시작 사이에 바뀌므로 [routeKey]를 쓰면 같은 여행도 다른 여행으로 보인다.
 */
internal fun resumeKey(r: com.emop.pause.core.model.RouteInfo?): String? =
    r?.let { "${it.destinationName}|${"%.3f,%.3f".format(java.util.Locale.US, it.destination.lat, it.destination.lng)}" }
