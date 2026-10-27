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
    }
}

rootProject.name = "every-moment-of-pause"

include(
    ":app",
    ":core:model", ":core:vehicle", ":core:navi", ":core:restriction",
    ":engine:fatigue", ":engine:needs", ":engine:planner", ":engine:pause",
    ":voice", ":data", ":sim",
)
