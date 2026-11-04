package com.emop.pause.di

import android.content.Context
import androidx.room.Room
import com.emop.pause.BuildConfig
import com.emop.pause.data.BundledRestStops
import com.emop.pause.data.PetPlaceRepository
import com.emop.pause.data.RestStopRepository
import com.emop.pause.data.cache.PauseDatabase
import com.emop.pause.data.cache.RoomRestStopCache
import com.emop.pause.data.pet.PetDatabase
import com.emop.pause.data.pet.PetStore
import com.emop.pause.data.pet.RoomPetStore
import com.emop.pause.trip.WaitShare
import com.emop.pause.trip.WaitShareApi
import com.emop.pause.data.net.EtaShareReq
import com.emop.pause.data.net.EtaUpdateReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.emop.pause.data.net.AlongRouteReq
import com.emop.pause.data.net.ApiClient
import com.emop.pause.data.net.DeviceReq
import com.emop.pause.data.net.PauseApi
import com.emop.pause.data.net.PhraseReq
import com.emop.pause.data.net.PlacesReq
import com.emop.pause.data.net.SummaryReq
import com.emop.pause.data.net.WaitStartReq
import com.emop.pause.data.net.WaitUpdateReq
import com.emop.pause.data.net.ServerSession
import com.emop.pause.data.net.TokenStore
import com.emop.pause.voice.AndroidStt
import com.emop.pause.voice.AndroidTts
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

    /** 반려견 프로필·이동 기록은 따로 둔다: 캐시 DB처럼 지우고 다시 만들면 안 된다 (database.md §3). */
    @Provides @Singleton
    fun petStore(@ApplicationContext context: Context): PetStore =
        RoomPetStore(Room.databaseBuilder(context, PetDatabase::class.java, "pet.db").build().pets())

    /** 함께 가는 장소·동물병원 (F4·F5). 서버가 없으면 앱에 묶은 샘플로 같은 계산. */
    @Provides @Singleton
    fun petPlaces(@ApplicationContext context: Context, server: Server): PetPlaceRepository = PetPlaceRepository(
        server.session?.let { s -> AuthedApi(s) },
        { context.assets.open(PetPlaceRepository.PLACES_ASSET).bufferedReader().use { it.readText() } },
        { context.assets.open(PetPlaceRepository.VETS_ASSET).bufferedReader().use { it.readText() } },
    )

    /** 대기 공유 업로더 (F3). 서버가 없으면(릴리스·오프라인) 차 화면 대기 모드만 동작한다. */
    @Provides @Singleton
    fun waitShare(server: Server): WaitShare {
        val api = server.session?.let { s ->
            object : WaitShareApi {
                override suspend fun start(limitMin: Int) = s.call { waitStart(WaitStartReq(limitMin)) }.let { it.token to it.pageUrl }
                override suspend fun update(token: String, body: WaitUpdateReq) = s.call { waitUpdate(token, body) }.returning
                override suspend fun updateWho(token: String, body: WaitUpdateReq) = s.call { waitUpdate(token, body) }.let { it.returning to it.returningBy }
                override suspend fun end(token: String) = s.call { waitEnd(token) }
            }
        }
        return WaitShare(api, CoroutineScope(SupervisorJob() + Dispatchers.IO), BuildConfig.SHARE_BASE_URL)
    }

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
    fun audioFocus(@ApplicationContext context: Context): AudioFocus = AndroidAudioFocus(context)
}

/** 등록이 필요한 호출을 [ServerSession]으로 감싼 PauseApi. */
private class AuthedApi(private val s: ServerSession) : PauseApi {
    override suspend fun register(body: DeviceReq) = s.api.register(body)
    override suspend fun alongRoute(body: AlongRouteReq) = s.call { alongRoute(body) }
    override suspend fun phrase(body: PhraseReq) = s.call { phrase(body) }
    override suspend fun deleteMe() = s.api.deleteMe()
    override suspend fun summary(body: SummaryReq) = s.call { summary(body) }
    override suspend fun placesAlongRoute(body: PlacesReq) = s.call { placesAlongRoute(body) }
    override suspend fun placesNear(lat: Double, lng: Double, radiusMeters: Int, categories: String) = s.call { placesNear(lat, lng, radiusMeters, categories) }
    override suspend fun vetsNearby(lat: Double, lng: Double, limit: Int) = s.call { vetsNearby(lat, lng, limit) }
    override suspend fun waitStart(body: WaitStartReq) = s.call { waitStart(body) }
    override suspend fun waitUpdate(token: String, body: WaitUpdateReq) = s.call { waitUpdate(token, body) }
    override suspend fun waitEnd(token: String) = s.call { waitEnd(token) }
    override suspend fun etaShare(body: EtaShareReq) = s.call { etaShare(body) }
    override suspend fun etaUpdate(token: String, body: EtaUpdateReq) = s.call { etaUpdate(token, body) }
    override suspend fun etaStatus(token: String) = s.call { etaStatus(token) }
    override suspend fun reportShare(body: com.emop.pause.data.net.ReportShareReq) = s.call { reportShare(body) }
    override suspend fun photoStart() = s.call { photoStart() }
    override suspend fun photoFetch(token: String) = s.call { photoFetch(token) }
    override suspend fun helperAsk(body: com.emop.pause.data.net.HelperAskReq) = s.call { helperAsk(body) }
}
