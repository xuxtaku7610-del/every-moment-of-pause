package com.emop.pause.trip

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emop.pause.MainActivity
import com.emop.pause.R

/**
 * 파워냅 기상 알람 (COMMERCIAL_READINESS E P0): 앱 타이머와 별도로 시스템 알람(AlarmManager)을 걸어 앱이 죽어도 깨운다.
 * 앱이 살아 있으면 화면 타이머가 먼저 깨우고 알람은 취소한다.
 */
interface NapAlarm {
    fun schedule(triggerWallMs: Long)
    fun cancel()
}

/** 알람 시각 계산 — 순수 함수 (`NapAlarmPlanTest`). */
object NapAlarmPlan {
    /**
     * @param remainingSec 파워냅 남은 시간(엔진 시계 기준 초)
     * @param simSpeed 시뮬레이션 배속 (실차는 1)
     * @param ticking 시계가 흐르는 중 (시뮬레이션 일시정지면 false)
     * @return 벽시계 기준 알람 시각, 시계가 멈췄으면 null(알람 취소)
     */
    fun triggerAt(nowWallMs: Long, remainingSec: Int, simSpeed: Int, ticking: Boolean): Long? {
        if (!ticking) return null
        val speed = simSpeed.coerceAtLeast(1)
        return nowWallMs + remainingSec.coerceAtLeast(0) * 1000L / speed
    }

    /** 이미 건 알람과 2초 넘게 다르면 다시 건다(배속 변경·10분 더). */
    fun needsReschedule(scheduled: Long?, wanted: Long?): Boolean = when {
        wanted == null -> scheduled != null
        scheduled == null -> true
        else -> kotlin.math.abs(scheduled - wanted) > 2_000
    }
}

/**
 * Android 구현. 정확한 알람 권한이 있으면 `setAlarmClock`(상태 표시줄에 알람 표시), 없으면 1분 창 `setWindow`.
 * 울릴 때 [NapAlarmReceiver]가 알람 소리 알림과 앱 화면(전체 화면 인텐트)을 띄운다.
 */
class AndroidNapAlarm(private val context: Context) : NapAlarm {
    private val am = context.getSystemService(AlarmManager::class.java)

    private fun pending(): PendingIntent = PendingIntent.getBroadcast(
        context, REQ, Intent(context, NapAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun schedule(triggerWallMs: Long) {
        val am = am ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        runCatching {
            if (exact) {
                val show = PendingIntent.getActivity(context, REQ, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerWallMs, show), pending())
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerWallMs, WINDOW_MS, pending())
            }
        }
    }

    override fun cancel() {
        runCatching { am?.cancel(pending()) }
    }

    private companion object {
        const val REQ = 7
        const val WINDOW_MS = 60_000L
    }
}

/** 기상 알람이 울림: 알람 소리 알림 + 앱 화면을 띄운다. 알림 권한이 없으면 앱 화면만 시도한다. */
class NapAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(EXTRA_WAKE, true)
        val pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "파워냅 기상", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
                )
            },
        )
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_comma)
                .setContentTitle("일어날 시간이에요")
                .setContentText("파워냅이 끝났어요. 천천히 기지개 켜고 출발해요.")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIFICATION_ID, n)
        } else {
            runCatching { context.startActivity(open) }
        }
    }

    companion object {
        const val CHANNEL = "nap_wake"
        const val NOTIFICATION_ID = 7
        const val EXTRA_WAKE = "napWake"
    }
}
