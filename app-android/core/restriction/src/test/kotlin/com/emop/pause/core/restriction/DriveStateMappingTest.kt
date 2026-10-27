package com.emop.pause.core.restriction

import android.car.drivingstate.CarUxRestrictions
import com.emop.pause.core.model.DriveState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DriveStateMappingTest {
    @Test
    fun `주의 분산 최적화가 필요 없으면 PARKED`() {
        assertEquals(DriveState.PARKED, CarUxDriveStateRepository.map(false, CarUxRestrictions.UX_RESTRICTIONS_BASELINE))
    }

    @Test
    fun `일부 제한이면 DRIVING`() {
        val partial = CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD or CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO
        assertEquals(DriveState.DRIVING, CarUxDriveStateRepository.map(true, partial))
    }

    @Test
    fun `모든 제한이면 RESTRICTED`() {
        assertEquals(DriveState.RESTRICTED, CarUxDriveStateRepository.map(true, CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED))
    }
}
