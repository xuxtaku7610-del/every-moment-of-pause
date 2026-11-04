package com.emop.pause.presentation

import com.emop.pause.MainViewModel
import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.runtime.Snapshot
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.VoicePhase
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DrivingRules
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.VoiceSheetState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** test_plan F11-01·F11-02 (v10-1 1·2번): 분할 창 앱 바 한 줄, [눌러서 말하기]와 버튼 2개 규칙. */
class DriveV10Test {
    private val driving = Snapshot(mode = AppMode.DRIVING, driveState = DriveState.DRIVING, clockMinute = 10 * 60 + 26, eta = EtaInfo(27_000, 19 * 60))

    @Test
    fun `F11-01 앱 바 오른쪽 = 남은 거리 · 도착 시각 한 줄 (회전·도착 카드 대신)`() {
        assertEquals("27km · 도착 오전 10:45", UiMapper.barLine(10 * 60 + 26, EtaInfo(27_000, 19 * 60)))
        assertEquals("27km · 도착 오전 10:45", UiMapper.drive(driving, TripUi()).barLine)
        assertNull(UiMapper.barLine(600, null))
        assertNull(UiMapper.barLine(600, EtaInfo(0, 0)), "도착 뒤에는 줄을 숨긴다")
    }

    @Test
    fun `F11-02 눌러서 말하기 - 평소 1개, 듣는 중·버튼 카드·RESTRICTED면 숨김`() {
        val calm = UiMapper.drive(driving, TripUi())
        assertTrue(DrivingRules.talkShown(calm))
        assertEquals(1, DrivingRules.buttons(calm))
        // 듣는 중이면 "듣고 있어요…" 줄이 대신
        val talking = UiMapper.drive(driving, TripUi(talking = true))
        assertEquals(VoiceSheetState.LISTENING, talking.voice)
        assertFalse(talking.talk)
        assertEquals(0, DrivingRules.buttons(talking))
        // 쉼표 추천 응답 창이 듣는 중이어도 숨김
        assertFalse(UiMapper.drive(driving, TripUi(voice = VoicePhase.LISTENING)).talk)
        // RESTRICTED
        assertFalse(UiMapper.drive(driving.copy(driveState = DriveState.RESTRICTED), TripUi()).talk)
        // 버튼 2개 카드(쉼표 추천·주변 추천·이상해요)는 기본값으로도 숨김 → 화면 버튼 2개를 넘지 않는다
        listOf(Fixtures.suggest, Fixtures.nearby).forEach {
            assertFalse(DrivingRules.talkShown(it), it.content.toString())
            assertEquals(2, DrivingRules.buttons(it))
        }
        // 마이크 거절이면 버튼 대신 글자 힌트
        assertEquals(0, DrivingRules.buttons(calm.copy(micDenied = true)))
        assertTrue(Fixtures.drive.content is DriveContent.Calm || Fixtures.drive.content is DriveContent.Heading)
    }

    @Test
    fun `F11-03 기존 분류가 알아듣는 말만 넘기고 모르는 말은 힌트 한 줄`() {
        listOf("콩이 토했어", "콩이 초콜릿 먹었어", "잠깐 다녀올게", "조용히 해 줘", "오늘은 안 타요", "응").forEach {
            assertTrue(MainViewModel.understood(it), it)
        }
        assertFalse(MainViewModel.understood("오늘 날씨 어때"))
    }

    @Test
    fun `F11-06 적게·조용히로 예보를 거르면 예보 카드도 안 뜬다`() {
        val t = TripUi(forecastMuted = true)
        assertFalse(UiMapper.drive(driving, t).content is DriveContent.Forecast)
    }
}
