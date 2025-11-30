package io.kite.dsl

import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.ScriptSourceAnnotation
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.with
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.dependencies.resolveFromScriptSourceAnnotations
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

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
        "kotlin.script.experimental.dependencies.Repository",
        // Kite-specific dependency annotations
        "io.kite.dsl.DependsOnJar",
        "io.kite.dsl.DependsOnMavenLocal",
    )

    // Make current context dependencies available to scripts
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    // IDE support
    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }

    // Enable dependency resolution via annotations
    // - @DependsOn / @Repository: Maven Central via Ivy (Java 17 compatible)
    // - @DependsOnJar: Local JAR files
    // - @DependsOnMavenLocal: Maven Local repository
    refineConfiguration {
        onAnnotations(
            DependsOn::class,
            Repository::class,
            DependsOnJar::class,
            DependsOnMavenLocal::class,
            handler = ::configureDepsOnAnnotations,
        )
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
 * Dependency resolver chain for all annotation types.
 *
 * Resolution order:
 * 1. KiteDependenciesResolver - Local JARs and Maven Local
 * 2. FileSystemDependenciesResolver - File system paths
 * 3. IvyDependenciesResolver - Maven Central via Ivy (Java 17 compatible)
 */
private val resolver by lazy {
    CompoundDependenciesResolver(
        KiteDependenciesResolver(), // @DependsOnJar, @DependsOnMavenLocal
        FileSystemDependenciesResolver(), // File paths
        IvyDependenciesResolver(), // @DependsOn (Maven Central)
    )
}

/**
 * Handler for dependency annotations.
 *
 * Processes:
 * - @DependsOn / @Repository: Maven Central dependencies
 * - @DependsOnJar: Local JAR files
 * - @DependsOnMavenLocal: Maven Local repository
 *
 * This follows the official Kotlin scripting pattern from:
 * https://kotlinlang.org/docs/custom-script-deps-tutorial.html
 */
private fun configureDepsOnAnnotations(
    context: ScriptConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations =
        context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

    // Transform Kite annotations into formats the resolver understands
    val transformedAnnotations =
        annotations.map { scriptAnnotation ->
            val annotation = scriptAnnotation.annotation
            val location = scriptAnnotation.location

            when (annotation) {
                is DependsOnJar -> {
                    // Prefix with localJar: so KiteDependenciesResolver can handle it
                    ScriptSourceAnnotation(DependsOn("localJar:${annotation.path}"), location)
                }

                is DependsOnMavenLocal -> {
                    // Prefix with mavenLocal: so KiteDependenciesResolver can handle it
                    ScriptSourceAnnotation(DependsOn("mavenLocal:${annotation.coordinates}"), location)
                }

                else -> scriptAnnotation // Keep DependsOn and Repository as-is
            }
        }

    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(transformedAnnotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}
