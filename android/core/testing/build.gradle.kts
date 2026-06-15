plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.bandu.tiji.core.testing"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}

dependencies {
    api(project(":core:common"))
    api(project(":core:model"))
    api(project(":domain"))
    api(project(":ai:api"))
    api("androidx.paging:paging-common:${libs.versions.paging.get()}")
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.junit)

    testImplementation(libs.truth)
}
