pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos Playground SDK (document.pleos.ai › Vehicle / NaviHelper 프로젝트 설정)
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
            content { includeGroup("ai.pleos.playground") }
        }
    }
}

rootProject.name = "every-moment-of-pause"

include(
    ":app",
    ":core:model", ":core:uistate", ":core:vehicle", ":core:navi", ":core:restriction",
    ":engine:fatigue", ":engine:motion", ":engine:needs", ":engine:planner", ":engine:pause", ":engine:pet",
    ":voice", ":data", ":sim",
)
