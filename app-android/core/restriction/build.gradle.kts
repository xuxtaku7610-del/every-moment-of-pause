plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.emop.pause.core.restriction"
    // CarUxRestrictionsManager (AAOS 플랫폼 android.car 라이브러리)
    useLibrary("android.car")
}

dependencies {
    api(project(":core:model"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
