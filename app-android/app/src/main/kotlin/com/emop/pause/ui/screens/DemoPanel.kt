package com.emop.pause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.emop.pause.uistate.Scene
import com.emop.pause.presentation.DemoScenes
import com.emop.pause.core.model.Signal
import com.emop.pause.runtime.SourceMode
import com.emop.pause.runtime.Snapshot
import com.emop.pause.sim.ScenarioPlayer
import com.emop.pause.trip.TripUi
import com.emop.pause.ui.components.ButtonKind
import com.emop.pause.ui.components.PauseButton
import com.emop.pause.ui.theme.Pause
import com.emop.pause.engine.motion.CompareResult
import java.util.Locale
import com.emop.pause.ui.components.Title
import com.emop.pause.ui.components.Body
import com.emop.pause.ui.components.Caption
import androidx.compose.foundation.layout.IntrinsicSize

class DemoActions(
    val scenario: (String) -> Unit = {},
    val vehicle: () -> Unit = {},
    val play: () -> Unit = {},
    val pause: () -> Unit = {},
    val speed: (Int) -> Unit = {},
    val jump: (Int) -> Unit = {},
    val say: (String) -> Unit = {},
    val scene: (Scene?) -> Unit = {},
    val close: () -> Unit = {},
    /** 고갯길 비교 전체 화면 (정차 중에만 열린다). */
    val compareBig: () -> Unit = {},
    /** 앞 구간 형상: true = 회전 안내(가짜 내비), false = 시연 경로(GPX). */
    val guidance: (Boolean) -> Unit = {},
    /** 장면용 실내 온도 (시뮬레이터만, null = 시나리오 값). */
    val cabin: (Float?) -> Unit = {},
    /** 콩이 도우미 질문 (HELPER_JUMPS). */
    val ask: (String) -> Unit = {},
)

/**
 * 발표 모드 패널 (COMMERCIAL_READINESS I P0). 로고 5번 탭으로 열린다. 릴리스에도 있지만 진입 경로는 알리지 않는다.
 * 예전 디버그 신호 화면을 이 안으로 옮겼다. 실차 주행 중에는 상위(PauseRoot)가 열지 않는다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoPanel(s: Snapshot, trip: TripUi, scenarios: List<String>, current: Scene?, a: DemoActions, compare: CompareResult? = null) {
    val c = Pause.colors
    Box(Modifier.fillMaxSize().background(c.scrim).clickable(remember { MutableInteractionSource() }, null, onClick = a.close)) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(760.dp)
                .background(c.surface)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .verticalScroll(rememberScrollState())
                .padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("발표 모드", style = Pause.type.title, color = c.text)
                Spacer(Modifier.weight(1f))
                PauseButton("닫기", a.close, kind = ButtonKind.SECONDARY, compact = true)
            }
            Section("시나리오")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                scenarios.forEach { n -> Choice(n, s.scenarioName == n && s.source == SourceMode.SIMULATION) { a.scenario(n) } }
                Choice("실차 SDK", s.source == SourceMode.VEHICLE, a.vehicle)
            }
            if (s.source == SourceMode.SIMULATION) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Choice(if (s.simPlaying) "일시정지" else "재생", false, if (s.simPlaying) a.pause else a.play)
                    ScenarioPlayer.SPEEDS.forEach { x -> Choice("${x}×", s.simSpeed == x) { a.speed(x) } }
                }
                Section("장면으로 이동")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoScenes.JUMPS.forEach { j -> Choice(j.label, false) { a.cabin(j.cabinTempC); a.jump(j.tSec); j.say?.let(a.say) } }
                    DemoScenes.HELPER_JUMPS.forEach { j -> Choice(j.label, false) { a.jump(j.tSec); j.ask?.let(a.ask) } }
                }
            }
            Section("운전자 응답 넣기")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Choice("\"여기서 쉴게요\"", false) { a.say("여기서 쉴게요") }
                Choice("\"나중에\"", false) { a.say("나중에") }
                Choice("\"잠깐 다녀올게\"", false) { a.say("잠깐 다녀올게") }
                Choice("\"콩이가 헥헥거려\"", false) { a.say("콩이가 헥헥거려") }
                Choice("\"콩이 토했어\"", false) { a.say("콩이 토했어") }
                Choice("\"콩이가 축 처졌어\"", false) { a.say("콩이가 축 처졌어") }
            }
            Section("고갯길 비교 (안내 없이 vs 안내대로, docs/plan/demo_compare.md)")
            if (compare == null) {
                Text("경로 형상이 있는 시나리오(pet-trip)에서 계산해요.", style = Pause.type.caption, color = c.sub)
            } else {
                Text(
                    "주행 자극 ${compare.unguidedFinal} → ${compare.guidedFinal}  ·  " +
                        "${compare.unguidedSec / 60}분 → ${compare.guidedSec / 60}분 (굽이 최저 ${compare.guidedMinKph}km/h)",
                    style = Pause.type.caption, color = c.text,
                )
                Text("차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님, 대관령 옛길(© OpenStreetMap, ODbL), 같은 모델 안의 상대 비교", style = Pause.type.caption, color = c.sub)
            }
            Choice("고갯길 비교 크게 보기 (정차 중)", false) { a.compareBig() }
            Section("앞 구간 형상 (지금: ${trip.shapeSource ?: "-"}) — 바꾼 뒤 장면 이동부터")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Choice("시연 경로 (GPX)", trip.shapeSource?.startsWith("시연 경로") == true) { a.guidance(false) }
                Choice("회전 안내 (GPX 없이)", trip.shapeSource?.startsWith("회전 안내 (시연") == true) { a.guidance(true) }
            }
            Section("시안 장면 고정 (화면 검증용)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Choice("실제 상태", current == null) { a.scene(null) }
                Scene.entries.forEach { sc -> Choice(sc.name.lowercase(), current == sc) { a.scene(sc) } }
            }
            Section("신호")
            signalRows(s, trip).forEach { (k, v) ->
                Row {
                    Text(k, Modifier.width(200.dp), style = Pause.type.caption, color = c.sub)
                    Text(v, style = Pause.type.caption.copy(fontFamily = FontFamily.Monospace), color = c.text)
                }
            }
        }
    }
}

@Composable
private fun Section(t: String) {
    Text(t, Modifier.padding(top = 12.dp), style = Pause.type.caption, color = Pause.colors.sub)
}

@Composable
private fun Choice(label: String, on: Boolean, onClick: () -> Unit) {
    val c = Pause.colors
    Box(
        Modifier
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(if (on) c.primarySoft else c.bg)
            .then(if (on) Modifier.border(2.dp, c.primary, RoundedCornerShape(36.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Pause.type.caption, color = if (on) c.primary else c.text, maxLines = 1) }
}

private fun signalRows(s: Snapshot, trip: TripUi): List<Pair<String, String>> = buildList {
    add("source" to (s.scenarioName?.let { "sim: $it" } ?: "vehicle SDK"))
    if (s.source == SourceMode.SIMULATION) add("simTime" to hms(s.simTimeSec) + "  (${s.simSpeed}×${if (s.simPlaying) " ▶" else " ❚❚"})")
    add("label" to (s.label ?: "-"))
    add("trip" to s.trip.name)
    add("DriveState" to s.driveState.name)
    add("AppMode" to s.mode.name + (s.pauseCause?.let { " ($it)" } ?: ""))
    add("speed" to sig(s, Signal.SPEED, s.sample.speedKph?.let { "%.1f km/h".format(Locale.US, it) }))
    add("gear" to sig(s, Signal.GEAR, s.sample.gear?.name))
    add("parking brake" to sig(s, Signal.PARKING_BRAKE, s.sample.parkingBrakeOn?.toString()))
    add("battery" to sig(s, Signal.BATTERY_SOC, s.sample.batterySocPercent?.let { "$it %" }))
    add("charging" to sig(s, Signal.CHARGING, s.sample.charging?.toString()))
    add("hvac temp" to sig(s, Signal.HVAC_TEMP, s.sample.hvacTempC?.let { "%.1f ℃".format(Locale.US, it) }))
    add("recirc" to sig(s, Signal.HVAC_RECIRCULATION, s.sample.recirculationOn?.toString()))
    add("doors" to sig(s, Signal.DOOR, s.sample.doors?.let { d -> "운전석 ${d.driverOpen} · 뒤 ${d.rearLeftOpen}/${d.rearRightOpen}" }))
    add("ETA" to (s.eta?.let { "${it.remainDistanceM / 1000} km · ${it.remainTimeSec / 60} 분" } ?: "-"))
    add("motion" to "${trip.motion.index} ${trip.motion.level} · 최고 ${trip.motion.peak} · 횡가속 ${trip.motion.lateralSource}")
    add("forecast" to (trip.forecast?.let { "${it.startInM.toInt()}m 뒤 ${it.durationSec}s · 예상 ${it.predictedIndex} · 권장 ${it.recommendedKph}km/h" } ?: "-"))
    add("wait" to (if (trip.wait.active) "${trip.wait.elapsedSec}s / ${trip.wait.limitSec}s${if (trip.wait.overdue) " 초과" else ""} · 폰 ${trip.share.pageUrl ?: "연결 안 됨"}" else "-"))
    add("arrival" to (trip.arrival?.let { if (it.strong) "내렸나요? (강하게)" else "내렸나요?" } ?: if (trip.leftConfirmed) "내렸어요" else "-"))
    add("heading" to sig(s, Signal.HEADING, s.sample.headingDeg?.let { "%.0f°".format(Locale.US, it) }))
    add("fatigue" to (trip.fatigue?.let { "${it.score} ${it.level} · 연속 ${it.continuousDriveMinutes}분" } ?: "-"))
    add("rest stops" to (trip.stopSource?.let { "${trip.stopCount}곳 ($it)" } ?: "-"))
    add("plan" to (trip.plan?.stops?.joinToString { it.stop.name } ?: "-"))
    add("suggestion" to (trip.suggestion ?: "-"))
    add("spoken" to (trip.lastSpoken ?: "-"))
    add("waypoints" to s.waypoints.joinToString().ifEmpty { "-" })
}

private fun sig(s: Snapshot, signal: Signal, value: String?): String =
    if (signal !in s.capabilities) "미지원" else "${value ?: "(값 없음)"}  [${s.sources[signal] ?: "?"}]"

private fun hms(tSec: Double): String {
    val t = tSec.toInt()
    return "%02d:%02d:%02d".format(Locale.US, t / 3600, t / 60 % 60, t % 60)
}
