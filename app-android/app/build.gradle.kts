import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 0 }

android {
    namespace = "com.emop.pause"

    defaultConfig {
        applicationId = "com.emop.pause"
        // 버전 체계 (COMMERCIAL_READINESS K P0): versionName은 사람이 올리고(제출본 1.0.0),
        // versionCode는 커밋 수로 자동 증가한다(git이 없으면 1). 같은 커밋에서 다시 빌드해도 값이 같다.
        versionCode = gitCommitCount.get() + 1
        versionName = "1.0.0"
    }

    // 릴리스 서명 (COMMERCIAL_READINESS S P1, docs/release.md): keystore는 저장소 밖(C:\keys\).
    // 값은 local.properties(gitignore)의 pause.keystore·pause.keyAlias·pause.keystorePassword·pause.keyPassword,
    // 또는 환경 변수 PAUSE_KEYSTORE·PAUSE_KEY_ALIAS·PAUSE_KEYSTORE_PASSWORD·PAUSE_KEY_PASSWORD(환경 변수가 우선).
    // 네 값이 모두 있을 때만 서명 설정을 만든다. 없으면 릴리스 패키징 전에 안내와 함께 멈춘다(아래 taskGraph).
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.isFile }?.reader(Charsets.UTF_8)?.use { load(it) }
    }
    fun secret(env: String, key: String): String? =
        providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }
            ?: localProps.getProperty(key)?.takeIf { it.isNotBlank() }
    val ks = secret("PAUSE_KEYSTORE", "pause.keystore")
    val ksPass = secret("PAUSE_KEYSTORE_PASSWORD", "pause.keystorePassword")
    val keyAlias = secret("PAUSE_KEY_ALIAS", "pause.keyAlias")
    val keyPass = secret("PAUSE_KEY_PASSWORD", "pause.keyPassword")
    val release = if (ks != null && ksPass != null && keyAlias != null && keyPass != null) {
        signingConfigs.create("release") {
            storeFile = file(ks)
            storePassword = ksPass
            this.keyAlias = keyAlias
            keyPassword = keyPass
        }
    } else {
        null
    }

    buildTypes {
        debug {
            // 에뮬레이터 → 호스트 PC 서버 (architecture §4). 실기기 시연은 local.properties 대신 이 값을 바꾼다.
            buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8000/api/v1/\"")
            // 보호자 폰이 여는 대기 페이지 주소의 앞부분(같은 Wi-Fi 노트북 IP). 비우면 경로만 보인다. adb `--es shareBase http://IP:8000`으로 바꾼다.
            buildConfigField("String", "SHARE_BASE_URL", "\"\"")
        }
        release {
            signingConfig = release
            // R8 축소는 `-Ppause.minify=true`일 때만. 제출본은 끈 채로 둔다(시연 직전 Hilt·Room 위험, docs/release.md).
            isMinifyEnabled = providers.gradleProperty("pause.minify").orNull == "true"
            isShrinkResources = isMinifyEnabled
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 오프라인 우선: 서버 주소가 없으면 캐시·내장 데이터·템플릿으로만 동작한다.
            buildConfigField("String", "SERVER_BASE_URL", "\"\"")
            buildConfigField("String", "SHARE_BASE_URL", "\"\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // v5 그림 (STYLE_V5 §1): docs/design/v5/assets/ = 사용자가 생성형 AI 디자인 도구로 직접 만든 그림(licenses/ASSETS.md).
    // 결정 10/30: 릴리스에도 묶는다. 한눈에 보기 시트·쓰지 않는 원본(앱 아이콘 PNG·주행 도로 사진)은 APK에서 뺀다.
    sourceSets.getByName("main").assets.srcDir("../../docs/design/v5/assets")
    // "콩이가 이상해요" 정차 후 체크리스트 (UPGRADE 7, 검수 전 reviewed=false)
    sourceSets.getByName("main").assets.srcDir("../../content/care")
    // 콩이 도우미 초안 카드(reviewed: false)는 디버그 빌드에만 — 릴리스 APK에는 넣지 않는다.
    // ignoreAssetsPatterns를 정하면 aapt 기본 규칙("<dir>_*")이 사라지므로 _drafts·_candidates 폴더를 직접 뺀다 (HelperAnswersTest F9-37)
    sourceSets.getByName("debug").assets.srcDir("../../content/care/_drafts")
    androidResources {
        ignoreAssetsPatterns += listOf("!_sheet.png", "!app_icon.png", "!app_icon_foreground.png", "!road_drive.png", "!README.md", "!pet_placeholder.png", "!<dir>_drafts", "!<dir>_candidates")
    }
}

// 릴리스 APK 이름: gatigage-v1.0.0.apk (docs/release.md)
@Suppress("DEPRECATION")
android.applicationVariants.configureEach {
    if (buildType.name == "release") {
        val name = "gatigage-v$versionName.apk"
        outputs.configureEach { (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = name }
    }
}

// 서명 값이 없으면 릴리스 패키징 전에 멈춘다. `./gradlew test`처럼 패키징하지 않는 작업은 그대로 돈다.
gradle.taskGraph.whenReady {
    if (android.buildTypes.getByName("release").signingConfig == null &&
        allTasks.any { it.project == project && it.name in setOf("packageRelease", "bundleRelease") }
    ) {
        throw GradleException(
            "릴리스 서명 값이 없습니다. docs/release.md 1~2단계대로 C:\\keys\\ 에 keystore를 만들고 " +
                "app-android/local.properties 에 pause.keystore · pause.keyAlias · pause.keystorePassword · pause.keyPassword " +
                "네 줄을 넣으세요(또는 환경 변수 PAUSE_KEYSTORE · PAUSE_KEY_ALIAS · PAUSE_KEYSTORE_PASSWORD · PAUSE_KEY_PASSWORD).",
        )
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:uistate"))
    implementation(project(":core:vehicle"))
    implementation(project(":core:navi"))
    implementation(project(":core:restriction"))
    implementation(project(":engine:fatigue"))
    implementation(project(":engine:motion"))
    implementation(project(":engine:pet"))
    implementation(project(":engine:needs"))
    implementation(project(":engine:planner"))
    implementation(project(":engine:pause"))
    implementation(project(":voice"))
    implementation(project(":data"))
    implementation(project(":sim"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.coil.compose)
    // 대기·도착 예정·병원 접수함 링크 QR (순수 Java, 인코더만 씀)
    implementation(libs.zxing.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
