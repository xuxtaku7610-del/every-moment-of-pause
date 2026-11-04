package com.emop.pause.service

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.emop.pause.MainActivity
import com.emop.pause.R
import com.emop.pause.core.model.TripPhase
import com.emop.pause.runtime.SignalRuntime
import com.emop.pause.trip.TripCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 여행 중(출발 ~ 도착) 포그라운드 서비스 (v10-1 3번, 사용자 결정 11/04): 내비가 전체 화면이어도 엔진·차량 신호·NaviHelper·음성이 계속 돈다.
 * 실제 일은 싱글턴(SignalRuntime·TripCoordinator)이 하고, 서비스는 프로세스를 앞쪽 우선순위로 붙잡고 상주 알림을 띄운다.
 *
 * 유형 (developer.android.com/develop/background-work/services/fgs/service-types, 11/04 확인):
 * - `specialUse` — 엔진·안내 음성 유지에 맞는 표준 유형이 없다(매니페스트 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`).
 * - `microphone` — 앱이 뒤에 있을 때 "여기서 쉴게요" 듣기. 앱이 **보일 때 시작**해야 하고 RECORD_AUDIO 허용이 있어야 붙인다.
 * 알림: AAOS는 일반 앱의 heads-up을 통화·메시지·내비 범주에만 허용한다(developer.android.com/training/cars/notifications).
 * 같이가개 안내는 셋 다 아니므로 heads-up을 쓰지 않고, 음성 + 상주 알림 한 줄(알림 센터)을 갱신한다. 누를 때만 앱을 앞으로.
 */
class TripService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mic = intent?.getBooleanExtra(EXTRA_MIC, false) == true &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: ONGOING
        val n = notification(this, intent?.getStringExtra(EXTRA_TITLE) ?: TITLE, text)
        val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or (if (mic) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIFICATION_ID, n, type) else startForeground(NOTIFICATION_ID, n)
        }.onFailure {
            // 뒤에서 마이크 유형으로 시작하려 했거나(while-in-use 제한) 시작이 막힘 → 마이크 없이 한 번 더
            Log.w(TAG, "startForeground mic=$mic 실패: ${it.javaClass.simpleName} ${it.message}")
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                else startForeground(NOTIFICATION_ID, n)
            }.onFailure { e -> Log.w(TAG, "startForeground 실패: ${e.message}"); stopSelf() }
        }
        Log.i(TAG, "foreground type=$type mic=$mic text=$text")
        return START_NOT_STICKY
    }

    companion object {
        const val TAG = "PauseService"
        const val CHANNEL = "trip"
        const val NOTIFICATION_ID = 7
        const val EXTRA_MIC = "mic"
        const val EXTRA_TEXT = "text"
        const val EXTRA_TITLE = "title"
        const val TITLE = "같이가개"
        /** 상주 알림 문구 (docs/wording.md v10-1). 반려견 이름은 [ongoing]으로 바꾼다. */
        const val ONGOING = "같이가개가 콩이와의 이동을 챙기고 있어요"

        fun ongoing(pet: String) = "같이가개가 ${pet.ifBlank { "반려견" }}${josaWa(pet)}의 이동을 챙기고 있어요"

        private fun josaWa(name: String): String {
            val ch = name.lastOrNull() ?: return "와"
            if (ch !in '가'..'힣') return "와"
            return if ((ch - '가') % 28 == 0) "와" else "과"
        }

        fun notification(ctx: Context, title: String, text: String): Notification {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                // 소리·heads-up 없는 조용한 채널 (음성 안내가 따로 나온다)
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "이동 중 안내", NotificationManager.IMPORTANCE_LOW))
            }
            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            return Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_comma)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(open)
                .build()
        }
    }
}

/**
 * 서비스를 켜고 끈다: 여행 ACTIVE이고 "오늘은 안 타요"가 아니면 켠다, 도착(여행 끝)·안 타요면 끈다.
 * 앱 화면이 보이는지 세어 [TripCoordinator.appVisible]에 알리고, 뒤에서 말한 안내는 상주 알림 한 줄로 갱신한다.
 * 뒤에서 시작이 막히면(자동 시작 등) 앱이 다시 보일 때 시작하고, 마이크 없이 시작했으면 보일 때 마이크 유형으로 다시 건다.
 */
@Singleton
class TripServiceController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val runtime: SignalRuntime,
    private val trip: TripCoordinator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var visible = 0
    private var want = false
    private var running = false
    private var runningMic = false
    private var lastLine: String? = null

    fun attach(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(a: Activity) {
                visible++
                onVisibility()
            }
            override fun onActivityStopped(a: Activity) {
                visible = (visible - 1).coerceAtLeast(0)
                onVisibility()
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityResumed(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        })
        scope.launch {
            combine(
                runtime.snapshot.map { it.trip == TripPhase.ACTIVE }.distinctUntilChanged(),
                trip.ui.map { it.dayOff }.distinctUntilChanged(),
            ) { active, dayOff -> active && !dayOff }
                .distinctUntilChanged()
                .collect { w ->
                    want = w
                    if (w) start() else stop()
                }
        }
        scope.launch {
            trip.alerts.collect { line ->
                lastLine = line
                if (running) update(line)
            }
        }
    }

    private fun onVisibility() {
        trip.appVisible = visible > 0
        if (visible > 0) {
            lastLine = null
            // 뒤에서 못 켰거나 마이크 없이 켰으면 지금(보이는 동안) 다시 건다
            if (want && (!running || !runningMic)) start()
            else if (running) update(null)
        }
    }

    private fun start() {
        val mic = visible > 0
        val i = intent(lastLine).putExtra(TripService.EXTRA_MIC, mic)
        runCatching { context.startForegroundService(i) }
            .onSuccess {
                running = true
                runningMic = mic
            }
            .onFailure { Log.w(TripService.TAG, "서비스 시작이 막힘(앱이 뒤) — 앱이 보일 때 다시: ${it.message}") }
    }

    private fun stop() {
        if (!running) return
        context.stopService(Intent(context, TripService::class.java))
        running = false
        runningMic = false
        lastLine = null
    }

    /** 상주 알림 한 줄 갱신 (같은 알림 ID라 새 알림이 쌓이지 않는다). null이면 기본 문구. */
    private fun update(line: String?) {
        val nm = context.getSystemService(NotificationManager::class.java)
        runCatching { nm.notify(TripService.NOTIFICATION_ID, TripService.notification(context, title(line), line ?: TripService.ongoing(trip.ui.value.pet.name))) }
    }

    private fun title(line: String?) = if (line == null) TripService.TITLE else TripService.ongoing(trip.ui.value.pet.name)

    private fun intent(line: String?) = Intent(context, TripService::class.java)
        .putExtra(TripService.EXTRA_TITLE, title(line))
        .putExtra(TripService.EXTRA_TEXT, line ?: TripService.ongoing(trip.ui.value.pet.name))
}
