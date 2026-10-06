plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.calebc42.ebp.renderer.model"
    compileSdk = 37
    defaultConfig { minSdk = 34 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(projects.wire)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    // CoreNodeMembersDriftTest re-reads the sibling `ebp` checkout's
    // contract.json; every build that includes this module maps the same
    // physical directory, so the path is resolved from this project.
    systemProperty("ebp.dir", projectDir.resolve("../../../ebp").canonicalPath)
}
