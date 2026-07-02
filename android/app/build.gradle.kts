plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val managedVersionFile = rootProject.layout.projectDirectory.file("../VERSION").asFile
val managedVersionName = managedVersionFile.readText().trim()
val managedVersionMatch = Regex("""^v([0-9])\.(0|[1-9][0-9]?)\.(0|[1-9][0-9]{0,2})$""")
    .matchEntire(managedVersionName)
    ?: error(
        "Invalid VERSION '$managedVersionName'. Expected v<major>.<minor>.<patch>, " +
            "with major 0-9, minor 0-99, patch 0-999.",
    )
val managedVersionMajor = managedVersionMatch.groupValues[1].toInt()
val managedVersionMinor = managedVersionMatch.groupValues[2].toInt()
val managedVersionPatch = managedVersionMatch.groupValues[3].toInt()
val managedVersionCode = managedVersionMajor * 100_000 +
    managedVersionMinor * 1_000 +
    managedVersionPatch
check(managedVersionCode > 0) {
    "VERSION must not be v0.0.0 because Android versionCode must be positive."
}

android {
    namespace = "com.bandu.tiji"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.bandu.tiji"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = managedVersionCode
        versionName = managedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:storage"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:devices"))
    implementation(project(":feature:home"))
    implementation(project(":feature:library"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:tags"))
    implementation(project(":feature:tutor"))
    implementation(project(":transfer:runtime"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
