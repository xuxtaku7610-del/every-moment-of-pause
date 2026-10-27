plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.emop.pause.sim"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:vehicle"))
    implementation(project(":core:navi"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
