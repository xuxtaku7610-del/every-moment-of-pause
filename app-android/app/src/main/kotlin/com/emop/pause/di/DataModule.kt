package com.emop.pause.di

import android.content.Context
import androidx.room.Room
import com.emop.pause.BuildConfig
import com.emop.pause.data.BundledRestStops
import com.emop.pause.data.RestStopRepository
import com.emop.pause.data.cache.PauseDatabase
import com.emop.pause.data.cache.RoomRestStopCache
import com.emop.pause.data.net.AlongRouteReq
import com.emop.pause.data.net.ApiClient
import com.emop.pause.data.net.DeviceReq
import com.emop.pause.data.net.PauseApi
import com.emop.pause.data.net.PhraseReq
import com.emop.pause.data.net.ServerSession
import com.emop.pause.data.net.TokenStore
import com.emop.pause.voice.AndroidStt
import com.emop.pause.voice.AndroidTts
import com.emop.pause.trip.AndroidNapAlarm
import com.emop.pause.trip.NapAlarm
import com.emop.pause.voice.AndroidAudioFocus
import com.emop.pause.voice.AudioFocus
import com.emop.pause.voice.PhraseProvider
import com.emop.pause.voice.SpeechInput
import com.emop.pause.voice.SpeechOutput
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 기기 토큰은 앱 전용 SharedPreferences에만 둔다. */
class PrefsTokenStore(context: Context) : TokenStore {
    private val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
    override var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()
}

/** 서버 주소가 없으면(릴리스·오프라인) session = null. Dagger는 null을 주입하지 않으므로 감싼다. */
class Server(val session: ServerSession?)

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun server(@ApplicationContext context: Context): Server {
        val base = BuildConfig.SERVER_BASE_URL.takeIf { it.isNotBlank() } ?: return Server(null)
        val tokens = PrefsTokenStore(context)
        return Server(ServerSession(ApiClient.create(base, tokens), tokens, BuildConfig.VERSION_NAME))
    }

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): PauseDatabase =
        // 캐시는 출발 때마다 다시 채우므로 스키마가 바뀌면 지우고 새로 만든다 (v2: 노선·방향 추가).
        Room.databaseBuilder(context, PauseDatabase::class.java, "pause.db").fallbackToDestructiveMigration(true).build()

    @Provides @Singleton
    fun restStops(@ApplicationContext context: Context, db: PauseDatabase, server: Server): RestStopRepository {
        val api = server.session?.let { s -> AuthedApi(s) }
        return RestStopRepository(api, RoomRestStopCache(db.restStops())) {
            context.assets.open(BundledRestStops.ASSET).bufferedReader().use { BundledRestStops.parse(it.readText()) }
        }
    }

    @Provides @Singleton
    fun phrases(server: Server): PhraseProvider =
        PhraseProvider(server.session?.let { s -> { facts -> s.call { phrase(PhraseReq("rest_suggestion", facts)).text } } })

    @Provides @Singleton
    fun speechOut(@ApplicationContext context: Context): SpeechOutput = AndroidTts(context)

    @Provides @Singleton
    fun speechIn(@ApplicationContext context: Context): SpeechInput = AndroidStt(context)

    @Provides @Singleton
    fun napAlarm(@ApplicationContext context: Context): NapAlarm = AndroidNapAlarm(context)

    @Provides @Singleton
    fun audioFocus(@ApplicationContext context: Context): AudioFocus = AndroidAudioFocus(context)
}

/** 등록이 필요한 호출을 [ServerSession]으로 감싼 PauseApi. */
private class AuthedApi(private val s: ServerSession) : PauseApi {
    override suspend fun register(body: DeviceReq) = s.api.register(body)
    override suspend fun alongRoute(body: AlongRouteReq) = s.call { alongRoute(body) }
    override suspend fun phrase(body: PhraseReq) = s.call { phrase(body) }
    override suspend fun deleteMe() = s.api.deleteMe()
}
