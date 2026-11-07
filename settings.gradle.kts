pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NewJourney"

include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:datastore")
include(":core:database")
include(":core:catalog")
include(":core:playback")
include(":core:journal")
include(":core:notifications")
include(":core:billing")
include(":feature:home")
include(":feature:explore")
include(":feature:meditate")
include(":feature:journal")
include(":feature:profile")
