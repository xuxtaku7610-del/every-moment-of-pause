package com.emop.pause

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.emop.pause.trip.VetMemo
import com.emop.pause.trip.ClinicIntake
import com.emop.pause.trip.ClinicReply
import com.emop.pause.uistate.IntakeUi
import com.emop.pause.trip.SymptomMemoStore
import com.emop.pause.trip.CareRepository
import com.emop.pause.engine.pet.DistressKind
import com.emop.pause.engine.pet.PetDistress
import com.emop.pause.data.PlaceSource
import com.emop.pause.trip.ShapeMode
import com.emop.pause.trip.CalibrationStore
import com.emop.pause.trip.MotionAnswerParser
import com.emop.pause.engine.motion.CalibrationSample
import com.emop.pause.engine.motion.MotionAnswer
import com.emop.pause.engine.motion.MotionCalibration
import com.emop.pause.uistate.MotionAnswerUi
import com.emop.pause.uistate.ActivityKind
import com.emop.pause.uistate.AppUiState
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
/** [arrivalIndex] = "내렸어요" 때 멀미 지수 (이동 기록 "도착 시점 지수", 기기에 따로 저장하지 않는다). */
data class SavedTrip(val id: String, val record: PetTripRecord, val series: List<Int>, val summary: String, val cautions: Int = 0, val arrivalIndex: Int? = null)

data class AppState(
    val overlay: Overlay = Overlay.NONE,
    val demoOpen: Boolean = false,
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
    val symptoms: Set<String> = emptySet(),
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
    /** 도착 뒤 "오늘 멀미했어요?"를 아직 묻는 중. */
    val askMotion: Boolean = false,
    /** 이 반려견 개인 보정 기록 수·계수 (프로필 화면 문구). */
    val calibrationCount: Int = 0,
    val calibrationScale: Double = 1.0,
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
                    .copy(care = t.careId?.let(care::ui), distressChips = DISTRESS_CHIPS.map { it.first }),
            )
            Route.WAIT -> Screen.Wait(UiMapper.wait(t.wait, t))
            Route.PLACES -> Screen.Places(UiMapper.places(a.along, a.near, a.loading))
            Route.VETS -> Screen.Vets(UiMapper.vets(a.vets, a.loading, a.vetMemo, a.etaUrl, a.memoListening, todayLine(a)).copy(intake = a.intake?.let(::intakeUi)))
            Route.ARRIVAL -> Screen.Arrival(UiMapper.arrival(t))
            Route.TRIPLOG -> Screen.TripLog(tripLog(s, t, a))
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
        )
    }

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
            UiEvent.Close -> _app.update { it.copy(overlay = Overlay.NONE, petDraft = null, profileSkipped = true) }
            is UiEvent.StartActivity -> if (e.kind == ActivityKind.PET_WALK) recorder.onPetWalk()
            is UiEvent.SetRestMinutes -> _app.update { it.copy(restChoice = e.minutes) }
            UiEvent.StartWait -> trip.startWait()
            UiEvent.StopWait -> trip.stopWait()
            is UiEvent.SetWaitLimit -> trip.setWaitLimit(e.minutes)
            is UiEvent.GoPlace -> goPlace(e.id)
            is UiEvent.GoVet -> goVet(e.id)
            UiEvent.ConfirmLeft -> confirmLeft()
            is UiEvent.ToggleSymptom -> _app.update { a -> a.copy(symptoms = UiMapper.toggleSymptom(a.symptoms, e.label)) }
            UiEvent.FinishTrip -> finishTrip()
            is UiEvent.AnswerMotion -> answerMotion(e.answer)
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
    fun reply(text: String) = trip.reply(text)

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
    fun setCompareOpen(open: Boolean) = _app.update { it.copy(compareOpen = open) }
    /** 발표 장면 실내 온도 (시뮬레이터만, UPGRADE 7). */
    fun setDemoCabin(c: Float?) {
        trip.demoCabinTempC = c
    }

    /**
     * 발표 장면 "쓸수록 맞춰짐" (디버그 adb `--es calib yes,yes,little`): 지난 여행 답을 미리 넣는다(멀미 최고 46과 짝).
     * `reset`이면 지운다. 실제 사용에서는 도착 뒤 질문으로만 쌓인다.
     */
    fun seedCalibration(spec: String) {
        val pet = trip.ui.value.pet.name
        calibration.reset(pet)
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
        val at = here(snapshot.value) ?: snapshot.value.route?.destination
        _app.update { it.copy(overlay = Overlay.VETS, loading = true) }
        viewModelScope.launch {
            val found = at?.let { places.vetsNear(it) }
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
        val vet = UiMapper.vetDrive(v.name, v.distanceKm).let { if (i != null) it.copy(minutes = i.etaMin, clinic = i.reply?.drive ?: if (i.seen) "병원이 접수함을 열어 봤어요" else "병원 사전 접수 보냄") else it }
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
            val req = EtaShareReq(trip.ui.value.pet.name, v.name, minutes, shared.symptoms, shared.memo, shared.tripLine, intake = true)
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
            _app.update { it.copy(saved = SavedTrip(id, record, log.motionSeries, summary, log.cautionCount, log.motionNow.takeIf { log.motionSeries.isNotEmpty() }), symptoms = emptySet(), askMotion = true) }
            // 음성으로도 한 번 묻는다 (정차 중). 못 들으면 화면 버튼으로.
            val heard = trip.ask("오늘 ${trip.ui.value.pet.name} 멀미했어요? 네, 조금, 아니요로 말해 주세요.")
            MotionAnswerParser.parse(heard)?.let { answerMotion(it) }
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

    /** 답 하나를 기록(건너뛰기는 기록 안 함)하고 계수를 다시 계산. 한 여행에 한 번만. */
    private fun answerMotion(answer: MotionAnswerUi?) {
        val a = _app.value
        if (!a.askMotion) return
        _app.update { it.copy(askMotion = false) }
        if (answer == null) return
        val pet = trip.ui.value.pet.name
        val peak = a.saved?.record?.motionPeak ?: recorder.log().motionPeak
        calibration.add(pet, CalibrationSample(peak, MotionAnswer.valueOf(answer.name)))
        applyCalibration(pet)
        val n = _app.value.calibrationCount
        trip.notify(if (n < MotionCalibration.MIN_SAMPLES) "고마워요. ${MotionCalibration.MIN_SAMPLES - n}번 더 답하면 예보에 반영해요." else "고마워요. 다음 여행 예보에 반영할게요 (기록 ${n}회).")
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
        val template = "이번 이동은 ${UiMapper.duration(r.drivingMin)}, 휴식 ${r.stops}번, 멀미 주의 예보 최고 ${r.motionPeak}였어요." +
            (limit?.let { " 평소 한계는 약 ${UiMapper.duration(it.minutes)}이에요." } ?: "")
        val session = server.session ?: return template
        val text = withTimeoutOrNull(2_500) {
            runCatching { session.call { summary(SummaryReq(SummaryFacts(r.drivingMin, r.stops, r.motionPeak, limit?.minutes))) }.text }.getOrNull()
        }
        return text?.takeIf { it.isNotBlank() } ?: template
    }

    private fun tripLog(s: Snapshot, t: TripUi, a: AppState): TripLogUi {
        val saved = a.saved
        val log = recorder.log()
        val record = saved?.record ?: PetTripRecord(0, log.drivingMin, log.firstCautionMin, log.motionPeak, log.visits.size)
        val note = a.symptoms.joinToString(", ").ifEmpty { null }
        val today = LocalDate.now()
        val askMotion = if (a.askMotion) "오늘 ${t.pet.name} 멀미했어요?" else null
        // 오늘 증상 기록 (UPGRADE 7): "오늘 헐떡임 1회(출발 10분 뒤)"
        val events = UiMapper.eventsLine(t.distressEvents)
        val series = saved?.series ?: log.motionSeries
        val arrivalIndex = if (saved != null) saved.arrivalIndex else log.motionNow.takeIf { series.isNotEmpty() }
        val cautions = saved?.cautions ?: log.cautionCount
        val eventNames = t.distressEvents.map { it.first }
        val status = UiMapper.petStatus(series.size, arrivalIndex, a.symptoms, eventNames)
        return TripLogUi(
            meta = "${today.monthValue}월 ${today.dayOfMonth}일, ${s.route?.destinationName ?: "목적지"}까지",
            summary = saved?.summary ?: "기록을 정리하는 중이에요.",
            durationMin = record.drivingMin,
            stops = record.stops,
            motionPeak = record.motionPeak,
            // 기록이 없으면 빈 그대로 (그래프가 "기록이 아직 없어요"를 그린다 — 0으로 채우지 않는다)
            motionSeries = series,
            limitLine = PetTripLog.insight(t.pet.name, record, a.limit),
            vetSummary = PetTripLog.vetSummary(t.pet.name, record.copy(symptomNote = note), a.limit),
            symptoms = UiMapper.SYMPTOMS.map { SymptomUi(it, it in a.symptoms) },
            warnings = cautions,
            askMotion = askMotion,
            events = events,
            calibration = if (a.askMotion) null else calibrationLine(t.pet.name, a),
            history = a.history.filter { it.startedMs != record.startedMs }.take(3).map { r ->
                val d = java.time.Instant.ofEpochMilli(r.startedMs).atZone(java.time.ZoneId.systemDefault())
                HistoryUi("${d.monthValue}월 ${d.dayOfMonth}일", "${UiMapper.duration(r.drivingMin)} · 멀미 최고 ${r.motionPeak}")
            },
            petName = t.pet.name,
            status = status,
            statusReason = UiMapper.statusReason(status, a.symptoms, eventNames, cautions),
            arrivalIndex = arrivalIndex,
            marks = UiMapper.trendMarks(log.startMinute, log.visits, t.distressEvents),
            vetRows = UiMapper.vetRows(record, a.symptoms, events, arrivalIndex, cautions, a.limit),
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
        shareVetEta(vet.id)
    }

    /** 디버그 adb `--es memo "구토 두 번"`: 에뮬레이터에 마이크가 없어서 받아쓰기 결과를 대신 넣는다. */
    fun heardMemo(text: String) = editMemo { it.append(text) }

    /** 오늘 이동 한 줄 (공유에 붙는다). */
    private fun todayLine(a: AppState): String? {
        val log = recorder.log()
        return VetMemo.tripLine(log.drivingMin, log.cautionCount, log.motionPeak, trip.ui.value.distressEvents.map { it.first })
    }

    /** 기록 확인 → 증상 메모를 함께 저장하고 새 여행 준비. 시뮬레이션은 처음으로 되감는다. */
    private fun finishTrip() {
        val a = _app.value
        a.saved?.let { sv ->
            val note = a.symptoms.joinToString(", ").ifEmpty { null }
            viewModelScope.launch { runCatching { petStore.saveTrip(sv.id, sv.record.copy(symptomNote = note), System.currentTimeMillis(), sv.series.mapIndexed { i, v -> i * TripRecorder.SAMPLE_MIN to v }) } }
        }
        recorder.reset()
        trip.clearResume()
        _app.update { it.copy(saved = null, symptoms = emptySet()) }
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
            row("방위 (멀미 예보 횡가속)", Signal.HEADING),
            row("배터리·충전", Signal.BATTERY_SOC, Signal.CHARGING),
            row("실내 온도 측정값 (대기·도착 경고)", Signal.HVAC_TEMP),
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
