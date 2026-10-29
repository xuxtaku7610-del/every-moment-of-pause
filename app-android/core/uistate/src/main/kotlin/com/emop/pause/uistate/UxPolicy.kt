package com.emop.pause.uistate

/**
 * CarUxRestrictions 플래그별 화면 정책 (test_plan DS-02·DS-04, docs/ui_contract.md "주행 중 제한").
 * 화면 계약 모듈은 Android를 모르므로 플래그를 [Flags]로 받는다 (앱이 `UxLimits`에서 옮겨 담는다).
 */
object UxPolicy {
    data class Flags(
        val noVideo: Boolean = false,
        val limitStringLength: Boolean = false,
        val maxStringLength: Int = Int.MAX_VALUE,
        val noKeyboard: Boolean = false,
        val noSetup: Boolean = false,
    )

    /** NO_VIDEO: 움직이는 화면 정지. [baseAllowed] = PAUSE && PARKED && 움직임 줄이기 꺼짐. */
    fun motionAllowed(baseAllowed: Boolean, f: Flags) = baseAllowed && !f.noVideo

    /** NO_SETUP·NO_KEYBOARD: 프로필 편집·설정(입력·설정 화면) 차단. */
    fun setupAllowed(f: Flags) = !f.noSetup && !f.noKeyboard

    /** LIMIT_STRING_LENGTH: 첫 문장만 남기고, 그래도 길면 [max]-1자 + "…". */
    fun shorten(text: String?, max: Int): String? {
        if (text == null || text.length <= max) return text
        val first = Regex("^.*?[.!?](?=\\s|$)").find(text)?.value
        if (first != null && first.length <= max) return first
        return text.take((max - 1).coerceAtLeast(1)) + "…"
    }

    /** 주행 화면 상태에 문자열 제한을 적용한다. 제한이 없으면 그대로. */
    fun apply(ui: DriveUi, f: Flags): DriveUi {
        if (!f.limitStringLength) return ui
        val m = f.maxStringLength
        return ui.copy(
            toast = shorten(ui.toast, m),
            content = when (val c = ui.content) {
                is DriveContent.Calm -> c.copy(line = shorten(c.line, m)!!)
                is DriveContent.Forecast -> c.copy(line = shorten(c.line, m)!!, title = shorten(c.title, m), coach = null)
                is DriveContent.Suggest -> c.copy(reasons = c.reasons.take(1))
                is DriveContent.Heading, is DriveContent.Vet -> c
            },
        )
    }
}
