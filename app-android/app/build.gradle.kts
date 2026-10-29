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
        // 버전 체계 (COMMERCIAL_READINESS K P0): versionName은 0.x.y로 사람이 올리고,
        // versionCode는 커밋 수로 자동 증가한다(git이 없으면 1). 같은 커밋에서 다시 빌드해도 값이 같다.
        versionCode = gitCommitCount.get() + 1
        versionName = "0.1.0"
    }

    // 릴리스 서명 (COMMERCIAL_READINESS S P1, docs/release.md): keystore는 저장소 밖, 비밀번호는 환경 변수로만.
    // 네 값이 모두 있을 때만 서명 설정을 만든다. 없으면 assembleRelease는 서명 안 된 APK를 만든다.
    val ks = providers.environmentVariable("PAUSE_KEYSTORE").orNull
    val ksPass = providers.environmentVariable("PAUSE_KEYSTORE_PASSWORD").orNull
    val keyAlias = providers.environmentVariable("PAUSE_KEY_ALIAS").orNull
    val keyPass = providers.environmentVariable("PAUSE_KEY_PASSWORD").orNull
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
        }
        release {
            signingConfig = release
            // R8 축소는 `-Ppause.minify=true`일 때만 (실기기 검증 전까지 기본 끔, docs/release.md).
            isMinifyEnabled = providers.gradleProperty("pause.minify").orNull == "true"
            isShrinkResources = isMinifyEnabled
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 오프라인 우선: 서버 주소가 없으면 캐시·내장 데이터·템플릿으로만 동작한다.
            buildConfigField("String", "SERVER_BASE_URL", "\"\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// 콘텐츠 (content/): 검수본(content/*.json, reviewed=true)은 모든 빌드에, 초안(content/drafts/)은 디버그 빌드에만 묶는다.
// 릴리스 APK에는 초안 파일이 아예 들어가지 않고, 들어가더라도 ContentLibrary가 reviewed=false를 거른다 (CLAUDE.md 규칙 4).
android.sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/contentAssets"))
android.sourceSets["debug"].assets.srcDir(layout.buildDirectory.dir("generated/draftAssets"))

val copyContent by tasks.registering(Copy::class) {
    from(rootProject.file("../content")) { include("*.json") }
    into(layout.buildDirectory.dir("generated/contentAssets/content"))
}
val copyDrafts by tasks.registering(Copy::class) {
    from(rootProject.file("../content/drafts")) { include("*.json") }
    into(layout.buildDirectory.dir("generated/draftAssets/content/drafts"))
}
tasks.named("preBuild") { dependsOn(copyContent, copyDrafts) }

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
    implementation(project(":engine:needs"))
    implementation(project(":engine:planner"))
    implementation(project(":engine:pause"))
    implementation(project(":engine:content"))
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
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
