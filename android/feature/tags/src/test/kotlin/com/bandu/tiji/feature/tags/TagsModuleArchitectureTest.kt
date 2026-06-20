package com.bandu.tiji.feature.tags

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class TagsModuleArchitectureTest {
    @Test
    fun `tags feature depends only on domain and core modules`() {
        val buildFile = findAndroidRoot().resolve("feature/tags/build.gradle.kts")
        val script = buildFile.readText()

        assertThat(script).doesNotContain("project(\":data\")")
        assertThat(script).doesNotContain("project(\":core:storage\")")
        assertThat(script).doesNotContain("project(\":core:network\")")
        assertThat(script).doesNotContain("project(\":ai:")
        assertThat(script).doesNotContain("project(\":transfer:")
        assertThat(script).doesNotContain("project(\":feature:")
    }

    private fun findAndroidRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile &&
                current.resolve("feature/tags").isDirectory
            ) {
                return current
            }
            current = current.parentFile
                ?: error("Unable to locate the Android project root")
        }
    }
}
