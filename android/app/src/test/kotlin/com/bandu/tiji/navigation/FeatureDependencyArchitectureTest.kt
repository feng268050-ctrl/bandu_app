package com.bandu.tiji.navigation

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class FeatureDependencyArchitectureTest {
    @Test
    fun `feature modules never depend directly on another feature`() {
        val androidRoot = findAndroidRoot()
        val violations = androidRoot.resolve("feature")
            .listFiles()
            .orEmpty()
            .map { it.resolve("build.gradle.kts") }
            .filter(File::isFile)
            .filter { buildFile ->
                buildFile.readText().contains("project(\":feature:")
            }
            .map(File::getPath)

        assertThat(violations).isEmpty()
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate the Android project root")
        }
    }
}
