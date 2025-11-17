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
    val logger: SegmentLoggerInterface = NoOpLogger
) {
    /**
     * Gets an environment variable value.
     */
    fun env(key: String): String? = environment[key]

    /**
     * Gets an environment variable value or throws if not found.
     */
    fun requireEnv(key: String): String =
        environment[key] ?: error("Required environment variable not found: $key")

    /**
     * Gets an environment variable value or returns a default.
     */
    fun envOrDefault(key: String, default: String): String =
        environment[key] ?: default

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
    GENERIC;

    /**
     * Returns a human-readable name for this platform.
     */
    val displayName: String
        get() = when (this) {
            GITLAB -> "GitLab CI"
            GITHUB -> "GitHub Actions"
            LOCAL -> "Local"
            GENERIC -> "Generic CI"
        }
}
