package com.emop.pause.core.vehicle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 차량 신호 공급원 (api_spec §3). 실제 구현은 Vehicle SDK, 시연은 시뮬레이터.
 *
 * TODO(pleos-sdk): M1에서 Vehicle SDK aar의 실제 시그니처를 확인한 뒤 Signal·SignalSample을 채우고
 * PleosVehicleSource를 구현한다. 확인 전에는 API를 추측해 쓰지 않는다.
 */
interface VehicleSource {
    val capabilities: StateFlow<Set<String>>
    fun samples(): Flow<Map<String, Any?>>
}
