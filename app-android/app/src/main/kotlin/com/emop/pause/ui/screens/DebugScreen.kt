package com.emop.pause.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.emop.pause.core.model.Signal
import com.emop.pause.runtime.SourceMode
import com.emop.pause.runtime.Snapshot
import com.emop.pause.sim.ScenarioPlayer
import com.emop.pause.ui.theme.LocalSpacing
import java.util.Locale

/**
 * 디버그 화면 (디버그 빌드 전용): 현재 신호·주행 상태·AppMode를 실시간으로 보여준다 (todo M1).
 * 개발 도구이므로 시뮬레이션 중 DriveState와 상관없이 표시한다. 릴리스 빌드에는 진입 경로가 없다.
 */
@Composable
fun DebugScreen(
    s: Snapshot,
    scenarios: List<String>,
    onScenario: (String) -> Unit,
    onVehicle: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSpeed: (Int) -> Unit,
) {
    val sp = LocalSpacing.current
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize()) {
        Row(Modifier.padding(sp.lg), horizontalArrangement = Arrangement.spacedBy(sp.xl)) {
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(sp.sm)) {
                Text("DEBUG · 신호 모니터", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                scenarios.forEach { name ->
                    FilterChip(selected = s.scenarioName == name, onClick = { onScenario(name) }, label = { Text(name) })
                }
                FilterChip(selected = s.source == SourceMode.VEHICLE, onClick = onVehicle, label = { Text("실차 SDK") })
                if (s.source == SourceMode.SIMULATION) {
                    Row(horizontalArrangement = Arrangement.spacedBy(sp.sm)) {
                        OutlinedButton(onClick = if (s.simPlaying) onPause else onPlay) { Text(if (s.simPlaying) "일시정지" else "재생") }
                        ScenarioPlayer.SPEEDS.forEach { x ->
                            FilterChip(selected = s.simSpeed == x, onClick = { onSpeed(x) }, label = { Text("${x}×") })
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(sp.xs)) {
                val rows = buildList {
                    add("source" to (s.scenarioName?.let { "sim: $it" } ?: "vehicle SDK"))
                    if (s.source == SourceMode.SIMULATION) add("simTime" to clock(s.simTimeSec) + "  (${s.simSpeed}×${if (s.simPlaying) " ▶" else " ❚❚"})")
                    add("label" to (s.label ?: "-"))
                    add("trip" to s.trip.name)
                    add("DriveState" to s.driveState.name)
                    add("AppMode" to s.mode.name + (s.pauseCause?.let { " ($it)" } ?: ""))
                    add("speed" to signal(s, Signal.SPEED, s.sample.speedKph?.let { "%.1f km/h".format(Locale.US, it) }))
                    add("gear" to signal(s, Signal.GEAR, s.sample.gear?.name))
                    add("battery" to signal(s, Signal.BATTERY_SOC, s.sample.batterySocPercent?.let { "$it %" }))
                    add("charging" to signal(s, Signal.CHARGING, s.sample.charging?.toString()))
                    add("hvac temp" to signal(s, Signal.HVAC_TEMP, s.sample.hvacTempC?.let { "%.1f ℃".format(Locale.US, it) }))
                    add("hvac A/C" to signal(s, Signal.HVAC_AC, s.sample.hvacAcOn?.toString()))
                    add("recirc" to signal(s, Signal.HVAC_RECIRCULATION, s.sample.recirculationOn?.toString()))
                    add("ETA" to (s.eta?.let { "${it.remainDistanceM / 1000} km · ${it.remainTimeSec / 60} 분" } ?: "-"))
                }
                rows.forEach { (k, v) ->
                    Row {
                        Text(k, Modifier.width(180.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

private fun signal(s: Snapshot, signal: Signal, value: String?): String =
    if (signal !in s.capabilities) "미지원" else value ?: "(값 없음)"

private fun clock(tSec: Double): String {
    val t = tSec.toInt()
    return "%02d:%02d:%02d".format(Locale.US, t / 3600, t / 60 % 60, t % 60)
}
