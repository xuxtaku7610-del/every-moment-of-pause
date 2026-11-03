# 같이가개 — 시연 전 점검 (2026-11-02). 실행하면 항목마다 한 줄씩 OK / WARN / FAIL을 찍는다.
#
# 사용:  powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1                 # 점검만 (아무것도 켜지 않음)
#        powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1 -Prepare        # 꺼진 것(DB·서버·에뮬레이터·앱)을 켜고 점검
#        powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1 -Prepare -ShareBase http://<노트북 IP>:8000 -Rehearsal
#        powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1 -Stop           # 쉴 때: Gradle 데몬·서버·에뮬레이터·DB 정리
#        powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1 -Prepare -DetectIp   # Wi-Fi IPv4를 찾아 -ShareBase로 (폰 QR, 11/03)
#
# 점검 항목: Gradle 데몬 종료 → adb 기기·부팅 완료 → 앱 설치·버전 → PostgreSQL 컨테이너 → 서버 상태 → Ollama·gemma4:12b → PC 남은 메모리.
# 종료 코드: FAIL이 하나라도 있으면 1. Ollama가 없어도 앱은 템플릿 문장으로 동작하므로 WARN.
# 비밀값(.env·local.properties·키·비밀번호)은 읽지도 출력하지도 않는다. 서버 응답은 status·db·llm 값만 찍는다.
# 이 스크립트는 전역 설치·시스템 설정·AVD 설정을 바꾸지 않는다.

param(
    # 꺼진 DB·서버·에뮬레이터를 켜고, 앱이 없으면(또는 -Install) 디버그 APK를 설치한다
    [switch]$Prepare,
    [switch]$Install,
    # 보호자 폰·병원 접수함 QR에 넣을 노트북 주소 (같은 Wi-Fi). -Prepare와 함께
    [string]$ShareBase = '',
    # Wi-Fi IPv4를 찾아 -ShareBase(http://<IP>:8000)로 넣는다. 방화벽은 바꾸지 않고 필요한 관리자 명령만 출력한다 (11/03)
    [switch]$DetectIp,
    # 점검 뒤 기본 내비 안내 걸기 안내 → 리허설 1회 (scripts/rehearsal.py)
    [switch]$Rehearsal,
    [switch]$NoPrompt,
    [switch]$Stop,
    # 에뮬레이터가 꺼져 있을 때 켜기에 필요한 남은 메모리 (에뮬레이터 하나가 호스트에서 5~6GB)
    [int]$MinFreeMB = 6000
)

# 네이티브 명령(adb·docker)이 stderr에 쓰면 PowerShell 5.1은 오류로 본다 — 결과는 종료 코드와 값으로 직접 확인한다.
$ErrorActionPreference = 'Continue'
$Root = Split-Path -Parent $PSScriptRoot
$Avd = 'Pleos_Connect_API34'
$Pkg = 'com.emop.pause'
$Sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$Emulator = Join-Path $Sdk 'emulator\emulator.exe'
$Health = 'http://127.0.0.1:8000/api/v1/health'
$Ollama = 'http://127.0.0.1:11434/api/tags'
$Model = 'gemma4:12b'
$DbName = 'pause-db'
$Log = Join-Path $Root 'artifacts\demo_check'
New-Item -ItemType Directory -Force $Log | Out-Null
$script:Fails = 0

function Ok([string]$msg) { Write-Host "OK    $msg" -ForegroundColor Green }
function Warn([string]$msg) { Write-Host "WARN  $msg" -ForegroundColor Yellow }
function Fail([string]$msg) { Write-Host "FAIL  $msg" -ForegroundColor Red; $script:Fails++ }
function Info([string]$msg) { Write-Host "      $msg" }

function FreeMB { [int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1KB) }

function HealthJson {
    try { Invoke-RestMethod -TimeoutSec 3 $Health } catch { $null }
}

function EmulatorSerial {
    $lines = & adb devices 2>$null | Select-String -Pattern '^(\S+)\s+device$'
    if ($lines) { $lines[0].Matches[0].Groups[1].Value } else { $null }
}

function Booted { (& adb shell getprop sys.boot_completed 2>$null) -eq '1' }

function DbRunning {
    $s = & docker ps --filter "name=^$DbName$" --format '{{.Status}}' 2>$null
    if ($LASTEXITCODE -eq 0 -and $s -match '^Up') { $s } else { $null }
}

function AppVersion {
    $line = & adb shell dumpsys package $Pkg 2>$null | Select-String -Pattern 'versionName=(\S+)' | Select-Object -First 1
    if ($line) { $line.Matches[0].Groups[1].Value } else { $null }
}

# ───────────── 정리 (-Stop) ─────────────
if ($Stop) {
    Push-Location (Join-Path $Root 'app-android'); & .\gradlew.bat --stop | Out-Null; Pop-Location; Ok 'Gradle 데몬 멈춤'
    Get-CimInstance Win32_Process -Filter "Name='python.exe'" | Where-Object { $_.CommandLine -match 'uvicorn app.main:app' } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    Ok '서버(uvicorn) 멈춤'
    if (EmulatorSerial) { & adb emu kill | Out-Null; Ok '에뮬레이터 끔' }
    Push-Location $Root; & docker compose stop db 2>$null | Out-Null; Pop-Location; Ok 'DB 컨테이너 멈춤 (데이터는 볼륨에 남음)'
    Ok "남은 메모리 $(FreeMB) MB"
    exit 0
}

# ───────────── 폰 QR 주소 찾기 (-DetectIp) ─────────────
# Wi-Fi(무선) 어댑터의 IPv4 중 사설 주소를 고른다. 없으면 기본 경로가 걸린 어댑터. 169.254(자동 사설)·가상 어댑터(vEthernet·Docker·WSL)는 뺀다.
function WifiIPv4 {
    $all = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object { $_.IPAddress -notlike '169.254.*' -and $_.IPAddress -ne '127.0.0.1' -and $_.InterfaceAlias -notmatch 'vEthernet|Docker|WSL|Loopback|VirtualBox|VMware|Tailscale' }
    $wifi = $all | Where-Object { $_.InterfaceAlias -match 'Wi-?Fi|WLAN|Wireless|무선' } | Select-Object -First 1
    if ($wifi) { return @($wifi.IPAddress, "$($wifi.InterfaceAlias), Wi-Fi") }
    $route = Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue | Sort-Object RouteMetric | Select-Object -First 1
    if ($route) {
        $hit = $all | Where-Object { $_.InterfaceIndex -eq $route.InterfaceIndex } | Select-Object -First 1
        if ($hit) { return @($hit.IPAddress, "$($hit.InterfaceAlias), Wi-Fi 없음 → 기본 경로 어댑터 — 폰과 같은 공유기인지 확인") }
    }
    return $null
}

if ($DetectIp) {
    Write-Host '== 폰 QR 주소 (-DetectIp)' -ForegroundColor Cyan
    $found = WifiIPv4
    if (-not $found) {
        Fail 'Wi-Fi IPv4를 찾지 못했다 — 노트북을 폰과 같은 Wi-Fi에 연결하고 다시 실행 (또는 -ShareBase http://<IP>:8000 직접)'
    } else {
        $ip = $found[0]
        if ($ShareBase) { Info "-ShareBase가 이미 있어 그대로 쓴다: $ShareBase (찾은 주소 $ip, $($found[1]))" }
        else { $ShareBase = "http://${ip}:8000"; Ok "IPv4 $ip ($($found[1])) → -ShareBase $ShareBase" }
        # 방화벽: 읽기만 한다. 8000 포트 허용 규칙이 없으면 관리자 PowerShell에서 실행할 명령을 보여 준다(이 스크립트는 실행하지 않는다)
        $rule = Get-NetFirewallPortFilter -Protocol TCP -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq '8000' } |
            Get-NetFirewallRule -ErrorAction SilentlyContinue | Where-Object { $_.Enabled -eq 'True' -and $_.Direction -eq 'Inbound' -and $_.Action -eq 'Allow' }
        if ($rule) { Ok "방화벽 8000 허용 규칙 있음: $($rule[0].DisplayName)" }
        else {
            Warn '방화벽에 8000 포트 허용 규칙이 보이지 않는다 — 폰에서 열리지 않으면 관리자 PowerShell에서 아래 한 줄 (끝나면 Remove-NetFirewallRule로 지울 수 있다)'
            Info 'New-NetFirewallRule -DisplayName "gatigage demo 8000" -Direction Inbound -Protocol TCP -LocalPort 8000 -Action Allow -Profile Private'
            Info 'Remove-NetFirewallRule -DisplayName "gatigage demo 8000"'
        }
        Info "폰 브라우저에서 $ShareBase/api/v1/health 가 열리면 QR도 열린다 (서버는 --host 0.0.0.0, -Prepare가 그렇게 켠다)"
    }
}

# ───────────── 켜기 (-Prepare) ─────────────
if ($Prepare) {
    Write-Host '== 켜기 (-Prepare)' -ForegroundColor Cyan
    if (-not (DbRunning)) {
        Push-Location $Root; & docker compose up -d db 2>$null | Out-Null; Pop-Location
        Info 'DB 컨테이너 켬'
    }
    if (-not (HealthJson)) {
        $py = Join-Path $Root 'server\.venv\Scripts\python.exe'
        Start-Process -FilePath $py -ArgumentList '-m', 'uvicorn', 'app.main:app', '--host', '0.0.0.0', '--port', '8000' `
            -WorkingDirectory (Join-Path $Root 'server') -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $Log 'uvicorn.out.log') -RedirectStandardError (Join-Path $Log 'uvicorn.err.log')
        $deadline = (Get-Date).AddSeconds(40)
        while (-not (HealthJson) -and (Get-Date) -lt $deadline) { Start-Sleep -Seconds 2 }
        Info "서버 켬 (로그 $Log)"
    }
    if (-not (EmulatorSerial)) {
        $free = FreeMB
        if ($free -lt $MinFreeMB) {
            Fail "에뮬레이터를 켜기에 메모리가 부족하다 ($free MB < $MinFreeMB MB) — 브라우저 탭·Gradle 데몬을 정리하고 다시 실행"
            exit 1
        }
        Start-Process -FilePath $Emulator -ArgumentList '-avd', $Avd, '-no-snapshot-save', '-no-boot-anim' -WindowStyle Normal `
            -RedirectStandardOutput (Join-Path $Log 'emulator.out.log') -RedirectStandardError (Join-Path $Log 'emulator.err.log')
        Info '에뮬레이터 켜는 중 (최대 4분)…'
        & adb wait-for-device
        $deadline = (Get-Date).AddMinutes(4)
        while (-not (Booted) -and (Get-Date) -lt $deadline) { Start-Sleep -Seconds 3 }
    }
    $apk = Join-Path $Root 'app-android\app\build\outputs\apk\debug\app-debug.apk'
    if ((Booted) -and ($Install -or -not (AppVersion))) {
        if (Test-Path $apk) { & adb install -r $apk | Out-Null; Info "디버그 APK 설치 ($([int]((Get-Item $apk).Length / 1MB)) MB)" }
        else { Info '디버그 APK가 없다 — app-android에서 .\gradlew assembleDebug -Pkotlin.compiler.execution.strategy=in-process' }
    }
    if (Booted) {
        & adb shell am force-stop $Pkg   # 발표 직전에는 앱을 새로 띄운다 (BLOCKED 11/01)
        if ($ShareBase) { & adb shell am start -n "$Pkg/.MainActivity" --es shareBase $ShareBase | Out-Null; Info "QR 주소 앞부분 $ShareBase" }
    }
}

# ───────────── 점검 ─────────────
Write-Host '== 점검' -ForegroundColor Cyan

# 1. Gradle 데몬 종료 (빌드 데몬이 2~6GB를 잡고 있다)
Push-Location (Join-Path $Root 'app-android')
& .\gradlew.bat --stop 2>$null | Out-Null
$gradleCode = $LASTEXITCODE
Pop-Location
$java = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'GradleDaemon|KotlinCompileDaemon' }
if ($gradleCode -eq 0 -and -not $java) { Ok 'Gradle 데몬 종료 (./gradlew --stop)' }
elseif ($java) { Warn "Gradle·Kotlin 데몬 $(@($java).Count)개가 아직 떠 있다 — 다른 프로젝트 데몬일 수 있음" }
else { Fail "./gradlew --stop 실패 (종료 코드 $gradleCode)" }

# 2. adb 기기 연결·부팅 완료
$serial = EmulatorSerial
if ($serial) { Ok "adb 기기 연결 $serial" } else { Fail 'adb 기기 없음 — 에뮬레이터를 켜거나 -Prepare' }
if ($serial -and (Booted)) { Ok '부팅 완료 (sys.boot_completed=1)' }
elseif ($serial) { Fail '부팅이 끝나지 않았다 (sys.boot_completed≠1)' }

# 3. 앱 설치·버전
if ($serial) {
    $ver = AppVersion
    if ($ver) { Ok "앱 설치됨 $Pkg $ver" } else { Fail "앱 없음 $Pkg — -Prepare 또는 adb install -r app\build\outputs\apk\debug\app-debug.apk" }
}

# 4. PostgreSQL 컨테이너
$dbStatus = DbRunning
if ($dbStatus) { Ok "PostgreSQL 컨테이너 $DbName ($dbStatus)" } else { Fail "PostgreSQL 컨테이너 $DbName 꺼짐 — docker compose up -d db (Docker Desktop 확인)" }

# 5. 서버 상태
$h = HealthJson
if ($h -and $h.status) {
    $line = "서버 $Health status=$($h.status) db=$($h.db) llm=$($h.llm)"
    if ($h.db -eq 'ok') { Ok $line } else { Fail $line }
} else { Fail "서버 응답 없음 $Health — -Prepare 또는 server에서 uvicorn 실행" }

# 6. Ollama·모델 (없어도 템플릿 문장으로 동작)
try {
    $tags = Invoke-RestMethod -TimeoutSec 3 $Ollama
    if (@($tags.models | Where-Object { $_.name -eq $Model }).Count -gt 0) { Ok "Ollama 127.0.0.1:11434 · $Model 있음" }
    else { Warn "Ollama는 응답하지만 $Model 이 없다 — 쉼표 문장은 템플릿으로 동작" }
} catch { Warn 'Ollama 응답 없음 (127.0.0.1:11434) — 쉼표 문장은 템플릿으로 동작' }

# 7. PC 남은 메모리
$free = FreeMB
$chrome = (Get-Process chrome -ErrorAction SilentlyContinue | Measure-Object WorkingSet64 -Sum).Sum
$chromeNote = if ($chrome) { " · Chrome $([int]($chrome / 1MB)) MB" } else { '' }
if ($serial) {
    if ($free -ge 3000) { Ok "PC 남은 메모리 $free MB (에뮬레이터 켜진 상태)$chromeNote" }
    else { Warn "PC 남은 메모리 $free MB — 3GB 아래면 Windows가 에뮬레이터를 끌 수 있다. 브라우저 탭을 줄인다$chromeNote" }
} else {
    if ($free -ge $MinFreeMB) { Ok "PC 남은 메모리 $free MB (에뮬레이터를 켜려면 $MinFreeMB MB 이상)$chromeNote" }
    else { Fail "PC 남은 메모리 $free MB < $MinFreeMB MB — 에뮬레이터가 켜지다 꺼질 수 있다$chromeNote" }
}

Write-Host ''
if ($script:Fails -gt 0) { Write-Host "점검 결과: FAIL $($script:Fails)개" -ForegroundColor Red; exit 1 }
Write-Host '점검 결과: 모두 OK (WARN은 시연 가능)' -ForegroundColor Green
if (-not $Rehearsal) { exit 0 }

# ───────────── 리허설 (-Rehearsal) ─────────────
Write-Host ''
Write-Host '== 기본 내비 안내 걸기 (사람이 한다 — docs/recording.md "기본 내비 안내 걸기")' -ForegroundColor Cyan
Info '1) 런처 아래 내비 아이콘으로 기본 내비를 연다'
Info '2) "어디로 갈까요?" → 최근 목록 강릉씨티호텔 오른쪽 화살표(바로 안내)'
Info '3) 아래 카드에 "안내 종료"가 보이면 안내 중'
& adb shell monkey -p ai.umos.maps.android.navigation.app -c android.intent.category.LAUNCHER 1 2>$null | Out-Null
if (-not $NoPrompt) { [void](Read-Host '      안내를 걸었으면 Enter (건너뛰려면 그냥 Enter)') }
$started = Get-Date
& python (Join-Path $Root 'scripts\rehearsal.py')
$code = $LASTEXITCODE
$sec = [int]((Get-Date) - $started).TotalSeconds
if ($code -ne 0) { Fail "리허설 실패 ($sec 초) — artifacts\rehearsal\timing.md 확인"; exit 1 }
Ok "리허설 통과 ($sec 초) · 남은 메모리 $(FreeMB) MB"
Info '쉴 때는 -Stop 으로 정리한다.'
