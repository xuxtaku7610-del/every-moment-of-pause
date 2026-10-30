package com.emop.pause.engine.pet

/**
 * 실내 온도 단계 (UPGRADE 2, CONCEPT F3·F6). 온도 신호는 Pleos Vehicle SDK `HVAC.getHvacTemperatureCurrent`·
 * `registerHvacTemperatureCurrent`(측정값, javap Vehicle 2.3.2) — 설정 온도(`getHvacTemperature`)가 아니다.
 * 값이 없으면 단계를 만들지 않고(null) 숫자를 지어내지 않는다. 그때는 6~9월에만 일반 문구 [seasonNote].
 *
 * 기준값(가정, 상수 한 곳):
 * - [CAUTION_C] 26℃: 개는 땀샘이 거의 없어 헐떡임으로만 열을 내보내고, 실내가 26℃를 넘으면 헐떡임이 늘기 시작한다는
 *   수의 일반 안내를 따른 주의선. 도착 체크의 "강한 알림"과 같은 값(BoardingCheck.HOT_C).
 * - [STRONG_C] 30℃: 닫힌 차 실내는 바깥보다 빠르게 오르고(그늘에서도 수십 분에 10℃ 이상), 30℃ 이상은 열사병 위험이
 *   커진다는 일반 안내에 따른 강한 경고선. 진단이 아니라 보호자에게 확인을 재촉하는 기준이다.
 * - [RISE_C] 2℃: 대기 중 시작보다 이만큼 오르면 "오르는 중" 배너 (센서 흔들림 ±0.5℃보다 충분히 크게).
 */
object CabinHeat {
    const val CAUTION_C = 26f
    const val STRONG_C = 30f
    const val RISE_C = 2f
    val SUMMER = 6..9

    enum class Level { OK, CAUTION, STRONG }

    fun level(tempC: Float?): Level? = when {
        tempC == null -> null
        tempC >= STRONG_C -> Level.STRONG
        tempC >= CAUTION_C -> Level.CAUTION
        else -> Level.OK
    }

    /** 대기 중 온도가 오르는가: 시작보다 [RISE_C] 이상 올랐거나 이미 주의 온도. 지금 값을 모르면 false. */
    fun rising(startC: Float?, nowC: Float?): Boolean {
        if (nowC == null) return false
        return nowC >= CAUTION_C || (startC != null && nowC - startC >= RISE_C)
    }

    /** 온도를 모를 때만, 여름(6~9월)에 일반 문구. 값이 있으면 숫자를 보여 주므로 null. */
    fun seasonNote(tempC: Float?, month: Int): String? = if (tempC == null && month in SUMMER) "여름에는 차 안이 빨리 더워져요" else null
}
