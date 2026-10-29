package com.emop.pause.trip

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.Gear
import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.Reason
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.StopType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

/** 같이가개: TripBrain이 멀미 예보(F1)와 반려견 맞춤 쉼표(F2)를 함께 돌린다. */
class TripBrainPetTest {
    private val pet = PetProfile("콩이", motionTendency = MotionTendency.HIGH)
    private val setup = TripSetup(listOf(Passenger.Dog(pet.pottyIntervalMin)), pet = pet)

    /** 직선 3km → 반경 100m 좌우 교대 커브 12km → 직선 5km. */
    private fun road(): RouteShape {
        val pts = mutableListOf(0.0 to 0.0)
        var x = 0.0
        var y = 0.0
        var h = 0.0
        var d = 0.0
        while (d < 20_000) {
            if (d in 3_000.0..15_000.0) h += (if (((d - 3_000) / 150).toInt() % 2 == 0) 1 else -1) * 10.0 / 100.0
            x += 10 * cos(h)
            y += 10 * sin(h)
            d += 10
            pts += x to y
        }
        return RouteShape.fromXY(pts)!!
    }

    @Test
    fun `F1-10 예보는 한 번만 말하고, 굽은 길에서 멀미가 오르면 놀이터 휴게소를 권한다`() {
        val total = 25_000
        val stops = listOf(RestStop("rs_pet", "가람휴게소", StopType.SERVICE_AREA, 17.0, setOf(Facility.PET_ZONE)))
        val brain = TripBrain(setup, road(), shapeStartM = 0) { stops }
        val kph = 50f
        var spoken = 0
        var firstSpeakAtM: Double? = null
        var suggestion: PlanDecision.Suggest? = null
        var traveled = 0.0
        for (t in 0..1150) {
            traveled = minOf(total.toDouble(), t * kph / 3.6)
            val eta = EtaInfo((total - traveled).toInt(), ((total - traveled) / (kph / 3.6)).toInt())
            val sample = SignalSample(t * 1000L, speedKph = kph, gear = Gear.DRIVE)
            val s = brain.tick(BrainInput(t * 1000L, 600 + t / 60, AppMode.DRIVING, DriveState.DRIVING, sample, setOf(Signal.SPEED, Signal.GEAR), eta, total))
            if (s.speakForecast != null) {
                spoken++
                if (firstSpeakAtM == null) firstSpeakAtM = traveled
                brain.onForecastSpoken(t * 1000L)
            }
            s.pending?.let { d ->
                brain.onShown(t * 1000L)
                if (d is PlanDecision.Suggest && suggestion == null) suggestion = d
            }
        }
        assertEquals(1, spoken, "같은 구간은 한 번만")
        assertTrue(firstSpeakAtM!! < 100, "처음부터 3km 앞 굽은 길을 본다: $firstSpeakAtM")
        assertTrue(brain.state.motion.index >= 40, "굽은 길 14분: ${brain.state.motion.index}")
        val d = suggestion!!
        assertEquals("rs_pet", d.stop.id)
        assertTrue(Reason.Motion in d.reasons || d.reasons.any { it is Reason.HasFacility }, d.reasons.toString())
    }

    @Test
    fun `경로 형상이 없으면 예보 없이 종방향만`() {
        val brain = TripBrain(setup) { emptyList() }
        for (t in 0..60) {
            val s = brain.tick(BrainInput(t * 1000L, 600, AppMode.DRIVING, DriveState.DRIVING, SignalSample(t * 1000L, speedKph = 80f), setOf(Signal.SPEED), EtaInfo(10_000, 450), 20_000))
            assertEquals(null, s.forecast)
        }
    }
}
