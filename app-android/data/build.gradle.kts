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
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/petPlaceAssets"))
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/vetAssets"))
}

val copyRestStops by tasks.registering(Copy::class) {
    from(rootProject.file("../server/app/data/sample_reststops.json"))
    into(layout.buildDirectory.dir("generated/restStopAssets"))
    rename { "reststops_default.json" }
}
// 반려동물 동반 장소·동물병원은 서버 DB의 **실제 데이터**를 내보낸 파일을 묶는다 (UI v9 마무리 — 제출 APK가 서버 없이도
// 주변 추천·가장 가까운 병원을 보인다). 만들기: scripts/export_embedded_places.py → data/embedded/. 휴게소는 샘플 그대로.
val copyPetPlaces by tasks.registering(Copy::class) {
    from(file("embedded/pet_places.json"))
    into(layout.buildDirectory.dir("generated/petPlaceAssets"))
    rename { "pet_places_default.json" }
}
val copyVets by tasks.registering(Copy::class) {
    from(file("embedded/vets.json"))
    into(layout.buildDirectory.dir("generated/vetAssets"))
    rename { "vets_default.json" }
}
tasks.named("preBuild") { dependsOn(copyRestStops, copyPetPlaces, copyVets) }

dependencies {
    api(project(":core:model"))
    api(project(":engine:planner"))
    api(project(":engine:pet"))
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
