plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.emop.pause.sim"
    // 저장소 루트 scenarios/*.json을 앱 assets(scenarios/)로 함께 묶는다.
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/scenarioAssets"))
}

val copyScenarios by tasks.registering(Copy::class) {
    from(rootProject.file("../scenarios")) { include("*.json") }
    into(layout.buildDirectory.dir("generated/scenarioAssets/scenarios"))
}
tasks.named("preBuild") { dependsOn(copyScenarios) }

dependencies {
    implementation(project(":core:model"))
    api(project(":core:vehicle"))
    api(project(":core:navi"))
    api(project(":core:restriction"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
