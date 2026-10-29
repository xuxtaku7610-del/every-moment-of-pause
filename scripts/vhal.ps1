# 쉼표 — 에뮬레이터 차량 상태를 시연 장면에 맞추고 런처·앱을 캡처한다 (BACKEND_FIRST 3번).
#
# 사용:  powershell -ExecutionPolicy Bypass -File scripts\vhal.ps1 <check|drive|pause|charge|arrive>
#
# 속성 ID는 SDK android.car.jar의 VehiclePropertyIds 상수(javap)와 에뮬레이터 dumpsys로 확인한 값이다 (docs/vhal_check.md).
# 확인된 제약 (Pleos_Connect_API34, 2026-10-29):
#   - ro.build.type = user → `cmd car_service`의 모든 하위 명령(-h, get-property-value, inject-vhal-event)이
#     "requires non-user build" SecurityException. `adb root`도 거부.
#   - PERF_VEHICLE_SPEED·GEAR_SELECTION·CURRENT_GEAR·PARKING_BRAKE_ON·EV_BATTERY_LEVEL·EV_CHARGE_STATE는 VHAL access=READ.
#     앱이 값을 쓸 수 없다.
# 그래서 user 빌드에서는 값을 넣는 대신 "넣어야 할 값"을 표로 보여 주고(에뮬레이터 확장 컨트롤 등으로 사람이 조작),
# 그 뒤 런처 캡처와 앱이 실제로 받은 신호(logcat PauseSignals)를 기록한다.
# non-user(userdebug/eng) 빌드에서는 AOSP CarShellCommand의 inject-vhal-event로 넣는다 — 이 형식은 이 에뮬레이터에서
# 확인하지 못했다(-h가 막힘). userdebug 이미지에서 `adb shell cmd car_service -h`로 먼저 확인할 것.

param(
    [Parameter(Mandatory = $true)][ValidateSet('check', 'drive', 'pause', 'charge', 'arrive')][string]$Scene,
    # 사람 조작을 기다리지 않고 바로 캡처 (자동 점검용)
    [switch]$NoWait
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root 'artifacts\vhal'
New-Item -ItemType Directory -Force $out | Out-Null

# VehiclePropertyIds (android.car.jar)
$P = @{
    PERF_VEHICLE_SPEED       = 291504647   # float, m/s
    GEAR_SELECTION           = 289408000   # int, VehicleGear: P=4 R=2 N=1 D=8
    CURRENT_GEAR             = 289408001
    PARKING_BRAKE_ON         = 287310850   # boolean
    SEAT_BELT_BUCKLED        = 354421634   # boolean, 좌석 영역 (운전석 ROW_1_LEFT = 1)
    EV_BATTERY_LEVEL         = 291504905   # float, Wh
    INFO_EV_BATTERY_CAPACITY = 291504390   # float, Wh
    EV_CHARGE_STATE          = 289410881   # int, EvChargeState: CHARGING=1 FULLY_CHARGED=2 NOT_CHARGING=3
}

# 장면별 목표 값. 배터리 Wh는 용량을 모르면 %로만 안내한다.
$Scenes = @{
    drive  = [ordered]@{ GEAR_SELECTION = 8; CURRENT_GEAR = 8; PERF_VEHICLE_SPEED = 27.8; PARKING_BRAKE_ON = 0; SEAT_BELT_BUCKLED = 1; EV_BATTERY_PCT = 62; EV_CHARGE_STATE = 3 }
    pause  = [ordered]@{ GEAR_SELECTION = 4; CURRENT_GEAR = 4; PERF_VEHICLE_SPEED = 0; PARKING_BRAKE_ON = 1; SEAT_BELT_BUCKLED = 0; EV_BATTERY_PCT = 55; EV_CHARGE_STATE = 3 }
    charge = [ordered]@{ GEAR_SELECTION = 4; CURRENT_GEAR = 4; PERF_VEHICLE_SPEED = 0; PARKING_BRAKE_ON = 1; SEAT_BELT_BUCKLED = 0; EV_BATTERY_PCT = 34; EV_CHARGE_STATE = 1 }
    arrive = [ordered]@{ GEAR_SELECTION = 4; CURRENT_GEAR = 4; PERF_VEHICLE_SPEED = 0; PARKING_BRAKE_ON = 1; SEAT_BELT_BUCKLED = 0; EV_BATTERY_PCT = 21; EV_CHARGE_STATE = 3 }
}

# Windows PowerShell 5.1은 네이티브 명령의 stderr를 오류 레코드로 감싸므로 이 안에서만 Continue로 두고 문자열로 모은다.
function Invoke-Adb([string[]]$a) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & adb.exe @a 2>&1 | ForEach-Object { "$_" } | Out-String } finally { $ErrorActionPreference = $old }
}

function BuildType { (Invoke-Adb @('shell', 'getprop', 'ro.build.type')).Trim() }

function CanInject { (BuildType) -ne 'user' }

function Check {
    $bt = BuildType
    Write-Output "build type: $bt"
    Write-Output "car_service shell: $((Invoke-Adb @('shell','cmd','car_service','-h')).Split("`n")[0..1] -join ' ')"
    $dump = Invoke-Adb @('shell', 'dumpsys', 'car_service', '--services', 'VehicleHal')
    foreach ($k in $P.Keys) {
        $line = ($dump -split "`n" | Where-Object { $_ -match "prop: $($P[$k])," } | Select-Object -First 1)
        $access = if ($line -match 'access: (\d+)') { $Matches[1] } else { '없음' }
        Write-Output ("{0,-26} {1,10}  access={2}" -f $k, $P[$k], $access)
    }
}

function Inject($name, $value) {
    $id = $P[$name]
    $area = if ($name -eq 'SEAT_BELT_BUCKLED') { @('1') } else { @() }
    # AOSP CarShellCommand: inject-vhal-event <PROPERTY_ID> [ZONE] <data> — 이 에뮬레이터(user)에서는 미확인.
    $cmdArgs = @('shell', 'cmd', 'car_service', 'inject-vhal-event', "$id") + $area + @("$value")
    Write-Output (Invoke-Adb $cmdArgs).Trim()
}

function Capture($scene) {
    Start-Sleep -Seconds 3
    $png = Join-Path $out "$scene.png"
    cmd /c "adb exec-out screencap -p > `"$png`""
    $log = (Invoke-Adb @('logcat', '-d', '-s', 'PauseSignals:I')) -split "`n" | Where-Object { $_ -match 'PauseSignals' } | Select-Object -Last 1
    Write-Output "캡처: $png"
    Write-Output "앱이 받은 신호: $log"
}

if ($Scene -eq 'check') { Check; exit 0 }

$target = $Scenes[$Scene]
Write-Output "장면 '$Scene' 목표 값:"
$target.GetEnumerator() | ForEach-Object { Write-Output ("  {0,-20} {1}" -f $_.Key, $_.Value) }

# 앱은 실차 모드로 띄운다 (디버그 빌드: --ez vehicle true). 속성 변화는 앱 로그로 확인.
Invoke-Adb @('logcat', '-c') | Out-Null
Invoke-Adb @('shell', 'am', 'start', '-n', 'com.emop.pause/.MainActivity', '--ez', 'vehicle', 'true') | Out-Null

if (CanInject) {
    foreach ($kv in $target.GetEnumerator()) {
        if ($kv.Key -eq 'EV_BATTERY_PCT') { continue } # Wh 변환에 용량이 필요 — check 결과로 계산해 수동 입력
        Inject $kv.Key $kv.Value
    }
} else {
    Write-Output ''
    Write-Output 'user 빌드라 값을 넣을 수 없습니다 (car_service 명령 차단, 속성은 READ 전용).'
    Write-Output '에뮬레이터 확장 컨트롤(…)의 차량 데이터 패널 등에서 위 값으로 직접 바꾼 뒤 Enter를 누르세요.'
    Write-Output '바꿀 수 없으면 시뮬레이터로 시연합니다: adb shell am start -n com.emop.pause/.MainActivity --es sim seoul-busan-family'
    if (-not $NoWait) { [void](Read-Host '준비되면 Enter') }
}
Capture $Scene
