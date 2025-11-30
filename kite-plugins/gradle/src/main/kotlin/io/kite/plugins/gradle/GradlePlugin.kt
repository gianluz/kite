package io.kite.plugins.gradle

import io.kite.core.ExecutionContext
import io.kite.core.exec

/**
 * Gradle plugin for Kite providing flexible Gradle task execution.
 *
 * This plugin is intentionally minimal and flexible to work with any Gradle setup:
 * - Standard Java/Kotlin projects
 * - Android projects (AGP tasks)
 * - Custom Gradle plugins
 * - Multi-module projects
 */
@Suppress("SpreadOperator") // Spread operator needed for varargs
class GradlePlugin(private val ctx: ExecutionContext) {
    /**
     * Execute arbitrary Gradle tasks with full control over arguments.
     *
     * This is the core method - all other methods are convenience wrappers.
     *
     * @param tasks Tasks to execute (e.g., "build", ":app:assembleDebug", "clean")
     * @param options Gradle options
     */
    suspend fun task(
        vararg tasks: String,
        options: GradleOptions.() -> Unit = {},
    ) {
        require(tasks.isNotEmpty()) { "At least one task must be specified" }

        val config = GradleOptions().apply(options)

        val args =
            buildList {
                // Add tasks
                addAll(tasks)

                // Add flags
                if (config.parallel) add("--parallel")
                if (!config.daemon) add("--no-daemon")
                if (config.stacktrace) add("--stacktrace")
                if (config.info) add("--info")
                if (config.debug) add("--debug")
                if (config.continueOnFailure) add("--continue")
                if (config.offline) add("--offline")
                if (config.refreshDependencies) add("--refresh-dependencies")

                // Add properties
                config.properties.forEach { (key, value) ->
                    add("-P$key=$value")
                }

                // Add system properties
                config.systemProperties.forEach { (key, value) ->
                    add("-D$key=$value")
                }

                // Add custom arguments
                addAll(config.arguments)
            }

        ctx.logger.info("⚙️  Gradle: ${tasks.joinToString(" ")}")
        ctx.exec("./gradlew", *args.toTypedArray())
    }

    // ===========================
    // Convenience Methods (Optional)
    // ===========================

    /**
     * Build the project (convenience for common use case).
     */
    suspend fun build(options: GradleOptions.() -> Unit = {}) {
        task("build", options = options)
    }

    /**
     * Clean build artifacts (convenience for common use case).
     */
    suspend fun clean(options: GradleOptions.() -> Unit = {}) {
        task("clean", options = options)
    }

    /**
     * Run tests (convenience for common use case).
     */
    suspend fun test(options: GradleOptions.() -> Unit = {}) {
        task("test", options = options)
    }
}

/**
 * Gradle execution options.
 *
 * Provides a type-safe DSL for Gradle command-line options.
 */
data class GradleOptions(
    var parallel: Boolean = false,
    var daemon: Boolean = true,
    var stacktrace: Boolean = false,
    var info: Boolean = false,
    var debug: Boolean = false,
    var continueOnFailure: Boolean = false,
    var offline: Boolean = false,
    var refreshDependencies: Boolean = false,
    var properties: MutableMap<String, String> = mutableMapOf(),
    var systemProperties: MutableMap<String, String> = mutableMapOf(),
    var arguments: MutableList<String> = mutableListOf(),
) {
    /**
     * Add a Gradle property (-P flag).
     */
    fun property(
        key: String,
        value: String,
    ) {
        properties[key] = value
    }

    /**
     * Add a system property (-D flag).
     */
    fun systemProperty(
        key: String,
        value: String,
    ) {
        systemProperties[key] = value
    }

    /**
     * Add a custom argument.
     */
    fun arg(argument: String) {
        arguments.add(argument)
    }
}

/**
 * Extension function to make Gradle plugin available in ExecutionContext.
 */
suspend fun ExecutionContext.gradle(configure: suspend GradlePlugin.() -> Unit) {
    GradlePlugin(this).configure()
}
