enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ebp-compose"
include(":ebp-kmp")
project(":ebp-kmp").projectDir = file("../ebp-kmp/ebp-kmp")
include(":wire")
project(":wire").projectDir = file("../ebp-kmp/wire")
include(":renderer:model")
include(":renderer:compose")
