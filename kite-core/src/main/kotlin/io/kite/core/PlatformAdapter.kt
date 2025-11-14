package io.kite.core

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Adapter for different CI/CD platforms.
 *
 * Platform adapters are responsible for:
 * - Detecting the current platform
 * - Populating ExecutionContext with platform-specific information
 * - Providing platform-specific functionality
 */
interface PlatformAdapter {
    /**
     * The CI platform this adapter supports.
     */
    val platform: CIPlatform

    /**
     * Detects if this adapter's platform is currently active.
     *
     * @param environment Environment variables
     * @return true if this platform is detected, false otherwise
     */
    fun detect(environment: Map<String, String>): Boolean

    /**
     * Creates an ExecutionContext populated with platform-specific information.
     *
     * @param environment Environment variables
     * @param artifacts Artifact manager to use
     * @return ExecutionContext with platform-specific data
     */
    fun createContext(
        environment: Map<String, String>,
        artifacts: ArtifactManager = InMemoryArtifactManager()
    ): ExecutionContext
}

/**
 * Platform adapter for GitLab CI.
 *
 * Reads GitLab CI environment variables to populate execution context.
 * See: https://docs.gitlab.com/ee/ci/variables/predefined_variables.html
 */
class GitLabCIPlatformAdapter : PlatformAdapter {
    override val platform = CIPlatform.GITLAB

    override fun detect(environment: Map<String, String>): Boolean {
        return environment["GITLAB_CI"] == "true"
    }

    override fun createContext(
        environment: Map<String, String>,
        artifacts: ArtifactManager
    ): ExecutionContext {
        val branch = environment["CI_COMMIT_REF_NAME"] ?: "unknown"
        val commitSha = environment["CI_COMMIT_SHA"] ?: "unknown"
        val mrNumber = environment["CI_MERGE_REQUEST_IID"]
        val labels = environment["CI_MERGE_REQUEST_LABELS"]?.split(",") ?: emptyList()
        val isRelease = labels.any { it.trim().equals("release", ignoreCase = true) }
        val workspace = environment["CI_PROJECT_DIR"]?.let { Paths.get(it) } ?: Paths.get(".")

        return ExecutionContext(
            branch = branch,
            commitSha = commitSha,
            mrNumber = mrNumber,
            isRelease = isRelease,
            isLocal = false,
            ciPlatform = CIPlatform.GITLAB,
            environment = environment,
            workspace = workspace,
            artifacts = artifacts
        )
    }
}

/**
 * Platform adapter for GitHub Actions.
 *
 * Reads GitHub Actions environment variables to populate execution context.
 * See: https://docs.github.com/en/actions/learn-github-actions/variables
 */
class GitHubActionsPlatformAdapter : PlatformAdapter {
    override val platform = CIPlatform.GITHUB

    override fun detect(environment: Map<String, String>): Boolean {
        return environment["GITHUB_ACTIONS"] == "true"
    }

    override fun createContext(
        environment: Map<String, String>,
        artifacts: ArtifactManager
    ): ExecutionContext {
        val ref = environment["GITHUB_REF"] ?: "unknown"
        val branch = when {
            ref.startsWith("refs/heads/") -> ref.removePrefix("refs/heads/")
            ref.startsWith("refs/pull/") -> "pr-${ref.split("/").getOrNull(2)}"
            else -> ref
        }
        val commitSha = environment["GITHUB_SHA"] ?: "unknown"
        val prNumber = environment["GITHUB_EVENT_NAME"]?.takeIf { it == "pull_request" }?.let {
            ref.split("/").getOrNull(2)
        }
        // TODO: Detect release label from PR event payload
        val isRelease = false
        val workspace = environment["GITHUB_WORKSPACE"]?.let { Paths.get(it) } ?: Paths.get(".")

        return ExecutionContext(
            branch = branch,
            commitSha = commitSha,
            mrNumber = prNumber,
            isRelease = isRelease,
            isLocal = false,
            ciPlatform = CIPlatform.GITHUB,
            environment = environment,
            workspace = workspace,
            artifacts = artifacts
        )
    }
}

/**
 * Platform adapter for local execution.
 *
 * Uses Git commands to determine branch and commit SHA.
 */
class LocalPlatformAdapter : PlatformAdapter {
    override val platform = CIPlatform.LOCAL

    override fun detect(environment: Map<String, String>): Boolean {
        // Local is the default fallback, detected by absence of CI indicators
        return environment["CI"] != "true" &&
                environment["GITLAB_CI"] != "true" &&
                environment["GITHUB_ACTIONS"] != "true"
    }

    override fun createContext(
        environment: Map<String, String>,
        artifacts: ArtifactManager
    ): ExecutionContext {
        // Use Git commands to get branch and SHA (would be implemented in Phase 5)
        // For now, use placeholders
        val branch = runCatching {
            // This will be implemented with actual Git command execution
            "main"
        }.getOrDefault("unknown")

        val commitSha = runCatching {
            // This will be implemented with actual Git command execution
            "0000000000000000000000000000000000000000"
        }.getOrDefault("unknown")

        val workspace = Paths.get(System.getProperty("user.dir"))

        return ExecutionContext(
            branch = branch,
            commitSha = commitSha,
            mrNumber = null,
            isRelease = false,
            isLocal = true,
            ciPlatform = CIPlatform.LOCAL,
            environment = environment,
            workspace = workspace,
            artifacts = artifacts
        )
    }
}

/**
 * Generic platform adapter for unknown CI platforms.
 *
 * Uses common CI environment variables.
 */
class GenericPlatformAdapter : PlatformAdapter {
    override val platform = CIPlatform.GENERIC

    override fun detect(environment: Map<String, String>): Boolean {
        // Detect generic CI by presence of common CI indicators
        return environment["CI"] == "true"
    }

    override fun createContext(
        environment: Map<String, String>,
        artifacts: ArtifactManager
    ): ExecutionContext {
        val branch = environment["CI_BRANCH"]
            ?: environment["BRANCH_NAME"]
            ?: environment["GIT_BRANCH"]
            ?: "unknown"
        val commitSha = environment["CI_COMMIT_SHA"]
            ?: environment["GIT_COMMIT"]
            ?: environment["COMMIT_SHA"]
            ?: "unknown"
        val workspace = environment["CI_WORKSPACE"]
            ?.let { Paths.get(it) }
            ?: Paths.get(".")

        return ExecutionContext(
            branch = branch,
            commitSha = commitSha,
            mrNumber = null,
            isRelease = false,
            isLocal = false,
            ciPlatform = CIPlatform.GENERIC,
            environment = environment,
            workspace = workspace,
            artifacts = artifacts
        )
    }
}

/**
 * Platform detector that selects the appropriate adapter.
 */
object PlatformDetector {
    private val adapters = listOf(
        GitLabCIPlatformAdapter(),
        GitHubActionsPlatformAdapter(),
        GenericPlatformAdapter(),
        LocalPlatformAdapter() // Local is always last (fallback)
    )

    /**
     * Detects the current platform and returns the appropriate adapter.
     *
     * @param environment Environment variables to use for detection
     * @return The detected platform adapter
     */
    fun detect(environment: Map<String, String> = System.getenv()): PlatformAdapter {
        return adapters.firstOrNull { it.detect(environment) }
            ?: LocalPlatformAdapter() // Should never happen, but safe fallback
    }
}
