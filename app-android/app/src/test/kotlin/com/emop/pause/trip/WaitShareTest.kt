package com.emop.pause.trip

import com.emop.pause.data.net.WaitUpdateReq
import com.emop.pause.engine.pet.WaitEndReason
import com.emop.pause.engine.pet.WaitEvent
import com.emop.pause.engine.pet.WaitStatus
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WaitShareTest {
    private class FakeApi(val fail: Boolean = false) : WaitShareApi {
        val updates = mutableListOf<WaitUpdateReq>()
        var ended: String? = null
        override suspend fun start(limitMin: Int): Pair<String, String> {
            if (fail) error("서버 없음")
            return "tok" to "/wait/tok"
        }
        override suspend fun update(token: String, body: WaitUpdateReq) { updates += body }
        override suspend fun end(token: String) { ended = token }
    }

    @Test
    fun `F3-07 시작하면 토큰을 받고 10초마다 올리고, 끝나면 지운다`() = runTest {
        val api = FakeApi()
        val share = WaitShare(api, this, "http://192.168.0.10:8000/")
        val st = WaitStatus(active = true, startedMs = 0, elapsedSec = 0, limitSec = 600, batteryStart = 62, batteryNow = 62)
        share.onChanged(st, WaitEvent.Started)
        runCurrent()
        assertEquals("http://192.168.0.10:8000/wait/tok", share.state.value.pageUrl)
        assertTrue(share.state.value.connected)
        share.onChanged(st.copy(elapsedSec = 15, batteryNow = 61), null)
        advanceTimeBy(WaitShare.UPLOAD_MS + 1)
        assertEquals(15, api.updates.last().elapsedSec)
        assertEquals(61, api.updates.last().batteryPercent)
        share.onChanged(WaitStatus(), WaitEvent.Ended(WaitEndReason.USER))
        runCurrent()
        assertEquals("tok", api.ended)
        assertNull(share.state.value.pageUrl)
    }

    @Test
    fun `F3-03 초과하면 바로 overdue로 올린다`() = runTest {
        val api = FakeApi()
        val share = WaitShare(api, this)
        share.onChanged(WaitStatus(active = true, limitSec = 600), WaitEvent.Started)
        runCurrent()
        share.onChanged(WaitStatus(active = true, elapsedSec = 600, limitSec = 600, overdue = true), WaitEvent.Overdue)
        runCurrent()
        assertEquals("overdue", api.updates.last().state)
        share.onChanged(WaitStatus(), WaitEvent.Ended(WaitEndReason.DRIVING))
        runCurrent()
    }

    @Test
    fun `F3-09 서버가 없거나 실패하면 연결 안 됨`() = runTest {
        val none = WaitShare(null, this)
        none.onChanged(WaitStatus(active = true), WaitEvent.Started)
        assertFalse(none.state.value.connected)
        val failing = WaitShare(FakeApi(fail = true), this)
        failing.onChanged(WaitStatus(active = true), WaitEvent.Started)
        runCurrent()
        assertFalse(failing.state.value.connected)
        assertNull(failing.state.value.pageUrl)
    }
}
