// 화면 계약(UiState·UiEvent·Fixtures). Android·Compose에 의존하지 않는 순수 Kotlin이라
// 디자인을 바꿔도 이 모듈과 테스트는 그대로 남는다 (docs/ui_contract.md).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
