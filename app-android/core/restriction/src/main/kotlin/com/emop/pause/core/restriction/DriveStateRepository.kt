package com.emop.pause.core.restriction

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.UxLimits
import kotlinx.coroutines.flow.StateFlow

/** CarUxRestrictions → DriveState (architecture §3.1 `core:restriction`). */
interface DriveStateRepository {
    val driveState: StateFlow<DriveState>

    /** 활성 제한 플래그 (NO_VIDEO·LIMIT_STRING_LENGTH·NO_KEYBOARD·NO_SETUP …). */
    val uxLimits: StateFlow<UxLimits>
}
