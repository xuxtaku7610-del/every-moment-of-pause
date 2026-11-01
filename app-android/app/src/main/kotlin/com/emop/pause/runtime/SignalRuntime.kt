package com.emop.pause.runtime

import android.content.Context
import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.LatLng
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.RouteInfo
import com.emop.pause.core.model.NextGuide
import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalMerge
import com.emop.pause.core.model.SignalSample
import com.emop.pause.core.model.SignalSmoother
import com.emop.pause.core.model.SignalSource
import com.emop.pause.core.model.SourceReading
import com.emop.pause.core.model.TripPhase
import com.emop.pause.core.model.UxLimits
import com.emop.pause.core.navi.NaviSource
import com.emop.pause.core.navi.PleosNaviSource
import com.emop.pause.core.restriction.CarUxDriveStateRepository
import com.emop.pause.core.vehicle.AaosPropertySource
import com.emop.pause.core.vehicle.PleosVehicleSource
import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.sim.Gpx
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import com.emop.pause.sim.ScenarioSetup
import com.emop.pause.sim.SimNaviSource
import com.emop.pause.sim.SimFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

enum class SourceMode { SIMULATION, VEHICLE }

/** 화면이 보는 현재 상태 한 장. */
data class Snapshot(
    val source: SourceMode = SourceMode.VEHICLE,
    val scenarioName: String? = null,
    val simTimeSec: Double = 0.0,
    val simSpeed: Int = 1,
    val simPlaying: Boolean = false,
    val label: String? = null,
    val capabilities: Set<Signal> = emptySet(),
    /** 신호마다 실제로 쓴 공급원 (COMMERCIAL_READINESS A P0). 없는 신호는 NONE. */
    val sources: Map<Signal, SignalSource> = emptyMap(),
    val sample: SignalSample = SignalSample(0),
    val driveState: DriveState = DriveState.PARKED,
    /** CarUxRestrictions 활성 플래그. 시뮬레이터는 DriveState로 근사. */
    val uxLimits: UxLimits = UxLimits.NONE,
    val trip: TripPhase = TripPhase.NOT_STARTED,
    val eta: EtaInfo? = null,
    val mode: AppMode = AppMode.PREPARE,
    val pauseCause: PauseCause? = null,
    /** 엔진 시계(ms). 시뮬레이션은 시나리오 시각, 실차는 벽시계. */
    val nowMs: Long = 0,
    /** 현지 시각(분). 피로 엔진의 시간대 요인. */
    val clockMinute: Int = 0,
    val route: RouteInfo? = null,
    /** NaviHelper 앞쪽 회전 안내 (실차만, 멀미 예보 형상 추정). */
    val turns: List<RouteTurn> = emptyList(),
    /** NaviHelper 바로 다음 길 안내 (실차만, 주행 화면 카드). 시뮬레이터는 null — 지어내지 않는다. */
    val guide: NextGuide? = null,
    /** 시나리오가 정한 출발 설정. 실차는 null(프리셋 사용). */
    val simSetup: ScenarioSetup? = null,
    /** 이번 여행에서 추가한 경유지 이름 (디버그 표시). */
    val waypoints: List<String> = emptyList(),
)

/**
 * 신호 공급원(시뮬레이터 또는 실제 SDK)을 하나로 묶고 AppModeReducer를 돌린다.
 * 실제 SDK와 시뮬레이터가 같은 계약을 따르므로 화면·엔진은 공급원을 구분하지 않는다.
 */
@Singleton
class SignalRuntime @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    private var modeState = ModeState()
    private var job: Job? = null
    private var player: ScenarioPlayer? = null
    private var vehicle: PleosVehicleSource? = null
    private var aaos: AaosPropertySource? = null
    private var fillPlayer: ScenarioPlayer? = null
    private var navi: PleosNaviSource? = null
    private var simNavi: SimNaviSource? = null
    /** 시뮬레이터 모드에서도 기본 내비가 안내 중이면 회전 안내(TBT)만 실제 NaviHelper 값으로 받는다 (시연 촬영, docs/recording.md). */
    private var simGuide: PleosNaviSource? = null

    /** 지금 공급원의 내비 (경유지 추가에 쓴다). */
    val naviSource: NaviSource? get() = simNavi ?: navi
    private var uxRestrictions: CarUxDriveStateRepository? = null
    private val vehicleTrip = MutableStateFlow(TripPhase.NOT_STARTED)

    fun scenarioNames(): List<String> =
        context.assets.list(SCENARIO_DIR).orEmpty().filter { it.endsWith(".json") }.map { it.removeSuffix(".json") }.sorted()

    /** 시뮬레이션 모드: assets/scenarios/<name>.json을 처음부터 재생할 준비를 한다. */
    fun useScenario(name: String) {
        stopAll()
        val p = ScenarioPlayer(loadScenario(name), scope)
        val sn = SimNaviSource(p)
        player = p
        simNavi = sn
        // 기본 내비 안내가 없으면 null → 주행 화면은 목적지 한 줄(또는 시연 안내 "시연")로 줄어든다
        val g = runCatching { PleosNaviSource(context).also { it.start() } }.getOrNull()
        simGuide = g
        job = scope.launch {
            g?.let { n ->
                launch {
                    while (true) {
                        delay(NAVI_REFRESH_MS)
                        n.refresh()
                    }
                }
            }
            combine(p.frame, p.speed, p.playing, sn.waypoints, g?.guide() ?: flowOf(null)) { f, speed, playing, wps, guide -> SimTick(f, speed, playing, wps, guide) }
                .collect { publishSim(p.scenario, it) }
        }
    }

    fun play() = player?.play()
    fun pause() = player?.pause()
    fun setSpeed(multiplier: Int) = player?.setSpeed(multiplier)
    /**
     * 시나리오 시각으로 점프. 타이머 상태를 맞추려고 처음부터 1초씩 다시 계산하고,
     * 지나간 프레임 묶음을 [replays]로 내보내 엔진(피로·니즈·기록)도 같은 과정을 조용히 거치게 한다.
     */
    fun seek(tSec: Double) {
        val p = player ?: return
        modeState = ModeState()
        val frames = ArrayList<Snapshot>()
        var t = 0.0
        while (t < tSec) {
            val f = ScenarioPlayer.frameAt(p.scenario, t)
            reduceSim(f)
            frames += simSnapshot(p.scenario, f, p.speed.value, false, emptyList())
            t += 1.0
        }
        _replays.tryEmit(frames)
        p.seek(tSec)
    }

    private val _replays = kotlinx.coroutines.flow.MutableSharedFlow<List<Snapshot>>(extraBufferCapacity = 1)

    /** 점프할 때 건너뛴 프레임 (1초 간격). 음성·제안 없이 엔진만 따라잡는다. */
    val replays: kotlinx.coroutines.flow.SharedFlow<List<Snapshot>> = _replays

    private fun tick() = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MS)
        }
    }

    private fun loadScenario(name: String): Scenario =
        Scenario.parse(context.assets.open("$SCENARIO_DIR/$name.json").bufferedReader().use { it.readText() })

    /** 시나리오의 경로 형상 GPX (멀미 예보용). 시나리오마다 한 번만 읽는다. */
    private var shapeCache: Pair<String, List<LatLng>>? = null

    private fun shapeOf(scenario: Scenario): List<LatLng> {
        val gpx = scenario.route.shapeGpx ?: return emptyList()
        shapeCache?.takeIf { it.first == gpx }?.let { return it.second }
        val pts = runCatching { Gpx.parse(context.assets.open("$SCENARIO_DIR/$gpx").bufferedReader().use { it.readText() }) }.getOrDefault(emptyList())
        shapeCache = gpx to pts
        return pts
    }

    /**
     * 실차 모드: Vehicle SDK + AAOS 차량 속성 + NaviHelper + CarUxRestrictions.
     * 신호는 신호마다 Pleos SDK → AAOS 속성 순으로 고른다 ([SignalMerge]).
     * [fillScenario]를 주면 두 곳 모두에 없는 신호만 그 시나리오(1배속)로 채운다 — 에뮬레이터 시연 보충용, 출처는 SIM으로 표시.
     * 합친 값은 [SignalSmoother]로 잡음을 거른다. 디바운스가 시간으로 확정되도록 값이 그대로여도 1초마다 다시 합친다.
     */
    fun useVehicle(fillScenario: String? = null) {
        stopAll()
        val v = PleosVehicleSource(context).also { it.start() }
        val a = AaosPropertySource(context).also { it.start() }
        val fill = fillScenario?.let { runCatching { loadScenario(it) }.getOrNull() }?.let { sc -> ScenarioPlayer(sc, scope).also { it.play() } }
        val fillFlow = fill?.frame?.map { SourceReading(SignalSource.SIM, fill.scenario.capabilities, it.sample) } ?: flowOf(null)
        val readings = combine(
            combine(v.samples(), v.capabilities) { sample, caps -> SourceReading(SignalSource.PLEOS_SDK, caps, sample) },
            combine(a.samples(), a.capabilities) { sample, caps -> SourceReading(SignalSource.AAOS_PROPERTY, caps, sample) },
            fillFlow,
            tick(),
        ) { p, aa, f, _ -> SignalMerge.merge(listOfNotNull(p, aa, f), System.currentTimeMillis()) }
        val smoother = SignalSmoother()
        val n = PleosNaviSource(context).also { it.start() }
        // 방위는 NaviHelper 현재 위치에서 받는다 (멀미 예보 횡가속, docs/plan/sdk_check.md). 잡음 처리 뒤에 붙인다.
        val smoothed = combine(readings.map { it.copy(sample = smoother.apply(it.sample)) }, n.heading()) { m, h ->
            if (h == null) m else m.copy(sample = m.sample.copy(headingDeg = h), sources = m.sources + (Signal.HEADING to SignalSource.PLEOS_SDK))
        }
        aaos = a
        fillPlayer = fill
        val ux = CarUxDriveStateRepository(context)
        val routeFlow = runCatching { n.route() }.getOrDefault(flowOf(null))
        vehicle = v
        navi = n
        uxRestrictions = ux
        job = scope.launch {
            launch {
                while (true) {
                    delay(NAVI_REFRESH_MS)
                    n.refresh()
                }
            }
            // TODO(pleos-sdk): onCurrentLocationInfo가 스스로 주기적으로 오는지 에뮬레이터에서 확인. 지금은 1초마다 요청한다.
            launch {
                while (true) {
                    delay(TICK_MS)
                    n.refreshLocation()
                }
            }
            combine(smoothed, combine(ux.driveState, ux.uxLimits, ::Pair), n.eta(), vehicleTrip, combine(routeFlow, n.turns(), n.guide(), ::Triple)) { merged, (drive, limits), eta, trip, (route, turns, guide) ->
                val now = System.currentTimeMillis()
                val sample = merged.sample
                modeState = AppModeReducer.reduce(modeState, ModeInput(now, drive, trip, sample.gear, sample.speedKph, sample.charging, sample.parkingBrakeOn))
                Snapshot(
                    source = SourceMode.VEHICLE,
                    capabilities = merged.capabilities,
                    sources = merged.sources,
                    uxLimits = limits,
                    sample = sample,
                    driveState = drive,
                    trip = trip,
                    eta = eta,
                    mode = modeState.mode,
                    pauseCause = modeState.pauseCause,
                    nowMs = now,
                    clockMinute = LocalTime.now().let { it.hour * 60 + it.minute },
                    route = route,
                    turns = turns,
                    guide = guide,
                )
            }.collect { snap ->
                logSignalsIfChanged(snap)
                _snapshot.value = snap
            }
        }
    }

    private var lastLogged: String? = null

    /** 실차 모드에서 신호가 바뀔 때만 한 줄 로그 (scripts/vhal.ps1이 `adb logcat -s PauseSignals`로 읽어 docs/vhal_check.md에 남긴다). */
    private fun logSignalsIfChanged(s: Snapshot) {
        val x = s.sample
        fun src(sig: Signal) = s.sources[sig]?.name ?: "NONE"
        val line = "speed=${x.speedKph}(${src(Signal.SPEED)}) gear=${x.gear}(${src(Signal.GEAR)}) brake=${x.parkingBrakeOn}(${src(Signal.PARKING_BRAKE)}) " +
            "soc=${x.batterySocPercent}(${src(Signal.BATTERY_SOC)}) charging=${x.charging}(${src(Signal.CHARGING)}) drive=${s.driveState} mode=${s.mode}"
        if (line != lastLogged) {
            lastLogged = line
            android.util.Log.i("PauseSignals", line)
        }
    }

    /** CAR_SPEED 같은 런타임 권한을 받은 뒤 AAOS 속성 구독을 다시 건다. */
    fun refreshVehicleProperties() {
        aaos?.let {
            it.stop()
            it.start()
        }
    }

    /** 실차 모드의 여행 시작·종료 (출발 버튼, 도착). */
    fun setVehicleTrip(phase: TripPhase) {
        vehicleTrip.value = phase
    }

    private data class SimTick(val f: SimFrame, val speed: Int, val playing: Boolean, val waypoints: List<String>, val guide: NextGuide? = null)

    private fun publishSim(scenario: Scenario, tick: SimTick) {
        val (f, speed, playing, waypoints, guide) = tick
        reduceSim(f)
        // 내비 안내는 주행 중 장면에만 (정차·도착 장면에 남은 회전 안내를 붙이지 않는다)
        _snapshot.value = simSnapshot(scenario, f, speed, playing, waypoints).let { if (guide != null && f.trip == TripPhase.ACTIVE) it.copy(guide = guide) else it }
    }

    private fun simSnapshot(scenario: Scenario, f: SimFrame, speed: Int, playing: Boolean, waypoints: List<String>): Snapshot {
        return Snapshot(
            source = SourceMode.SIMULATION,
            scenarioName = scenario.name,
            simTimeSec = f.tSec,
            simSpeed = speed,
            simPlaying = playing,
            label = f.label,
            capabilities = scenario.capabilities,
            sources = Signal.entries.associateWith { if (it in scenario.capabilities) SignalSource.SIM else SignalSource.NONE },
            sample = f.sample,
            driveState = f.driveState,
            uxLimits = UxLimits.approximate(f.driveState),
            trip = f.trip,
            eta = if (f.remainDistanceM != null && f.remainTimeSec != null) EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!) else null,
            mode = modeState.mode,
            pauseCause = modeState.pauseCause,
            nowMs = (f.tSec * 1000).toLong(),
            clockMinute = scenario.startMinute + (f.tSec / 60).toInt(),
            route = scenario.route.let { r -> RouteInfo(r.destinationName, r.destination.toModel(), r.totalDistanceM, r.points.map { it.toModel() }, shapeOf(scenario), r.shapeStartM) },
            simSetup = scenario.setup,
            waypoints = waypoints,
        )
    }

    private fun reduceSim(f: SimFrame) {
        modeState = AppModeReducer.reduce(
            modeState,
            ModeInput((f.tSec * 1000).toLong(), f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging, f.sample.parkingBrakeOn),
        )
    }

    private fun stopAll() {
        job?.cancel()
        player?.pause()
        vehicle?.stop()
        aaos?.stop()
        fillPlayer?.pause()
        navi?.stop()
        simGuide?.let { runCatching { it.stop() } }
        simGuide = null
        uxRestrictions?.release()
        player = null
        simNavi = null
        vehicle = null
        aaos = null
        fillPlayer = null
        navi = null
        uxRestrictions = null
        modeState = ModeState()
    }

    private companion object {
        const val SCENARIO_DIR = "scenarios"
        const val NAVI_REFRESH_MS = 10_000L
        const val TICK_MS = 1_000L
    }
}
