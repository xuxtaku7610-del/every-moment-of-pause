package com.emop.pause

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emop.pause.uistate.AppUiState
import com.emop.pause.uistate.Scene
import com.emop.pause.uistate.Screen
import com.emop.pause.uistate.UiEvent
import com.emop.pause.ui.screens.ArrivalScreen
import com.emop.pause.ui.screens.SummaryScreen
import com.emop.pause.ui.screens.DemoActions
import com.emop.pause.ui.screens.DemoPanel
import com.emop.pause.ui.screens.DepartScreen
import com.emop.pause.ui.screens.DriveScreen
import com.emop.pause.ui.screens.EmergencyScreen
import com.emop.pause.ui.screens.PetProfileScreen
import com.emop.pause.ui.screens.PlacesScreen
import com.emop.pause.ui.screens.RestScreen
import com.emop.pause.ui.screens.SettingsScreen
import com.emop.pause.ui.screens.OnboardingScreen
import com.emop.pause.ui.screens.CompareScreen
import com.emop.pause.uistate.CompareChart
import com.emop.pause.ui.screens.LocalDemoData
import com.emop.pause.ui.screens.StimulusScreen
import com.emop.pause.ui.screens.TripLogScreen
import com.emop.pause.ui.screens.VetsScreen
import com.emop.pause.ui.screens.WaitScreen
import com.emop.pause.ui.theme.LocalMotionAllowed
import com.emop.pause.ui.theme.PauseTheme
import com.emop.pause.ui.theme.designScale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    /** 디버그 전용: 앱 창 폭을 강제로 줄여 1/2·1/3 창 레이아웃을 검증한다 (UI_SPEC §7-2). 0이면 끔. */
    private var forceWidth by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (BuildConfig.DEBUG) viewModel.useScenario(DEFAULT_SCENARIO) else viewModel.useVehicle()
            requestSpeedIfNeeded()
            // 처음 만들 때만: 분할 창 전환·다크 전환으로 다시 만들어질 때 첫 실행 명령(seek 등)을 또 적용하지 않게 (10/31)
            applyDebugExtras(intent)
        }
        val licenses = runCatching { assets.open("licenses/Pretendard-OFL.txt").bufferedReader().use { it.readText() } }.getOrDefault("")
        // 고갯길 비교 큰 그래프 (UPGRADE 1): docs/plan/demo_compare.csv·summary 사본(DemoCompareTest가 같은지 확인)
        val compareChart = runCatching {
            fun read(n: String) = assets.open("compare/$n").bufferedReader().use { it.readText() }
            CompareChart.parse(read("demo_compare.csv"), read("demo_compare_summary.txt"))
        }.getOrNull()
        setContent {
            PauseTheme(scale = designScale(screenWidthDp())) {
                val ui by viewModel.ui.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalMotionAllowed provides ui.motionAllowed, LocalDemoData provides ui.demoData) {
                    val w = forceWidth
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                        Box(if (w > 0) Modifier.width(w.dp).fillMaxHeight() else Modifier.fillMaxSize()) {
                            PauseRoot(ui, licenses, ::onEvent)
                        }
                    }
                    if (ui.demoOpen) DemoOverlay(viewModel, ui)
                    val app by viewModel.app.collectAsStateWithLifecycle()
                    // 정차 중에만 (주행이 시작되면 그리지 않는다, DS-01)
                    if (app.compareOpen && !ui.drivingLocked && compareChart != null) CompareScreen(compareChart) { viewModel.setCompareOpen(false) }
                }
            }
        }
    }

    /** 화면 전체 폭(dp). 분할 창이어도 글자 배율이 바뀌지 않게 창이 아니라 화면 기준 (STYLE_V5 §0). */
    private fun screenWidthDp(): Int {
        val density = resources.displayMetrics.density
        val px = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) windowManager.maximumWindowMetrics.bounds.width()
        else resources.displayMetrics.widthPixels
        return (px / density).toInt()
    }

    /** 마이크 권한이 필요한 이벤트는 권한 창을 먼저 띄운다 (정차 중 화면에서만 나온다). */
    private fun onEvent(e: UiEvent) {
        if (e == UiEvent.StartTrip) requestMicIfNeeded()
        viewModel.onEvent(e)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyDebugExtras(intent)
    }

    /**
     * 디버그 빌드 전용 adb 제어 (스크린샷·시연 리허설용):
     * `adb shell am start -n com.emop.pause/.MainActivity --es sim pet-trip --ei speed 100 --es action play`
     * - sim: 시나리오 이름 / vehicle: true면 실차 SDK (+ fill: 실차에 없는 신호만 채울 시나리오 이름)
     * - action: play | pause, seek: 초(정수), speed: 1 | 10 | 100
     * - say: 운전자 응답 문장 주입 (예: `--es say 응`)
     * - scene: 시안 장면 고정 (drive, calm, urgent, …, none) / forceWidth: 앱 폭(dp) 강제 / demo: 발표 모드 패널
     * - onboard: done이면 첫 실행 프로필 화면 건너뜀, reset이면 내 데이터 삭제 후 처음부터
     * - shareBase: 보호자 폰이 여는 대기 페이지 주소 앞부분 (예: `--es shareBase http://192.168.0.10:8000`)
     * - door: 시뮬레이터 문 상태 rear | driver | both | closed | none(시나리오 값) — 문 이벤트 수동 재현
     * - captureClean: true면 "시연 데이터" 표시를 숨김 (스토어 스크린샷 캡처 전용, false로 되돌림)
     * - emergency: 긴급 도움 toxin | breathing | seizure | bleeding (그 카드) | menu (상황 고르기) | none (닫기) — 정차 중만
     */
    private fun applyDebugExtras(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        intent.getStringExtra("sim")?.let(viewModel::useScenario)
        if (intent.getBooleanExtra("vehicle", false)) viewModel.useVehicle(intent.getStringExtra("fill"))
        if (intent.hasExtra("speed")) viewModel.setSpeed(intent.getIntExtra("speed", 1))
        if (intent.hasExtra("seek")) viewModel.seek(intent.getIntExtra("seek", 0).toDouble())
        when (intent.getStringExtra("action")) {
            "play" -> viewModel.play()
            "pause" -> viewModel.pause()
        }
        intent.getStringExtra("say")?.let(viewModel::reply)
        intent.getStringExtra("scene")?.let { n -> viewModel.setScene(Scene.entries.firstOrNull { it.name.equals(n, true) }) }
        if (intent.hasExtra("captureClean")) viewModel.setCaptureClean(intent.getBooleanExtra("captureClean", false))
        if (intent.hasExtra("forceWidth")) forceWidth = intent.getIntExtra("forceWidth", 0)
        if (intent.hasExtra("demo")) viewModel.setDemoOpen(intent.getBooleanExtra("demo", false))
        if (intent.hasExtra("compare")) viewModel.setCompareOpen(intent.getBooleanExtra("compare", false))
        intent.getStringExtra("memo")?.let(viewModel::heardMemo)
        if (intent.hasExtra("guidance")) viewModel.setGuidanceMode(intent.getBooleanExtra("guidance", false))
        if (intent.hasExtra("cabin")) viewModel.setDemoCabin(intent.getIntExtra("cabin", 0).toFloat())
        intent.getStringExtra("shareBase")?.let(viewModel::setShareBase)
        intent.getStringExtra("calib")?.let(viewModel::seedCalibration)
        intent.getStringExtra("door")?.let(viewModel::setDemoDoors)
        intent.getStringExtra("emergency")?.let(viewModel::adbEmergency)
        when (intent.getStringExtra("onboard")) {
            "done" -> viewModel.skipProfile()
            "reset" -> viewModel.deleteData()
        }
    }

    /** 음성 응답용 마이크 권한. 정차 중(출발 버튼)에서만 묻는다. 거절해도 버튼으로 응답할 수 있다. */
    private fun requestMicIfNeeded() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
        }
    }

    /**
     * AAOS 차량 속성 중 속도(CAR_SPEED)와 배터리·충전 상태(CAR_ENERGY)는 dangerous 권한이다 (에뮬레이터 dumpsys로 확인, 2026-10-29).
     * 정차 중 앱 시작 때 한 번 묻는다. 거절하면 그 신호 없이 동작한다.
     */
    private fun requestSpeedIfNeeded() {
        val missing = VEHICLE_PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), REQ_SPEED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_SPEED && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) viewModel.onVehiclePermissionGranted()
    }

    private companion object {
        val VEHICLE_PERMISSIONS = listOf("android.car.permission.CAR_SPEED", "android.car.permission.CAR_ENERGY")
        const val REQ_SPEED = 2
        const val DEFAULT_SCENARIO = "pet-trip"
        const val REQ_MIC = 1
    }
}

/**
 * 화면 계약(docs/ui_contract.md)대로 [Screen]만 보고 그린다. 화면 선택 규칙은 ViewModel의 ScreenRouter가 정한다.
 * 새 디자인으로 바꿀 때는 이 함수 안의 화면 호출만 갈아 끼운다.
 */
@Composable
fun PauseRoot(ui: AppUiState, licenses: String, on: (UiEvent) -> Unit) {
    when (val sc = ui.screen) {
        is Screen.Depart -> DepartScreen(sc.ui, on)
        is Screen.Drive -> DriveScreen(sc.ui, on)
        is Screen.Rest -> RestScreen(sc.ui, on)
        is Screen.Wait -> WaitScreen(sc.ui, on)
        is Screen.Places -> PlacesScreen(sc.ui, on)
        is Screen.Vets -> VetsScreen(sc.ui, on)
        is Screen.Emergency -> EmergencyScreen(sc.ui, on)
        is Screen.Arrival -> ArrivalScreen(sc.ui, on)
        is Screen.Summary -> SummaryScreen(sc.ui, on)
        is Screen.TripLog -> TripLogScreen(sc.ui, on)
        is Screen.Stimulus -> StimulusScreen(sc.ui, on)
        is Screen.PetProfile -> PetProfileScreen(sc.ui, on)
        is Screen.Settings -> SettingsScreen(sc.ui, on, licenses)
        is Screen.Onboarding -> OnboardingScreen(sc.ui, on)
    }
}

/** 발표 모드 패널 (디버그 도구라 엔진 상태를 직접 본다. 화면 계약 밖). */
@Composable
private fun DemoOverlay(vm: MainViewModel, ui: AppUiState) {
    val s by vm.snapshot.collectAsStateWithLifecycle()
    val t by vm.tripUi.collectAsStateWithLifecycle()
    val compare by vm.compare.collectAsStateWithLifecycle()
    DemoPanel(
        s, t, vm.scenarioNames(), ui.scene,
        DemoActions(
            vm::useScenario, vm::useVehicle, vm::play, vm::pause, vm::setSpeed, vm::jump, vm::reply, vm::setScene,
            close = { vm.setDemoOpen(false) },
            compareBig = {
                vm.setDemoOpen(false)
                vm.setCompareOpen(true)
            },
            guidance = vm::setGuidanceMode,
            cabin = vm::setDemoCabin,
        ),
        compare,
    )
}
