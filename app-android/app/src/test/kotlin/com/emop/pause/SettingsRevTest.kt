package com.emop.pause

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * test_plan V10-05: 설정 스위치(음성으로 제안·움직임 줄이기·안내 빈도)는 [AppState.settingsRev]를 늘려 화면 상태를 새로 낸다.
 * 예전 `copy()`만 하던 bump는 같은 값이라 StateFlow가 버려, 스위치가 설정 화면을 다시 열 때까지 그대로 보였다(11/05 캡처 중 발견).
 */
class SettingsRevTest {
    @Test
    fun `같은 값 copy는 새 상태가 아니고 settingsRev를 늘리면 새 상태다`() {
        val flow = MutableStateFlow(AppState())
        val before = flow.value
        flow.update { it.copy() }
        assertEquals(before, flow.value)
        flow.update { it.copy(settingsRev = it.settingsRev + 1) }
        assertNotEquals(before, flow.value)
        assertEquals(1, flow.value.settingsRev)
    }
}
