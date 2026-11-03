# 릴리스 APK 빌드 절차

릴리스 빌드는 **오프라인**이다(서버 주소 비어 있음 → 내장 휴게소·캐시·템플릿). 초안 콘텐츠(`content/drafts/`)는 들어가지 않는다.

## 0. keystore 없이 미리 끝낸 것 (2026-11-02)
- 서명 값: `app-android/local.properties`(gitignore)의 네 줄, 또는 환경 변수(환경 변수가 우선). 저장소에는 비밀이 없다(`app/build.gradle.kts`).
  | local.properties 키 | 환경 변수 | 값 |
  | --- | --- | --- |
  | `pause.keystore` | `PAUSE_KEYSTORE` | `C:/keys/pause-release.jks` |
  | `pause.keyAlias` | `PAUSE_KEY_ALIAS` | `pause` |
  | `pause.keystorePassword` | `PAUSE_KEYSTORE_PASSWORD` | <비밀번호> |
  | `pause.keyPassword` | `PAUSE_KEY_PASSWORD` | <비밀번호> |
- 값이 없으면 `assembleRelease`가 패키징 전에 "릴리스 서명 값이 없습니다 … docs/release.md 1~2단계" 안내로 멈춘다. `./gradlew test`·디버그 빌드는 영향 없음.
- versionName **1.0.0**(versionCode는 커밋 수 + 1, 11/02 기준 31). 출력 파일 이름 **`gatigage-v1.0.0.apk`**.
- R8/minify: **기본 꺼짐**(제출본도 끈 채로). `-Ppause.minify=true`를 줄 때만 켜진다 — 시연 직전 Hilt·Room 위험 때문에 쓰지 않는다.
- 11/02 확인: 디버그 키로 임시 서명한 빌드 → `gatigage-v1.0.0.apk` 약 57 MB(축소 없음), `apksigner verify` 통과, versionName 1.0.0. 확인 뒤 지웠다.
- 참고(옵트인 R8로 확인한 것): 축소본 11.2 MB. 첫 실행 크래시(`TripCoordinator` 초기화 순서)를 찾아 고쳤다 — 이 수정은 축소 없는 빌드에도 들어 있다.

## 1. 서명 키 만들기 (처음 한 번, 사용자)
키는 **저장소 밖 `C:\keys\`**에 둔다. 인증서 이름 필드(-dname)에 실명을 넣지 않는다(공모전 실명 노출 금지).
```powershell
# 1) 폴더
New-Item -ItemType Directory -Force C:\keys
# 2) keytool 위치 확인 (Temurin 21의 bin)
& "$env:JAVA_HOME\bin\keytool.exe" -help 2>$null | Select-Object -First 1
# 3) 키 만들기 — <비밀번호> 자리에 직접 정한 비밀번호 (명령 기록에 남기 싫으면 -storepass·-keypass를 빼면 물어본다)
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore C:\keys\pause-release.jks `
  -alias pause -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass <비밀번호> -keypass <비밀번호> `
  -dname "CN=Gatigage, OU=EMOP, O=EMOP, C=KR"
# 4) 확인 — 소유자(Owner)에 실명이 없어야 한다
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore C:\keys\pause-release.jks -alias pause | Select-String "Owner|Valid"
```
- 비밀번호는 비밀번호 관리자에만 적는다. `C:\keys\pause-release.jks`는 USB 등 저장소 밖에 한 벌 더 백업한다(잃으면 같은 앱으로 업데이트 불가).
- `C:\keys`는 저장소 밖이라 git에 잡히지 않는다. `.gitignore`에 `*.jks`·`*.keystore`·`local.properties`도 들어 있다(11/02 확인).

## 2. 서명 값 넣기 (사용자, 둘 중 하나)
**가) local.properties** — `app-android/local.properties`(이미 있는 `sdk.dir` 줄은 그대로 두고) 맨 아래에 추가:
```properties
pause.keystore=C:/keys/pause-release.jks
pause.keyAlias=pause
pause.keystorePassword=<비밀번호>
pause.keyPassword=<비밀번호>
```
- 경로는 `/`로 쓴다(`\`는 properties에서 이스케이프 문자). 이 파일은 gitignore이지만, 제출 뒤에는 비밀번호 두 줄을 지워 두는 것을 권한다.

**나) 환경 변수** — 파일에 남기지 않으려면 새 PowerShell 창에서(창을 닫으면 사라진다):
```powershell
$env:PAUSE_KEYSTORE = "C:\keys\pause-release.jks"
$env:PAUSE_KEY_ALIAS = "pause"
$env:PAUSE_KEYSTORE_PASSWORD = Read-Host "keystore password"
$env:PAUSE_KEY_PASSWORD = Read-Host "key password"
```

## 3. 빌드 (사용자)
```powershell
cd C:\every-moment-of-pause\app-android
.\gradlew --stop
.\gradlew -Pkotlin.compiler.execution.strategy=in-process assembleRelease
.\gradlew --stop
```
- APK 위치: **`app-android\app\build\outputs\apk\release\gatigage-v1.0.0.apk`**
- 서명 값이 없으면 "릴리스 서명 값이 없습니다 …"로 멈춘다 → 2단계를 다시 본다.
- versionName은 `app/build.gradle.kts`에서 사람이 올린다(파일 이름도 따라 바뀐다).

## 4. 서명·설치 확인 (사용자)
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\gatigage-v1.0.0.apk
adb uninstall com.emop.pause      # 디버그 APK가 깔려 있으면 서명이 달라 설치 실패
adb install app\build\outputs\apk\release\gatigage-v1.0.0.apk
adb shell am start -n com.emop.pause/.MainActivity
adb logcat -b crash -d
```
- `Verified using v2 scheme … true`, `Signer #1 certificate DN`에 실명이 없는지 본다.
- 점검: 첫 화면(온보딩) → 출발 준비 → 발표 모드(로고 5번) 장면 점프, 크래시 버퍼 비어 있음.
- 시연은 디버그 APK로 한다(서버 연결·QR). 릴리스는 오프라인 제출본이다. 다시 시연하려면 `adb uninstall` 후 디버그 APK 설치.

## 5. 제출
- APK는 GitHub 저장소 Releases에 소스와 함께 올린다(APK만 따로 내지 않음, README §6).
- `.jks`·비밀번호·`local.properties`는 어디에도 올리지 않는다. 키를 잃으면 같은 앱으로 업데이트할 수 없으니 저장소 밖에 백업한다.

## 6. 공개 준비 (2026-11-03, 사용자가 GitHub에서)
- **Release v1.0.0**: 태그 `v1.0.0`, 제목 "같이가개 v1.0.0", 본문은 [release_notes_v1.0.0.md](release_notes_v1.0.0.md)를 붙여 넣는다(링크는 저장소 기준 경로라 Releases 본문에서는 README 주소로 바꿔도 된다). 첨부: `gatigage-v1.0.0.apk`.
- **저장소 About 한 줄**: `반려견과 차로 가는 모든 순간 — Pleos Connect(Android Automotive) 차량 앱: 굽은 길 예보, 반려견 맞춤 쉼표, 잠깐 대기, 긴급 도움`
- **Topics**: `android-automotive`, `pleos`, `jetpack-compose`, `kotlin`, `fastapi`
- **개인정보 처리방침 공개 (GitHub Pages)**: 저장소 Settings → Pages → Build and deployment의 Source를 "Deploy from a branch", Branch를 `main`, 폴더를 `/docs`로 저장한다. 몇 분 뒤 `https://<계정>.github.io/<저장소>/privacy.html`에서 열린다(`docs/privacy.html`은 외부 스크립트·글꼴 없는 정적 페이지). 이 주소를 스토어 등록 문서(docs/store/listing.md)의 처리방침 URL에 넣는다(넣어 둠: `https://xuxtaku7610-del.github.io/every-moment-of-pause/privacy.html`, 11/13 Pages를 켜면 열림). 저장소가 비공개면 Pages는 유료 요금제에서만 공개 주소가 생긴다 — 그때는 공개 저장소로 바꾼 뒤 켠다. `docs/` 아래 다른 문서도 같은 주소로 열릴 수 있으니, 공개하면 안 되는 파일이 `docs/`에 없는지 먼저 본다(`_private/`는 저장소 밖).
