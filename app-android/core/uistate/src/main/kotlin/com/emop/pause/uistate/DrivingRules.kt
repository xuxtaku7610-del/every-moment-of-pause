package com.emop.pause.uistate

/**
 * 주행 중 화면 제한 (CONCEPT 5장, architecture §5, test_plan DS-02, docs/ui_contract.md "주행 중 제한").
 * 텍스트 2줄 이하, 버튼 2개 + 음성 힌트, 스크롤·자동 애니메이션 없음.
 * 어떤 디자인이든 [DriveUi]가 이 규칙을 지키면 주행 화면이 2초 안에 읽힌다는 전제를 테스트로 고정한다.
 */
object DrivingRules {
    /** 한 덩어리 텍스트 최대 2줄, 1/3 창(600dp) 기준 한 줄 약 24자. */
    const val MAX_BLOCK_CHARS = 48

    /** 한 줄(제목·코칭 따로 그릴 때). */
    const val MAX_LINE_CHARS = 24

    /** 장소 이름 한 줄. */
    const val MAX_PLACE_CHARS = 14
    const val MAX_BUTTONS = 2
    const val MAX_REASONS = 2

    /** 지금 상태에서 화면에 보일 버튼 수. 음성 실패 시트가 뜨면 시트의 버튼(2개)이 카드 버튼을 대신한다. */
    fun buttons(ui: DriveUi): Int = when {
        ui.voice == VoiceSheetState.FAILED -> 2
        else -> when (val c = ui.content) {
            is DriveContent.Suggest -> if (c.urgent) 1 else 2
            is DriveContent.Distress -> 2
            else -> 0
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
            is DriveContent.Calm -> block("안내", c.line)
            is DriveContent.Forecast -> {
                block("예보", c.line)
                // v5: 제목 1줄 + 코칭 1줄 (한 줄 ≈ 24자)
                if ((c.title?.length ?: 0) > MAX_LINE_CHARS) add("예보 제목 ${c.title!!.length}자 > $MAX_LINE_CHARS: ${c.title}")
                if ((c.coach?.length ?: 0) > MAX_LINE_CHARS) add("코칭 ${c.coach!!.length}자 > $MAX_LINE_CHARS: ${c.coach}")
            }
            is DriveContent.Vet -> place(c.name)
            is DriveContent.Distress -> {
                block("반려견 안내", c.line)
                c.target?.let { place(it) }
            }
            is DriveContent.Suggest -> {
                place(c.place)
                if (c.reasons.size > MAX_REASONS) add("이유 ${c.reasons.size}개 > $MAX_REASONS")
                block("이유", c.reasons.joinToString(" · "))
            }
            is DriveContent.Heading -> place(c.place)
        }
        block("결과", ui.toast)
        block("멀미", ui.motion?.label)
    }
}
