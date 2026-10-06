plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.calebc42.ebp.renderer.compose.testing"
    compileSdk = 37
    defaultConfig { minSdk = 34 }
    buildFeatures { compose = true }
    // The self-tests compose the core components under Robolectric.
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // A test-support library: consumers add it to their test or androidTest
    // configurations and reach the Compose test rules through it.
    api(projects.renderer.compose)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.activity.compose)
    api(libs.junit)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.robolectric)
}

tasks.withType<Test>().configureEach {
    // EbpGoldens reads the sibling `ebp` checkout's goldens; every build that
    // includes this module maps the same physical directory.
    systemProperty("ebp.dir", projectDir.resolve("../../../ebp").canonicalPath)
}
