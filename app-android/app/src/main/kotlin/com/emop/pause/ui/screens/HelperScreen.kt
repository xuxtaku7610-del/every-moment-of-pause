package com.emop.pause.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import com.emop.pause.ui.components.Block
import com.emop.pause.ui.components.Body
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.Caption
import com.emop.pause.ui.components.Choice
import com.emop.pause.ui.components.Divider
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.StatusChip
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.theme.Pause
import com.emop.pause.uistate.HelperAnswerUi
import com.emop.pause.uistate.HelperUi
import com.emop.pause.uistate.UiEvent

/*
 * 콩이 도우미 (HELPER_PROMPT 4번) — 정차 중 겹침 화면. 주행 중에는 열리지 않는다(화면 선택이 주행 화면을 먼저 고른다).
 * 면 2장(v10 §2): ① 묻기(상황별 질문 칩 6개 · 말하기 · 정차 중에만 입력 칸) ② 답(최근 답 크게 + 이전 4개는 제목 한 줄).
 * 답 = 제목 + 3~4줄 + 행동 1개 + 출처. 검수 전 카드(디버그만)는 "검수 전" 칩. 응급은 기존 긴급 도움 흐름이 먼저 열린다.
 */

@Composable
internal fun HelperButton(on: On, compact: Boolean = true) {
    PauseButton(HelperUi.TITLE, { on(UiEvent.OpenHelper) }, kind = ButtonKind.SECONDARY, compact = compact)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelperScreen(ui: HelperUi, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    BackHandler { on(UiEvent.Close) }
    Page(null, on, scroll = true, title = HelperUi.TITLE, trailing = { CloseButton(on) }) {
        Block(Modifier.fillMaxWidth()) {
            Title("무엇이 궁금해요?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(d.unit), verticalArrangement = Arrangement.spacedBy(d.unit)) {
                ui.chips.forEach { q -> Choice(q, false, { on(UiEvent.AskHelper(q)) }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                if (ui.typing) {
                    BasicTextField(
                        ui.text, { on(UiEvent.SetHelperText(it)) },
                        Modifier.weight(1f).heightIn(min = d.touch).clip(RoundedCornerShape(d.buttonRadius)).background(c.surfaceAlt)
                            .border(d.border, c.border, RoundedCornerShape(d.buttonRadius)).padding(horizontal = d.u(16f), vertical = d.gapS),
                        textStyle = Pause.type.body.copy(color = c.text), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { on(UiEvent.AskHelper(ui.text)) }),
                        cursorBrush = SolidColor(c.primary),
                    )
                    PauseButton("묻기", { on(UiEvent.AskHelper(ui.text)) }, kind = ButtonKind.SECONDARY, icon = Icons.Rounded.Send, enabled = ui.text.isNotBlank() && !ui.busy, compact = true)
                }
                PauseButton(if (ui.listening) "듣고 있어요" else "말로 묻기", { on(UiEvent.HelperVoice) }, icon = Icons.Rounded.Mic, enabled = !ui.listening && !ui.busy, compact = true)
            }
            Caption(if (ui.busy) "카드에서 찾는 중이에요" else HelperUi.NOTE)
        }
        if (ui.answers.isNotEmpty()) {
            Block(Modifier.fillMaxWidth(), tint = if (ui.answers.first().emergency) c.warningSoft else null) {
                Answer(ui.answers.first(), 0, on)
                ui.answers.drop(1).forEachIndexed { i, a ->
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
                        Column(Modifier.weight(1f)) {
                            Body(a.title, maxLines = 1)
                            Caption("“${a.question}”", maxLines = 1)
                        }
                        a.actionLabel?.let { PauseButton(it, { on(UiEvent.HelperAct(i + 1)) }, kind = ButtonKind.QUIET, compact = true) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Answer(a: HelperAnswerUi, index: Int, on: On) {
    val c = Pause.colors
    val d = Pause.dimens
    Caption("“${a.question}”", maxLines = 1)
    FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(d.gapS)) {
        Title(a.title, color = if (a.emergency) c.warningInk else c.text)
        if (!a.reviewed) StatusChip(HelperUi.DRAFT, neutral = true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(d.unit)) {
        a.lines.take(4).forEach { Body("· $it") }
    }
    if (a.explained) Caption("카드 내용을 풀어 썼어요")
    a.source?.let { Caption("출처 $it", maxLines = 2) }
    a.actionLabel?.let { PauseButton(it, { on(UiEvent.HelperAct(index)) }, kind = if (a.emergency) ButtonKind.WARNING else ButtonKind.PRIMARY) }
}
