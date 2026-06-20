package com.bandu.tiji.feature.library

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class LibraryModuleArchitectureTest {
    @Test
    fun `library feature only depends on allowed project modules`() {
        val buildFile = findAndroidRoot().resolve("feature/library/build.gradle.kts")
        val projectDependencies = Regex("""project\("([^"]+)"\)""")
            .findAll(buildFile.readText())
            .map { it.groupValues[1] }
            .toSet()

        assertThat(projectDependencies).containsExactly(
            ":core:common",
            ":core:model",
            ":core:designsystem",
            ":core:testing",
            ":domain",
        )
        assertThat(buildFile.readText()).doesNotContain("project(\":data")
        assertThat(buildFile.readText()).doesNotContain("project(\":feature:")
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature/library").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate the Android project root")
        }
    }
}
