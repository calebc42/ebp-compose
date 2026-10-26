// :renderer:model is Kotlin Multiplatform with an Android and a JVM target so
// a desktop host can link it. Both targets are JVM platforms, so
// commonMain has no metadata compilation: it compiles once per platform, may
// use JDK APIs, and sees the jvm-only `:wire` module exactly as the Android
// library did.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvmToolchain(21)

    jvm()

    android {
        namespace = "com.calebc42.ebp.renderer.model"
        compileSdk = 37
        minSdk = 34
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.wire)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
        // Plain JUnit tests: they run on both `jvmTest` and
        // `testAndroidHostTest`.
        commonTest.dependencies {
            implementation(libs.junit)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnit()
}

tasks.withType<Test>().configureEach {
    // CoreNodeMembersDriftTest re-reads the sibling `ebp` checkout's
    // contract.json; every build that includes this module maps the same
    // physical directory, so the path is resolved from this project.
    systemProperty("ebp.dir", projectDir.resolve("../../../ebp").canonicalPath)
}
