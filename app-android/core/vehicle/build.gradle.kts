plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.emop.pause.core.vehicle"
    // CarPropertyManager (AAOS 플랫폼 android.car 라이브러리): Pleos SDK에 없는 속도·주차 브레이크·충전 상태
    useLibrary("android.car")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.pleos.vehicle)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
