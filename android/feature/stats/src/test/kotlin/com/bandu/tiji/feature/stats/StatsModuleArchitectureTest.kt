package com.bandu.tiji.feature.stats

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class StatsModuleArchitectureTest {
    @Test
    fun `stats feature has no network AI or feature dependency`() {
        val buildFile = findAndroidRoot().resolve("feature/stats/build.gradle.kts")
        val script = buildFile.readText()

        assertThat(script).doesNotContain("project(\":core:network\")")
        assertThat(script).doesNotContain("project(\":ai:")
        assertThat(script).doesNotContain("project(\":feature:")
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature/stats").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate the Android project root")
        }
    }
}
