package io.kite.dsl

import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlinx.coroutines.runBlocking

/**
 * Compilation configuration for Kite scripts.
 *
 * Defines how .kite.kts files should be compiled, including:
 * - Dependencies available to scripts
 * - Implicit imports
 * - IDE support configuration
 * - Maven dependency resolution via @DependsOn and @Repository
 *
 * ## Features:
 *
 * ### 1. Implicit Imports
 * All Kite DSL classes are automatically imported, so you can write:
 * ```kotlin
 * segments {
 *     segment("build") { ... }
 * }
 * ```
 *
 * ### 2. Maven Dependencies via @DependsOn ✅
 * Use @DependsOn to add external dependencies dynamically:
 * ```kotlin
 * @file:DependsOn("com.google.code.gson:gson:2.10.1")
 * @file:Repository("https://repo.maven.apache.org/maven2/")
 *
 * import com.google.gson.Gson
 *
 * segments {
 *     segment("parse-json") {
 *         execute {
 *             val gson = Gson()
 *             val json = gson.toJson(mapOf("status" to "works!"))
 *             println(json)
 *         }
 *     }
 * }
 * ```
 *
 * ### 3. Helper Functions
 * Define reusable functions in your scripts:
 * ```kotlin
 * // Helper function
 * fun buildGradle(task: String) = exec("./gradlew", task)
 *
 * segments {
 *     segment("build") {
 *         execute { buildGradle("build") }
 *     }
 * }
 * ```
 */
object KiteScriptCompilationConfiguration : ScriptCompilationConfiguration({
    // Implicit imports available in all Kite scripts
    defaultImports(
        "io.kite.core.*",
        "io.kite.dsl.*",
        "kotlin.time.Duration",
        "kotlin.time.Duration.Companion.seconds",
        "kotlin.time.Duration.Companion.minutes",
        "kotlin.time.Duration.Companion.hours",
        // Common Kotlin stdlib imports
        "kotlin.io.*",
        "java.io.File",
        "java.nio.file.*",
        // Scripting annotations for dependency resolution
        "kotlin.script.experimental.dependencies.DependsOn",
        "kotlin.script.experimental.dependencies.Repository"
    )

    // Make current context dependencies available to scripts
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    // IDE support
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }

    // Enable Maven dependency resolution via @DependsOn and @Repository
    refineConfiguration {
        onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
    }
}) {
    // Ensure proper singleton behavior after deserialization
    @Suppress("unused")
    private fun readResolve(): Any = KiteScriptCompilationConfiguration
}

/**
 * Evaluation configuration for Kite scripts.
 *
 * Defines how .kite.kts files should be executed.
 */
object KiteScriptEvaluationConfiguration : ScriptEvaluationConfiguration({
    // Scripts can be evaluated in any context
    jvm {
        // No special JVM options needed for now
    }
}) {
    // Ensure proper singleton behavior after deserialization
    @Suppress("unused")
    private fun readResolve(): Any = KiteScriptEvaluationConfiguration
}

/**
 * Maven dependency resolver for @DependsOn and @Repository annotations.
 * Combines file system and Maven repository resolution.
 */
private val resolver = CompoundDependenciesResolver(
    FileSystemDependenciesResolver(),
    MavenDependenciesResolver()
)

/**
 * Handler for @DependsOn and @Repository annotations.
 * Resolves Maven dependencies dynamically and makes them available to the script.
 *
 * This follows the official Kotlin scripting pattern from:
 * https://kotlinlang.org/docs/custom-script-deps-tutorial.html
 */
private fun configureMavenDepsOnAnnotations(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()

    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}