package com.emop.pause.trip

import com.emop.pause.core.model.DoorState
import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.TripPhase
import com.emop.pause.core.model.PetProfile
import com.emop.pause.data.pet.PetStore
import com.emop.pause.engine.motion.MotionForecast
import com.emop.pause.engine.motion.MotionPhrases
import com.emop.pause.engine.motion.MotionState
import com.emop.pause.data.PetPlaceRepository
import com.emop.pause.data.RouteGeo
import com.emop.pause.core.model.LatLng
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.motion.TbtRouteShape
import com.emop.pause.engine.motion.CalibrationSample
import com.emop.pause.engine.motion.GuidanceFromShape
import com.emop.pause.engine.motion.MotionConstants
import com.emop.pause.engine.pet.DistressAction
import com.emop.pause.engine.pet.DistressAdvice
import com.emop.pause.engine.pet.DistressKind
import com.emop.pause.engine.pet.DistressSignals
import com.emop.pause.engine.pet.EmergencyKind
import com.emop.pause.engine.pet.PetDistress
import com.emop.pause.engine.planner.StopType
import com.emop.pause.data.Vet
import com.emop.pause.presentation.DemoScenes
import com.emop.pause.presentation.UiMapper
import kotlin.math.roundToInt
import com.emop.pause.engine.pet.ArrivalInput
import com.emop.pause.engine.pet.ArrivalPrompt
import com.emop.pause.engine.pet.BoardingCheck
import com.emop.pause.engine.pet.DoorEvent
import com.emop.pause.engine.pet.DoorInput
import com.emop.pause.engine.pet.DoorWatch
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
import com.emop.pause.engine.needs.NeedsEngine
import com.emop.pause.engine.needs.NeedsResult
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.PetTripPreview
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.RestPhrases
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.TripPlan
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
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
    /** 앞 1km 곡률 (주행 화면 도로 그림). */
    val roadAhead: List<Float> = emptyList(),
    /** 다음 굽은 길까지(m), 최근 30분 멀미(1분 간격) — 주행 상태 패널. */
    val nextCurveM: Double? = null,
    val motionRecent: List<Int> = emptyList(),
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
    /** 앞 구간 형상을 무엇으로 보는지 (설정·발표 모드 표시, UPGRADE 5). */
    val shapeSource: String? = null,
    /** "콩이가 이상해요" 지금 안내 중 (UPGRADE 7). 수락·괜찮아졌어요면 null. */
    val distress: DistressState? = null,
    /** 정차하면 보여 줄 체크리스트 (content/care/care_ID.md). 괜찮아졌어요·여행 끝이면 null. */
    val careId: String? = null,
    /** 이번 여행 관찰 신고 (보호자 음성 "콩이 토했어" 등) — 콩이 관찰 상태·기록 리포트·병원 공유·개인 보정의 유일한 입력. */
    val distressEvents: List<DistressRecord> = emptyList(),
    /** 시뮬레이터 주행 화면 "다음 회전" (시연 데이터). 실차는 Snapshot.guide. */
    val demoTurn: RouteTurn? = null,
    /** 주행 중 주변 추천 (UI v9, 일반 도로에서만) — 지금 묻고 있는 장소 하나. */
    val nearby: NearbyOffer? = null,
)

/**
 * 관찰 신고 한 건과 그때의 주행 정보 (재정비 상태 5). [drivingMin]은 출발 뒤 주행 분(정차 제외), [clockMinute]은 하루 중 분(신호 시계),
 * [speedKph]·[stimulus]는 신고 순간 값(없으면 null) — 병원 공유·리포트에 그대로 쓰고 지어내지 않는다.
 */
data class DistressRecord(
    val label: String,
    val drivingMin: Int,
    val kind: DistressKind? = null,
    val clockMinute: Int? = null,
    val speedKph: Float? = null,
    val stimulus: Int? = null,
    /** 시뮬레이터 시각(초). 실차는 null. */
    val simSec: Int? = null,
    /** 보호자가 실제로 말한 신고(발표 대본 장면이 아님) — 장면 점프 뒤에도 이어서 남긴다 (결정 11/01). */
    val live: Boolean = false,
    /** 응급이면 상황 종류 (긴급 도움, 11/03) — [label]도 그 이름("위험 물질 섭취"). */
    val emergency: EmergencyKind? = null,
)

/** 장면 점프 때 이어서 남길 실시간 신고 (사용자 결정 11/01): 같은 여행(시나리오)이고 점프 끝 시각 안쪽인 것만, 시뮬레이터 초별로. */
internal object LiveReports {
    fun carry(events: List<DistressRecord>, sameTrip: Boolean, untilSec: Int): Map<Int, List<DistressRecord>> =
        if (!sameTrip) emptyMap()
        else events.filter { it.live && it.kind != null && it.simSec != null && it.simSec <= untilSec }.groupBy { it.simSec!! }
}

/** 점프로 따라잡은 1초 한 장: 그 시각 신호 + 엔진 결과 (이동 기록을 실제로 달린 것과 같게 채운다). */
data class ReplayTick(val s: Snapshot, val ui: TripUi)

/** "콩이가 이상해요" 한 건: 안내 문구·행동과 고른 정차 장소(또는 병원). */
data class DistressState(
    val kind: DistressKind,
    val advice: DistressAdvice,
    val stop: RestStop? = null,
    val vet: Vet? = null,
    val minutes: Int? = null,
)

/** 앞 구간 형상: 시연 GPX / 회전 안내(GPX에서 만든 가짜 내비 안내). 실차는 항상 회전 안내 + 진행 방위. */
enum class ShapeMode { DEMO_GPX, GUIDANCE }

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
    private val petPlaces: PetPlaceRepository,
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
    /** 쉼표 제안 응답(음성 8초 → 버튼). 감시·화면 반영은 엔진 스레드와 다른 스레드에서 (BLOCKED 11/01). */
    private val watchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gate = ReplyGate(
        listen = { ms -> speechIn.listen(ms) },
        listenScope = scope,
        watchScope = watchScope,
        onStuck = { turn -> android.util.Log.w("PauseVoice", "듣기 상태가 ${ReplyGate.VOICE_MS}ms를 넘어 버튼으로 바꿈 turn=$turn") },
    )
    private var lastMode: AppMode? = null
    /** 앱 시작 뒤 저장된 여행을 한 번만 확인한다. */
    private var resumeChecked = false
    private var pendingResume: TripResume? = null
    private var lastSaveMs: Long? = null

    /** 음성 제안을 켤지 (설정 "음성으로 제안"). 꺼지면 화면 카드와 버튼만. */
    @Volatile var voiceEnabled: Boolean = true

    private val waitMode = WaitMode()
    /** 주행 중 주변 추천 (UI v9, F4-10~15): 일반 도로에서만, 한 번에 하나, 같은 곳 다시 안 뜸, 15분 간격. 도착 전 산책(F4-03)도 여기로. */
    private val nearbyPick = NearbyPick()
    /** 고속도로 판단 = 최근 5분 평균 속도 80km/h 이상 (TBT에 도로 이름이 없어서, 사용자 결정 11/03). */
    private val highway = HighwayGuess()
    /** 병원으로 가는 중 (ViewModel이 알려 준다) — 주변 추천을 띄우지 않는다. */
    @Volatile var goingVet: Boolean = false
    private val boarding = BoardingCheck()
    /** 기기에 저장된 반려견 프로필 (실차). 시뮬레이션은 시나리오 값을 쓴다. */
    @Volatile private var savedPet: PetProfile? = null

    /** 들은 말 (STT 결과, 카드 버튼, 디버그 주입). "콩이가 헥헥거려" 같은 반려견 이상이면 그 안내가 먼저(UPGRADE 7),
     *  "잠깐 다녀올게"면 대기 모드, 그 밖의 말은 제안 응답으로 듣기 창에 넘긴다. */
    fun reply(text: String) {
        PetDistress.classify(text)?.let { reportDistress(it, PetDistress.emergencyKind(text)); return }
        when (VoiceIntent.parse(text)) {
            Utterance.StartWait -> startWait()
            is Utterance.Answer -> {
                replies.trySend(text)
                gate.offer(text)
            }
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

    /** 화면 한 줄 + 같은 문장 음성 (병원 응답처럼 주행 중에도 알려야 하는 것). */
    fun announce(text: String) {
        scope.launch {
            showToast(text)
            say(text, SpeechPriority.SAFETY)
        }
    }

    /** 화면 결과 한 줄 (장소를 경유지로 넣었을 때 등). */
    fun notify(text: String) {
        scope.launch { showToast(text) }
    }

    // ───────────── "콩이가 이상해요" (UPGRADE 7) ─────────────

    /**
     * 점프로 지나간 "콩이가 이상해요" — 발표 장면(DemoScenes `say`)과 점프 전에 보호자가 실제로 말한 신고([live]):
     * 안내·음성·장소 없이 증상 기록과 잠시 오른 민감도만 남긴다.
     */
    private fun replayDistress(kind: DistressKind, cabinTempC: Float?, s: Snapshot, live: Boolean = false, emergency: EmergencyKind? = null) {
        val t = _ui.value
        val drivingMin = (t.motion.drivingSec / 60).toInt()
        val signals = DistressSignals(
            cabinTempC = cabinTempC ?: s.sample.hvacTempC,
            month = java.time.LocalDate.now().monthValue,
            drivingMin = drivingMin,
            motionIndex = t.motion.personal,
            motionRising = t.motion.personal >= MotionConstants.CAUTION || (t.forecast?.personalPredicted ?: 0) > t.motion.personal,
        )
        val advice = PetDistress.advise(kind, signals, emergency)
        if (advice.boost > 1.0) brain?.boost(advice.boost, s.nowMs)
        _ui.update { it.copy(distressEvents = it.distressEvents + record(kind, drivingMin, s, t, emergency).copy(live = live)) }
        // 점프로 지나간 장면의 신고는 개인 보정에 넣지 않는다(실시간 신고는 말한 때 이미 넣었다) (발표 점프를 반복할 때마다 표본이 쌓여 숫자가 바뀌지 않게)
        auto.onReport(kind, t.motionRecent.maxOrNull() ?: t.motion.index)
    }

    private fun record(kind: DistressKind, drivingMin: Int, s: Snapshot, t: TripUi, emergency: EmergencyKind? = null) =
        DistressRecord(
            PetDistress.label(kind, emergency), drivingMin, kind, s.clockMinute, s.sample.speedKph, t.motion.index,
            simSec = s.simTimeSec.toInt().takeIf { s.source == SourceMode.SIMULATION },
            emergency = emergency.takeIf { kind == DistressKind.EMERGENCY },
        )

    /** 개인 보정 자동 표본 (신고·높음 구간 뒤 조용함). ViewModel이 받아 기기에 저장한다. */
    private val auto = AutoCalibration()
    private val _calibration = MutableSharedFlow<CalibrationSample>(extraBufferCapacity = 8)
    val calibrationSamples: SharedFlow<CalibrationSample> = _calibration

    /** 발표 장면용 실내 온도(시뮬레이터 여름 장면). 실차에서는 쓰지 않는다 — 측정값만. */
    @Volatile var demoCabinTempC: Float? = null

    /** [emergency] = 응급 상황 종류(말에서 고른 것, 없으면 null) — 문구·행동은 같고 카드·기록 이름만 달라진다. */
    fun reportDistress(kind: DistressKind, emergency: EmergencyKind? = null) {
        scope.launch { handleDistress(kind, emergency) }
    }

    private suspend fun handleDistress(kind: DistressKind, emergency: EmergencyKind? = null) {
        val s = runtime.snapshot.value
        val t = _ui.value
        val drivingMin = (t.motion.drivingSec / 60).toInt()
        val cabin = (if (s.source == SourceMode.SIMULATION) demoCabinTempC else null) ?: s.sample.hvacTempC
        val signals = DistressSignals(
            cabinTempC = cabin,
            month = java.time.LocalDate.now().monthValue,
            drivingMin = drivingMin,
            motionIndex = t.motion.personal,
            motionRising = t.motion.personal >= MotionConstants.CAUTION || (t.forecast?.personalPredicted ?: 0) > t.motion.personal,
        )
        val advice = PetDistress.advise(kind, signals, emergency)
        val stop = DistressStops.pick(advice.action, t.stops, t.traveledKm)
        // 응급이면 가까운 병원 — 시뮬레이터는 시연용 제휴 병원을 맨 앞에(사전 접수 시연), 실차는 실제 병원만(전화·링크)
        val vet = if (advice.action == DistressAction.VET) here(s)?.let { at ->
            runCatching { DemoPartner.prepend(petPlaces.vetsNear(at).items, at, s.source == SourceMode.SIMULATION).firstOrNull() }.getOrNull()
        } else null
        val speed = t.kmPerMin.takeIf { it > 0 } ?: (100.0 / 60)
        val minutes = stop?.let { maxOf(1, ((it.distanceAlongRouteKm - t.traveledKm) / speed).roundToInt()) } ?: vet?.let { UiMapper.vetMinutes(it.distanceKm) }
        if (advice.boost > 1.0) brain?.boost(advice.boost, s.nowMs)
        _ui.update {
            it.copy(
                distress = DistressState(kind, advice, stop, vet, minutes),
                careId = advice.care,
                distressEvents = it.distressEvents + record(kind, drivingMin, s, t, emergency).copy(live = true),
            )
        }
        auto.onReport(kind, t.motionRecent.maxOrNull() ?: t.motion.index)?.let { _calibration.tryEmit(it) }
        val where = (stop?.name ?: vet?.name)?.let { n -> minutes?.let { " ${n}까지 약 ${it}분이에요." } ?: " $n." }.orEmpty()
        val extra = if (advice.action == DistressAction.GENTLE_DRIVE) " 조용한 음악도 좋아요." else ""
        say(advice.line + where + extra, SpeechPriority.SAFETY)
    }

    /** 안내를 받아들임: 정차 장소를 경유지로(병원은 ViewModel이 도착 예정 공유와 함께). 체크리스트는 정차 때까지 남는다. */
    fun acceptDistress(): DistressState? {
        val d = _ui.value.distress ?: return null
        scope.launch {
            d.stop?.let { st ->
                val added = runtime.naviSource?.addWaypoint(st.lat, st.lng, st.name)
                brain?.onAccepted(st)
                showToast(if (added?.isSuccess == true) "${st.name}, 경로에 추가했어요." else "경로에 넣지 못했어요. 내비에서 ${st.name}을 골라 주세요.")
            }
            _ui.update { it.copy(distress = null) }
        }
        return d
    }

    /** "괜찮아졌어요": 안내와 체크리스트를 닫는다(기록은 남는다). */
    fun dismissDistress() {
        scope.launch { _ui.update { it.copy(distress = null, careId = null) } }
    }

    /** 지금 위치: 실차는 경로 첫 점(NaviHelper 현재 위치), 시뮬레이터는 진행 비율로 경로 위 지점. */
    private fun here(s: Snapshot): LatLng? {
        val r = s.route ?: return null
        if (s.source == SourceMode.VEHICLE) return r.points.firstOrNull()
        val frac = s.eta?.let { 1.0 - it.remainDistanceM.toDouble() / r.totalDistanceM.coerceAtLeast(1) } ?: 0.0
        return PetPlaceRepository.pointAlong(r.points, frac)
    }

    /** 앞 구간 형상 (발표 모드에서 전환). 바꾸면 엔진을 새로 만든다 → 다음 장면 이동부터 반영. */
    @Volatile var shapeMode: ShapeMode = ShapeMode.DEMO_GPX
        private set

    fun setShapeMode(m: ShapeMode) {
        shapeMode = m
        scope.launch { brain = null }
    }

    /** 개인 보정 계수 (CalibrationStore → MotionCalibration.scale). 다음 여행(엔진을 새로 만들 때)부터 반영. */
    @Volatile var motionScale: Double = 1.0

    /** 음성으로 묻고 한 번 듣기 (병원 전달 동의 등). 음성 제안을 껐거나 못 들으면 null → 화면 버튼. */
    suspend fun ask(question: String): String? {
        if (!voiceEnabled) return null
        say(question)
        while (replies.tryReceive().isSuccess) Unit
        // 마이크 받아쓰기와 화면·adb 응답(`--es say 네`) 중 먼저 온 것. 받아쓰기가 실패해도 남은 시간 동안 응답을 기다린다.
        return withTimeoutOrNull(DICTATE_MS + 2_000) {
            coroutineScope {
                val heard = async { dictate() }
                select<String?> {
                    replies.onReceive { heard.cancel(); it }
                    heard.onAwait { it ?: replies.receive() }
                }
            }
        }
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
        val arrivalInput = ArrivalInput(s.nowMs, s.sample.gear, s.sample.parkingBrakeOn, ending, s.sample.hvacTempC, java.time.LocalDate.now().monthValue)
        val doorEvents = doorWatch.tick(
            DoorInput(
                gear = s.sample.gear,
                parkingBrakeOn = s.sample.parkingBrakeOn,
                doors = (if (s.source == SourceMode.SIMULATION) demoDoors else null) ?: s.sample.doors,
                tripActive = s.trip != TripPhase.NOT_STARTED,
                tripEnding = ending,
                waitActive = waitMode.status.active,
                leftConfirmed = boarding.confirmedLeft,
            ),
        )
        // a) 뒷문이 열리면 60초를 기다리지 않고 바로 묻는다. c) 운전석 문이 먼저 열려도 아직 안 물었으면 지금 묻는다.
        val now = if (DoorEvent.ARRIVAL_CHECK in doorEvents || DoorEvent.LEFT_BEHIND in doorEvents) boarding.promptNow(arrivalInput) else null
        val prompt = boarding.tick(arrivalInput) ?: now
        if (prompt != null) _ui.update { it.copy(arrival = prompt) }
        if (silent) return
        val name = _ui.value.pet.name
        if (DoorEvent.WAIT_OFFER in doorEvents) scope.launch {
            showToast(WAIT_DOOR_OFFER)
            say(WAIT_DOOR_OFFER, SpeechPriority.SUGGESTION)
        }
        if (DoorEvent.LEFT_BEHIND in doorEvents) {
            // 기존 "내렸나요?" 안전 안내를 한 번만 (반복·경적·외부 전송 없음). 앞에 문 신호 사실만 붙인다.
            val strong = (prompt ?: boarding.pending)?.strong == true
            scope.launch { say("$DOOR_OPENED ${arrivalLine(name, strong)}", SpeechPriority.SAFETY) }
            return
        }
        prompt ?: return
        scope.launch { say(arrivalLine(name, prompt.strong), if (prompt.strong) SpeechPriority.SAFETY else SpeechPriority.SUGGESTION) }
    }

    private fun arrivalLine(name: String, strong: Boolean) =
        if (strong) "$name 내렸나요? 차 안이 더워질 수 있어요. 꼭 확인해 주세요." else "$name 내렸나요?"

    /** 문 이벤트 (차량 이벤트 연결, 2026-11-02). 문 신호가 없으면 아무것도 하지 않는다. */
    private val doorWatch = DoorWatch()

    /** 발표용 문 상태 (디버그 adb `--es door rear|driver|both|closed|none`). 시뮬레이터에서만 시나리오 값보다 먼저 쓴다. */
    @Volatile var demoDoors: DoorState? = null

    private val _replayed = MutableSharedFlow<List<ReplayTick>>(extraBufferCapacity = 1)

    /** 점프로 따라잡은 결과 (1초마다). ViewModel이 이동 기록을 이것으로 다시 쌓는다 — 멀미·휴식·증상이 실제로 달린 것과 같다. */
    val replayed: SharedFlow<List<ReplayTick>> = _replayed

    /**
     * 발표 모드 점프: 건너뛴 구간을 음성 없이 따라잡는다. 그 사이 제안은 없었던 것으로 보고,
     * 지나간 발표 장면의 증상(DemoScenes `say`, 예: 2,400초 "콩이가 헥헥거려")은 그 시각에 있었던 것으로 기록한다.
     * 점프 전에 보호자가 실제로 말한 신고(예: PT3 "콩이 토했어")도 지우지 않고 그 시각에 다시 넣는다 — 리포트·병원 요약에 이어서 남는다.
     */
    private fun replay(frames: List<Snapshot>) {
        voiceJob?.cancel()
        scope.launch { speech.clear() }
        val started = android.os.SystemClock.elapsedRealtime()
        val first = frames.firstOrNull()
        val live = LiveReports.carry(
            _ui.value.distressEvents,
            sameTrip = first != null && (first.source to first.scenarioName) == tripKey,
            untilSec = frames.lastOrNull()?.simTimeSec?.toInt() ?: -1,
        )
        reset()
        val scripted = if (frames.firstOrNull()?.scenarioName == DemoScenes.SCENARIO) {
            DemoScenes.JUMPS.mapNotNull { j -> j.say?.let(PetDistress::classify)?.let { j.tSec to (it to j.cabinTempC) } }.toMap()
        } else emptyMap()
        val ticks = ArrayList<ReplayTick>(frames.size)
        frames.forEach { s ->
            onSnapshot(s, silent = true)
            brain?.let { b ->
                if (b.state.pending != null) {
                    b.onShown(s.nowMs)
                    b.onDeclined(s.nowMs)
                }
            }
            scripted[s.simTimeSec.toInt()]?.let { (kind, cabin) -> if (brain != null) replayDistress(kind, cabin, s) }
            live[s.simTimeSec.toInt()]?.forEach { r -> if (brain != null) replayDistress(r.kind!!, null, s, live = true, emergency = r.emergency) }
            ticks += ReplayTick(s, _ui.value)
        }
        brain?.policy?.onAccepted()
        android.util.Log.i("PauseReplay", "frames=${frames.size} ms=${android.os.SystemClock.elapsedRealtime() - started}")
        _replayed.tryEmit(ticks)
        // 처음(0초)으로 점프하면 따라잡을 프레임이 없다. 멈춘 시뮬레이터는 새 프레임을 보내지 않으므로, 위 reset()이
        // 먼저 읽은 출발 준비(프로필·쉼표 계획)를 지운 채 남지 않게 지금 프레임을 한 번 더 읽는다 (11/01 경쟁 조건)
        if (frames.isEmpty()) scope.launch {
            delay(REPLAY_SETTLE_MS)
            runtime.snapshot.value.takeIf { it.source == SourceMode.SIMULATION && it.simTimeSec < 1.0 }?.let { onSnapshot(it) }
        }
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
        // 점프 따라잡기(silent)에서는 경로 키를 만들지 않는다 — 프레임마다 문자열 포맷이 1만 번이면 에뮬레이터에서 수 초(10/31)
        val rk = if (silent) null else routeKey(s.route)
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
        val state = b.tick(BrainInput(s.nowMs, s.clockMinute, s.mode, s.driveState, s.sample, s.capabilities, s.eta, s.route?.totalDistanceM, s.turns))
        // 주행 중 쉼표 계획을 지금 위치·니즈로 다시 잡는다 (출발 전 계획이 낡아 "77분 뒤"가 보이지 않게).
        if (s.mode == AppMode.DRIVING && (s.mode != lastMode || lastReplanMs.let { it == null || s.nowMs - it >= REPLAN_MS })) {
            lastReplanMs = s.nowMs
            replanDriving(s, state)
        }
        lastMode = s.mode
        highway.add(s.nowMs, s.sample.speedKph)
        if (s.mode == AppMode.DRIVING && state.pending == null) {
            val now = _ui.value
            val why = nearbyPick.due(
                NearbyInput(
                    s.nowMs, highway.highway(),
                    busy = voiceJob?.isActive == true || now.asking != null || now.distress != null || now.nearby != null || goingVet ||
                        state.forecast?.let { it.startInM <= UiMapper.FORECAST_SHOW_M } == true,
                    reports = now.distressEvents.size,
                    emergency = now.distressEvents.any { it.emergency != null },
                    remainKm = s.eta?.remainDistanceM?.let { it / 1000.0 },
                ),
            )
            if (why != null) {
                nearbyPick.onSearched(why)
                // 점프로 따라잡는 중에는 지나간 것으로만 친다 (목적지 추천은 한 번뿐이라 지나가면 끝).
                if (!silent) voiceJob = scope.launch { offerNearby(s, why) }
            }
        }
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
                roadAhead = state.roadAhead,
                nextCurveM = state.nextCurveM,
                motionRecent = state.motionRecent,
                demoTurn = if (s.source == SourceMode.SIMULATION) state.demoTurn else null,
            )
        }
        auto.onTick(s.nowMs / 1000, state.motion.index)?.let { if (!silent) _calibration.tryEmit(it) }
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
        // 앞 구간 형상: 시연 GPX가 있으면 그것, 실차는 NaviHelper 회전 안내 + 진행 방위 추정 (docs/plan/sdk_check.md).
        // 시연 "회전 안내" 모드(UPGRADE 5): GPX 대신 GPX에서 만든 가짜 내비 안내 + 진행 방위만으로 예보한다.
        val gpx = s.route?.shape?.takeIf { it.size >= 2 }?.let(RouteShape::fromLatLng)
        // 시뮬레이터 주행 화면 "다음 회전"(시연 데이터): GPX에서 만든 안내를 표시에만 쓴다
        val demoGuidance = if (s.source == SourceMode.SIMULATION) gpx?.let(::GuidanceFromShape) else null
        val (shape, guidance) = when {
            gpx != null && shapeMode == ShapeMode.GUIDANCE -> TbtRouteShape() to GuidanceFromShape(gpx)
            gpx != null -> gpx to null
            s.source == SourceMode.VEHICLE -> TbtRouteShape() to null
            else -> null to null
        }
        val shapeLabel = when {
            guidance != null -> "회전 안내 (시연: OSM 길에서 만든 안내) + 진행 방위"
            gpx != null -> "시연 경로 (OSM GPX)"
            shape != null -> "회전 안내 + 진행 방위 (NaviHelper)"
            else -> "없음 (진행 방위만)"
        }
        _ui.update { it.copy(shapeSource = shapeLabel) }
        // 개인 보정 계수(UPGRADE 4)를 반려견 프로필에 싣는다 — 엔진은 예보 단계·쉼표 시각에만 곱하고 주행 자극 숫자는 보정 전 값 (결정 11/01)
        val b = TripBrain(setup.copy(pet = setup.pet.copy(motionScale = motionScale)), shape, s.route?.shapeStartM ?: 0, guidance, demoGuidance) { stops }
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
        // 주행 중 추천과 같은 플래너(PetRestPlanner)를 앞으로 돌려 본다 — 미리보기와 실제 추천이 같은 휴게소.
        val plan = PetTripPreview.plan(stops, totalKm, speed, setup.passengers, setup.sleepHours, preferred(), s.clockMinute)
        _ui.update { it.copy(plan = plan, kmPerMin = if (it.kmPerMin > 0) it.kmPerMin else speed) }
    }

    /** 설정 "제안 시점: 빠르게" 또는 출발 준비 "먼저 알려 주기". */
    @Volatile var eager: Boolean = false

    private var lastReplanMs: Long? = null

    private fun replanDriving(s: Snapshot, st: BrainState) {
        val b = brain ?: return
        val totalKm = (s.route?.totalDistanceM ?: return) / 1000.0
        val speed = _ui.value.kmPerMin.takeIf { it > 0 } ?: (100.0 / 60)
        // 지금까지의 피로·니즈 누적값과 받아들인 휴게소에서 시작한다 (주행 중 추천과 같은 플래너).
        val saved = b.save(s.nowMs, null)
        val plan = PetTripPreview.plan(
            stops, totalKm, speed, setup.passengers, setup.sleepHours, preferred(), s.clockMinute,
            fromKm = st.traveledKm, fatigue = saved.fatigue, needsElapsedSec = saved.needsElapsedSec, heading = st.heading,
        )
        _ui.update { it.copy(plan = plan) }
    }

    private fun preferred() = setup.pet.facilityPrefs.mapNotNull(Facility::fromCode).toSet()

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
        gate.invalidate()
        lastMode = null
        lastReplanMs = null
        lastSaveMs = null
        scope.launch { speech.clear() }
        brain = null
        stops = emptyList()
        nearbyPick.reset()
        highway.reset()
        previewFor = null
        waitMode.stop()
        boarding.reset()
        doorWatch.reset()
        auto.reset()
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
        val turn = gate.begin()
        val phrase = phrases.restSuggestion(d, level, setup.passengers, setup.pet.name)
        // 문장을 받는 사이 점프·초기화가 있었으면 이 제안은 없던 것으로 한다 (늦게 닿은 취소가 화면을 덮지 않게).
        currentCoroutineContext().ensureActive()
        if (!gate.isCurrent(turn)) return
        _ui.update { it.copy(asking = d, suggestion = phrase.text) }
        // 피로 위험 제안은 안전 경고로 낸다 (큐에서 가장 먼저, 포커스가 없어도 말한다).
        val priority = if (d.fatigueFirst) SpeechPriority.SAFETY else SpeechPriority.SUGGESTION
        // 음성은 8초 한 번, 못 들으면 버튼(응답이 없으면 "나중에"). 음성 제안을 껐으면 바로 버튼.
        val answer = try {
            gate.await(turn, voiceEnabled) { say(phrase.text, priority) }
        } finally {
            if (gate.isCurrent(turn)) _ui.update { it.copy(asking = null, suggestion = null) }
        }
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

    /** 증상 메모 받아쓰기 한 번 (UPGRADE 3, 정차 중 화면 버튼). 못 들으면 null. */
    suspend fun dictate(): String? = runCatching { speechIn.listen(DICTATE_MS) }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * 주변 추천 하나 (UI v9): 후보를 받아 고르고, 쉼표 추천과 같은 응답 창(음성 8초 → 버튼)으로 묻는다.
     * [들를게요] = 쉼표 추천과 같은 경유지 추가(`addWaypoint`). 후보 이름은 실제 데이터(서버 TourAPI·인허가)만 — 내장 샘플은 버린다.
     */
    private suspend fun offerNearby(s: Snapshot, why: NearbyWhy) {
        val offer = runCatching { nearbyPick.pick(why, nearbyCandidates(s, why)) }.getOrNull()
        if (offer == null) {
            if (why == NearbyWhy.REPORT_VET) nearbyPick.onEmpty(s.nowMs)
            return
        }
        currentCoroutineContext().ensureActive()
        if (_ui.value.asking != null || _ui.value.distress != null) return
        nearbyPick.onShown(offer, runtime.snapshot.value.nowMs)
        android.util.Log.i("PauseNearby", "offer why=$why kind=${offer.place.kind} name=${offer.place.name} ahead=${offer.place.aheadKm} sim=${runtime.snapshot.value.simTimeSec.toInt()}")
        val turn = gate.begin()
        _ui.update { it.copy(nearby = offer) }
        val answer = try {
            gate.await(turn, voiceEnabled) { say(offer.phrase(_ui.value.pet.name)) }
        } finally {
            if (gate.isCurrent(turn)) _ui.update { it.copy(nearby = null) }
        }
        if (answer == Reply.YES) {
            val p = offer.place
            val added = runtime.naviSource?.addWaypoint(p.lat, p.lng, p.name)
            val line = if (added?.isSuccess == true) "경로에 ${Josa.eulReul(p.name)} 넣었어요." else "경유지를 넣지 못했어요. 기본 내비에서 ${Josa.eulReul(p.name)} 찾아 주세요."
            showToast(line)
            say(line)
        }
    }

    /** 후보: 병원은 지금 위치 근처 → 경로에 투영해 앞 거리·경로 거리, 목적지는 반경 3km 산책로·쇼핑. 위치는 계산에만 쓰고 저장하지 않는다. */
    private suspend fun nearbyCandidates(s: Snapshot, why: NearbyWhy): List<NearbyPlace> {
        val route = s.route ?: return emptyList()
        val points = route.points
        val geo = RouteGeo.length(points)
        val scale = if (geo > 0) route.totalDistanceM / geo else 1.0
        val traveled = _ui.value.traveledKm
        fun ahead(lat: Double, lng: Double): Pair<Double, Double>? {
            if (points.size < 2) return null
            val p = RouteGeo.project(points, lat, lng)
            return (p.alongM * scale / 1000 - traveled) to p.offsetM / 1000
        }
        return when (why) {
            NearbyWhy.REPORT_VET -> {
                val at = here(s) ?: return emptyList()
                petPlaces.vetsNear(at, VET_LOOKUP).items
                    // 농장 동물 전문 병원은 반려견 추천에서 뺀다 (동물병원 적재 규칙과 같은 이름 규칙)
                    .filter { "산업동물" !in it.name }
                    .mapNotNull { v -> ahead(v.lat, v.lng)?.let { (a, o) -> NearbyPlace(v.id, v.name, NearbyKind.VET, v.lat, v.lng, a, o, sample = v.sample || v.partner) } }
            }
            NearbyWhy.DESTINATION -> petPlaces.near(route.destination, (NearbyPick.DEST_RADIUS_KM * 1000).toInt(), NearbyPick.PLACE_CATEGORIES).items.mapNotNull { p ->
                val kind = when (p.category) {
                    "walk" -> NearbyKind.WALK
                    "shop" -> NearbyKind.SHOP
                    else -> return@mapNotNull null
                }
                val (a, o) = ahead(p.lat, p.lng) ?: return@mapNotNull null
                NearbyPlace(p.id, p.name, kind, p.lat, p.lng, a.coerceAtLeast(0.1), o, fromDestKm = p.offKm, sample = p.sample)
            }
        }
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

    // 수집은 모든 속성을 만든 뒤에 시작한다 — 엔진 스레드가 생성자보다 먼저 onSnapshot을 부르면 아래에 선언된
    // 속성(auto·shapeMode·motionScale 등)이 아직 null/0이다(릴리스 R8 빌드 첫 실행 NPE, 2026-11-02).
    init {
        scope.launch { runtime.snapshot.collect(::onSnapshot) }
        scope.launch { runtime.replays.collect(::replay) }
        scope.launch { share.state.collect { st -> _ui.update { it.copy(share = st) } } }
        watchScope.launch { gate.state.collect { g -> _ui.update { if (it.voice == g.phase) it else it.copy(voice = g.phase) } } }
        scope.launch {
            savedPet = runCatching { petStore.profile() }.getOrNull()
            _ui.update { it.copy(profileSaved = savedPet != null) }
            savedPet?.let { p -> if (brain == null && runtime.snapshot.value.simSetup?.pet == null) _ui.update { it.copy(pet = p) } }
        }
    }

    private companion object {
        /** 0초 점프 뒤 지금 프레임을 다시 읽기 전 기다림 (시뮬레이터 seek가 끝나도록). */
        private const val REPLAY_SETTLE_MS = 300L

        const val DICTATE_MS = 10_000L
        /** 주변 추천 병원 후보를 몇 곳 받아 경로에 투영할지. */
        const val VET_LOOKUP = 20
        const val REPLAN_MS = 5 * 60_000L
        const val TOAST_MS = 8_000L
        const val RESUME_PHRASE = "이어서 안내할게요."
        const val WAIT_PARK_ONLY = "차가 P단일 때만 대기 모드를 켤 수 있어요."
        const val WAIT_OVERDUE = "대기 시간이 지났어요. 보호자 화면에도 알렸어요."
        /** 문 신호 사실 (docs/wording.md 11/02 차량 이벤트). */
        const val DOOR_OPENED = "문이 열렸어요."
        /** b) 휴게소 P단 + 운전석 문: 대기 모드는 켜지 않고 진입 방법만. */
        const val WAIT_DOOR_OFFER = "문이 열렸어요. 차에 두고 다녀오면 \"잠깐 다녀올게\"라고 말해 주세요."

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

/** "콩이가 이상해요" 정차 장소 고르기 (UPGRADE 7). 지나온 곳(지금 + 0.5km 안)은 뺀다. */
object DistressStops {
    fun pick(action: DistressAction, stops: List<RestStop>, traveledKm: Double): RestStop? {
        val ahead = stops.filter { it.distanceAlongRouteKm > traveledKm + 0.5 }.sortedBy { it.distanceAlongRouteKm }
        return when (action) {
            // 가장 가까운 안전한 정차 장소: 졸음쉼터도 된다
            DistressAction.NEAREST_STOP -> ahead.firstOrNull()
            // 쉼표 앞당김: 반려견 놀이터가 있는 휴게소 → 휴게소 → 아무 정차 장소
            DistressAction.EARLIER_REST -> ahead.firstOrNull { it.type == StopType.SERVICE_AREA && Facility.PET_ZONE in it.facilities }
                ?: ahead.firstOrNull { it.type == StopType.SERVICE_AREA } ?: ahead.firstOrNull()
            DistressAction.GENTLE_DRIVE, DistressAction.VET -> null
        }
    }
}
