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

rootProject.name = "bandu-tiji"

include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:storage")
include(":core:network")
include(":core:testing")
include(":domain")
include(":data")
include(":ai:api")
include(":ai:gemini")
include(":ai:openai")
include(":transfer:protocol")
include(":transfer:runtime")
include(":feature:home")
include(":feature:capture")
include(":feature:library")
include(":feature:tags")
include(":feature:stats")
include(":feature:tutor")
include(":feature:devices")
include(":feature:profile")
