import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dev.flutter.flutter-gradle-plugin")
}

val releaseSigningPropertiesFile = rootProject.file("key.properties")
val releaseSigningProperties = Properties()
if (releaseSigningPropertiesFile.exists()) {
    releaseSigningPropertiesFile.inputStream().use(releaseSigningProperties::load)
}

val requiredReleaseSigningKeys =
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val missingReleaseSigningKeys =
    requiredReleaseSigningKeys.filter { releaseSigningProperties.getProperty(it).isNullOrBlank() }
val releaseStoreFile =
    releaseSigningProperties.getProperty("storeFile")?.let(rootProject::file)
val releaseBuildRequested =
    gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

if (releaseBuildRequested) {
    if (!releaseSigningPropertiesFile.exists()) {
        throw GradleException(
            "Release signing is required. Copy android/key.properties.example to " +
                "android/key.properties and configure the fixed release keystore.",
        )
    }
    if (missingReleaseSigningKeys.isNotEmpty()) {
        throw GradleException(
            "Missing release signing properties: ${missingReleaseSigningKeys.joinToString()}",
        )
    }
    if (releaseStoreFile?.exists() != true) {
        throw GradleException("Release keystore does not exist: $releaseStoreFile")
    }
}

val hasReleaseSigningConfig =
    releaseSigningPropertiesFile.exists() &&
        missingReleaseSigningKeys.isEmpty() &&
        releaseStoreFile?.exists() == true

android {
    namespace = "com.bandu.tiji"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.bandu.tiji"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig =
                if (hasReleaseSigningConfig) signingConfigs.getByName("release") else null
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
