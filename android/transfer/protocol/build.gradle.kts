plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.nimbus.srp)
    testImplementation(libs.junit)
}
