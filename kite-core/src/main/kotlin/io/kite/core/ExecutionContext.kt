package io.kite.core

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Execution context provided to segments during their execution.
 *
 * Provides platform-agnostic access to the execution environment, Git state,
 * and utilities for segments to use.
 *
 * **Design Philosophy:**
 * Kite is intentionally platform-agnostic. Instead of relying on opinionated
 * properties like [mrNumber] or [isRelease], segments should query environment
 * variables directly using [env] to check platform-specific values.
 *
 * **Migration Guide:**
 * ```kotlin
 * // Instead of: ctx.mrNumber != null
 * // Use: ctx.env("CI_MERGE_REQUEST_IID") != null (GitLab)
 * //  or: ctx.env("GITHUB_EVENT_NAME") == "pull_request" (GitHub)
 *
 * // Instead of: ctx.isRelease
 * // Use: ctx.env("CI_MERGE_REQUEST_LABELS")?.contains("release") == true
 * //  or: Define your own convention
 * ```
 *
 * @property branch The current Git branch (detected from git or CI env vars)
 * @property commitSha The current Git commit SHA (detected from git or CI env vars)
 * @property environment Environment variables as a map - query these directly for platform-specific info
 * @property workspace The workspace root directory
 * @property artifacts Artifact manager for sharing data between segments
 * @property logger Logger for this segment execution
 * @property mrNumber DEPRECATED: Use env("CI_MERGE_REQUEST_IID") or env("GITHUB_REF") instead
 * @property isRelease DEPRECATED: Define your own release detection logic using env()
 * @property isLocal DEPRECATED: Use isCI instead or check env("CI") directly
 * @property ciPlatform DEPRECATED: Check environment variables directly instead
 */
data class ExecutionContext(
    val branch: String,
    val commitSha: String,
    val environment: Map<String, String> = emptyMap(),
    val workspace: Path = Paths.get("."),
    val artifacts: ArtifactManager = InMemoryArtifactManager(),
    val logger: SegmentLoggerInterface = NoOpLogger,
    // Deprecated properties for backward compatibility
    @Deprecated(
        message = "Platform-specific property. Use env(\"CI_MERGE_REQUEST_IID\") for GitLab or env(\"GITHUB_REF\") for GitHub instead.",
        replaceWith = ReplaceWith("env(\"CI_MERGE_REQUEST_IID\")"),
        level = DeprecationLevel.WARNING,
    )
    val mrNumber: String? = null,
    @Deprecated(
        message = "Platform-specific property. Define your own release detection logic using env().",
        replaceWith = ReplaceWith("env(\"CI_MERGE_REQUEST_LABELS\")?.contains(\"release\") == true"),
        level = DeprecationLevel.WARNING,
    )
    val isRelease: Boolean = false,
    @Deprecated(
        message = "Use !isCI instead, or check env(\"CI\") directly for more control.",
        replaceWith = ReplaceWith("!isCI"),
        level = DeprecationLevel.WARNING,
    )
    val isLocal: Boolean = environment["CI"] != "true",
    @Deprecated(
        message = "Platform detection is unnecessary. Check environment variables directly instead.",
        level = DeprecationLevel.WARNING,
    )
    val ciPlatform: CIPlatform = CIPlatform.GENERIC,
) {
    /**
     * Gets an environment variable value.
     *
     * This is the primary way to check platform-specific information:
     * ```kotlin
     * // Check if in GitLab MR
     * val isGitLabMR = env("CI_MERGE_REQUEST_IID") != null
     *
     * // Check if in GitHub PR
     * val isGitHubPR = env("GITHUB_EVENT_NAME") == "pull_request"
     *
     * // Check for release label (your convention)
     * val isRelease = env("CI_MERGE_REQUEST_LABELS")?.contains("release") == true
     * ```
     *
     * Common environment variables:
     * - GitLab: `CI_MERGE_REQUEST_IID`, `CI_COMMIT_REF_NAME`, `CI_MERGE_REQUEST_LABELS`
     * - GitHub: `GITHUB_REF`, `GITHUB_EVENT_NAME`, `GITHUB_SHA`
     * - Jenkins: `CHANGE_ID`, `BRANCH_NAME`, `GIT_COMMIT`
     * - CircleCI: `CIRCLE_PULL_REQUEST`, `CIRCLE_BRANCH`
     */
    fun env(key: String): String? = environment[key]

    /**
     * Gets an environment variable value or throws if not found.
     */
    fun requireEnv(key: String): String = environment[key] ?: error("Required environment variable not found: $key")

    /**
     * Gets an environment variable value or returns a default.
     */
    fun envOrDefault(
        key: String,
        default: String,
    ): String = environment[key] ?: default

    /**
     * Gets an environment variable and automatically registers it as a secret.
     * The value will be masked in all logs and outputs.
     *
     * Use this for sensitive values like API keys, tokens, passwords.
     *
     * Example:
     * ```
     * val apiKey = secret("API_KEY")
     * exec("curl", "-H", "Authorization: Bearer $apiKey")
     * // Logs will show: "Authorization: Bearer [API_KEY:***]"
     * ```
     *
     * @param key The environment variable name
     * @return The value, or null if not found
     */
    fun secret(key: String): String? {
        val value = environment[key]
        if (value != null) {
            SecretMasker.registerSecret(value, hint = key)
        }
        return value
    }

    /**
     * Gets an environment variable as a secret with a required non-null assertion.
     * Throws IllegalStateException if the variable is not set.
     *
     * @param key The environment variable name
     * @return The non-null value
     * @throws IllegalStateException if the environment variable is not set
     */
    fun requireSecret(key: String): String {
        val value = secret(key)
        requireNotNull(value) { "Required secret environment variable '$key' is not set" }
        return value
    }

    /**
     * Returns true if this is a merge/pull request build.
     *
     * @deprecated Platform-specific detection. Check environment variables directly:
     * - GitLab: `env("CI_MERGE_REQUEST_IID") != null`
     * - GitHub: `env("GITHUB_EVENT_NAME") == "pull_request"`
     * - Jenkins: `env("CHANGE_ID") != null`
     */
    @Deprecated(
        message = "Platform-specific detection. Check env(\"CI_MERGE_REQUEST_IID\") or env(\"GITHUB_EVENT_NAME\") instead.",
        replaceWith = ReplaceWith("env(\"CI_MERGE_REQUEST_IID\") != null"),
        level = DeprecationLevel.WARNING,
    )
    val isMergeRequest: Boolean
        get() = mrNumber != null

    /**
     * Returns true if running in a CI environment.
     *
     * Checks for common CI environment variables across multiple platforms.
     * Most CI platforms set `CI=true`, but this also checks platform-specific
     * indicators for better reliability:
     *
     * - `CI=true` - Standard across GitHub Actions, GitLab CI, Jenkins, CircleCI, Travis, etc.
     * - `GITHUB_ACTIONS=true` - GitHub Actions
     * - `GITLAB_CI=true` - GitLab CI
     * - `JENKINS_HOME` - Jenkins
     * - `CIRCLECI=true` - CircleCI
     * - `TRAVIS=true` - Travis CI
     * - `BUILDKITE=true` - Buildkite
     * - `TEAMCITY_VERSION` - TeamCity
     *
     * **Note:** If using a custom CI system, set `CI=true` in your environment.
     */
    val isCI: Boolean
        get() =
            environment["CI"]?.equals("true", ignoreCase = true) == true ||
                environment["GITHUB_ACTIONS"]?.equals("true", ignoreCase = true) == true ||
                environment["GITLAB_CI"]?.equals("true", ignoreCase = true) == true ||
                environment["JENKINS_HOME"] != null ||
                environment["CIRCLECI"]?.equals("true", ignoreCase = true) == true ||
                environment["TRAVIS"]?.equals("true", ignoreCase = true) == true ||
                environment["BUILDKITE"]?.equals("true", ignoreCase = true) == true ||
                environment["TEAMCITY_VERSION"] != null

    override fun toString(): String {
        return "ExecutionContext(branch='$branch', commitSha='${commitSha.take(8)}', isCI=$isCI)"
    }
}

/**
 * Represents different CI/CD platforms.
 *
 * @deprecated Platform detection is unnecessary in a platform-agnostic design.
 * Check environment variables directly instead.
 */
@Deprecated(
    message = "Platform detection is unnecessary. Check environment variables directly using env().",
    level = DeprecationLevel.WARNING,
)
enum class CIPlatform {
    /** GitLab CI */
    GITLAB,

    /** GitHub Actions */
    GITHUB,

    /** Local execution */
    LOCAL,

    /** Generic/unknown CI platform */
    GENERIC,

    ;

    /**
     * Returns a human-readable name for this platform.
     */
    val displayName: String
        get() =
            when (this) {
                GITLAB -> "GitLab CI"
                GITHUB -> "GitHub Actions"
                LOCAL -> "Local"
                GENERIC -> "Generic CI"
            }
}
