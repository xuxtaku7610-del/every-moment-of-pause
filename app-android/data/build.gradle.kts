plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.emop.pause.data"
    // 서버 샘플 휴게소(server/app/data/sample_reststops.json)를 앱 내장 기본 데이터로 묶는다 (database.md: 앱 번들 기본 데이터).
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/restStopAssets"))
}

val copyRestStops by tasks.registering(Copy::class) {
    from(rootProject.file("../server/app/data/sample_reststops.json"))
    into(layout.buildDirectory.dir("generated/restStopAssets"))
    rename { "reststops_default.json" }
}
tasks.named("preBuild") { dependsOn(copyRestStops) }

dependencies {
    api(project(":core:model"))
    api(project(":engine:planner"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly(libs.junit.platform.launcher)
}
