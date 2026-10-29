package com.emop.pause.core.model

/**
 * CarUxRestrictions의 활성 제한 플래그 (COMMERCIAL_READINESS D P0).
 * 비트값은 SDK `android.car.jar`의 `CarUxRestrictions.UX_RESTRICTIONS_*` 상수를 javap로 확인했다 (2026-10-29).
 * 순수 모델에 두어 시뮬레이터·테스트에서도 같은 정책을 쓴다.
 */
data class UxLimits(
    val active: Int = BASELINE,
    /** `getMaxRestrictedStringLength()`. LIMIT_STRING_LENGTH일 때 한 문자열 최대 길이. */
    val maxStringLength: Int = Int.MAX_VALUE,
) {
    val noVideo: Boolean get() = has(NO_VIDEO)
    val limitStringLength: Boolean get() = has(LIMIT_STRING_LENGTH)
    val noKeyboard: Boolean get() = has(NO_KEYBOARD)
    val noSetup: Boolean get() = has(NO_SETUP)
    val noTextMessage: Boolean get() = has(NO_TEXT_MESSAGE)

    private fun has(flag: Int) = active and flag == flag

    companion object {
        const val BASELINE = 0
        const val NO_DIALPAD = 1
        const val NO_FILTERING = 2
        const val LIMIT_STRING_LENGTH = 4
        const val NO_KEYBOARD = 8
        const val NO_VIDEO = 16
        const val LIMIT_CONTENT = 32
        const val NO_SETUP = 64
        const val NO_TEXT_MESSAGE = 128
        const val NO_VOICE_TRANSCRIPTION = 256
        const val FULLY_RESTRICTED = 511

        /** AAOS 기본 maxRestrictedStringLength (CarUxRestrictions 문서 기본값 120). */
        const val DEFAULT_MAX_STRING = 120

        val NONE = UxLimits()

        /**
         * 시뮬레이터·제한 정보가 없을 때 DriveState로 근사한다.
         * DRIVING = 주행 중 흔한 조합(영상·키보드·설정·문자 금지, 문자열 길이 제한), RESTRICTED = 전부.
         */
        fun approximate(state: DriveState): UxLimits = when (state) {
            DriveState.PARKED -> NONE
            DriveState.DRIVING -> UxLimits(NO_VIDEO or NO_KEYBOARD or NO_SETUP or NO_TEXT_MESSAGE or LIMIT_STRING_LENGTH, DEFAULT_MAX_STRING)
            DriveState.RESTRICTED -> UxLimits(FULLY_RESTRICTED, DEFAULT_MAX_STRING)
        }
    }
}
