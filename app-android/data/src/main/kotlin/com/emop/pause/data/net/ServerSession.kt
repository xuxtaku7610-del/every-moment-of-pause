package com.emop.pause.data.net

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/**
 * 토큰이 없으면 먼저 기기를 등록하고(api_spec §2.2), 서버가 토큰을 모르면(401) 한 번 다시 등록한다.
 */
class ServerSession(val api: PauseApi, private val tokens: TokenStore, private val appVersion: String) {
    private val lock = Mutex()

    suspend fun <T> call(block: suspend PauseApi.() -> T): T {
        ensureToken()
        return try {
            api.block()
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
            lock.withLock { tokens.token = null }
            ensureToken()
            api.block()
        }
    }

    /**
     * 내 데이터 삭제: 토큰이 있으면 서버에서 기기를 지우고(204, 이미 없으면 401도 성공으로 본다) 토큰을 버린다.
     * 서버에 닿지 못하면 false — 기기 쪽 데이터는 이미 지웠고, 토큰은 남겨 다음에 다시 시도할 수 있다.
     */
    suspend fun forget(): Boolean = lock.withLock {
        if (tokens.token == null) return@withLock true
        val ok = runCatching { api.deleteMe() }.fold({ true }, { it is HttpException && it.code() == 401 })
        if (ok) tokens.token = null
        ok
    }

    private suspend fun ensureToken() = lock.withLock {
        if (tokens.token == null) tokens.token = api.register(DeviceReq("pleos-connect", appVersion)).token
    }
}
