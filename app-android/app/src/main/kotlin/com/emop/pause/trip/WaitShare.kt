package com.emop.pause.trip

import com.emop.pause.data.net.WaitStartReq
import com.emop.pause.data.net.WaitUpdateReq
import com.emop.pause.engine.pet.WaitEvent
import com.emop.pause.engine.pet.WaitStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 보호자 폰 공유 상태 (차 화면에 주소·연결 상태로 보인다). */
data class WaitShareState(
    /** 폰에서 열 주소 (예: http://192.168.0.10:8000/wait/토큰). 서버가 없으면 null. */
    val pageUrl: String? = null,
    /** 마지막 업로드가 성공했는지. 서버가 없거나 실패하면 false → "폰 공유 연결 안 됨" (F3-09). */
    val connected: Boolean = false,
)

/** 서버 호출부. 테스트에서 가짜로 바꾼다. */
interface WaitShareApi {
    suspend fun start(limitMin: Int): Pair<String, String>
    suspend fun update(token: String, body: WaitUpdateReq)
    suspend fun end(token: String)
}

/**
 * 대기 모드 상태를 서버에 올린다 (CONCEPT F3, api_spec §2.8). **위치는 보내지 않는다.**
 * 시작하면 토큰을 받고 [UPLOAD_MS]마다 마지막 상태를 PUT, 끝나면 DELETE. 서버가 없거나 실패해도 차 화면 대기 모드는 그대로다.
 * [shareBase]는 폰이 닿는 서버 주소(같은 Wi-Fi의 노트북 IP). 에뮬레이터용 10.0.2.2는 폰에서 열리지 않으므로 따로 받는다.
 */
class WaitShare(
    private val api: WaitShareApi?,
    private val scope: CoroutineScope,
    @Volatile var shareBase: String = "",
) {
    private val _state = MutableStateFlow(WaitShareState())
    val state: StateFlow<WaitShareState> = _state.asStateFlow()

    @Volatile private var latest: WaitStatus = WaitStatus()
    private var token: String? = null
    private var loop: Job? = null

    fun onChanged(status: WaitStatus, event: WaitEvent?) {
        latest = status
        when (event) {
            WaitEvent.Started -> begin(status)
            is WaitEvent.Ended -> finish()
            WaitEvent.Overdue -> scope.launch { push() }
            null -> Unit
        }
    }

    private fun begin(status: WaitStatus) {
        val a = api ?: return _state.update { WaitShareState(null, false) }
        loop?.cancel()
        loop = scope.launch {
            val started = runCatching { a.start(maxOf(1, status.limitSec / 60)) }.getOrNull()
            if (started == null) {
                _state.value = WaitShareState(null, false)
                return@launch
            }
            token = started.first
            _state.value = WaitShareState(shareBase.trimEnd('/').takeIf { it.isNotBlank() }?.let { it + started.second } ?: started.second, true)
            while (true) {
                push()
                delay(UPLOAD_MS)
            }
        }
    }

    private suspend fun push() {
        val a = api ?: return
        val t = token ?: return
        val s = latest
        val body = WaitUpdateReq(if (s.overdue) "overdue" else "waiting", s.elapsedSec, maxOf(1, s.limitSec / 60), s.batteryNow, s.cabinTempC)
        val ok = runCatching { a.update(t, body) }.isSuccess
        _state.update { it.copy(connected = ok) }
    }

    private fun finish() {
        loop?.cancel()
        loop = null
        val t = token
        token = null
        _state.value = WaitShareState()
        val a = api ?: return
        if (t != null) scope.launch { runCatching { a.end(t) } }
    }

    companion object {
        const val UPLOAD_MS = 10_000L
    }
}
