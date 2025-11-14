package io.kite.dsl

import kotlin.script.experimental.api.*
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
 * ### 2. Maven Dependencies (Experimental)
 * Use @DependsOn to add external dependencies:
 * ```kotlin
 * @file:DependsOn("com.google.code.gson:gson:2.10.1")
 *
 * import com.google.gson.Gson
 *
 * segments {
 *     segment("parse-json") {
 *         execute {
 *             val gson = Gson()
 *             // ...
 *         }
 *     }
 * }
 * ```
 *
 * **Note**: Maven dependency resolution requires the Kotlin scripting host to be configured
 * with dependency resolvers. Currently, dependencies are resolved from the classpath.
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

    // Note: Advanced dependency resolution via @DependsOn would require additional
    // configuration in the scripting host. For now, scripts can use any dependencies
    // already on the classpath from kite-core and kite-dsl modules.
})

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
})
