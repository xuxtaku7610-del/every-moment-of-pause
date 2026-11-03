package com.emop.pause

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emop.pause.core.model.DoorState
import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSource
import com.emop.pause.core.model.TripPhase
import com.emop.pause.data.Found
import com.emop.pause.data.PetPlace
import com.emop.pause.data.PetPlaceRepository
import com.emop.pause.data.Vet
import com.emop.pause.data.net.SummaryFacts
import com.emop.pause.data.net.SummaryReq
import com.emop.pause.data.pet.PetStore
import com.emop.pause.di.Server
import com.emop.pause.engine.motion.CompareResult
import com.emop.pause.engine.motion.MotionCompare
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.pet.LimitEstimate
import com.emop.pause.engine.pet.PetTripLog
import com.emop.pause.engine.pet.PetTripRecord
import com.emop.pause.presentation.Overlay
import com.emop.pause.presentation.Route
import com.emop.pause.presentation.ScreenRouter
import com.emop.pause.presentation.UiMapper
import com.emop.pause.runtime.SignalRuntime
import com.emop.pause.runtime.Snapshot
import com.emop.pause.runtime.SourceMode
import com.emop.pause.trip.PresetStore
import com.emop.pause.trip.SettingsStore
import com.emop.pause.trip.TripCoordinator
import com.emop.pause.trip.TripRecorder
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.DemoPartner
import com.emop.pause.trip.VetMemo
import com.emop.pause.trip.ClinicIntake
import com.emop.pause.trip.ClinicReply
import com.emop.pause.uistate.IntakeUi
import com.emop.pause.trip.SymptomMemoStore
import com.emop.pause.trip.CareRepository
import com.emop.pause.engine.pet.DistressKind
import com.emop.pause.engine.pet.EmergencyKind
import com.emop.pause.engine.pet.PetDistress
import com.emop.pause.data.PlaceSource
import com.emop.pause.trip.ShapeMode
import com.emop.pause.trip.CalibrationStore
import com.emop.pause.engine.motion.CalibrationSample
import com.emop.pause.engine.motion.MotionAnswer
import com.emop.pause.engine.motion.MotionCalibration
import com.emop.pause.uistate.ActivityKind
import com.emop.pause.uistate.AppUiState
import com.emop.pause.uistate.EmergencyCardUi
import com.emop.pause.uistate.EmergencyUi
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.Scene
import com.emop.pause.uistate.Screen
import com.emop.pause.uistate.SettingsUi
import com.emop.pause.uistate.SignalRowUi
import com.emop.pause.uistate.SymptomUi
import com.emop.pause.uistate.TripLogUi
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.UxPolicy
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.HistoryUi
import com.emop.pause.uistate.MotionQuiz
import com.emop.pause.uistate.OnboardingUi
import com.emop.pause.core.model.AppMode
import com.emop.pause.data.net.EtaShareReq
import com.emop.pause.data.net.EtaUpdateReq
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** 저장한 이번 여행 기록 (이동 기록 화면). */
/** [arrivalIndex] = "내렸어요" 때 주행 자극 (기기에 따로 저장하지 않는다). */
data class SavedTrip(val id: String, val record: PetTripRecord, val series: List<Int>, val summary: String, val cautions: Int = 0, val arrivalIndex: Int? = null)

data class AppState(
    val overlay: Overlay = Overlay.NONE,
    val demoOpen: Boolean = false,
    /** 디버그 adb `--ez captureClean true`: 스토어 스크린샷 캡처 때만 "시연 데이터" 표시를 숨긴다(11/03). 앱 기본 동작은 false. */
    val captureClean: Boolean = false,
    val scene: Scene? = null,
    val restChoice: Int = 15,
    /** 프로필 편집 중인 값 (저장 전). */
    val petDraft: PetProfile? = null,
    /** 첫 실행 프로필 화면을 이번 실행에서 "나중에"로 넘겼다. */
    val profileSkipped: Boolean = false,
    val along: Found<PetPlace>? = null,
    val near: Found<PetPlace>? = null,
    val vets: Found<Vet>? = null,
    val loading: Boolean = false,
    val limit: LimitEstimate? = null,
    val saved: SavedTrip? = null,
    /** 첫 실행 온보딩 단계(1~3)와 멀미 민감도 답(0 없음 · 1 가끔 · 2 자주). */
    val onboardStep: Int = 1,
    val quiz: List<Int?> = List(MotionQuiz.questions.size) { null },
    /** 안내를 시작한 동물병원 (주행 중엔 1순위 한 곳 2줄로 보인다). */
    val goingVet: Vet? = null,
    /** 병원으로 주행을 시작했는지 — 그 뒤 멈추면 병원 안내를 끝낸다. */
    val vetDriving: Boolean = false,
    /** 도착 예정 공유 링크. */
    val etaUrl: String? = null,
    /** 병원 사전 접수 (UPGRADE 3): 접수함 링크·도착 예정·병원 응답. */
    val intake: ClinicIntake? = null,
    /** 이 차에 저장된 이전 기록 (최근 순). */
    val history: List<PetTripRecord> = emptyList(),
    /** 발표 모드 "고갯길 비교" 전체 화면. */
    val compareOpen: Boolean = false,
    /** 증상 메모 (기기에만, SymptomMemoStore). */
    val vetMemo: VetMemo = VetMemo(),
    val memoListening: Boolean = false,
    /** 이 반려견 개인 보정 기록 수·계수 (프로필 화면 문구). */
    val calibrationCount: Int = 0,
    val calibrationScale: Double = 1.0,
    /** 긴급 도움에서 고른 상황 (응급 카드 id). 안 골랐으면 null. */
    val emergency: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val runtime: SignalRuntime,
    private val trip: TripCoordinator,
    private val presets: PresetStore,
    private val settings: SettingsStore,
    private val server: Server,
    private val petStore: PetStore,
    private val places: PetPlaceRepository,
    private val memoStore: SymptomMemoStore,
    private val calibration: CalibrationStore,
    private val care: CareRepository,
) : ViewModel() {
    val snapshot: StateFlow<Snapshot> = runtime.snapshot
    val tripUi: StateFlow<TripUi> = trip.ui
    private val _app = MutableStateFlow(AppState())
    val app: StateFlow<AppState> = _app.asStateFlow()

    private val recorder = TripRecorder()
    private var lastRecordedSec = -1L
    private val logoTaps = mutableListOf<Long>()

    /** 화면 계약 (docs/ui_contract.md): 화면은 이 값만 그린다. */
    val ui: StateFlow<AppUiState> = combine(runtime.snapshot, trip.ui, _app) { s, t, a -> uiState(s, t, a) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, uiState(runtime.snapshot.value, trip.ui.value, _app.value))

    init {
        _app.update { it.copy(vetMemo = runCatching { memoStore.load() }.getOrDefault(VetMemo())) }
        viewModelScope.launch { trip.ui.collect { t -> if (t.pet.name != calibratedFor) applyCalibration(t.pet.name) } }
        trip.voiceEnabled = settings.voiceSuggest
        trip.eager = settings.timing == 0
        viewModelScope.launch { refreshLimit() }
        // 개인 보정은 음성 신고·조용한 구간에서 자동으로 (질문 없음, 재정비 원칙 3)
        viewModelScope.launch {
            trip.calibrationSamples.collect { sample ->
                val pet = trip.ui.value.pet.name
                calibration.add(pet, sample)
                applyCalibration(pet)
            }
        }
        // 발표 모드 점프: 건너뛴 구간을 엔진이 따라잡은 값(멀미·피로·정차·증상)으로 이동 기록을 다시 쌓는다 — 처음부터 달린 것과 같은 숫자.
        // 이때 휴게소 목록은 다시 받는 중일 수 있어 정차 이름은 비워 두고(빈 목록) 목록이 오면 채운다.
        viewModelScope.launch {
            trip.replayed.collect { ticks ->
                recorder.reset()
                ticks.forEach { (s, t) ->
                    recorder.onMotion(t.motion.index, t.motion.drivingSec, t.motion.firstCautionDrivingSec)
                    recorder.tick(s.clockMinute, s.mode, s.pauseCause, t.fatigue?.score, t.fatigue?.continuousDriveMinutes, t.traveledKm, emptyList())
                }
                lastRecordedSec = ticks.lastOrNull()?.s?.nowMs?.div(1000) ?: -1L
            }
        }
        viewModelScope.launch {
            runtime.snapshot.collect { s ->
                if (s.trip == TripPhase.NOT_STARTED) recorder.reset()
                if (_app.value.demoOpen) computeCompare(s, trip.ui.value.pet)
                val sec = s.nowMs / 1000
                if (sec != lastRecordedSec) {
                    lastRecordedSec = sec
                    val t = trip.ui.value
                    recorder.onMotion(t.motion.index, t.motion.drivingSec, t.motion.firstCautionDrivingSec)
                    recorder.tick(s.clockMinute, s.mode, s.pauseCause, t.fatigue?.score, t.fatigue?.continuousDriveMinutes, t.traveledKm, t.stops)
                }
                // 주행이 시작되면 위로 연 화면을 닫는다 (DS-01).
                if (s.driveState.forcesDrivingUi && _app.value.overlay != Overlay.NONE) _app.update { it.copy(overlay = Overlay.NONE, petDraft = null) }
                if (s.driveState.forcesDrivingUi && _app.value.compareOpen) _app.update { it.copy(compareOpen = false) }
                // 병원 안내: 주행을 시작했으면 표시, 그 뒤 멈추면(도착·정차) 끝낸다.
                val a = _app.value
                if (a.goingVet != null) {
                    if (s.mode == AppMode.DRIVING && !a.vetDriving) _app.update { it.copy(vetDriving = true) }
                    else if (s.mode != AppMode.DRIVING && a.vetDriving) _app.update { it.copy(goingVet = null, vetDriving = false) }
                    // 새 여행(출발 전)이면 지난 병원 안내는 끝낸다 (긴급 도움 "경로 안내" 뒤 출발하지 않고 새 여행을 시작한 경우, 11/03)
                    else if (s.mode == AppMode.PREPARE) _app.update { it.copy(goingVet = null, vetDriving = false) }
                }
                tickIntake(s)
            }
        }
    }

    // ───────────── 화면 상태 ─────────────

    private fun uiState(s: Snapshot, t: TripUi, a: AppState): AppUiState {
        val simulation = s.source == SourceMode.SIMULATION
        val ux = s.uxLimits.let { UxPolicy.Flags(it.noVideo, it.limitStringLength, it.maxStringLength, it.noKeyboard, it.noSetup) }
        // 시뮬레이션 시나리오가 반려견을 정해 주면 첫 실행 프로필을 묻지 않는다.
        val needsProfile = t.profileSaved == false && !a.profileSkipped && s.simSetup?.pet == null
        val route = ScreenRouter.route(
            ScreenRouter.Input(
                s.driveState, s.mode, simulation,
                sceneLocked = a.scene != null,
                overlay = a.overlay,
                needsProfile = needsProfile,
                waiting = t.wait.active,
                leftConfirmed = t.leftConfirmed,
                setupAllowed = UxPolicy.setupAllowed(ux),
            ),
        )
        val screen = when (route) {
            Route.SCENE -> Fixtures.screen(a.scene!!)
            Route.DRIVE -> Screen.Drive(UxPolicy.apply(withVet(UiMapper.drive(s, t), a), ux))
            Route.DEPART -> Screen.Depart(UiMapper.depart(s, t, a.limit?.let { PetTripLog.limitPhrase(t.pet.name, it) }, t.profileSaved == true || s.simSetup?.pet != null))
            Route.REST -> Screen.Rest(
                UiMapper.rest(s, t, a.restChoice, recorder.currentStop, t.wait.limitSec / 60)
                    .copy(care = t.careId?.let { care.ui(it, showDrafts(s, a)) }, distressChips = DISTRESS_CHIPS.map { it.first }),
            )
            Route.WAIT -> Screen.Wait(UiMapper.wait(t.wait, t, acOn = s.sample.hvacAcOn))
            Route.PLACES -> Screen.Places(UiMapper.places(a.along, a.near, a.loading))
            Route.VETS -> Screen.Vets(
                UiMapper.vets(a.vets, a.loading, a.vetMemo, a.etaUrl, a.memoListening, todayLine(a)).copy(
                    intake = a.intake?.let(::intakeUi),
                    preview = (a.goingVet ?: a.vets?.items?.firstOrNull())?.takeIf { it.partner }
                        ?.let { v -> UiMapper.vetPreview(t.pet.name, a.vetMemo, todayLine(a), UiMapper.vetMinutes(v.distanceKm)) }.orEmpty(),
                ),
            )
            Route.EMERGENCY -> Screen.Emergency(
                EmergencyUi(
                    EmergencyUi.OPTIONS,
                    selected = a.emergency,
                    card = a.emergency?.let { care.emergency(it, showDrafts(s, a)) },
                    vet = UiMapper.vets(a.vets, a.loading).items.firstOrNull(),
                    vetLoading = a.loading,
                ),
            )
            Route.ARRIVAL -> Screen.Arrival(UiMapper.arrival(t))
            Route.TRIPLOG -> Screen.TripLog(tripLog(s, t, a))
            Route.STIMULUS -> Screen.Stimulus(UiMapper.stimulus(t, recorder.log().motionPeak.takeIf { t.motion.drivingSec > 0 }))
            Route.PET -> Screen.PetProfile(UiMapper.petEdit(a.petDraft ?: t.pet, firstRun = false).copy(calibration = calibrationLine(t.pet.name, a)))
            Route.SETTINGS -> Screen.Settings(settingsUi(s))
            Route.ONBOARDING -> Screen.Onboarding(
                OnboardingUi(a.onboardStep, UiMapper.petEdit(a.petDraft ?: t.pet, firstRun = true), signalRows(s), MotionQuiz.ui(a.quiz), MotionQuiz.result(a.quiz)),
            )
        }
        val motion = when {
            route != Route.SCENE -> UxPolicy.motionAllowed(ScreenRouter.motionAllowed(s.mode, s.driveState, settings.reduceMotion), ux)
            else -> false
        }
        return AppUiState(
            screen = screen,
            drivingLocked = s.driveState.forcesDrivingUi,
            motionAllowed = motion,
            demoOpen = ScreenRouter.demoVisible(a.demoOpen, simulation, s.driveState),
            scene = a.scene,
            state = ScreenRouter.serviceState(route, (screen as? Screen.Drive)?.ui?.content, a.goingVet != null),
            // 시뮬레이터 값·발표 장면 고정은 화면에 "시연 데이터"를 붙인다 (재정비 원칙 5)
            demoData = (simulation || route == Route.SCENE) && !a.captureClean,
        )
    }

    /** 검수 전 돌봄·응급 카드 본문을 보여 줄지: 디버그 빌드, 발표 모드(패널), 시뮬레이터 시연만. 릴리스 실차는 검수된 카드만. */
    private fun showDrafts(s: Snapshot, a: AppState): Boolean =
        EmergencyCardUi.showDrafts(BuildConfig.DEBUG, a.demoOpen || s.source == SourceMode.SIMULATION)

    /** 화면 → ViewModel 이벤트 하나로 받는다. 마이크 권한 창은 Activity가 먼저 띄운다. */
    fun onEvent(e: UiEvent) {
        when (e) {
            UiEvent.LogoTap -> onLogoTap()
            UiEvent.AcceptSuggestion -> reply("응")
            UiEvent.DeclineSuggestion -> reply("나중에")
            is UiEvent.ToggleCheck -> trip.toggleCheck(UiMapper.checkItem(e.kind))
            UiEvent.StartTrip -> startTrip()
            UiEvent.OpenPet -> _app.update { it.copy(overlay = Overlay.PET, petDraft = trip.ui.value.pet) }
            UiEvent.OpenSettings -> _app.update { it.copy(overlay = Overlay.SETTINGS) }
            UiEvent.OpenPlaces -> openPlaces()
            UiEvent.OpenVets -> openVets()
            UiEvent.Close -> _app.update { it.copy(overlay = Overlay.NONE, petDraft = null, profileSkipped = true, emergency = null) }
            UiEvent.OpenEmergency -> openEmergency(null)
            is UiEvent.PickEmergency -> openEmergency(e.id)
            is UiEvent.StartActivity -> if (e.kind == ActivityKind.PET_WALK) recorder.onPetWalk()
            is UiEvent.SetRestMinutes -> _app.update { it.copy(restChoice = e.minutes) }
            UiEvent.StartWait -> trip.startWait()
            UiEvent.StopWait -> trip.stopWait()
            is UiEvent.SetWaitLimit -> trip.setWaitLimit(e.minutes)
            is UiEvent.GoPlace -> goPlace(e.id)
            is UiEvent.GoVet -> goVet(e.id)
            UiEvent.ConfirmLeft -> confirmLeft()
            UiEvent.FinishTrip -> finishTrip()
            UiEvent.OpenStimulus -> _app.update { it.copy(overlay = Overlay.STIMULUS) }
            UiEvent.ShareReport -> openVets()
            UiEvent.ResetCalibration -> {
                calibration.reset(trip.ui.value.pet.name)
                applyCalibration(trip.ui.value.pet.name)
            }
            is UiEvent.SetPetName -> editPet { it.copy(name = e.name.take(PetProfile.NAME_MAX)) }
            is UiEvent.SetPetSize -> editPet { it.copy(size = PetSize.valueOf(e.size.name)) }
            is UiEvent.SetPetAge -> editPet { it.copy(ageYears = e.years.coerceIn(0, PetProfile.AGE_MAX)) }
            is UiEvent.SetPetTendency -> editPet { it.copy(motionTendency = MotionTendency.valueOf(e.tendency.name)) }
            is UiEvent.SetPotty -> editPet { it.copy(pottyIntervalMin = e.minutes) }
            UiEvent.TogglePetZone -> editPet { p -> p.copy(facilityPrefs = if ("pet_zone" in p.facilityPrefs) p.facilityPrefs - "pet_zone" else p.facilityPrefs + "pet_zone") }
            UiEvent.SavePet -> savePet()
            UiEvent.OnboardNext -> onboardNext()
            UiEvent.OnboardBack -> _app.update { it.copy(onboardStep = (it.onboardStep - 1).coerceAtLeast(1)) }
            is UiEvent.AnswerQuiz -> _app.update { a -> a.copy(quiz = a.quiz.mapIndexed { i, v -> if (i == e.question) e.option else v }) }
            is UiEvent.ShareVetEta -> shareVetEta(e.id)
            UiEvent.AcceptDistress -> acceptDistress()
            UiEvent.DismissDistress -> trip.dismissDistress()
            is UiEvent.ReportDistress -> DISTRESS_CHIPS.firstOrNull { it.first == e.label }?.let { trip.reportDistress(it.second) }
            is UiEvent.ToggleMemoChip -> editMemo { it.toggle(e.label) }
            is UiEvent.SetMemoText -> editMemo { it.withText(e.text) }
            UiEvent.ToggleMemoInclude -> editMemo { it.copy(include = !it.include) }
            UiEvent.StartMemoVoice -> startMemoVoice()
            is UiEvent.SetTiming -> { settings.timing = e.value; trip.eager = e.value == 0; trip.refreshPreview() }
            UiEvent.ToggleVoice -> { settings.voiceSuggest = !settings.voiceSuggest; trip.voiceEnabled = settings.voiceSuggest; bump() }
            UiEvent.ToggleReduceMotion -> { settings.reduceMotion = !settings.reduceMotion; bump() }
            UiEvent.DeleteData -> deleteData()
        }
    }

    private fun bump() = _app.update { it.copy() }

    // ───────────── 공급원 ─────────────
    fun scenarioNames() = runtime.scenarioNames()
    fun useScenario(name: String) = runtime.useScenario(name)
    fun useVehicle(fillScenario: String? = null) = runtime.useVehicle(fillScenario)
    fun onVehiclePermissionGranted() = runtime.refreshVehicleProperties()
    fun play() = runtime.play()
    fun pause() = runtime.pause()
    fun setSpeed(multiplier: Int) = runtime.setSpeed(multiplier)
    fun seek(tSec: Double) = runtime.seek(tSec)
    /** 들은 말. 정차 중 응급 말("콩이 초콜릿 먹었어")이면 긴급 도움 카드도 같이 연다 (기록·음성은 기존 흐름 그대로). */
    fun reply(text: String) {
        val s = snapshot.value
        if (!s.driveState.forcesDrivingUi && PetDistress.classify(text) == DistressKind.EMERGENCY) {
            openEmergency(PetDistress.emergencyKind(text)?.id)
        }
        trip.reply(text)
    }

    /** 디버그 adb `--es emergency toxin|breathing|seizure|bleeding|menu|none` (시연 재현). */
    fun adbEmergency(v: String) {
        when (v.lowercase()) {
            "none" -> _app.update { it.copy(overlay = Overlay.NONE, emergency = null) }
            "menu" -> openEmergency(null)
            else -> openEmergency(EmergencyKind.of(v)?.id)
        }
    }

    // ───────────── 발표 모드 ─────────────
    /** 로고 5번 탭(3초 안) → 숨은 발표 모드 패널. 릴리스에도 있다. */
    fun onLogoTap(nowMs: Long = System.currentTimeMillis()) {
        logoTaps.add(nowMs)
        logoTaps.removeAll { nowMs - it > 3_000 }
        if (logoTaps.size >= 5) {
            logoTaps.clear()
            _app.update { it.copy(demoOpen = !it.demoOpen) }
        }
    }
    fun setDemoOpen(open: Boolean) = _app.update { it.copy(demoOpen = open) }
    /** 디버그 전용 (Activity가 BuildConfig.DEBUG일 때만 부른다): 캡처용으로 "시연 데이터" 표시 숨김. */
    fun setCaptureClean(on: Boolean) = _app.update { it.copy(captureClean = on) }
    fun setCompareOpen(open: Boolean) = _app.update { it.copy(compareOpen = open) }
    /** 발표 장면 실내 온도 (시뮬레이터만, UPGRADE 7). */
    fun setDemoCabin(c: Float?) {
        trip.demoCabinTempC = c
    }

    /** 발표용 문 상태 (디버그 adb `--es door`): rear·driver·both = 그 문 열림, closed = 모두 닫힘, none = 시나리오 값으로. */
    fun setDemoDoors(name: String) {
        trip.demoDoors = when (name) {
            "rear" -> DoorState.CLOSED.copy(rearRightOpen = true)
            "driver" -> DoorState.CLOSED.copy(driverOpen = true)
            "both" -> DoorState(driverOpen = true, rearLeftOpen = false, rearRightOpen = true)
            "closed" -> DoorState.CLOSED
            else -> null
        }
    }

    /**
     * 발표 장면 "쓸수록 맞춰짐" (디버그 adb `--es calib yes,yes,little`): 지난 여행 표본을 미리 넣는다(주행 자극 최고 46과 짝).
     * `reset`이면 모두 지운다. 실제 사용에서는 음성 신고·조용한 구간에서 자동으로 쌓인다(AutoCalibration).
     */
    fun seedCalibration(spec: String) {
        val pet = trip.ui.value.pet.name
        // 출발 전에는 시나리오 반려견 이름이 아직 안 들어와 있을 수 있어 "reset"은 모든 반려견 기록을 지운다
        if (spec == "reset") calibration.clear() else calibration.reset(pet)
        if (spec != "reset") spec.split(',').mapNotNull { a -> runCatching { MotionAnswer.valueOf(a.trim().uppercase()) }.getOrNull() }
            .forEach { calibration.add(pet, CalibrationSample(DEMO_PEAK, it)) }
        applyCalibration(pet)
    }

    /** 발표 모드: 앞 구간 형상 시연 경로 ↔ 회전 안내 (UPGRADE 5). */
    fun setGuidanceMode(on: Boolean) = trip.setShapeMode(if (on) ShapeMode.GUIDANCE else ShapeMode.DEMO_GPX)
    fun setScene(scene: Scene?) = _app.update { it.copy(scene = scene) }
    fun setShareBase(base: String) { trip.share.shareBase = base }

    /** 시연 장면으로 점프: 시나리오 시각으로 이동하고 재생한다. */
    fun jump(tSec: Int) {
        _app.update { it.copy(scene = null, overlay = Overlay.NONE) }
        seek(tSec.toDouble())
        play()
    }

    /** 발표 모드 "고갯길 비교" (F1-07). 경로 형상이 있는 시나리오에서 한 번만 계산한다. */
    private val _compare = MutableStateFlow<CompareResult?>(null)
    val compare: StateFlow<CompareResult?> = _compare.asStateFlow()
    private var compareFor: String? = null

    private fun computeCompare(s: Snapshot, pet: PetProfile) {
        val pts = s.route?.shape?.takeIf { it.size >= 2 } ?: return
        val key = "${s.scenarioName}|$pet"
        if (key == compareFor) return
        compareFor = key
        viewModelScope.launch(Dispatchers.Default) {
            _compare.value = RouteShape.fromLatLng(pts)?.let { MotionCompare.run(it, pet) }
        }
    }

    /** 디버그 adb `--es onboard done`: 첫 실행 프로필 화면을 넘긴다. */
    fun skipProfile() = _app.update { it.copy(profileSkipped = true) }

    // ───────────── 출발 체크·프로필 ─────────────

    /** 실차 모드의 출발 버튼. 시뮬레이터에서는 시나리오가 여행 단계를 정하므로 재생을 시작한다. 체크를 다 해야 출발 (F6-01). */
    fun startTrip() {
        val t = trip.ui.value
        if (snapshot.value.source != SourceMode.SIMULATION && !UiMapper.depart(snapshot.value, t, null, true).ready) return
        if (snapshot.value.source == SourceMode.SIMULATION) play() else runtime.setVehicleTrip(TripPhase.ACTIVE)
    }

    /** 온보딩 다음: 1 프로필(검사 통과) → 2 차량 연동 → 3 멀미 민감도(답 3개) → 결과를 성향으로 저장. */
    private fun onboardNext() {
        val a = _app.value
        val draft = a.petDraft ?: trip.ui.value.pet
        when (a.onboardStep) {
            1 -> if (draft.problems().isEmpty()) _app.update { it.copy(onboardStep = 2, petDraft = draft) }
            2 -> _app.update { it.copy(onboardStep = 3) }
            else -> {
                val r = MotionQuiz.result(a.quiz) ?: return
                _app.update { it.copy(petDraft = draft.copy(motionTendency = MotionTendency.valueOf(r.name))) }
                savePet()
                _app.update { it.copy(onboardStep = 1, quiz = List(MotionQuiz.questions.size) { null }) }
            }
        }
    }

    private fun editPet(f: (PetProfile) -> PetProfile) = _app.update { a -> a.copy(petDraft = f(a.petDraft ?: trip.ui.value.pet)) }

    private fun savePet() {
        val draft = _app.value.petDraft ?: trip.ui.value.pet
        if (draft.problems().isNotEmpty()) return
        trip.savePet(draft.copy(name = draft.name.trim()))
        _app.update { it.copy(overlay = Overlay.NONE, petDraft = null, profileSkipped = true) }
        viewModelScope.launch { refreshLimit() }
    }

    private suspend fun refreshLimit() {
        val trips = runCatching { petStore.recentTrips() }.getOrDefault(emptyList())
        _app.update { it.copy(limit = PetTripLog.limit(trips), history = trips.sortedByDescending { r -> r.startedMs }) }
    }

    // ───────────── 장소·병원 (F4·F5) ─────────────

    /** 지금 위치: 실차는 NaviHelper 현재 위치(경로 첫 점), 시뮬레이터는 진행 비율로 경로 위 지점을 근사한다. */
    private fun here(s: Snapshot): LatLng? {
        val r = s.route ?: return null
        if (s.source == SourceMode.VEHICLE) return r.points.firstOrNull()
        val frac = s.eta?.let { 1.0 - it.remainDistanceM.toDouble() / r.totalDistanceM.coerceAtLeast(1) } ?: 0.0
        return PetPlaceRepository.pointAlong(r.points, frac)
    }

    private fun openPlaces() {
        val s = snapshot.value
        _app.update { it.copy(overlay = Overlay.PLACES, loading = true) }
        viewModelScope.launch {
            val r = s.route
            val along = r?.let { places.alongRoute(it.points, it.totalDistanceM.toDouble()) }
            val near = r?.let { places.near(it.destination) }
            _app.update { it.copy(along = along, near = near, loading = false) }
        }
    }

    private fun openVets() {
        _app.update { it.copy(overlay = Overlay.VETS) }
        loadVets()
    }

    /**
     * 긴급 도움 (정차 중만 — 주행 중이면 화면 선택이 주행 화면을 먼저 고른다). [id]가 있으면 그 응급 카드.
     * 가장 가까운 병원 요약을 바로 보이도록 병원 목록을 함께 불러온다(이미 있으면 그대로).
     */
    private fun openEmergency(id: String?) {
        if (snapshot.value.driveState.forcesDrivingUi) return
        val fresh = _app.value.overlay != Overlay.EMERGENCY
        _app.update { it.copy(overlay = Overlay.EMERGENCY, emergency = id ?: it.emergency.takeIf { _ -> !fresh }) }
        // 처음 열 때만 지금 위치로 병원 목록을 새로 (고르기만 바꿀 때는 그대로 — 화면이 깜빡이지 않게)
        if (fresh || _app.value.vets == null) loadVets()
    }

    private fun loadVets() {
        val at = here(snapshot.value) ?: snapshot.value.route?.destination
        _app.update { it.copy(loading = true) }
        viewModelScope.launch {
            val found = at?.let { places.vetsNear(it) }?.let { f -> f.copy(items = DemoPartner.prepend(f.items, at, snapshot.value.source == SourceMode.SIMULATION)) }
            _app.update { it.copy(vets = found, loading = false) }
        }
    }

    /** 장소를 경유지로 (NaviHelper addWaypoint, sdk_check.md). */
    private fun goPlace(id: String) {
        val a = _app.value
        val p = (a.near?.items.orEmpty() + a.along?.items.orEmpty()).firstOrNull { it.id == id } ?: return
        addWaypoint(p.lat, p.lng, p.name)
    }

    /**
     * 동물병원 안내 (F5). 목적지 바꾸기(`requestRoute`)는 에뮬레이터에서 확인 전이라 경유지로 넣는다 — TODO(pleos-sdk).
     * 가는 동안 흔들림 안내는 멀미 엔진이 그대로 한다.
     */
    private fun goVet(id: String) {
        val v = _app.value.vets?.items?.firstOrNull { it.id == id } ?: return
        _app.update { it.copy(goingVet = v, vetDriving = false) }
        addWaypoint(v.lat, v.lng, v.name)
    }

    /** 주행 화면: 병원 안내 중이면 안내 한 줄 대신 1순위 병원 2줄 (제안·예보가 있으면 그것이 먼저). */
    private fun withVet(d: DriveUi, a: AppState): DriveUi {
        val v = a.goingVet ?: return d
        if (d.content !is DriveContent.Calm && d.content !is DriveContent.Heading) return d
        val i = a.intake?.takeIf { it.vetName == v.name }
        val vet = UiMapper.vetDrive(v.name, v.distanceKm).let {
            when {
                i != null -> it.copy(minutes = i.etaMin, clinic = i.reply?.drive ?: if (i.seen) "병원이 접수함을 열어 봤어요" else "병원 사전 접수 보냄")
                v.partner -> it.copy(clinic = "정차하면 보낼 내용을 확인해요")
                // 제휴가 없는 병원: 접수 대신 전화 (주행 중엔 번호만, 거는 것은 정차 후)
                else -> it.copy(clinic = v.phone?.let { p -> "정차 후 전화 $p" } ?: "접수 연동 없음 · 정차 후 전화")
            }
        }
        return d.copy(content = vet, nextStop = null, nextStopName = null, nextStopMin = null, meta = "동물병원까지 ${vet.minutes}분")
    }

    /**
     * 동물병원 사전 접수 (UPGRADE 3, V5 §5 도착 예정 공유를 확장): 보호자용 도착 예정 링크 + 병원 접수함 링크를 함께 만든다.
     * 둘 다 보호자가 직접 전달한다(앱·서버가 병원에 보내지 않는다). 위치는 보내지 않고 반려견 이름·병원·도착 예정 분,
     * 체크했을 때만 증상·메모·오늘 요약. 가는 동안 도착 예정 분을 갱신하고 병원 응답을 받는다([tickIntake]). 서버가 없으면 만들지 않는다.
     */
    private fun shareVetEta(id: String) {
        val a = _app.value
        val v = a.vets?.items?.firstOrNull { it.id == id } ?: a.goingVet?.takeIf { it.id == id } ?: return
        val session = server.session
        if (session == null) {
            trip.notify("서버 연결이 없어 공유 링크를 만들지 못했어요.")
            return
        }
        viewModelScope.launch {
            // 증상 메모·오늘 요약은 체크했을 때만 (기기 밖으로 나가는 유일한 길)
            val shared = VetMemo.share(a.vetMemo, todayLine(a))
            val minutes = UiMapper.vetMinutes(v.distanceKm)
            // 사전 접수(병원 접수함)는 제휴 병원만. 그 밖의 병원은 도착 예정 링크만 만든다(접수 완료를 보이지 않는다)
            val req = EtaShareReq(trip.ui.value.pet.name, v.name, minutes, shared.symptoms, shared.memo, shared.tripLine, intake = v.partner)
            val res = runCatching { session.call { etaShare(req) } }.getOrNull()
            if (res == null) {
                trip.notify("사전 접수 링크를 만들지 못했어요. 병원에 전화해 주세요.")
                return@launch
            }
            val base = trip.share.shareBase.trimEnd('/').takeIf { it.isNotBlank() }
            val url = base?.let { it + res.pageUrl } ?: res.pageUrl
            val clinic = res.clinicUrl?.let { c -> base?.let { it + c } ?: c }
            val intake = clinic?.let { ClinicIntake(res.token, it, url, v.name, minutes, minutes, System.currentTimeMillis(), snapshot.value.clockMinute) }
            _app.update { it.copy(etaUrl = url, intake = intake) }
            if (BuildConfig.DEBUG) Log.i("Intake", "clinic=${res.clinicUrl} share=${res.pageUrl}")
            trip.notify(if (intake != null) "병원 접수함 링크를 만들었어요. 병원에 보내 주세요." else "도착 예정 공유 링크를 만들었어요. 보호자 폰으로 보내 주세요.")
        }
    }

    private var intakeBusy = false
    private var intakePolledMs = 0L

    /**
     * 사전 접수 갱신 (F5-08·09): 도착 예정이 바뀌면 보내고(응답에 병원 응답이 실려 온다), 아니면 15초마다 응답만 묻는다.
     * 병원 응답이 새로 오면 화면 한 줄 + 같은 문장 음성. 실패는 조용히 다음 차례에 다시.
     */
    private fun tickIntake(s: Snapshot) {
        val i = _app.value.intake ?: return
        if (i.arrived || intakeBusy) return
        val session = server.session ?: return
        val vet = _app.value.vets?.items?.firstOrNull { it.name == i.vetName } ?: _app.value.goingVet
        val distKm = if (s.source == SourceMode.VEHICLE) vet?.let { v -> here(s)?.let { h -> PetPlaceRepository.distanceM(h, LatLng(v.lat, v.lng)) / 1000.0 } } else null
        val eta = distKm?.let { UiMapper.vetMinutes(it) } ?: ClinicIntake.simEta(i, s.clockMinute)
        val arrived = (distKm != null && distKm < ClinicIntake.ARRIVE_KM) || (distKm == null && eta == 0)
        val now = System.currentTimeMillis()
        val push = arrived || ClinicIntake.shouldPush(i, eta, now)
        if (!push && now - intakePolledMs < ClinicIntake.POLL_MS) {
            if (eta != i.etaMin) _app.update { it.copy(intake = it.intake?.copy(etaMin = eta)) }
            return
        }
        intakeBusy = true
        intakePolledMs = now
        viewModelScope.launch {
            val st = runCatching { session.call { if (push) etaUpdate(i.token, EtaUpdateReq(eta, arrived)) else etaStatus(i.token) } }.getOrNull()
            intakeBusy = false
            val cur = _app.value.intake?.takeIf { it.token == i.token } ?: return@launch
            val next = cur.copy(
                etaMin = if (arrived) 0 else eta,
                sentEtaMin = if (push && st != null) eta else cur.sentEtaMin,
                sentAtMs = if (push && st != null) now else cur.sentAtMs,
                reply = ClinicReply.of(st?.reply) ?: cur.reply,
                seen = st?.seen ?: cur.seen,
                arrived = cur.arrived || (arrived && st != null),
            )
            _app.update { it.copy(intake = next) }
            if (next.reply != null && next.reply != cur.reply) trip.announce(next.reply.drive)
        }
    }

    private fun intakeUi(i: ClinicIntake) = IntakeUi(
        vetName = i.vetName,
        clinicUrl = i.clinicUrl,
        status = ClinicIntake.status(i),
        etaMinutes = i.etaMin,
        replied = i.reply != null,
        busy = i.reply == ClinicReply.BUSY,
        arrived = i.arrived,
    )

    private fun addWaypoint(lat: Double, lng: Double, name: String) {
        viewModelScope.launch {
            val ok = runtime.naviSource?.addWaypoint(lat, lng, name)?.isSuccess == true
            trip.notify(if (ok) "${name}, 경로에 추가했어요." else "경로에 넣지 못했어요. 내비에서 직접 골라 주세요.")
            _app.update { it.copy(overlay = Overlay.NONE) }
        }
    }

    // ───────────── 도착·이동 기록 (F6·F7) ─────────────

    /** "내렸어요": 이번 여행을 기기에 저장하고(서버로 보내지 않음) 요약 문장을 만든다. */
    private fun confirmLeft() {
        trip.confirmLeft()
        val log = recorder.log()
        val record = PetTripRecord(System.currentTimeMillis(), log.drivingMin, log.firstCautionMin, log.motionPeak, log.visits.size)
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            runCatching { petStore.saveTrip(id, record, System.currentTimeMillis(), log.motionSeries.mapIndexed { i, v -> i * TripRecorder.SAMPLE_MIN to v }) }
            refreshLimit()
            val summary = summarize(record, _app.value.limit)
            _app.update { it.copy(saved = SavedTrip(id, record, log.motionSeries, summary, log.cautionCount, log.motionNow.takeIf { log.motionSeries.isNotEmpty() })) }
        }
    }

    // ───────────── 개인 맞춤 보정 (UPGRADE 4) ─────────────

    private var calibratedFor: String? = null

    private fun applyCalibration(pet: String) {
        calibratedFor = pet
        val samples = runCatching { calibration.load(pet) }.getOrDefault(emptyList())
        val scale = MotionCalibration.scale(samples)
        trip.motionScale = scale
        _app.update { it.copy(calibrationCount = samples.size, calibrationScale = scale) }
    }

    private fun calibrationLine(pet: String, a: AppState): String? {
        if (a.calibrationCount == 0) return null
        val base = "보통"
        val now = MotionCalibration.label(a.calibrationScale)
        val shift = if (a.calibrationCount < MotionCalibration.MIN_SAMPLES) "${MotionCalibration.MIN_SAMPLES}회부터 반영" else "민감도 $base → $now"
        return "$pet 맞춤 정도: 기록 ${a.calibrationCount}회, $shift"
    }

    /** 기록 요약 (F7-04): 서버 LLM 2.5초, 실패하면 템플릿. 반려견 이름·증상은 보내지 않는다. */
    private suspend fun summarize(r: PetTripRecord, limit: LimitEstimate?): String {
        val template = "이번 주행은 ${UiMapper.duration(r.drivingMin)}(휴식 제외), 휴식 ${r.stops}번, 주행 자극 최고 ${r.motionPeak}였어요." +
            (limit?.let { " 지난 여행들은 출발 약 ${UiMapper.duration(it.minutes)} 뒤 주행 자극이 보통 이상이 됐어요." } ?: "")
        val session = server.session ?: return template
        val text = withTimeoutOrNull(2_500) {
            runCatching { session.call { summary(SummaryReq(SummaryFacts(r.drivingMin, r.stops, r.motionPeak, limit?.minutes))) }.text }.getOrNull()
        }
        return text?.takeIf { it.isNotBlank() } ?: template
    }

    /** 도착 후 기록 리포트 (상태 8): 타임라인·주행 자극 그래프·병원 공유. 값은 기록기·신고만, 없는 줄은 뺀다. */
    private fun tripLog(s: Snapshot, t: TripUi, a: AppState): TripLogUi {
        val saved = a.saved
        val log = recorder.log()
        val record = saved?.record ?: PetTripRecord(0, log.drivingMin, log.firstCautionMin, log.motionPeak, log.visits.size)
        val today = LocalDate.now()
        val dest = s.route?.destinationName
        // 리포트는 한 시계(출발 뒤, 정차 포함)로 말한다 — 타임라인·한 줄·병원 공유가 같은 시각을 쓰게
        val stops = log.startMinute?.let { st -> log.visits.map { (it.arrivedMinute - st) to it.stayedMin } }.orEmpty()
        val onClock = t.distressEvents.map { it.copy(drivingMin = UiMapper.elapsedMin(it.drivingMin, stops)) }
        return TripLogUi(
            meta = "${today.monthValue}월 ${today.dayOfMonth}일, ${dest ?: "목적지"}까지",
            petName = t.pet.name,
            totalMin = log.elapsedMin,
            distance = log.distanceKm.takeIf { it >= 1 }?.let { UiMapper.km(it) },
            stops = record.stops,
            reports = t.distressEvents.size,
            timeline = UiMapper.timeline(log, t.distressEvents, dest, a.goingVet?.name ?: a.intake?.vetName),
            // 기록이 없으면 빈 그대로 (그래프가 "기록이 아직 없어요"를 그린다 — 0으로 채우지 않는다)
            stimulusSeries = saved?.series ?: log.motionSeries,
            marks = UiMapper.trendMarks(log.startMinute, log.visits, t.distressEvents, log.segments),
            insight = PetTripLog.insight(t.pet.name, record.copy(firstCautionMin = UiMapper.firstStimulusMin(log)), a.limit),
            summary = saved?.summary ?: "기록을 정리하는 중이에요.",
            share = UiMapper.reportShare(record, onClock, log.distanceKm),
            history = a.history.filter { it.startedMs != record.startedMs }.take(3).map { r ->
                val d = java.time.Instant.ofEpochMilli(r.startedMs).atZone(java.time.ZoneId.systemDefault())
                HistoryUi("${d.monthValue}월 ${d.dayOfMonth}일", "${UiMapper.duration(r.drivingMin)} · " + (r.symptomNote?.let { "신고: $it" } ?: "신고 없음"))
            },
            calibration = calibrationLine(t.pet.name, a),
        )
    }

    private fun editMemo(f: (VetMemo) -> VetMemo) {
        val m = f(_app.value.vetMemo)
        _app.update { it.copy(vetMemo = m) }
        runCatching { memoStore.save(m) }
    }

    /** 음성 메모 한 번 받아쓰기 → 화면에 그대로(고칠 수 있음). */
    private fun startMemoVoice() {
        if (_app.value.memoListening) return
        _app.update { it.copy(memoListening = true) }
        viewModelScope.launch {
            val heard = trip.dictate()
            _app.update { it.copy(memoListening = false) }
            if (heard != null) editMemo { it.append(heard) } else trip.notify("잘 못 들었어요. 버튼으로 골라 주세요.")
        }
    }

    /**
     * "콩이가 이상해요" 수락 (UPGRADE 7). 정차 장소는 경유지로(TripCoordinator), 응급(병원)이면 병원 안내 + 증상을 메모에 넣고
     * 병원 사전 접수(UPGRADE 3)로 넘어간다 — 접수함 링크·도착 예정 자동 갱신(서버가 있을 때, 보호자가 직접 전달).
     */
    private fun acceptDistress() {
        val d = trip.acceptDistress() ?: return
        val vet = d.vet ?: return
        _app.update { it.copy(vets = Found(listOf(vet), PlaceSource.SERVER), goingVet = vet, vetDriving = false) }
        // 증상 고르기 칸에 맞는 칩("축 처짐·호흡 곤란" → "축 처짐", 없으면 "기타") + 메모에 원래 이름 (병원 접수함에 둘 다 보인다)
        val label = PetDistress.label(d.kind)
        val chip = VetMemo.CHIPS.firstOrNull { label.startsWith(it) } ?: "기타"
        editMemo { m -> m.copy(chips = m.chips + chip, include = true).let { if (label in it.text) it else it.append(label) } }
        addWaypoint(vet.lat, vet.lng, vet.name)
        // 전달 동의 (상태 7): 주행 중이라 음성으로 묻는다. "네"일 때만 보낸다. 제휴가 없는 병원은 접수하지 않고 전화 안내.
        if (!vet.partner) {
            trip.notify("${vet.name}은 사전 접수 연동이 없어요. 정차 후 전화해 주세요.")
            return
        }
        viewModelScope.launch {
            val heard = trip.ask("${vet.name}에 증상과 도착 예정을 보낼까요? 네 또는 아니요로 말해 주세요.")
            if (DemoPartner.consent(heard) == true) shareVetEta(vet.id)
            else trip.notify("보내지 않았어요. 정차하면 동물병원 화면에서 내용을 보고 보낼 수 있어요.")
        }
    }

    /** 디버그 adb `--es memo "구토 두 번"`: 에뮬레이터에 마이크가 없어서 받아쓰기 결과를 대신 넣는다. */
    fun heardMemo(text: String) = editMemo { it.append(text) }

    /** 오늘 이동 한 줄 (공유에 붙는다). */
    private fun todayLine(a: AppState): String? {
        val log = recorder.log()
        return VetMemo.tripLine(log.drivingMin, log.cautionCount, log.motionPeak, trip.ui.value.distressEvents.map { UiMapper.symptomName(it.label) })
    }

    /** 기록 확인 → 증상 메모를 함께 저장하고 새 여행 준비. 시뮬레이션은 처음으로 되감는다. */
    private fun finishTrip() {
        val a = _app.value
        a.saved?.let { sv ->
            // 증상 메모 = 음성 신고 (체크 칸은 없앴다, 재정비 원칙 3)
            val note = UiMapper.eventsLine(trip.ui.value.distressEvents)?.removePrefix("오늘 ")
            viewModelScope.launch { runCatching { petStore.saveTrip(sv.id, sv.record.copy(symptomNote = note), System.currentTimeMillis(), sv.series.mapIndexed { i, v -> i * TripRecorder.SAMPLE_MIN to v }) } }
        }
        recorder.reset()
        trip.clearResume()
        _app.update { it.copy(saved = null) }
        if (snapshot.value.source == SourceMode.SIMULATION) seek(0.0) else runtime.setVehicleTrip(TripPhase.NOT_STARTED)
    }

    // ───────────── 설정 ─────────────

    private fun settingsUi(s: Snapshot): SettingsUi = SettingsUi(
        timing = settings.timing,
        voiceSuggest = settings.voiceSuggest,
        reduceMotion = settings.reduceMotion,
        signals = signalRows(s),
        version = BuildConfig.VERSION_NAME,
        shapeSource = trip.ui.value.shapeSource,
    )

    private fun signalRows(s: Snapshot): List<SignalRowUi> {
        fun label(src: SignalSource) = when (src) {
            SignalSource.PLEOS_SDK -> "Pleos SDK"
            SignalSource.AAOS_PROPERTY -> "AAOS 차량 속성"
            SignalSource.SIM -> "시뮬레이터"
            SignalSource.NONE -> null
        }
        fun row(name: String, vararg sig: Signal): SignalRowUi {
            val used = sig.map { s.sources[it] ?: SignalSource.NONE }.filter { it != SignalSource.NONE }.distinct()
            return SignalRowUi(name, used.isNotEmpty(), used.mapNotNull(::label).joinToString(" · ").ifEmpty { null })
        }
        return listOf(
            SignalRowUi("주행 제한 상태", true, "CarUxRestrictions"),
            row("속도·기어·주차 브레이크", Signal.SPEED, Signal.GEAR, Signal.PARKING_BRAKE),
            row("방위 (주행 자극 예보 횡가속)", Signal.HEADING),
            row("배터리·충전", Signal.BATTERY_SOC, Signal.CHARGING),
            row("실내 온도 측정값 (대기·도착 경고)", Signal.HVAC_TEMP),
            row("문 열림 (도착·잠깐 내릴 때)", Signal.DOOR),
        )
    }

    /** 내 데이터 삭제: 기기 설정·프로필·기록 초기화 + 서버 기기 삭제(`DELETE /devices/me`, 오프라인이면 다음에). */
    fun deleteData() {
        viewModelScope.launch {
            server.session?.forget()
            runCatching { petStore.clear() }
        }
        presets.clear()
        settings.clear()
        memoStore.clear()
        calibration.clear()
        trip.motionScale = 1.0
        trip.clearResume()
        trip.forgetPet()
        recorder.reset()
        _app.update { AppState() }
    }

    private companion object {
        const val DEMO_PEAK = 46
        /** 정차 중 "콩이가 이상해요" 고르기 (주행 중에는 음성으로만). */
        val DISTRESS_CHIPS = listOf(
            "헐떡임" to DistressKind.PANTING, "구토" to DistressKind.VOMIT, "침 흘림·하품" to DistressKind.NAUSEA,
            "낑낑·떨림" to DistressKind.ANXIOUS, "축 처짐" to DistressKind.EMERGENCY,
        )

    }
}
