package com.emop.pause.sim

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.RouteInfo
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import com.emop.pause.core.model.TripPhase
import com.emop.pause.core.navi.NaviSource
import com.emop.pause.core.restriction.DriveStateRepository
import com.emop.pause.core.vehicle.VehicleSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 실제 SDK 구현과 같은 인터페이스로 시나리오 값을 주입한다 (architecture 원칙 5). */
class SimVehicleSource(private val player: ScenarioPlayer) : VehicleSource {
    override val capabilities: StateFlow<Set<Signal>> = MutableStateFlow(player.scenario.capabilities).asStateFlow()
    override fun samples(): Flow<SignalSample> = player.frame.map { it.sample }
}

class SimNaviSource(private val player: ScenarioPlayer) : NaviSource {
    private val _waypoints = MutableStateFlow<List<String>>(emptyList())
    val waypoints: StateFlow<List<String>> = _waypoints.asStateFlow()

    override fun route(): Flow<RouteInfo?> = player.frame.map { f ->
        if (f.trip != TripPhase.ACTIVE) return@map null
        val r = player.scenario.route
        RouteInfo(r.destinationName, r.destination.toModel(), r.totalDistanceM, r.points.map { it.toModel() })
    }.distinctUntilChanged()

    override fun eta(): Flow<EtaInfo?> = player.frame.map { f ->
        val d = f.remainDistanceM
        val t = f.remainTimeSec
        if (f.trip != TripPhase.ACTIVE || d == null || t == null) null else EtaInfo(d, t)
    }

    override suspend fun addWaypoint(lat: Double, lng: Double, name: String): Result<Unit> {
        _waypoints.value = _waypoints.value + name
        return Result.success(Unit)
    }
}

class SimDriveStateRepository(player: ScenarioPlayer, scope: CoroutineScope) : DriveStateRepository {
    override val driveState: StateFlow<DriveState> =
        player.frame.map { it.driveState }.stateIn(scope, SharingStarted.Eagerly, player.frame.value.driveState)
}
