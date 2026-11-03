package com.emop.pause.engine.pet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 긴급 도움 (2026-11-03, test_plan F10-01~F10-03): 응급 말 → EMERGENCY + 상황 4가지. 기존 분류(PetDistressTest)는 그대로.
 * 구토 단독은 응급이 아니다(VOMIT). 반려견 이름 "초코"·"포도"는 위험 물질로 보지 않는다.
 */
class EmergencyKindTest {
    private val cases: List<Pair<String, EmergencyKind?>> = listOf(
        // 호흡 곤란
        "콩이가 숨을 못 쉬어" to EmergencyKind.BREATHING,
        "숨이 가빠" to EmergencyKind.BREATHING,
        "숨이 너무 가빠 보여" to EmergencyKind.BREATHING,
        "헐떡이며 잇몸이 파래" to EmergencyKind.BREATHING,
        "잇몸이 파랗게 됐어" to EmergencyKind.BREATHING,
        "혀가 보라색이야" to EmergencyKind.BREATHING,
        "숨쉬기 힘들어해" to EmergencyKind.BREATHING,
        "호흡이 이상해" to EmergencyKind.BREATHING,
        "숨을 안 쉬는 것 같아" to EmergencyKind.BREATHING,
        "목에 뭐가 걸렸나 봐 켁켁대" to EmergencyKind.BREATHING,
        "숨이차" to EmergencyKind.BREATHING,
        // 경련·의식 이상
        "콩이가 경련해" to EmergencyKind.SEIZURE,
        "발작해" to EmergencyKind.SEIZURE,
        "갑자기 쓰러졌어" to EmergencyKind.SEIZURE,
        "정신을 잃었어" to EmergencyKind.SEIZURE,
        "축 처졌어" to EmergencyKind.SEIZURE,
        "콩이가 축처졌어" to EmergencyKind.SEIZURE,
        "기절한 것 같아" to EmergencyKind.SEIZURE,
        "의식이 없어" to EmergencyKind.SEIZURE,
        "불러도 반응이 없어" to EmergencyKind.SEIZURE,
        "몸을 떨면서 경련을 해" to EmergencyKind.SEIZURE,
        // 외상·출혈
        "피가 나" to EmergencyKind.BLEEDING,
        "다리에서 피가 나고 있어" to EmergencyKind.BLEEDING,
        "콩이 다쳤어" to EmergencyKind.BLEEDING,
        "다른 개한테 물렸어" to EmergencyKind.BLEEDING,
        "절뚝거려 못 걸어" to EmergencyKind.BLEEDING,
        "발바닥이 찢어졌어" to EmergencyKind.BLEEDING,
        "상처가 깊어" to EmergencyKind.BLEEDING,
        "피를 흘려" to EmergencyKind.BLEEDING,
        "다리가 부러진 것 같아" to EmergencyKind.BLEEDING,
        "휴게소에서 차에 치였어" to EmergencyKind.BLEEDING,
        // 위험 물질 섭취
        "콩이 초콜릿 먹었어" to EmergencyKind.TOXIN,
        "초콜렛을 먹은 것 같아" to EmergencyKind.TOXIN,
        "포도 먹었어" to EmergencyKind.TOXIN,
        "포도를 몇 알 주워 먹었어" to EmergencyKind.TOXIN,
        "양파 먹었어" to EmergencyKind.TOXIN,
        "자일리톨 껌 먹었어" to EmergencyKind.TOXIN,
        "껌을 삼켰어" to EmergencyKind.TOXIN,
        "약을 먹었어" to EmergencyKind.TOXIN,
        "사람 약을 삼켰어" to EmergencyKind.TOXIN,
        "쥐약" to EmergencyKind.TOXIN,
        "쥐약 먹은 것 같아" to EmergencyKind.TOXIN,
        "바닥에서 뭘 주워 먹었어" to EmergencyKind.TOXIN,
        "담배꽁초를 먹었어" to EmergencyKind.TOXIN,
        "세제를 핥아 먹었어" to EmergencyKind.TOXIN,
        // 기존 응급 말 — 응급이지만 종류는 모름(기존 카드 emergency)
        "계속 토해" to null,
        "또 토했어" to null,
    )

    @Test
    fun `F10-01 응급 말 40개 이상 → EMERGENCY와 상황 종류`() {
        assertTrue(cases.size >= 40, "${cases.size}개")
        val fails = cases.filter { (t, k) -> PetDistress.classify(t) != DistressKind.EMERGENCY || PetDistress.emergencyKind(t) != k }
            .map { (t, k) -> "$t → ${PetDistress.classify(t)}/${PetDistress.emergencyKind(t)} (기대 EMERGENCY/$k)" }
        assertEquals(emptyList<String>(), fails)
        println("F10-01 응급 분류 ${cases.size}/${cases.size} 통과")
    }

    @Test
    fun `F10-02 응급이 아닌 말은 그대로 (구토 단독, 반려견 이름 초코·포도, 보호자가 준 약)`() {
        val same = listOf(
            "콩이 토했어" to DistressKind.VOMIT,
            "구토했어" to DistressKind.VOMIT,
            "헐떡여" to DistressKind.PANTING,
            "콩이가 헥헥거려" to DistressKind.PANTING,
            "침을 흘려" to DistressKind.NAUSEA,
            "떨고 있어" to DistressKind.ANXIOUS,
            "앞좌석으로 넘어오려고 해" to DistressKind.CLIMBING,
            "콩이 이상해" to DistressKind.UNSURE,
            "초코가 토했어" to DistressKind.VOMIT,
            "포도가 낑낑대" to DistressKind.ANXIOUS,
            "초코가 밥 먹었어" to null,
            "멀미약 먹였어" to DistressKind.NAUSEA, // 기존 규칙("멀미") 그대로 — 위험 물질 아님
            "여기서 쉴게요" to null,
            "나중에" to null,
            "잠깐 다녀올게" to null,
        )
        same.forEach { (t, k) -> assertEquals(k, PetDistress.classify(t), t) }
        same.forEach { (t, _) -> assertNull(PetDistress.emergencyKind(t), t) }
    }

    @Test
    fun `F10-03 응급은 문구·행동이 같고 카드와 기록 이름만 상황별`() {
        val s = DistressSignals(23f, 10, 60, 20, false)
        EmergencyKind.entries.forEach { k ->
            val a = PetDistress.advise(DistressKind.EMERGENCY, s, k)
            assertEquals("가까운 동물병원으로 안내할게요", a.line)
            assertEquals(DistressAction.VET, a.action)
            assertEquals(k.id, a.care)
        }
        assertEquals("emergency", PetDistress.advise(DistressKind.EMERGENCY, s).care)
        assertEquals("위험 물질 섭취", PetDistress.label(DistressKind.EMERGENCY, EmergencyKind.TOXIN))
        assertEquals("축 처짐·호흡 곤란", PetDistress.label(DistressKind.EMERGENCY, null))
        assertEquals("구토", PetDistress.label(DistressKind.VOMIT, EmergencyKind.TOXIN))
        assertEquals(EmergencyKind.TOXIN, EmergencyKind.of("toxin"))
    }
}
