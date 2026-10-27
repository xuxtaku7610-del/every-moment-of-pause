package com.emop.pause

import androidx.lifecycle.ViewModel
import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.restriction.DriveStateRepository
import com.emop.pause.core.restriction.StubDriveStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** M0: AppMode는 수동 전환. TODO(M1): AppModeReducer로 계산. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val driveStateRepository: DriveStateRepository,
) : ViewModel() {
    private val mode = MutableStateFlow(AppMode.PREPARE)
    val appMode: StateFlow<AppMode> = mode.asStateFlow()
    val driveState: StateFlow<DriveState> = driveStateRepository.driveState

    fun setAppMode(value: AppMode) {
        mode.value = value
    }

    fun setDriveState(value: DriveState) {
        (driveStateRepository as? StubDriveStateRepository)?.set(value)
    }
}
