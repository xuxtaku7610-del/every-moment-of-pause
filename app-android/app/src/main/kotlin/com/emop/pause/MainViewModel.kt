package com.emop.pause

import androidx.lifecycle.ViewModel
import com.emop.pause.core.model.TripPhase
import com.emop.pause.runtime.Snapshot
import com.emop.pause.runtime.SignalRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val runtime: SignalRuntime,
) : ViewModel() {
    val snapshot: StateFlow<Snapshot> = runtime.snapshot

    fun scenarioNames() = runtime.scenarioNames()
    fun useScenario(name: String) = runtime.useScenario(name)
    fun useVehicle() = runtime.useVehicle()
    fun play() = runtime.play()
    fun pause() = runtime.pause()
    fun setSpeed(multiplier: Int) = runtime.setSpeed(multiplier)
    fun seek(tSec: Double) = runtime.seek(tSec)

    /** 실차 모드의 출발 버튼. 시뮬레이터에서는 시나리오가 여행 단계를 정한다. */
    fun startTrip() = runtime.setVehicleTrip(TripPhase.ACTIVE)
}
