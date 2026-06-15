package com.bandu.tiji.core.testing.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.streams.asSequence

data class GradleModuleDescriptor(
    val path: String,
    val plugins: Set<String>,
    val projectDependencies: Set<String>,
) {
    val isFeature: Boolean
        get() = path.startsWith(":feature:")

    val isJvm: Boolean
        get() = "kotlin.jvm" in plugins || "org.jetbrains.kotlin.jvm" in plugins

    val isAndroid: Boolean
        get() = plugins.any { it == "android.application" || it == "android.library" } ||
            plugins.any { it == "com.android.application" || it == "com.android.library" }
}

enum class ArchitectureRule {
    FEATURE_ISOLATION,
    JVM_WITH_ANDROID_PLUGIN,
    JVM_DEPENDS_ON_ANDROID_MODULE,
}

data class ArchitectureViolation(
    val rule: ArchitectureRule,
    val modulePath: String,
    val dependencyPath: String? = null,
) {
    override fun toString(): String =
        when (rule) {
            ArchitectureRule.FEATURE_ISOLATION ->
                "$modulePath must not depend on feature module $dependencyPath"
            ArchitectureRule.JVM_WITH_ANDROID_PLUGIN ->
                "$modulePath must not apply both Kotlin/JVM and Android plugins"
            ArchitectureRule.JVM_DEPENDS_ON_ANDROID_MODULE ->
                "$modulePath must not depend on Android module $dependencyPath"
        }
}

object GradleModuleScanner {
    private val aliasPluginPattern =
        Regex("""alias\s*\(\s*libs\.plugins\.([A-Za-z0-9_.-]+)\s*\)""")
    private val idPluginPattern =
        Regex("""id\s*\(\s*["']([^"']+)["']\s*\)""")
    private val kotlinPluginPattern =
        Regex("""kotlin\s*\(\s*["']([^"']+)["']\s*\)""")
    private val projectDependencyPattern =
        Regex("""project\s*\(\s*["'](:[^"']+)["']\s*\)""")

    fun scan(gradleRoot: Path): List<GradleModuleDescriptor> {
        require(Files.exists(gradleRoot.resolve("settings.gradle.kts"))) {
            "Not a Gradle root: $gradleRoot"
        }
        return Files.walk(gradleRoot).use { paths ->
            paths
                .asSequence()
                .filter { it.name == "build.gradle.kts" }
                .filterNot { it.parent == gradleRoot }
                .filterNot { "build" in gradleRoot.relativize(it).map(Path::toString) }
                .map { buildFile -> parse(gradleRoot, buildFile) }
                .sortedBy(GradleModuleDescriptor::path)
                .toList()
        }
    }

    fun findGradleRoot(start: Path): Path {
        val absoluteStart = start.toAbsolutePath().normalize()
        val ancestorRoot =
            generateSequence(absoluteStart) { it.parent }
                .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        if (ancestorRoot != null) return ancestorRoot

        val childRoot = absoluteStart.resolve("android")
        require(Files.exists(childRoot.resolve("settings.gradle.kts"))) {
            "Could not find settings.gradle.kts from $absoluteStart"
        }
        return childRoot
    }

    private fun parse(
        gradleRoot: Path,
        buildFile: Path,
    ): GradleModuleDescriptor {
        val script = buildFile.readText().withoutComments()
        val relativeDirectory = gradleRoot.relativize(buildFile.parent)
        val path = ":" + relativeDirectory.joinToString(":") { it.toString() }
        val plugins =
            buildSet {
                aliasPluginPattern.findAll(script).mapTo(this) { it.groupValues[1] }
                idPluginPattern.findAll(script).mapTo(this) { it.groupValues[1] }
                kotlinPluginPattern.findAll(script).mapTo(this) { "kotlin.${it.groupValues[1]}" }
            }
        val dependencies =
            projectDependencyPattern
                .findAll(script)
                .map { it.groupValues[1] }
                .toSet()
        return GradleModuleDescriptor(path, plugins, dependencies)
    }

    private fun String.withoutComments(): String =
        replace(Regex("""(?s)/\*.*?\*/"""), "")
            .lineSequence()
            .joinToString("\n") { it.substringBefore("//") }
}

object GradleModuleArchitecture {
    fun violations(modules: Collection<GradleModuleDescriptor>): List<ArchitectureViolation> {
        val modulesByPath = modules.associateBy(GradleModuleDescriptor::path)
        return buildList {
            modules.forEach { module ->
                if (module.isFeature) {
                    module.projectDependencies
                        .filter { it.startsWith(":feature:") }
                        .forEach { dependency ->
                            add(
                                ArchitectureViolation(
                                    rule = ArchitectureRule.FEATURE_ISOLATION,
                                    modulePath = module.path,
                                    dependencyPath = dependency,
                                ),
                            )
                        }
                }
                if (module.isJvm && module.isAndroid) {
                    add(
                        ArchitectureViolation(
                            rule = ArchitectureRule.JVM_WITH_ANDROID_PLUGIN,
                            modulePath = module.path,
                        ),
                    )
                }
                if (module.isJvm) {
                    module.projectDependencies
                        .filter { modulesByPath[it]?.isAndroid == true }
                        .forEach { dependency ->
                            add(
                                ArchitectureViolation(
                                    rule = ArchitectureRule.JVM_DEPENDS_ON_ANDROID_MODULE,
                                    modulePath = module.path,
                                    dependencyPath = dependency,
                                ),
                            )
                        }
                }
            }
        }
    }

    fun assertValid(modules: Collection<GradleModuleDescriptor>) {
        val violations = violations(modules)
        if (violations.isNotEmpty()) {
            throw AssertionError(
                violations.joinToString(
                    prefix = "Invalid Gradle module architecture:\n",
                    separator = "\n",
                ),
            )
        }
    }
}
