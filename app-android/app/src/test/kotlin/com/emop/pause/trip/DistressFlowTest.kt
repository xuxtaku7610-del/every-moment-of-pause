package com.emop.pause.trip

import com.emop.pause.data.Vet
import com.emop.pause.engine.pet.DistressAction
import com.emop.pause.engine.pet.DistressKind
import com.emop.pause.engine.pet.DistressSignals
import com.emop.pause.engine.pet.PetDistress
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.StopType
import com.emop.pause.presentation.UiMapper
import com.emop.pause.runtime.Snapshot
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DrivingRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** UPGRADE 7 (test_plan F9-06·F9-07): 정차 장소 고르기, 주행 화면 2줄·버튼 2개, 응급은 병원 안내. */
class DistressFlowTest {
    private val stops = listOf(
        RestStop("a", "지나온휴게소", StopType.SERVICE_AREA, 10.0, setOf(Facility.PET_ZONE)),
        RestStop("b", "둔내 졸음쉼터", StopType.DROWSY_SHELTER, 42.0),
        RestStop("c", "평창휴게소", StopType.SERVICE_AREA, 55.0),
        RestStop("d", "강릉휴게소", StopType.SERVICE_AREA, 70.0, setOf(Facility.PET_ZONE)),
    )

    @Test
    fun `F9-06 가장 가까운 정차는 졸음쉼터도, 쉼표 앞당김은 놀이터 휴게소, 병원·부드러운 주행은 정차 없음`() {
        assertEquals("둔내 졸음쉼터", DistressStops.pick(DistressAction.NEAREST_STOP, stops, 40.0)?.name)
        assertEquals("강릉휴게소", DistressStops.pick(DistressAction.EARLIER_REST, stops, 40.0)?.name)
        assertNull(DistressStops.pick(DistressAction.VET, stops, 40.0))
        assertNull(DistressStops.pick(DistressAction.GENTLE_DRIVE, stops, 40.0))
        assertNull(DistressStops.pick(DistressAction.NEAREST_STOP, stops, 80.0)) // 앞에 없음
    }

    @Test
    fun `F9-07 주행 화면 - 반려견 안내가 가장 먼저, 2줄·버튼 2개, 응급은 병원으로`() {
        val signals = DistressSignals(31f, 10, 40, 20, false)
        val hot = PetDistress.advise(DistressKind.PANTING, signals)
        val t = TripUi(distress = DistressState(DistressKind.PANTING, hot, stops[1], null, 3))
        val ui = UiMapper.drive(Snapshot(), t)
        val c = ui.content as DriveContent.Distress
        assertEquals("에어컨을 낮추고, 다음 안전한 곳에서 멈춰요", c.line)
        assertEquals("둔내 졸음쉼터", c.target)
        assertEquals(2, DrivingRules.buttons(ui))
        assertEquals(emptyList<String>(), DrivingRules.violations(ui))

        val sos = PetDistress.advise(DistressKind.EMERGENCY, signals)
        assertEquals(DistressAction.VET, sos.action)
        val vet = Vet("vt_1", "강릉중앙동물메디컬센터", 37.7, 128.9, 2.1, null, null, sample = false)
        val v = UiMapper.drive(Snapshot(), TripUi(distress = DistressState(DistressKind.EMERGENCY, sos, null, vet, 6))).content as DriveContent.Distress
        assertTrue(v.vet)
        assertTrue(v.target!!.length <= DrivingRules.MAX_PLACE_CHARS, v.target)
        assertEquals("가까운 동물병원으로 안내할게요", v.line)
    }
}
