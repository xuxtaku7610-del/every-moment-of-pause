package com.emop.pause.trip

import com.emop.pause.core.model.TripPhase
import com.emop.pause.core.model.PetProfile
import com.emop.pause.data.pet.PetStore
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.MotionPhrases
import com.emop.pause.engine.motion.MotionState
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.pet.ArrivalInput
import com.emop.pause.engine.pet.ArrivalPrompt
import com.emop.pause.engine.pet.BoardingCheck
import com.emop.pause.engine.pet.CheckItem
import com.emop.pause.engine.pet.Josa
import com.emop.pause.engine.pet.WaitEvent
import com.emop.pause.engine.pet.WaitInput
import com.emop.pause.engine.pet.WaitMode
import com.emop.pause.engine.pet.WaitStatus
import com.emop.pause.data.RestStopRepository
import com.emop.pause.data.StopSource
import com.emop.pause.engine.fatigue.FatigueResult
import com.emop.pause.engine.needs.NeedKind
import com.emop.pause.engine.needs.NeedsResult
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
import com.emop.pause.core.model.AppMode
import com.emop.pause.voice.SpeechPriority
import com.emop.pause.voice.SpeechQueue
import com.emop.pause.voice.Utterance
import com.emop.pause.engine.needs.Passenger
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
    /** 반려견 프로필 (시뮬레이션은 시나리오 값, 실차는 기기 저장 값). */
    val pet: PetProfile = PetProfile.DEFAULT,
    /** 멀미 예보 (F1). */
    val motion: MotionState = MotionState(),
    val forecast: MotionForecast? = null,
    /** 잠깐 대기 모드 (F3). */
    val wait: WaitStatus = WaitStatus(),
    /** 출발 전 체크한 항목 (F6). */
    val checks: Set<CheckItem> = emptySet(),
    /** "내렸나요?" 알림 (F6). 확인하면 null. */
    val arrival: ArrivalPrompt? = null,
    val leftConfirmed: Boolean = false,
    /** 보호자 폰 공유 (주소·연결 상태). */
    val share: WaitShareState = WaitShareState(),
    /** 기기에 저장된 프로필이 있는지 (없으면 첫 실행 → 프로필 화면). 불러오기 전에는 null. */
    val profileSaved: Boolean? = null,
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
    private val resumeStore: TripResumeStore,
    private val petStore: PetStore,
    val share: WaitShare,
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
    /** 안전 경고 > 제안 > 안내 > 배경음, 오디오 포커스 (COMMERCIAL_READINESS C P0). */
    private val speech = SpeechQueue(speechOut, focus, scope)
    private var lastMode: AppMode? = null
    /** 앱 시작 뒤 저장된 여행을 한 번만 확인한다. */
    private var resumeChecked = false
    private var pendingResume: TripResume? = null
    private var lastSaveMs: Long? = null

    /** 음성 제안을 켤지 (설정 "음성으로 제안"). 꺼지면 화면 카드와 버튼만. */
    @Volatile var voiceEnabled: Boolean = true

    private val waitMode = WaitMode()
    private val boarding = BoardingCheck()
    /** 기기에 저장된 반려견 프로필 (실차). 시뮬레이션은 시나리오 값을 쓴다. */
    @Volatile private var savedPet: PetProfile? = null

    init {
        scope.launch { runtime.snapshot.collect(::onSnapshot) }
        scope.launch { runtime.replays.collect(::replay) }
        scope.launch { share.state.collect { st -> _ui.update { it.copy(share = st) } } }
        scope.launch {
            savedPet = runCatching { petStore.profile() }.getOrNull()
            _ui.update { it.copy(profileSaved = savedPet != null) }
            savedPet?.let { p -> if (brain == null && runtime.snapshot.value.simSetup?.pet == null) _ui.update { it.copy(pet = p) } }
        }
    }

    /** 들은 말 (STT 결과, 카드 버튼, 디버그 주입). "잠깐 다녀올게"면 대기 모드, 그 밖의 말은 제안 응답으로 듣기 창에 넘긴다. */
    fun reply(text: String) {
        when (VoiceIntent.parse(text)) {
            Utterance.StartWait -> startWait()
            is Utterance.Answer -> replies.trySend(text)
        }
    }

    // ───────────── 반려견 프로필·체크·대기 (F3·F6) ─────────────

    fun savePet(p: PetProfile) {
        scope.launch {
            runCatching { petStore.saveProfile(p) }
            savedPet = p
            _ui.update { it.copy(pet = p, profileSaved = true) }
        }
        refreshPreview()
    }

    fun toggleCheck(item: CheckItem) {
        scope.launch {
            boarding.toggle(item)
            _ui.update { it.copy(checks = boarding.checkedItems) }
        }
    }

    /** "잠깐 다녀올게" (P단에서만, F3-05). */
    fun startWait() {
        scope.launch {
            val ev = waitMode.start(waitInput(runtime.snapshot.value))
            if (ev.isEmpty()) {
                if (!waitMode.status.active) showToast(WAIT_PARK_ONLY)
                return@launch
            }
            publishWait(ev.first())
            say("대기 모드를 켰어요. ${Josa.waGwa(_ui.value.pet.name)} 함께 기다릴게요.")
        }
    }

    fun stopWait() {
        scope.launch { waitMode.stop().firstOrNull()?.let(::publishWait) }
    }

    fun setWaitLimit(minutes: Int) {
        scope.launch {
            waitMode.setLimit(minutes)
            publishWait(null)
        }
    }

    /** 화면 결과 한 줄 (장소를 경유지로 넣었을 때 등). */
    fun notify(text: String) {
        scope.launch { showToast(text) }
    }

    /** "내 데이터 삭제": 저장한 프로필도 잊는다. */
    fun forgetPet() {
        scope.launch {
            savedPet = null
            _ui.update { it.copy(pet = PetProfile.DEFAULT, profileSaved = false) }
        }
    }

    /** "내렸어요" (F6-04). */
    fun confirmLeft() {
        scope.launch {
            boarding.confirmLeft()
            _ui.update { it.copy(arrival = null, leftConfirmed = true) }
        }
    }

    private fun waitInput(s: Snapshot) = WaitInput(s.nowMs, s.driveState, s.sample.gear, s.sample.parkingBrakeOn, s.sample.batterySocPercent, s.sample.hvacTempC)

    private fun publishWait(event: WaitEvent?) {
        _ui.update { it.copy(wait = waitMode.status) }
        share.onChanged(waitMode.status, event)
    }

    /** 대기 모드·도착 체크는 여행 단계와 상관없이 1초마다 본다. */
    private fun tickPetCare(s: Snapshot, silent: Boolean) {
        val events = waitMode.tick(waitInput(s))
        if (events.isNotEmpty() || waitMode.status.active) publishWait(events.firstOrNull())
        if (!silent && WaitEvent.Overdue in events) scope.launch { say(WAIT_OVERDUE, SpeechPriority.SAFETY) }
        val near = s.trip == TripPhase.ACTIVE && (s.eta?.remainDistanceM ?: Int.MAX_VALUE) <= ARRIVE_NEAR_M
        val ending = s.mode == AppMode.ARRIVED || s.trip == TripPhase.ENDED || near
        val prompt = boarding.tick(ArrivalInput(s.nowMs, s.sample.gear, s.sample.parkingBrakeOn, ending, s.sample.hvacTempC, java.time.LocalDate.now().monthValue))
            ?: return
        _ui.update { it.copy(arrival = prompt) }
        if (silent) return
        val name = _ui.value.pet.name
        val line = if (prompt.strong) "$name 내렸나요? 차 안이 더워질 수 있어요. 꼭 확인해 주세요." else "$name 내렸나요?"
        scope.launch { say(line, if (prompt.strong) SpeechPriority.SAFETY else SpeechPriority.SUGGESTION) }
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
        tickPetCare(s, silent)
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
                motion = state.motion,
                forecast = state.forecast,
            )
        }
        // 멀미 예보는 제안·질문 중이 아닐 때 한 줄로 말한다 (F1-10: 같은 구간은 다시 말하지 않음).
        state.speakForecast?.let { f ->
            if (silent) {
                b.dropForecast()
            } else if (voiceJob?.isActive != true) {
                b.onForecastSpoken(s.nowMs)
                val line = MotionPhrases.forecast(f, setup.pet.name)
                showToast(line)
                scope.launch { say(line) }
            }
        }
        val d = state.pending ?: return
        if (silent || voiceJob?.isActive == true) return
        b.onShown(s.nowMs)
        voiceJob = scope.launch { handle(b, d) }
    }

    private fun startTrip(s: Snapshot): TripBrain {
        setup = setupFor(s)
        val shape = s.route?.shape?.takeIf { it.size >= 2 }?.let(RouteShape::fromLatLng)
        val b = TripBrain(setup, shape, s.route?.shapeStartM ?: 0) { stops }
        brain = b
        _ui.update { it.copy(passengers = setup.passengers, sleepHours = setup.sleepHours, pet = setup.pet) }
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

    /** 시뮬레이션은 시나리오 설정, 실차는 저장된 반려견 프로필 + 프리셋(전기차·수면). 같이가개는 반려견이 늘 함께 탄다. */
    private fun setupFor(s: Snapshot): TripSetup {
        s.simSetup?.let { return PresetStore.fromScenario(it) }
        val pet = savedPet ?: PetProfile.DEFAULT
        return presets.setup().copy(passengers = listOf(Passenger.Dog(pet.pottyIntervalMin)), pet = pet)
    }

    /** 출발 준비 화면의 "오늘의 쉼표" (경로를 알면 출발 전에 미리 받는다). */
    private fun loadPreview(s: Snapshot, key: Pair<SourceMode, String?>) {
        if (s.route == null) return
        previewFor = key
        lastSnapshot = s
        scope.launch {
            loadStops(s)
            setup = setupFor(s)
            _ui.update { it.copy(passengers = setup.passengers, sleepHours = setup.sleepHours, pet = setup.pet) }
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
        lastMode = null
        lastReplanMs = null
        lastSaveMs = null
        scope.launch { speech.clear() }
        brain = null
        stops = emptyList()
        previewFor = null
        waitMode.stop()
        boarding.reset()
        _ui.value = TripUi(pet = savedPet ?: PetProfile.DEFAULT, share = share.state.value, profileSaved = _ui.value.profileSaved)
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
        val phrase = phrases.restSuggestion(d, level, setup.passengers, setup.pet.name)
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
        const val WAIT_PARK_ONLY = "차가 P단일 때만 대기 모드를 켤 수 있어요."
        const val WAIT_OVERDUE = "대기 시간이 지났어요. 보호자 화면에도 알렸어요."

        /** 목적지 이만큼 안에서 P단이면 여행이 끝나는 중으로 본다 (F6). */
        const val ARRIVE_NEAR_M = 300
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
