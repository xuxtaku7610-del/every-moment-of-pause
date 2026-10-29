package com.emop.pause.data.net

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** 기기 토큰 저장소. 서버에는 해시만 남고 원문은 기기에만 있다. */
interface TokenStore {
    var token: String?
}

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** 일반 요청 타임아웃 5초 (api_spec §0). LLM 문장 요청은 호출 측에서 2.5초로 자른다. */
    const val CONNECT_TIMEOUT_MS = 1_500L

    fun create(baseUrl: String, tokens: TokenStore, timeoutMs: Long = 5_000): PauseApi {
        val auth = Interceptor { chain ->
            val req = chain.request()
            val token = tokens.token
            chain.proceed(if (token == null) req else req.newBuilder().header("X-Device-Token", token).build())
        }
        val http = OkHttpClient.Builder()
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            // 서버가 없으면 빨리 포기하고 캐시·내장 데이터로 (오프라인 우선).
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(auth)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PauseApi::class.java)
    }
}
