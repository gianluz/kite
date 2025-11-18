package io.kite.core

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Execution context provided to segments during their execution.
 *
 * Contains information about the current CI/CD environment, Git state,
 * and provides access to artifacts and utilities.
 *
 * @property branch The current Git branch
 * @property commitSha The current Git commit SHA
 * @property mrNumber The merge/pull request number (null if not in MR context)
 * @property isRelease Whether this is a release build
 * @property isLocal Whether this is running locally (not in CI)
 * @property ciPlatform The CI platform being used
 * @property environment Environment variables as a map
 * @property workspace The workspace root directory
 * @property artifacts Artifact manager for sharing data between segments
 * @property logger Logger for this segment execution
 */
data class ExecutionContext(
    val branch: String,
    val commitSha: String,
    val mrNumber: String? = null,
    val isRelease: Boolean = false,
    val isLocal: Boolean = false,
    val ciPlatform: CIPlatform = CIPlatform.LOCAL,
    val environment: Map<String, String> = emptyMap(),
    val workspace: Path = Paths.get("."),
    val artifacts: ArtifactManager = InMemoryArtifactManager(),
    val logger: SegmentLoggerInterface = NoOpLogger,
) {
    /**
     * Gets an environment variable value.
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
     */
    val isMergeRequest: Boolean
        get() = mrNumber != null

    /**
     * Returns true if this is running in a CI environment.
     */
    val isCI: Boolean
        get() = !isLocal

    override fun toString(): String {
        return "ExecutionContext(branch='$branch', commitSha='${commitSha.take(8)}', " +
            "mrNumber=$mrNumber, isRelease=$isRelease, isLocal=$isLocal, ciPlatform=$ciPlatform)"
    }
}

/**
 * Represents different CI/CD platforms.
 */
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
