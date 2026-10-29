package com.emop.pause

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSource
import com.emop.pause.core.model.TripPhase
import com.emop.pause.runtime.SignalRuntime
import com.emop.pause.runtime.Snapshot
import com.emop.pause.runtime.SourceMode
import com.emop.pause.trip.PresetStore
import com.emop.pause.trip.SettingsStore
import com.emop.pause.di.Server
import com.emop.pause.trip.NapAlarm
import com.emop.pause.trip.NapAlarmPlan
import com.emop.pause.trip.TripCoordinator
import com.emop.pause.trip.TripRecorder
import com.emop.pause.trip.TripUi
import com.emop.pause.uistate.AvatarKind
import com.emop.pause.uistate.ActivityKind
import com.emop.pause.uistate.MemberUi
import com.emop.pause.presentation.Names
import com.emop.pause.uistate.NapUi
import com.emop.pause.uistate.OnboardStep
import com.emop.pause.uistate.ReportStopUi
import com.emop.pause.uistate.ReportUi
import com.emop.pause.uistate.SettingsSection
import com.emop.pause.uistate.SettingsUi
import com.emop.pause.uistate.SignalRowUi
import com.emop.pause.presentation.UiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emop.pause.presentation.Route
import com.emop.pause.presentation.ScreenRouter
import com.emop.pause.uistate.AppUiState
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.OnboardUi
import com.emop.pause.uistate.PrepareScreenUi
import com.emop.pause.uistate.Scene
import com.emop.pause.uistate.Screen
import com.emop.pause.uistate.UiEvent
import com.emop.pause.uistate.UxPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class Overlay { NONE, SETTINGS }

/** 파워냅 타이머. 시뮬레이션 시계(Snapshot.nowMs)로 흐르므로 배속에서도 맞다. */
data class NapState(val startMs: Long, val totalSec: Int, val soundOn: Boolean)

data class AppState(
    val overlay: Overlay = Overlay.NONE,
    val settingsSection: SettingsSection = SettingsSection.SUGGEST,
    val onboard: OnboardStep? = null,
    val demoOpen: Boolean = false,
    val scene: Scene? = null,
    val nap: NapState? = null,
    val restChoice: Int = 15,
    val prefsVersion: Int = 0,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val runtime: SignalRuntime,
    private val trip: TripCoordinator,
    private val presets: PresetStore,
    private val settings: SettingsStore,
    private val napAlarm: NapAlarm,
    private val server: Server,
) : ViewModel() {
    val snapshot: StateFlow<Snapshot> = runtime.snapshot
    val tripUi: StateFlow<TripUi> = trip.ui
    private val _app = MutableStateFlow(AppState(onboard = OnboardStep.entries.getOrNull(settings.onboardDone)))
    val app: StateFlow<AppState> = _app.asStateFlow()

    private val recorder = TripRecorder()
    private var lastRecordedSec = -1L
    private var logoTaps = mutableListOf<Long>()

    /** 화면 계약 (docs/ui_contract.md): 화면은 이 값만 그린다. */
    val ui: StateFlow<AppUiState> = combine(runtime.snapshot, trip.ui, _app) { s, t, a -> uiState(s, t, a) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, uiState(runtime.snapshot.value, trip.ui.value, _app.value))

    init {
        trip.voiceEnabled = settings.voiceSuggest
        trip.kidAnswerEnabled = settings.kidAnswer
        trip.eager = settings.timing == 0 || settings.proactive
        // 발표 모드 점프: 건너뛴 구간의 정차 기록을 다시 쌓는다 (피로 곡선은 점프 이후부터).
        viewModelScope.launch {
            runtime.replays.collect { frames ->
                recorder.reset()
                frames.forEach { s -> recorder.tick(s.clockMinute, s.mode, s.pauseCause, null, null, s.route?.totalDistanceM?.let { tot -> (tot - (s.eta?.remainDistanceM ?: tot)) / 1000.0 } ?: 0.0, trip.ui.value.stops) }
            }
        }
        viewModelScope.launch {
            runtime.snapshot.collect { s ->
                if (s.trip == TripPhase.NOT_STARTED) recorder.reset()
                val sec = s.nowMs / 1000
                if (sec != lastRecordedSec) {
                    lastRecordedSec = sec
                    val t = trip.ui.value
                    recorder.tick(s.clockMinute, s.mode, s.pauseCause, t.fatigue?.score, t.fatigue?.continuousDriveMinutes, t.traveledKm, t.stops)
                }
                // 안전: 주행이 시작되거나 멈춤이 끝나면 파워냅을 즉시 끝낸다 (test_plan 6장 [E][P0]).
                val nap = _app.value.nap
                if (nap != null && (s.driveState.forcesDrivingUi || s.mode != AppMode.PAUSE)) _app.update { it.copy(nap = null) }
                syncNapAlarm(s)
                if (s.driveState.forcesDrivingUi && _app.value.overlay != Overlay.NONE) _app.update { it.copy(overlay = Overlay.NONE) }
            }
        }
    }

    // ───────────── 화면 상태 ─────────────
    private fun uiState(s: Snapshot, t: TripUi, a: AppState): AppUiState {
        val simulation = s.source == SourceMode.SIMULATION
        val ux = s.uxLimits.let { UxPolicy.Flags(it.noVideo, it.limitStringLength, it.maxStringLength, it.noKeyboard, it.noSetup) }
        val route = ScreenRouter.route(
            ScreenRouter.Input(
                s.driveState, s.mode, simulation,
                sceneLocked = a.scene != null,
                settingsOpen = a.overlay == Overlay.SETTINGS,
                onboarding = a.onboard != null,
                napping = a.nap != null,
                setupAllowed = UxPolicy.setupAllowed(ux),
            ),
        )
        val screen = when (route) {
            Route.SCENE -> Fixtures.screen(a.scene!!)
            Route.DRIVE -> Screen.Drive(UxPolicy.apply(UiMapper.drive(s, t, names()), ux))
            Route.SETTINGS -> Screen.Settings(settingsUi(s, a))
            Route.ONBOARD -> Screen.Onboard(OnboardUi(a.onboard!!, onboardMembers()))
            Route.PREPARE -> Screen.Prepare(prepareUi(s, t))
            Route.PAUSE -> Screen.Pause(UiMapper.pause(s, t, pauseExtra(a), names()))
            Route.NAP -> Screen.Nap(napUi(s, a.nap!!))
            Route.REPORT -> Screen.Report(report(s, t))
        }
        val motion = when {
            route != Route.SCENE -> UxPolicy.motionAllowed(ScreenRouter.motionAllowed(s.mode, s.driveState, settings.reduceMotion), ux)
            a.scene == Scene.PAUSE -> true
            a.scene == Scene.NAP -> !settings.reduceMotion
            else -> false
        }
        return AppUiState(
            screen = screen,
            drivingLocked = s.driveState.forcesDrivingUi,
            motionAllowed = motion,
            dimmed = route == Route.NAP,
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
            is UiEvent.ToggleMember -> toggleMember(e.id)
            is UiEvent.SetSleep -> setSleep(e.hours)
            UiEvent.ToggleProactive -> toggleProactive()
            UiEvent.StartTrip -> startTrip()
            UiEvent.OpenSettings -> openSettings()
            is UiEvent.StartActivity -> onActivity(e.kind)
            is UiEvent.SetRestMinutes -> setRestChoice(e.minutes)
            is UiEvent.CompleteMission -> trip.completeMission(e.id)
            is UiEvent.SetRadio -> trip.reply(if (e.on) "라디오 틀어 줘" else "라디오 꺼")
            UiEvent.Wake -> wake()
            UiEvent.NapMore -> napMore()
            UiEvent.ToggleNapSound -> if (_app.value.nap != null) napSound() else toggleNapSound()
            UiEvent.FinishReport -> finishReport()
            UiEvent.OnboardNext -> onboardNext()
            UiEvent.AllowMic, UiEvent.ButtonsOnly -> onboardFinish()
            is UiEvent.SelectSettingsSection -> settingsSection(e.section)
            is UiEvent.SetTiming -> setTiming(e.value)
            UiEvent.ToggleVoice -> toggleVoice()
            UiEvent.ToggleKidAnswer -> toggleKidAnswer()
            UiEvent.ToggleReduceMotion -> toggleReduceMotion()
            UiEvent.DeleteData -> deleteData()
            UiEvent.CloseSettings -> closeSettings()
        }
    }

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
    /** 로고 5번 탭(3초 안) → 숨은 데모 패널 (COMMERCIAL_READINESS I P0). 릴리스에도 있다. */
    fun onLogoTap(nowMs: Long = System.currentTimeMillis()) {
        logoTaps.add(nowMs)
        logoTaps.removeAll { nowMs - it > 3_000 }
        if (logoTaps.size >= 5) {
            logoTaps.clear()
            _app.update { it.copy(demoOpen = !it.demoOpen) }
        }
    }
    fun setDemoOpen(open: Boolean) = _app.update { it.copy(demoOpen = open) }
    fun setScene(scene: Scene?) = _app.update { it.copy(scene = scene) }

    /** 시연 장면으로 점프: 시나리오 시각으로 이동하고 재생한다. */
    fun jump(tSec: Int, nap: Boolean = false) {
        _app.update { it.copy(scene = null, nap = null) }
        seek(tSec.toDouble())
        play()
        if (nap) startNap()
    }

    // ───────────── 출발 준비 ─────────────
    /** 시뮬레이션은 시나리오의 동승자 설정을, 실차는 프리셋을 보여 준다. */
    fun members(): List<MemberUi> {
        val sim = snapshot.value.simSetup
        val kids = sim?.childAges ?: presets.childAges
        val dog = sim?.dog ?: presets.dog
        return listOf(
            MemberUi("driver", "운전자", AvatarKind.ADULT, true),
            MemberUi("relief", "교대 1명", AvatarKind.ADULT, presets.relief),
            MemberUi("kid", "${presets.childName} ${kids.firstOrNull() ?: 5}살", AvatarKind.KID, kids.isNotEmpty()),
            MemberUi("dog", presets.dogName, AvatarKind.PET, dog),
        )
    }

    fun names() = Names(presets.childName, presets.dogName)

    /** 온보딩 2단계 타일: 운전자는 빼고, 교대 운전자는 "어른 1명/2명" 한 타일로. */
    private fun onboardMembers() = members().filter { it.id != "driver" }.map {
        if (it.id == "relief") it.copy(label = if (it.selected) "어른 2명" else "어른 1명", selected = true) else it
    }

    private fun prepareUi(s: Snapshot, t: TripUi): PrepareScreenUi {
        val (rows, _) = UiMapper.preparePlan(t)
        val plan = t.plan
        val journey = UiMapper.journey(s, t)?.let { j -> j.copy(progress = 0f, start = null, stops = j.stops.map { st -> st.copy(label = st.label.substringBefore(" 뒤")) }) }
        val sleep = sleepHours()
        val rationale = when {
            plan == null -> "경로를 받으면 쉴 곳을 잡아 드려요."
            plan.firstAdvancedMin > 0 -> "평소보다 ${plan.firstAdvancedMin}분 일찍 첫 휴식을 잡았어요."
            sleep == null -> "수면 시간을 고르지 않아 7시간으로 계산했어요."
            else -> "${UiMapper.duration(plan.intervalMin)}마다 쉬도록 잡았어요."
        }
        return PrepareScreenUi(
            meta = UiMapper.meta(s)?.replace("까지 ", "까지 예상 "),
            members = members(),
            sleepHours = sleep,
            proactive = proactive(),
            plan = rows,
            journey = journey,
            rationale = rationale,
        )
    }

    fun toggleMember(id: String) {
        when (id) {
            "relief" -> presets.relief = !presets.relief
            "kid" -> presets.childAges = if (presets.childAges.isEmpty()) listOf(5) else emptyList()
            "dog" -> presets.dog = !presets.dog
        }
        prefsChanged()
    }

    fun setSleep(hours: Int?) {
        presets.sleepHours = hours?.toFloat()
        prefsChanged()
    }

    fun sleepHours(): Int? = presets.sleepHours?.toInt()
    fun proactive() = settings.proactive
    fun toggleProactive() {
        settings.proactive = !settings.proactive
        trip.eager = settings.timing == 0 || settings.proactive
        prefsChanged()
    }

    /** 실차 모드의 출발 버튼. 시뮬레이터에서는 시나리오가 여행 단계를 정하므로 재생을 시작한다. */
    fun startTrip() {
        if (snapshot.value.source == SourceMode.SIMULATION) play() else runtime.setVehicleTrip(TripPhase.ACTIVE)
    }

    private fun prefsChanged() {
        trip.refreshPreview()
        _app.update { it.copy(prefsVersion = it.prefsVersion + 1) }
    }

    // ───────────── 멈춤 ─────────────
    fun setRestChoice(min: Int) = _app.update { it.copy(restChoice = min) }

    fun onActivity(kind: ActivityKind) {
        when (kind) {
            ActivityKind.NAP -> startNap()
            ActivityKind.EXPEDITION -> recorder.onExpedition()
            ActivityKind.PET_WALK -> recorder.onPetWalk()
            else -> Unit
        }
    }

    private fun pauseExtra(a: AppState = _app.value) = UiMapper.PauseExtra(recorder.currentArrived ?: snapshot.value.clockMinute, a.restChoice, recorder.currentStop)

    private fun startNap() {
        val s = snapshot.value
        val t = trip.ui.value
        val extra = pauseExtra()
        val plan = UiMapper.pause(s, t, extra, names())
        val napMin = plan.activities.firstOrNull { it.kind == ActivityKind.NAP }?.title?.substringBefore("분")?.toIntOrNull() ?: 20
        recorder.onNap(napMin)
        _app.update { it.copy(nap = NapState(s.nowMs, napMin * 60, settings.napSound)) }
    }

    private fun napUi(s: Snapshot, nap: NapState): NapUi {
        val remaining = (nap.totalSec - ((s.nowMs - nap.startMs) / 1000).toInt()).coerceAtLeast(0)
        val wakeMinute = s.clockMinute + (remaining + 59) / 60
        val charging = s.pauseCause == com.emop.pause.core.model.PauseCause.CHARGING
        val line = if (charging) "충전이 끝나기 2분 전, ${UiMapper.clock(wakeMinute)}에 깨워 드릴게요" else "${UiMapper.clock(wakeMinute)}에 깨워 드릴게요"
        return NapUi(remaining, nap.totalSec, line, nap.soundOn, waking = remaining <= 120)
    }

    fun wake() = _app.update { it.copy(nap = null) }

    private var alarmAt: Long? = null

    /** 파워냅 중이면 시스템 알람을 남은 시간에 맞춰 둔다(배속·일시정지·10분 더 반영). 끝나면 취소. */
    private fun syncNapAlarm(s: Snapshot) {
        val nap = _app.value.nap
        val wanted = nap?.let {
            val remaining = it.totalSec - ((s.nowMs - it.startMs) / 1000).toInt()
            val simulation = s.source == SourceMode.SIMULATION
            NapAlarmPlan.triggerAt(System.currentTimeMillis(), remaining, if (simulation) s.simSpeed else 1, !simulation || s.simPlaying)
        }
        if (!NapAlarmPlan.needsReschedule(alarmAt, wanted)) return
        if (wanted == null) napAlarm.cancel() else napAlarm.schedule(wanted)
        alarmAt = wanted
    }
    fun napMore() = _app.update { a -> a.copy(nap = a.nap?.let { it.copy(totalSec = it.totalSec + 600) }) }
    fun napSound() = _app.update { a -> a.copy(nap = a.nap?.let { it.copy(soundOn = !it.soundOn) }) }

    // ───────────── 리포트 ─────────────
    private fun report(s: Snapshot, t: TripUi): ReportUi {
        val log = recorder.log()
        val dest = s.route?.destinationName ?: "목적지"
        val parts = mutableListOf("출발해")
        log.visits.forEach { v ->
            val short = UiMapper.shortName(v.name)
            parts += when {
                v.chargedMin > 0 && v.napMin > 0 -> " ${short}에서 충전하며 ${v.napMin}분 자고"
                v.chargedMin > 0 -> " ${short}에서 충전하며 쉬고"
                v.expedition -> " ${short}에서 ${presets.childName}와 원정대를 마치고"
                else -> " ${short}에서 ${v.stayedMin}분 쉬고"
            }
        }
        parts[parts.lastIndex] = parts.last() + " ${dest}에 도착했어요."
        val today = LocalDate.now()
        return ReportUi(
            meta = "${today.monthValue}월 ${today.dayOfMonth}일, ${dest}까지",
            sentence = parts,
            distanceKm = log.distanceKm.toInt(),
            stops = log.visits.size,
            longestMin = log.longestDriveMin,
            log = log.visits.map { v ->
                ReportStopUi(
                    v.name,
                    listOfNotNull(
                        "충전 ${v.chargedMin}분".takeIf { v.chargedMin > 0 },
                        "파워냅 ${v.napMin}분".takeIf { v.napMin > 0 },
                        "원정대 완료".takeIf { v.expedition },
                        "${presets.dogName} 산책".takeIf { v.petWalk },
                    ).ifEmpty { listOf("휴식") },
                    UiMapper.clock(v.arrivedMinute),
                    "${v.stayedMin}분",
                )
            },
            fatigueSeries = log.fatigueSeries.ifEmpty { listOf(0, 0) },
            restMarks = log.restMarks,
            tip = TripRecorder.tip(log),
        )
    }

    /** 리포트 확인 → 새 여행 준비. 시뮬레이션은 처음으로 되감는다. */
    fun finishReport() {
        recorder.reset()
        trip.clearResume()
        if (snapshot.value.source == SourceMode.SIMULATION) seek(0.0) else runtime.setVehicleTrip(TripPhase.NOT_STARTED)
    }

    // ───────────── 온보딩·설정 ─────────────
    fun onboardNext() = _app.update { a ->
        val next = a.onboard?.ordinal?.plus(1) ?: 3
        settings.onboardDone = next
        a.copy(onboard = OnboardStep.entries.getOrNull(next))
    }
    fun onboardFinish() {
        settings.onboardDone = 3
        _app.update { it.copy(onboard = null) }
    }

    fun openSettings() = _app.update { it.copy(overlay = Overlay.SETTINGS) }
    fun closeSettings() = _app.update { it.copy(overlay = Overlay.NONE) }
    fun settingsSection(s: SettingsSection) = _app.update { it.copy(settingsSection = s) }

    private fun settingsUi(s: Snapshot, a: AppState): SettingsUi = SettingsUi(
        section = a.settingsSection,
        timing = settings.timing,
        voiceSuggest = settings.voiceSuggest,
        kidAnswer = settings.kidAnswer,
        napSound = settings.napSound,
        reduceMotion = settings.reduceMotion,
        signals = signalRows(s),
        members = members(),
        version = BuildConfig.VERSION_NAME,
    )

    private fun signalRows(s: Snapshot): List<SignalRowUi> {
        fun label(src: SignalSource) = when (src) {
            SignalSource.PLEOS_SDK -> "Pleos Vehicle SDK"
            SignalSource.AAOS_PROPERTY -> "AAOS 차량 속성"
            SignalSource.SIM -> "시뮬레이터"
            SignalSource.NONE -> null
        }
        /** 여러 신호를 한 줄로: 쓰는 출처를 모아 보여 준다 (COMMERCIAL_READINESS A P0). */
        fun row(name: String, vararg sig: Signal): SignalRowUi {
            val used = sig.map { s.sources[it] ?: SignalSource.NONE }.filter { it != SignalSource.NONE }.distinct()
            return SignalRowUi(name, used.isNotEmpty(), used.mapNotNull(::label).joinToString(" · ").ifEmpty { null })
        }
        return listOf(
            SignalRowUi("주행 제한 상태", true, "CarUxRestrictions"),
            row("속도와 기어", Signal.SPEED, Signal.GEAR, Signal.PARKING_BRAKE),
            row("배터리와 충전", Signal.BATTERY_SOC, Signal.CHARGING),
            row("공조와 실내 순환", Signal.HVAC_TEMP, Signal.HVAC_RECIRCULATION),
        )
    }

    fun setTiming(v: Int) { settings.timing = v; trip.eager = v == 0 || settings.proactive; prefsChanged() }
    fun toggleVoice() { settings.voiceSuggest = !settings.voiceSuggest; trip.voiceEnabled = settings.voiceSuggest; prefsChanged() }
    fun toggleKidAnswer() { settings.kidAnswer = !settings.kidAnswer; trip.kidAnswerEnabled = settings.kidAnswer; prefsChanged() }
    fun toggleNapSound() { settings.napSound = !settings.napSound; prefsChanged() }
    fun toggleReduceMotion() { settings.reduceMotion = !settings.reduceMotion; prefsChanged() }
    fun reduceMotion() = settings.reduceMotion

    /** 내 데이터 삭제 (G P1): 기기 설정·프리셋·기록 초기화 + 서버 기기 삭제(`DELETE /devices/me`, 오프라인이면 다음에). */
    fun deleteData() {
        viewModelScope.launch { server.session?.forget() }
        presets.clear()
        settings.clear()
        trip.clearResume()
        recorder.reset()
        _app.update { AppState(onboard = OnboardStep.CONCEPT) }
        prefsChanged()
    }
}
