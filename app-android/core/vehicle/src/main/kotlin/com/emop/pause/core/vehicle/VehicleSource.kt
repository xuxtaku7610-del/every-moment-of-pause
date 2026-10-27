package com.emop.pause.core.vehicle

import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** 차량 신호 공급원 (api_spec §3). 실제 구현은 Vehicle SDK, 시연은 시뮬레이터. */
interface VehicleSource {
    /** 이 차에서 읽을 수 있는 신호. 엔진은 여기 없는 신호의 가중치를 0으로 둔다. */
    val capabilities: StateFlow<Set<Signal>>

    /** 1Hz 신호 묶음. */
    fun samples(): Flow<SignalSample>
}
