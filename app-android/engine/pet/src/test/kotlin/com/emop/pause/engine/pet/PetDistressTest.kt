package com.emop.pause.engine.pet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** UPGRADE 7 (test_plan F9-01~F9-03): "콩이가 이상해요" — 규칙 표의 각 줄, 주행 중 2줄 제한, 응급은 병원으로. */
class PetDistressTest {
    private val cool = DistressSignals(cabinTempC = 23f, month = 10, drivingMin = 60, motionIndex = 20, motionRising = false)

    @Test
    fun `F9-01 말 → 증상 분류 (응급·앞좌석이 먼저)`() {
        assertEquals(DistressKind.PANTING, PetDistress.classify("콩이가 헥헥거려"))
        assertEquals(DistressKind.PANTING, PetDistress.classify("헐떡여"))
        assertEquals(DistressKind.VOMIT, PetDistress.classify("콩이 토했어"))
        assertEquals(DistressKind.NAUSEA, PetDistress.classify("침을 흘려"))
        assertEquals(DistressKind.NAUSEA, PetDistress.classify("자꾸 하품하고 입맛 다셔"))
        assertEquals(DistressKind.ANXIOUS, PetDistress.classify("콩이 낑낑대"))
        assertEquals(DistressKind.ANXIOUS, PetDistress.classify("떨고 있어"))
        assertEquals(DistressKind.CLIMBING, PetDistress.classify("앞좌석으로 넘어오려고 해"))
        assertEquals(DistressKind.EMERGENCY, PetDistress.classify("콩이가 축 처졌어"))
        assertEquals(DistressKind.EMERGENCY, PetDistress.classify("계속 토해"))
        assertEquals(DistressKind.EMERGENCY, PetDistress.classify("숨이 너무 가빠"))
        assertEquals(DistressKind.UNSURE, PetDistress.classify("콩이 이상해"))
        assertNull(PetDistress.classify("여기서 쉴게요"))
        assertNull(PetDistress.classify("나중에"))
    }

    @Test
    fun `F9-02 규칙 표 각 줄 (증상 + 함께 보는 신호 → 1줄과 다음 행동)`() {
        // 헐떡임 + 실내 온도 높음 → 에어컨·가장 가까운 정차
        val hot = PetDistress.advise(DistressKind.PANTING, cool.copy(cabinTempC = 29f))
        assertEquals("에어컨을 낮추고, 다음 안전한 곳에서 멈춰요", hot.line)
        assertEquals(DistressAction.NEAREST_STOP, hot.action)
        assertEquals("panting_hot", hot.care)
        // 헐떡임 + 온도 모름 + 여름 → 더운 쪽으로
        assertEquals(DistressAction.NEAREST_STOP, PetDistress.advise(DistressKind.PANTING, cool.copy(cabinTempC = null, month = 8)).action)
        // 헐떡임 + 온도 정상 + 출발 직후 → 불안, 부드럽게
        val early = PetDistress.advise(DistressKind.PANTING, cool.copy(drivingMin = 8))
        assertEquals("불안해서 그럴 수 있어요. 부드럽게 달릴게요", early.line)
        assertEquals(DistressAction.GENTLE_DRIVE, early.action)
        assertTrue(early.boost > 1.0)
        // 침·하품 + 멀미 지수 상승 → 창문, 다음 쉼표 앞당김
        val nausea = PetDistress.advise(DistressKind.NAUSEA, cool.copy(motionIndex = 45, motionRising = true))
        assertEquals("멀미 초기일 수 있어요. 창문을 조금 열어요", nausea.line)
        assertEquals(DistressAction.EARLIER_REST, nausea.action)
        // 구토 → 다음 쉼터
        val vomit = PetDistress.advise(DistressKind.VOMIT, cool)
        assertEquals("다음 쉼터에서 멈춰요. 지금은 운전에 집중해요", vomit.line)
        assertEquals(DistressAction.NEAREST_STOP, vomit.action)
        // 낑낑·떨림 → 다음 쉼표
        val anxious = PetDistress.advise(DistressKind.ANXIOUS, cool)
        assertEquals("정차 후 켄넬과 벨트를 확인해요", anxious.line)
        assertEquals(DistressAction.EARLIER_REST, anxious.action)
        // 앞좌석 → 가장 가까운 정차 (최우선)
        val climb = PetDistress.advise(DistressKind.CLIMBING, cool)
        assertEquals("안전한 곳에 먼저 멈춰요", climb.line)
        assertEquals(DistressAction.NEAREST_STOP, climb.action)
        assertTrue(climb.priority > vomit.priority)
        // 응급 → 동물병원
        val sos = PetDistress.advise(DistressKind.EMERGENCY, cool)
        assertEquals("가까운 동물병원으로 안내할게요", sos.line)
        assertEquals(DistressAction.VET, sos.action)
        assertTrue(sos.priority > climb.priority)
    }

    @Test
    fun `F9-03 주행 중 안내는 2줄(48자) 이하, 장소는 버튼에`() {
        val all = DistressKind.entries.flatMap { k ->
            listOf(cool, cool.copy(cabinTempC = 31f), cool.copy(drivingMin = 5), cool.copy(motionRising = true, motionIndex = 50)).map { PetDistress.advise(k, it) }
        }
        all.forEach { assertTrue(it.line.length <= PetDistress.LINE_MAX, "${it.line} ${it.line.length}자") }
        all.forEach { assertTrue(it.care.isNotBlank()) }
    }

    @Test
    fun `F9-04 기록용 증상 이름`() {
        assertEquals("헐떡임", PetDistress.label(DistressKind.PANTING))
        assertEquals("구토", PetDistress.label(DistressKind.VOMIT))
    }
}
