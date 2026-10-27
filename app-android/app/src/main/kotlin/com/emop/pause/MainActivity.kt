package com.emop.pause

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emop.pause.core.model.AppMode
import com.emop.pause.runtime.Snapshot
import com.emop.pause.ui.screens.ArrivedScreen
import com.emop.pause.ui.screens.DebugScreen
import com.emop.pause.ui.screens.GlanceScreen
import com.emop.pause.ui.screens.PauseScreen
import com.emop.pause.ui.screens.PrepareScreen
import com.emop.pause.ui.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var showDebug by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (BuildConfig.DEBUG) viewModel.useScenario(DEFAULT_SCENARIO) else viewModel.useVehicle()
        }
        applyDebugExtras(intent)
        setContent {
            PauseTheme {
                val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
                if (BuildConfig.DEBUG && showDebug) {
                    DebugScreen(
                        s = snapshot,
                        scenarios = viewModel.scenarioNames(),
                        onScenario = viewModel::useScenario,
                        onVehicle = viewModel::useVehicle,
                        onPlay = viewModel::play,
                        onPause = viewModel::pause,
                        onSpeed = viewModel::setSpeed,
                    )
                } else {
                    PauseRoot(snapshot, onStart = viewModel::startTrip)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyDebugExtras(intent)
    }

    /**
     * 디버그 빌드 전용 adb 제어 (스크린샷·시연 리허설용):
     * `adb shell am start -n com.emop.pause/.MainActivity --es sim seoul-busan-family --ei speed 100 --es action play --ez debug true`
     * - sim: 시나리오 이름 / vehicle: true면 실차 SDK
     * - action: play | pause, seek: 초(정수), speed: 1 | 10 | 100, debug: 디버그 화면 on/off
     */
    private fun applyDebugExtras(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        intent.getStringExtra("sim")?.let(viewModel::useScenario)
        if (intent.getBooleanExtra("vehicle", false)) viewModel.useVehicle()
        if (intent.hasExtra("speed")) viewModel.setSpeed(intent.getIntExtra("speed", 1))
        if (intent.hasExtra("seek")) viewModel.seek(intent.getIntExtra("seek", 0).toDouble())
        when (intent.getStringExtra("action")) {
            "play" -> viewModel.play()
            "pause" -> viewModel.pause()
        }
        if (intent.hasExtra("debug")) showDebug = intent.getBooleanExtra("debug", false)
    }

    private companion object {
        const val DEFAULT_SCENARIO = "seoul-busan-family"
    }
}

@Composable
fun PauseRoot(s: Snapshot, onStart: () -> Unit) {
    // 최우선 규칙: DriveState가 DRIVING/RESTRICTED면 AppMode와 상관없이 주행 UI (architecture §5).
    if (s.driveState.forcesDrivingUi) {
        GlanceScreen(s)
        return
    }
    when (s.mode) {
        AppMode.PREPARE -> PrepareScreen(onStart)
        AppMode.DRIVING -> GlanceScreen(s)
        AppMode.PAUSE -> PauseScreen(s)
        AppMode.ARRIVED -> ArrivedScreen()
    }
}
