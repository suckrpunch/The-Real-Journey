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
include(":feature:home")
include(":feature:explore")
include(":feature:meditate")
include(":feature:journal")
include(":feature:profile")
