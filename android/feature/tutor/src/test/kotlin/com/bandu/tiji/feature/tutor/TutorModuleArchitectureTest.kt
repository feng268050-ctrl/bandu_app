package com.bandu.tiji.feature.tutor

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class TutorModuleArchitectureTest {
    @Test
    fun `tutor feature only depends on allowed project modules`() {
        val buildFile = findAndroidRoot().resolve("feature/tutor/build.gradle.kts")
        val text = buildFile.readText()
        val dependencies = Regex("""project\("([^"]+)"\)""")
            .findAll(text)
            .map { it.groupValues[1] }
            .toSet()

        assertThat(dependencies).containsExactly(
            ":core:common",
            ":core:model",
            ":core:designsystem",
            ":core:testing",
            ":domain",
        )
        assertThat(text).doesNotContain("project(\":data")
        assertThat(text).doesNotContain("project(\":feature:")
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature/tutor").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate the Android project root")
        }
    }
}
