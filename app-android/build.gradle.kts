import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// 빌드 설정 (README §3): compileSdk 35 / targetSdk 34 / minSdk 28, jvmTarget·compileOptions 17.
// Gradle 자체는 JAVA_HOME(Temurin 21)으로 실행한다.
val pauseCompileSdk = 35
val pauseMinSdk = 28
val pauseTargetSdk = 34

subprojects {
    fun configureAndroid() = extensions.configure<BaseExtension> {
        compileSdkVersion(pauseCompileSdk)
        buildToolsVersion = "36.0.0"
        defaultConfig {
            minSdk = pauseMinSdk
            targetSdk = pauseTargetSdk
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        testOptions.unitTests.all { it.useJUnitPlatform() }
    }
    plugins.withId("com.android.application") { configureAndroid() }
    plugins.withId("com.android.library") { configureAndroid() }
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinAndroidProjectExtension> { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        extensions.configure<KotlinJvmProjectExtension> { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
    }
}
