package com.emop.pause.trip

import kotlin.math.abs

/** 병원 접수함 응답 (서버 `REPLIES`와 같은 코드). [drive]는 주행 중 한 줄(24자 이하), [line]은 정차 화면. */
enum class ClinicReply(val code: String, val line: String, val drive: String) {
    RECEIVED("received", "병원: 접수 확인했어요", "병원이 접수를 확인했어요"),
    READY("ready", "병원: 도착하면 바로 볼게요", "병원: 도착하면 바로 봐요"),
    BUSY("busy", "병원: 지금은 어렵대요 · 다른 병원을 찾아요", "병원이 어렵대요 · 정차 후 확인"),
    ;

    companion object {
        fun of(code: String?): ClinicReply? = entries.firstOrNull { it.code == code }
    }
}

/**
 * 동물병원 사전 접수 (UPGRADE 3, test_plan F5-07~10). 도착 예정 공유([shareUrl], 보호자용)에
 * 병원 접수함 링크([clinicUrl])를 더한 것 — 증상 메모·오늘 이동 한 줄은 [VetMemo.share] 그대로 재사용한다.
 * 가는 동안 도착 예정 분을 서버로 갱신하고(위치는 보내지 않는다), 병원 응답을 받아 화면·음성으로 알린다.
 *
 * 도착 예정: 실차는 지금 위치에서 병원까지 거리로 다시 계산, 시뮬레이터는 경로를 실제로 바꾸지 않으므로
 * 처음 예상에서 시뮬레이션 시계로 흐른 분만큼 줄인다([simEta]).
 */
data class ClinicIntake(
    val token: String,
    val clinicUrl: String,
    val shareUrl: String,
    val vetName: String,
    val etaMin: Int,
    val sentEtaMin: Int,
    val sentAtMs: Long,
    val startClockMin: Int,
    /** 처음 만든 때의 예상 분 (시뮬레이터 카운트다운 기준). */
    val initialEtaMin: Int = etaMin,
    val reply: ClinicReply? = null,
    val seen: Boolean = false,
    val arrived: Boolean = false,
) {
    companion object {
        /** 같은 값이면 60초마다, 1분 이상 바뀌면 15초 간격으로 보낸다. 응답 확인도 15초마다. */
        const val PUSH_EVERY_MS = 60_000L
        const val PUSH_MIN_GAP_MS = 15_000L
        const val POLL_MS = 15_000L

        /** 실차 도착 판정: 병원 150m 안. */
        const val ARRIVE_KM = 0.15

        fun shouldPush(i: ClinicIntake, etaMin: Int, nowMs: Long): Boolean {
            if (i.arrived) return false
            val gap = nowMs - i.sentAtMs
            return gap >= PUSH_EVERY_MS || (abs(etaMin - i.sentEtaMin) >= 1 && gap >= PUSH_MIN_GAP_MS)
        }

        fun simEta(i: ClinicIntake, clockMin: Int): Int = (i.initialEtaMin - (clockMin - i.startClockMin)).coerceAtLeast(0)

        /** 정차 화면·병원 화면 한 줄. */
        fun status(i: ClinicIntake): String = when {
            i.arrived -> "병원에 도착했어요"
            i.reply != null -> "${i.reply.line} · ${i.etaMin}분 뒤 도착"
            i.seen -> "병원이 접수함을 열어 봤어요 · ${i.etaMin}분 뒤 도착"
            else -> "병원 접수함에 보냈어요 · ${i.etaMin}분 뒤 도착"
        }
    }
}
