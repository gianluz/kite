package io.kite.plugins.gradle

import io.kite.core.ExecutionContext
import io.kite.core.exec

/**
 * Gradle plugin for Kite providing type-safe Gradle operations.
 */
@Suppress(
    "TooManyFunctions",
    "SpreadOperator",
) // Gradle operations naturally require many functions and spread operator for varargs
class GradlePlugin(private val ctx: ExecutionContext) {
    /**
     * Build the project.
     *
     * @param tasks Tasks to execute (default: build)
     * @param parallel Enable parallel execution
     * @param daemon Enable Gradle daemon
     * @param stacktrace Show stacktrace on errors
     * @param properties Additional Gradle properties
     */
    suspend fun build(
        tasks: List<String> = listOf("build"),
        parallel: Boolean = false,
        daemon: Boolean = true,
        stacktrace: Boolean = false,
        properties: Map<String, String> = emptyMap(),
    ) {
        ctx.logger.info("🏗️  Building with Gradle...")

        val args =
            buildList {
                addAll(tasks)
                if (parallel) add("--parallel")
                if (!daemon) add("--no-daemon")
                if (stacktrace) add("--stacktrace")
                properties.forEach { (key, value) ->
                    add("-P$key=$value")
                }
            }

        ctx.exec("./gradlew", *args.toTypedArray())
        ctx.logger.info("✅ Build complete")
    }

    /**
     * Run tests.
     *
     * @param parallel Enable parallel test execution
     * @param continueAfterFailure Continue running tests after failures
     * @param testNamePattern Run specific tests matching pattern
     */
    suspend fun test(
        parallel: Boolean = false,
        continueAfterFailure: Boolean = false,
        testNamePattern: String? = null,
    ) {
        ctx.logger.info("🧪 Running tests...")

        val args =
            buildList {
                add("test")
                if (parallel) add("--parallel")
                if (continueAfterFailure) add("--continue")
                if (testNamePattern != null) {
                    add("--tests")
                    add(testNamePattern)
                }
            }

        ctx.exec("./gradlew", *args.toTypedArray())
        ctx.logger.info("✅ Tests complete")
    }

    /**
     * Clean build artifacts.
     */
    suspend fun clean() {
        ctx.logger.info("🧹 Cleaning...")
        ctx.exec("./gradlew", "clean")
        ctx.logger.info("✅ Clean complete")
    }

    /**
     * Assemble artifacts without running tests.
     */
    suspend fun assemble() {
        ctx.logger.info("📦 Assembling...")
        ctx.exec("./gradlew", "assemble")
        ctx.logger.info("✅ Assemble complete")
    }

    /**
     * Publish artifacts to Maven Local.
     */
    suspend fun publishToMavenLocal() {
        ctx.logger.info("📤 Publishing to Maven Local...")
        ctx.exec("./gradlew", "publishToMavenLocal")
        ctx.logger.info("✅ Published to Maven Local")
    }

    /**
     * Publish artifacts to remote repository.
     *
     * @param repository Repository name (if specified in build.gradle.kts)
     */
    suspend fun publish(repository: String? = null) {
        ctx.logger.info("📤 Publishing...")

        val task =
            if (repository != null) {
                "publishAllPublicationsTo${repository.capitalize()}Repository"
            } else {
                "publish"
            }

        ctx.exec("./gradlew", task)
        ctx.logger.info("✅ Published")
    }

    /**
     * Generate dependency report.
     */
    suspend fun dependencies() {
        ctx.logger.info("📋 Generating dependency report...")
        ctx.exec("./gradlew", "dependencies")
    }

    /**
     * Check for dependency updates.
     */
    suspend fun dependencyUpdates() {
        ctx.logger.info("🔍 Checking for dependency updates...")
        ctx.exec("./gradlew", "dependencyUpdates")
    }

    /**
     * Run custom Gradle tasks.
     *
     * @param tasks Tasks to execute
     * @param args Additional arguments
     */
    suspend fun tasks(
        vararg tasks: String,
        args: List<String> = emptyList(),
    ) {
        require(tasks.isNotEmpty()) { "At least one task must be specified" }

        ctx.logger.info("⚙️  Running tasks: ${tasks.joinToString()}")

        val allArgs =
            buildList {
                addAll(tasks)
                addAll(args)
            }

        ctx.exec("./gradlew", *allArgs.toTypedArray())
        ctx.logger.info("✅ Tasks complete")
    }

    /**
     * Wrapper update.
     *
     * @param version Gradle version to use
     */
    suspend fun wrapper(version: String) {
        ctx.logger.info("🔄 Updating Gradle wrapper to $version...")
        ctx.exec("./gradlew", "wrapper", "--gradle-version=$version")
        ctx.logger.info("✅ Wrapper updated")
    }
}

/**
 * Extension function to make Gradle plugin available in ExecutionContext.
 */
suspend fun ExecutionContext.gradle(configure: suspend GradlePlugin.() -> Unit) {
    GradlePlugin(this).configure()
}
