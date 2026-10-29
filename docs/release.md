# 릴리스 APK 빌드 절차

릴리스 빌드는 **오프라인**이다(서버 주소 비어 있음 → 내장 휴게소·캐시·템플릿). 초안 콘텐츠(`content/drafts/`)는 들어가지 않는다.

## 1. 서명 키 만들기 (처음 한 번)
키는 저장소 밖에 둔다. 인증서 이름 필드에 실명을 넣지 않는다(공모전 실명 노출 금지).
```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\.keystores"
keytool -genkeypair -v -keystore "$env:USERPROFILE\.keystores\pause-release.jks" `
  -alias pause -keyalg RSA -keysize 2048 -validity 10000 `
  -dname "CN=Pause, OU=EMOP, O=EMOP, C=KR"
```
`keytool`은 Temurin 21의 `bin`에 있다. 비밀번호는 비밀번호 관리자에만 적는다.

## 2. 빌드
비밀번호는 환경 변수로만 넘긴다(`local.properties`·gradle.properties에 쓰지 않는다). 네 값이 다 있어야 서명 설정이 생기고, 없으면 서명 안 된 APK가 나온다.
```powershell
$env:PAUSE_KEYSTORE = "$env:USERPROFILE\.keystores\pause-release.jks"
$env:PAUSE_KEY_ALIAS = "pause"
$env:PAUSE_KEYSTORE_PASSWORD = Read-Host "keystore password"
$env:PAUSE_KEY_PASSWORD = Read-Host "key password"
cd app-android
./gradlew --stop
./gradlew -Pkotlin.compiler.execution.strategy=in-process -Ppause.minify=true assembleRelease
```
결과: `app-android/app/build/outputs/apk/release/app-release.apk`.
- `-Ppause.minify=true`: R8 축소(+리소스 축소). 10/29 기준 9.9 MB(축소 없음 31.9 MB). 규칙은 `app/proguard-rules.pro`(Pleos SDK 클래스 유지).
- versionCode는 커밋 수 + 1로 자동, versionName은 `app/build.gradle.kts`에서 사람이 올린다.

## 3. 확인
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
adb install -r app\build\outputs\apk\release\app-release.apk
adb shell am start -n com.emop.pause/.MainActivity
adb logcat -b crash -d
```
- 인증서 `Signer #1 certificate DN`에 실명이 없는지 본다.
- 디버그 APK가 깔려 있으면 서명이 달라 설치가 실패한다 → `adb uninstall com.emop.pause` 후 설치.
- 점검: 첫 화면(온보딩) → 출발 준비 → 발표 모드(로고 5번) 장면 점프, 크래시 버퍼 비어 있음.

## 4. 제출
- APK는 GitHub 저장소 Releases에 소스와 함께 올린다(APK만 따로 내지 않음, README §6).
- `.jks`·비밀번호·`local.properties`는 어디에도 올리지 않는다. 키를 잃으면 같은 앱으로 업데이트할 수 없으니 저장소 밖에 백업한다.
