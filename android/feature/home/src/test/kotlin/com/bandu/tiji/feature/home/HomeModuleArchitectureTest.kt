package com.bandu.tiji.feature.home

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class HomeModuleArchitectureTest {
    @Test
    fun `home feature has no direct dependency on another feature`() {
        val buildFile = findAndroidRoot().resolve("feature/home/build.gradle.kts")

        assertThat(buildFile.isFile).isTrue()
        assertThat(buildFile.readText()).doesNotContain("project(\":feature:")
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature/home").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate the Android project root")
        }
    }
}
