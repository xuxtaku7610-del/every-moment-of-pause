package com.emop.pause.core.restriction

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.UxLimits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CarUxRestrictionsManager → DriveState. 입력은 CarUxRestrictions뿐이다 (architecture §5.1).
 *
 * - 주의 분산 최적화가 필요 없음 → PARKED
 * - 모든 제한이 걸림(FULLY_RESTRICTED) → RESTRICTED
 * - 그 외 제한 → DRIVING
 *
 * 차량 서비스에 연결할 수 없으면 PARKED로 두고 로그만 남긴다.
 */
class CarUxDriveStateRepository(context: Context) : DriveStateRepository {
    private val state = MutableStateFlow(DriveState.PARKED)
    override val driveState: StateFlow<DriveState> = state.asStateFlow()
    private val limits = MutableStateFlow(UxLimits.NONE)
    override val uxLimits: StateFlow<UxLimits> = limits.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    init {
        try {
            car = Car.createCar(context)
            manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            manager?.let { m ->
                m.currentCarUxRestrictions?.let(::publish)
                m.registerListener(::publish)
            }
        } catch (e: Exception) {
            Log.w(TAG, "CarUxRestrictions 연결 실패, PARKED 유지: ${e.javaClass.simpleName}")
        }
    }

    private fun publish(r: CarUxRestrictions) {
        state.value = map(r)
        limits.value = limitsOf(r)
    }

    fun release() {
        manager?.unregisterListener()
        car?.disconnect()
    }

    companion object {
        private const val TAG = "DriveState"

        fun map(r: CarUxRestrictions): DriveState = map(r.isRequiresDistractionOptimization, r.activeRestrictions)

        /** 최적화가 필요 없으면(정차) 제한 없음으로 본다. */
        fun limitsOf(r: CarUxRestrictions): UxLimits =
            if (!r.isRequiresDistractionOptimization) UxLimits.NONE else UxLimits(r.activeRestrictions, r.maxRestrictedStringLength)

        fun map(requiresOptimization: Boolean, activeRestrictions: Int): DriveState {
            val full = CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED
            return when {
                !requiresOptimization -> DriveState.PARKED
                (activeRestrictions and full) == full -> DriveState.RESTRICTED
                else -> DriveState.DRIVING
            }
        }
    }
}
