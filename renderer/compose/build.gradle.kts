// :renderer:compose is Kotlin Multiplatform with an Android and a JVM target
// (a desktop host links it). commonMain keeps Google's
// Compose coordinates: Android resolves them as before, and a standalone JVM
// target compiles against their published stubs. Only a build that hosts the
// desktop host substitutes JetBrains' desktop builds on its
// JVM classpaths, so Compose must not run on this module's own `jvmTest`.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)

    jvm()

    android {
        namespace = "com.calebc42.ebp.renderer.compose"
        compileSdk = 37
        minSdk = 34
        withHostTestBuilder {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.renderer.model)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.ui)
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.androidx.test.espresso.core)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}
