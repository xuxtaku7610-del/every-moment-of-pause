package com.emop.pause.core.restriction

import com.emop.pause.core.model.DriveState
import kotlinx.coroutines.flow.StateFlow

/** CarUxRestrictions → DriveState (architecture §3.1 `core:restriction`). */
interface DriveStateRepository {
    val driveState: StateFlow<DriveState>
}
