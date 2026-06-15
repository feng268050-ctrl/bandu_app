package com.bandu.tiji.core.testing.architecture

import com.google.common.truth.Truth.assertThat
import java.nio.file.Path
import org.junit.Test

class GradleModuleArchitectureTest {
    @Test
    fun `root module graph follows architecture rules`() {
        val root = GradleModuleScanner.findGradleRoot(Path.of(System.getProperty("user.dir")))
        val modules = GradleModuleScanner.scan(root)

        assertThat(modules).hasSize(22)
        GradleModuleArchitecture.assertValid(modules)
    }

    @Test
    fun `feature dependency on another feature produces violation`() {
        val modules =
            listOf(
                module(":feature:home", "android.library", dependencies = setOf(":feature:stats")),
                module(":feature:stats", "android.library"),
            )

        val violations = GradleModuleArchitecture.violations(modules)

        assertThat(violations)
            .containsExactly(
                ArchitectureViolation(
                    rule = ArchitectureRule.FEATURE_ISOLATION,
                    modulePath = ":feature:home",
                    dependencyPath = ":feature:stats",
                ),
            )
        val failure = runCatching { GradleModuleArchitecture.assertValid(modules) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(AssertionError::class.java)
        assertThat(failure).hasMessageThat().contains(":feature:home")
    }

    @Test
    fun `JVM dependency on Android module produces violation`() {
        val modules =
            listOf(
                module(":domain", "kotlin.jvm", dependencies = setOf(":core:storage")),
                module(":core:storage", "android.library"),
            )

        assertThat(GradleModuleArchitecture.violations(modules))
            .containsExactly(
                ArchitectureViolation(
                    rule = ArchitectureRule.JVM_DEPENDS_ON_ANDROID_MODULE,
                    modulePath = ":domain",
                    dependencyPath = ":core:storage",
                ),
            )
    }

    @Test
    fun `JVM module applying Android plugin produces violation`() {
        val modules =
            listOf(
                GradleModuleDescriptor(
                    path = ":ai:api",
                    plugins = setOf("kotlin.jvm", "android.library"),
                    projectDependencies = emptySet(),
                ),
            )

        assertThat(GradleModuleArchitecture.violations(modules).single().rule)
            .isEqualTo(ArchitectureRule.JVM_WITH_ANDROID_PLUGIN)
    }

    private fun module(
        path: String,
        plugin: String,
        dependencies: Set<String> = emptySet(),
    ): GradleModuleDescriptor =
        GradleModuleDescriptor(
            path = path,
            plugins = setOf(plugin),
            projectDependencies = dependencies,
        )
}
