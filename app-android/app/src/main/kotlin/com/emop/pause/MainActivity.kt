package com.emop.pause

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.DriveState
import com.emop.pause.ui.screens.GlanceScreen
import com.emop.pause.ui.screens.PauseScreen
import com.emop.pause.ui.screens.PrepareScreen
import com.emop.pause.ui.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDebugExtras(intent)
        setContent {
            PauseTheme {
                val appMode by viewModel.appMode.collectAsStateWithLifecycle()
                val driveState by viewModel.driveState.collectAsStateWithLifecycle()
                PauseRoot(appMode, driveState, onStart = { viewModel.setAppMode(AppMode.DRIVING) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyDebugExtras(intent)
    }

    /**
     * 디버그 빌드 전용: `adb shell am start -n com.emop.pause/.MainActivity --es mode PAUSE --es drive PARKED`
     * 로 화면을 전환한다 (M0 화면 전환 확인용, M1에서 시뮬레이터로 대체).
     */
    private fun applyDebugExtras(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        intent.getStringExtra("mode")?.let { name -> AppMode.entries.find { it.name == name }?.let(viewModel::setAppMode) }
        intent.getStringExtra("drive")?.let { name -> DriveState.entries.find { it.name == name }?.let(viewModel::setDriveState) }
    }
}

@Composable
fun PauseRoot(appMode: AppMode, driveState: DriveState, onStart: () -> Unit) {
    // 최우선 규칙: DriveState가 DRIVING/RESTRICTED면 AppMode와 상관없이 주행 UI (architecture §5).
    if (driveState.forcesDrivingUi) {
        GlanceScreen()
        return
    }
    when (appMode) {
        AppMode.PREPARE, AppMode.ARRIVED -> PrepareScreen(onStart) // TODO(M3): ARRIVED → 여행 리포트
        AppMode.DRIVING -> GlanceScreen()
        AppMode.PAUSE -> PauseScreen()
    }
}
