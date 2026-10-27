package com.emop.pause.runtime

import android.content.Context
import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import com.emop.pause.core.model.TripPhase
import com.emop.pause.core.navi.PleosNaviSource
import com.emop.pause.core.restriction.CarUxDriveStateRepository
import com.emop.pause.core.vehicle.PleosVehicleSource
import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
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
import kotlinx.coroutines.launch
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
    val sample: SignalSample = SignalSample(0),
    val driveState: DriveState = DriveState.PARKED,
    val trip: TripPhase = TripPhase.NOT_STARTED,
    val eta: EtaInfo? = null,
    val mode: AppMode = AppMode.PREPARE,
    val pauseCause: PauseCause? = null,
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
    private var navi: PleosNaviSource? = null
    private var uxRestrictions: CarUxDriveStateRepository? = null
    private val vehicleTrip = MutableStateFlow(TripPhase.NOT_STARTED)

    fun scenarioNames(): List<String> =
        context.assets.list(SCENARIO_DIR).orEmpty().filter { it.endsWith(".json") }.map { it.removeSuffix(".json") }.sorted()

    /** 시뮬레이션 모드: assets/scenarios/<name>.json을 처음부터 재생할 준비를 한다. */
    fun useScenario(name: String) {
        stopAll()
        val text = context.assets.open("$SCENARIO_DIR/$name.json").bufferedReader().use { it.readText() }
        val p = ScenarioPlayer(Scenario.parse(text), scope)
        player = p
        job = scope.launch {
            combine(p.frame, p.speed, p.playing) { f, speed, playing -> Triple(f, speed, playing) }
                .collect { (f, speed, playing) -> publishSim(p.scenario, f, speed, playing) }
        }
    }

    fun play() = player?.play()
    fun pause() = player?.pause()
    fun setSpeed(multiplier: Int) = player?.setSpeed(multiplier)
    fun seek(tSec: Double) {
        // 뒤로 점프하면 타이머 상태가 맞지 않으므로 처음부터 다시 계산한다.
        val p = player ?: return
        modeState = ModeState()
        val step = 1.0
        var t = 0.0
        while (t < tSec) {
            reduceSim(ScenarioPlayer.frameAt(p.scenario, t))
            t += step
        }
        p.seek(tSec)
    }

    /** 실차 모드: Vehicle SDK·NaviHelper·CarUxRestrictions에 연결한다. */
    fun useVehicle() {
        stopAll()
        val v = PleosVehicleSource(context).also { it.start() }
        val n = PleosNaviSource(context).also { it.start() }
        val ux = CarUxDriveStateRepository(context)
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
            combine(v.samples(), v.capabilities, ux.driveState, n.eta(), vehicleTrip) { sample, caps, drive, eta, trip ->
                val now = System.currentTimeMillis()
                modeState = AppModeReducer.reduce(modeState, ModeInput(now, drive, trip, sample.gear.takeIf { Signal.GEAR in caps }, sample.speedKph, sample.charging))
                Snapshot(
                    source = SourceMode.VEHICLE,
                    capabilities = caps,
                    sample = sample,
                    driveState = drive,
                    trip = trip,
                    eta = eta,
                    mode = modeState.mode,
                    pauseCause = modeState.pauseCause,
                )
            }.collect { _snapshot.value = it }
        }
    }

    /** 실차 모드의 여행 시작·종료 (출발 버튼, 도착). */
    fun setVehicleTrip(phase: TripPhase) {
        vehicleTrip.value = phase
    }

    private fun publishSim(scenario: Scenario, f: SimFrame, speed: Int, playing: Boolean) {
        reduceSim(f)
        _snapshot.value = Snapshot(
            source = SourceMode.SIMULATION,
            scenarioName = scenario.name,
            simTimeSec = f.tSec,
            simSpeed = speed,
            simPlaying = playing,
            label = f.label,
            capabilities = scenario.capabilities,
            sample = f.sample,
            driveState = f.driveState,
            trip = f.trip,
            eta = if (f.remainDistanceM != null && f.remainTimeSec != null) EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!) else null,
            mode = modeState.mode,
            pauseCause = modeState.pauseCause,
        )
    }

    private fun reduceSim(f: SimFrame) {
        modeState = AppModeReducer.reduce(
            modeState,
            ModeInput((f.tSec * 1000).toLong(), f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging),
        )
    }

    private fun stopAll() {
        job?.cancel()
        player?.pause()
        vehicle?.stop()
        navi?.stop()
        uxRestrictions?.release()
        player = null
        vehicle = null
        navi = null
        uxRestrictions = null
        modeState = ModeState()
    }

    private companion object {
        const val SCENARIO_DIR = "scenarios"
        const val NAVI_REFRESH_MS = 10_000L
    }
}
