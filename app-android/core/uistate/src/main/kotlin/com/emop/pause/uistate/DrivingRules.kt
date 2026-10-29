package com.emop.pause.uistate

/**
 * 주행 중 화면 제한 (architecture §5, COMMERCIAL_READINESS D, docs/ui_contract.md "주행 중 제한").
 * 어떤 디자인이든 [DriveUi]가 이 규칙을 지키면 주행 화면이 2초 안에 읽힌다는 전제를 테스트로 고정한다.
 */
object DrivingRules {
    /** 한 덩어리 텍스트 최대 2줄, 1/3 창(600dp) 기준 한 줄 약 24자. */
    const val MAX_BLOCK_CHARS = 48

    /** 장소 이름 한 줄 (headline). */
    const val MAX_PLACE_CHARS = 14
    const val MAX_BUTTONS = 2
    const val MAX_CHIPS = 3
    const val MAX_FAMILY_LINES = 3

    /** 지금 상태에서 화면에 보일 버튼 수. 음성 실패 시트가 뜨면 시트의 버튼(2개)이 카드 버튼을 대신한다. */
    fun buttons(ui: DriveUi): Int = when {
        ui.voice == VoiceSheetState.FAILED -> 2
        else -> when (val c = ui.content) {
            is DriveContent.Next -> if (c.asking) 2 else 0
            is DriveContent.Urgent -> 1
            is DriveContent.Calm -> 0
        }
    }

    /** 규칙 위반 목록. 비어 있으면 통과. */
    fun violations(ui: DriveUi): List<String> = buildList {
        val b = buttons(ui)
        if (b > MAX_BUTTONS) add("버튼 ${b}개 > $MAX_BUTTONS")
        fun block(name: String, text: String?) {
            if (text != null && text.length > MAX_BLOCK_CHARS) add("$name ${text.length}자 > $MAX_BLOCK_CHARS: $text")
        }
        fun place(name: String) {
            if (name.length > MAX_PLACE_CHARS) add("장소 ${name.length}자 > $MAX_PLACE_CHARS: $name")
        }
        when (val c = ui.content) {
            is DriveContent.Next -> {
                place(c.place)
                if (c.chips.size > MAX_CHIPS) add("칩 ${c.chips.size}개 > $MAX_CHIPS")
            }
            is DriveContent.Calm -> {
                block("편안 문장", c.line)
                if (c.chips.size > MAX_CHIPS) add("칩 ${c.chips.size}개 > $MAX_CHIPS")
            }
            is DriveContent.Urgent -> place(c.place)
        }
        block("피로 설명", ui.fatigue?.detail)
        block("안내", ui.toast)
        if (ui.family.size > MAX_FAMILY_LINES) add("가족 ${ui.family.size}줄 > $MAX_FAMILY_LINES")
        ui.family.forEach { block("가족", it.text) }
    }
}
