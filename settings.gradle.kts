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

rootProject.name = "navix"

include(
    ":contracts",
    ":navix-annotations",
    ":navix-runtime",
    ":navix-compose",
    ":navix-compiler",
    ":navix-telemetry",
    ":navix-devtools",
    ":navix-lint",
    ":navix-testing",
    ":navix-demo-app",
)
