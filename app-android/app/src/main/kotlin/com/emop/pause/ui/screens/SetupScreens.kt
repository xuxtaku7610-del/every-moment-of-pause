package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emop.pause.ui.components.AppBar
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.CardKey
import com.emop.pause.ui.components.Landscape
import com.emop.pause.ui.components.LandscapeKind
import com.emop.pause.ui.components.MemberTile
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.components.PauseCard
import com.emop.pause.ui.components.PauseIcons
import com.emop.pause.ui.components.ReasonChip
import com.emop.pause.ui.components.Segmented
import com.emop.pause.ui.components.SettingRow
import com.emop.pause.uistate.Fixtures
import com.emop.pause.uistate.MemberUi
import com.emop.pause.uistate.OnboardStep
import com.emop.pause.uistate.SettingsSection
import com.emop.pause.uistate.SettingsUi
import com.emop.pause.ui.theme.Pause
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.WidthClass

// ═════════════════════════ 온보딩 (13~15) ═════════════════════════

class OnboardActions(
    val next: () -> Unit = {},
    val skip: () -> Unit = {},
    val toggleMember: (String) -> Unit = {},
    val addMember: () -> Unit = {},
    val allowMic: () -> Unit = {},
    val buttonsOnly: () -> Unit = {},
    val logoTap: () -> Unit = {},
)

/** 첫 실행 온보딩 3단계 (UI_SPEC §4.11). PARKED에서만 보인다 — 주행이 시작되면 상위가 주행 화면으로 바꾸고 다음 정차 때 이어서 보여준다. */
@Composable
fun OnboardScreen(step: OnboardStep, members: List<MemberUi>, actions: OnboardActions = OnboardActions()) {
    val c = Pause.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.paper)) {
        val wide = WidthClass.of(maxWidth) == WidthClass.WIDE
        Column(Modifier.fillMaxSize()) {
            AppBar("정차 중에만 설정할 수 있어요", onLogoTap = actions.logoTap)
            Row(
                Modifier.weight(1f).padding(start = if (wide) 56.dp else 40.dp, end = if (wide) 56.dp else 40.dp, top = 24.dp, bottom = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) { OnboardText(step, actions) }
                if (wide) {
                    Box(Modifier.weight(1f).fillMaxHeight()) { OnboardArt(step, members, actions) }
                }
            }
        }
    }
}

@Composable
private fun OnboardText(step: OnboardStep, actions: OnboardActions) {
    val c = Pause.colors
    val index = step.ordinal
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { i -> Box(Modifier.size(56.dp, 10.dp).clip(RoundedCornerShape(5.dp)).background(if (i <= index) c.rest else c.line)) }
    }
    Spacer(Modifier.height(40.dp))
    val (title, body) = when (step) {
        OnboardStep.CONCEPT -> "여정은 문장이고,\n휴식은 쉼표예요" to "쉼표는 운전 피로와 가족의 필요를 함께 보고, 언제 어디서 쉬면 좋을지 먼저 알려 드려요."
        OnboardStep.MEMBERS -> "누구와 함께\n타나요?" to "아이 나이와 반려동물을 알려 주시면 화장실, 산책 시간까지 챙겨서 쉬는 곳을 골라요. 이 정보는 이 차에만 저장돼요."
        OnboardStep.MIC -> "운전 중엔\n말로 답해요" to "\"응\", \"나중에\"처럼 짧게 답할 수 있도록 마이크를 써요. 제안 직후 8초만 듣고, 녹음은 저장하지 않아요."
    }
    Text(title, style = Pause.type.onboard, color = c.ink)
    Spacer(Modifier.height(28.dp))
    Text(body, Modifier.widthIn(max = 640.dp), style = Pause.type.lead.copy(fontWeight = FontWeight.Normal, lineHeight = Pause.type.lead.fontSize * 1.45f), color = c.ink2)
    Spacer(Modifier.height(64.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        when (step) {
            OnboardStep.CONCEPT -> PauseButton("다음", actions.next)
            OnboardStep.MEMBERS -> {
                PauseButton("다음", actions.next)
                PauseButton("나중에 할게요", actions.skip, kind = ButtonKind.GHOST)
            }
            OnboardStep.MIC -> {
                PauseButton("마이크 허용하고 시작", actions.allowMic)
                PauseButton("버튼으로만 쓸게요", actions.buttonsOnly, kind = ButtonKind.GHOST)
            }
        }
    }
}

@Composable
private fun OnboardArt(step: OnboardStep, members: List<MemberUi>, actions: OnboardActions) {
    val c = Pause.colors
    val shape = RoundedCornerShape(36.dp)
    when (step) {
        OnboardStep.CONCEPT -> Landscape(LandscapeKind.ONBOARD, Modifier.fillMaxSize().clip(shape))
        OnboardStep.MEMBERS -> Column(
            Modifier.fillMaxSize().clip(shape).background(c.surface).padding(40.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val tiles = members.take(3)
            tiles.chunked(2).forEachIndexed { row, pair ->
                if (row > 0) Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    pair.forEach { m -> MemberTile(m.label, m.kind, m.selected, { actions.toggleMember(m.id) }, Modifier.weight(1f), height = 220.dp, large = true) }
                    if (pair.size == 1) MemberTile("+ 추가", null, false, actions.addMember, Modifier.weight(1f), height = 220.dp, large = true)
                }
            }
            if (tiles.size % 2 == 0) {
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MemberTile("+ 추가", null, false, actions.addMember, Modifier.weight(1f), height = 220.dp, large = true)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        OnboardStep.MIC -> Column(
            Modifier.fillMaxSize().clip(shape).background(c.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(240.dp).clip(CircleShape).background(c.restSoft), contentAlignment = Alignment.Center) {
                Icon(PauseIcons.Mic, null, Modifier.size(120.dp), tint = c.restInk)
            }
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("\"응\"", "\"나중에\"", "\"아직 멀었어?\"").forEach { ReasonChip(it, onCard = true) }
            }
        }
    }
}

// ═════════════════════════ 설정 (16) ═════════════════════════

class SettingsActions(
    val section: (SettingsSection) -> Unit = {},
    val timing: (Int) -> Unit = {},
    val toggleVoice: () -> Unit = {},
    val toggleKidAnswer: () -> Unit = {},
    val toggleNapSound: () -> Unit = {},
    val toggleReduceMotion: () -> Unit = {},
    val toggleMember: (String) -> Unit = {},
    val deleteData: () -> Unit = {},
    val close: () -> Unit = {},
    val logoTap: () -> Unit = {},
)

private val timingDetail = listOf(
    "빠르게: 피로가 오르기 시작하면 바로, 1시간 30분을 넘기기 전에 제안해요.",
    "보통: 피로가 주의 단계에 닿기 전, 그리고 2시간을 넘기기 전에 제안해요.",
    "여유 있게: 피로가 주의 단계에 닿으면 제안해요. 위험 단계 안내는 그대로예요.",
)

/** 설정 (UI_SPEC §4.12). PARKED에서만 — 주행 중엔 [DrivingLockedScreen]. */
@Composable
fun SettingsScreen(ui: SettingsUi, actions: SettingsActions = SettingsActions(), licenses: String = "") {
    val c = Pause.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.paper)) {
        val wide = WidthClass.of(maxWidth) == WidthClass.WIDE
        Column(Modifier.fillMaxSize()) {
            AppBar(null, onLogoTap = actions.logoTap, trailing = { PauseButton("닫기", actions.close, kind = ButtonKind.GHOST, small = true) })
            Row(Modifier.weight(1f).padding(start = 56.dp, end = 56.dp, top = 24.dp, bottom = 40.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.width(if (wide) 420.dp else 300.dp)) {
                    NavItem(SettingsSection.SUGGEST, "제안과 알림", PauseIcons.Bell, ui.section, actions)
                    NavItem(SettingsSection.FAMILY, "함께 타는 가족", PauseIcons.User, ui.section, actions)
                    NavItem(SettingsSection.SIGNALS, "이 차에서 쓰는 신호", PauseIcons.Car, ui.section, actions)
                    NavItem(SettingsSection.PRIVACY, "개인정보와 데이터", PauseIcons.Lock, ui.section, actions)
                    NavItem(SettingsSection.ABOUT, "앱 정보", PauseIcons.Info, ui.section, actions)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    when (ui.section) {
                        SettingsSection.SUGGEST -> {
                            PauseCard(Modifier.fillMaxWidth()) {
                                Text("쉬자고 제안하는 시점", style = Pause.type.item.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
                                Spacer(Modifier.height(24.dp))
                                Segmented(listOf("빠르게", "보통", "여유 있게"), ui.timing, actions.timing)
                                Spacer(Modifier.height(16.dp))
                                Text(timingDetail[ui.timing], style = Pause.type.label.copy(fontWeight = FontWeight.Normal), color = c.ink2)
                            }
                            PauseCard(Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SettingRow("음성으로 제안", "끄면 화면 카드로만 알려 드려요.", ui.voiceSuggest, actions.toggleVoice)
                                    SettingRow("\"아직 멀었어?\" 답하기", "뒷좌석 질문에 남은 시간을 대신 답해요.", ui.kidAnswer, actions.toggleKidAnswer)
                                    SettingRow("파워냅 장작 소리", "파워냅 중에 작은 소리로 틀어요.", ui.napSound, actions.toggleNapSound)
                                    SettingRow("움직임 줄이기", "벽난로 불꽃과 음성 링을 멈춰 둬요.", ui.reduceMotion, actions.toggleReduceMotion)
                                }
                            }
                            SignalsCard(ui)
                        }
                        SettingsSection.FAMILY -> PauseCard(Modifier.fillMaxWidth()) {
                            Text("함께 타는 가족", style = Pause.type.item.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
                            Spacer(Modifier.height(8.dp))
                            Text("고른 사람에 맞춰 화장실·수유·산책 시간을 챙겨요. 이 정보는 이 차에만 저장돼요.", style = Pause.type.label.copy(fontWeight = FontWeight.Normal), color = c.ink2)
                            Spacer(Modifier.height(28.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ui.members.forEach { m -> MemberTile(m.label, m.kind, m.selected, { actions.toggleMember(m.id) }, Modifier.weight(1f)) }
                            }
                        }
                        SettingsSection.SIGNALS -> SignalsCard(ui, detailed = true)
                        SettingsSection.PRIVACY -> PrivacyCards(actions)
                        SettingsSection.ABOUT -> PauseCard(Modifier.fillMaxWidth()) {
                            Text("쉼표 ${ui.version}", style = Pause.type.item.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
                            Spacer(Modifier.height(8.dp))
                            Text("달릴 때는 언제 어디서 멈출지, 멈췄을 때는 그 시간을 무엇으로 채울지 골라 드려요.", style = Pause.type.body, color = c.ink2)
                            Spacer(Modifier.height(28.dp))
                            CardKey("오픈소스 라이선스")
                            Spacer(Modifier.height(12.dp))
                            Text(licenses.ifBlank { "Pretendard — SIL Open Font License 1.1" }, style = Pause.type.caption, color = c.ink2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(s: SettingsSection, label: String, icon: ImageVector, current: SettingsSection, actions: SettingsActions) {
    val c = Pause.colors
    val on = s == current
    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (on) c.surface else c.paper)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { actions.section(s) }
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(32.dp), tint = if (on) c.ink else c.ink2)
        Spacer(Modifier.width(20.dp))
        Text(label, style = Pause.type.item.copy(fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold), color = if (on) c.ink else c.ink2, maxLines = 1)
    }
}

@Composable
private fun SignalsCard(ui: SettingsUi, detailed: Boolean = false) {
    val c = Pause.colors
    PauseCard(Modifier.fillMaxWidth()) {
        CardKey("이 차에서 쓰는 신호")
        if (detailed) {
            Spacer(Modifier.height(8.dp))
            Text("이 차가 주지 않는 신호는 빼고 나머지로 피로를 계산해요.", style = Pause.type.label.copy(fontWeight = FontWeight.Normal), color = c.ink2)
        }
        Spacer(Modifier.height(8.dp))
        ui.signals.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(thickness = 2.dp, color = c.paper)
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = Pause.type.body, color = c.ink)
                    if (detailed && s.source != null) Text(s.source!!, style = Pause.type.caption, color = c.ink3)
                }
                Box(Modifier.size(14.dp).clip(CircleShape).background(if (s.inUse) c.rest else c.line))
                Spacer(Modifier.width(10.dp))
                Text(if (s.inUse) "사용 중" else "이 차는 지원 안 함", style = Pause.type.captionStrong.copy(fontWeight = FontWeight.Bold), color = if (s.inUse) c.ink else c.ink3)
            }
        }
    }
}

@Composable
private fun PrivacyCards(actions: SettingsActions) {
    val c = Pause.colors
    PauseCard(Modifier.fillMaxWidth()) {
        Text("모으는 것과 모으지 않는 것", style = Pause.type.item.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
        Spacer(Modifier.height(20.dp))
        PrivacyLine("서버로 보내는 것", "휴게소 단위 여행 기록(어느 휴게소에서 몇 분 쉬었는지)과 익명 기기 번호. 계정은 없어요.")
        PrivacyLine("서버로 보내지 않는 것", "경로 좌표, 수면 시간, 동승자 이름·나이. 이 차 안에만 저장돼요.")
        PrivacyLine("음성", "제안 직후 8초만 듣고, 녹음은 저장하지 않아요.")
        PrivacyLine("보관 기간", "여행 기록은 서버에서 90일 뒤 지워요.")
    }
    PauseCard(Modifier.fillMaxWidth()) {
        Text("내 데이터 삭제", style = Pause.type.item.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text("이 차에 저장된 가족·여행 기록과 서버의 기기 번호를 지워요. 되돌릴 수 없어요.", style = Pause.type.label.copy(fontWeight = FontWeight.Normal), color = c.ink2)
        Spacer(Modifier.height(24.dp))
        PauseButton("모두 지우기", actions.deleteData, kind = ButtonKind.QUIET, onCard = true, small = true)
    }
}

@Composable
private fun PrivacyLine(k: String, v: String) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(k, style = Pause.type.captionStrong, color = Pause.colors.ink2)
        Spacer(Modifier.height(4.dp))
        Text(v, style = Pause.type.body, color = Pause.colors.ink)
    }
}

/** 주행 중 설정·온보딩·편집 진입 시 (UI_SPEC §4.12, test_plan 1장 P0). */
@Composable
fun DrivingLockedScreen(meta: String? = null, onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Pause.colors.paper)) {
        AppBar(meta)
        Column(Modifier.weight(1f).padding(horizontal = 56.dp), verticalArrangement = Arrangement.Center) {
            Text("정차하면 설정할 수 있어요", style = Pause.type.headline, color = Pause.colors.ink, maxLines = 1)
            Spacer(Modifier.height(56.dp))
            PauseButton("주행 화면으로", onBack)
        }
    }
}

@Preview(name = "13 온보딩 1", widthDp = 1600, heightDp = 1280)
@Composable
private fun Onboard1Preview() = PauseTheme(dark = false) { OnboardScreen(OnboardStep.CONCEPT, Fixtures.prepare.members) }

@Preview(name = "14 온보딩 2", widthDp = 1600, heightDp = 1280)
@Composable
private fun Onboard2Preview() = PauseTheme(dark = false) { OnboardScreen(OnboardStep.MEMBERS, Fixtures.prepare.members) }

@Preview(name = "16 설정", widthDp = 1600, heightDp = 1280)
@Composable
private fun SettingsPreview() = PauseTheme(dark = false) { SettingsScreen(Fixtures.settings) }
