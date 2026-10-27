package com.emop.pause.core.restriction

import com.emop.pause.core.model.DriveState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** CarUxRestrictions → DriveState (architecture §3.1 `core:restriction`). */
interface DriveStateRepository {
    val driveState: StateFlow<DriveState>
}

/**
 * M0 스텁. 항상 PARKED에서 시작하고 디버그·시뮬레이터가 값을 넣는다.
 *
 * TODO(M1): CarUxRestrictionsManager 구독 구현으로 교체.
 */
class StubDriveStateRepository(initial: DriveState = DriveState.PARKED) : DriveStateRepository {
    private val state = MutableStateFlow(initial)
    override val driveState: StateFlow<DriveState> = state.asStateFlow()

    fun set(value: DriveState) {
        state.value = value
    }
}
