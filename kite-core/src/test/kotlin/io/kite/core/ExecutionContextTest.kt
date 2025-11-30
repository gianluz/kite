package io.kite.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExecutionContextTest {
    @Test
    fun `context with minimal properties`() {
        val context =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
            )

        assertEquals("main", context.branch)
        assertEquals("abc123", context.commitSha)
        assertFalse(context.isCI) // No CI env var, so not in CI
        assertEquals(emptyMap(), context.environment)
    }

    @Test
    fun `context with all properties`() {
        val env = mapOf("CI" to "true", "KEY" to "value")
        val artifacts = InMemoryArtifactManager()
        val workspace = Paths.get("/workspace")

        val context =
            ExecutionContext(
                branch = "feature/test",
                commitSha = "def456",
                environment = env,
                workspace = workspace,
                artifacts = artifacts,
            )

        assertEquals("feature/test", context.branch)
        assertEquals("def456", context.commitSha)
        assertTrue(context.isCI) // CI=true present
        assertEquals(env, context.environment)
        assertEquals(workspace, context.workspace)
        assertEquals(artifacts, context.artifacts)
    }

    @Test
    fun `env returns environment variable`() {
        val context =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("KEY" to "value", "NUM" to "42"),
            )

        assertEquals("value", context.env("KEY"))
        assertEquals("42", context.env("NUM"))
        assertNull(context.env("MISSING"))
    }

    @Test
    fun `requireEnv returns value or throws`() {
        val context =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("KEY" to "value"),
            )

        assertEquals("value", context.requireEnv("KEY"))

        assertThrows<IllegalStateException> {
            context.requireEnv("MISSING")
        }
    }

    @Test
    fun `envOrDefault returns value or default`() {
        val context =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("KEY" to "value"),
            )

        assertEquals("value", context.envOrDefault("KEY", "default"))
        assertEquals("default", context.envOrDefault("MISSING", "default"))
    }

    @Test
    fun `platform-agnostic MR detection using env variables`() {
        // GitLab MR detection
        val gitlabContext =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("CI_MERGE_REQUEST_IID" to "42"),
            )
        val isGitLabMR = gitlabContext.env("CI_MERGE_REQUEST_IID") != null
        assertTrue(isGitLabMR)

        // GitHub PR detection
        val githubContext =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("GITHUB_EVENT_NAME" to "pull_request"),
            )
        val isGitHubPR = githubContext.env("GITHUB_EVENT_NAME") == "pull_request"
        assertTrue(isGitHubPR)

        // No MR/PR
        val noMrContext =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = emptyMap(),
            )
        val isNoMR =
            noMrContext.env("CI_MERGE_REQUEST_IID") == null &&
                noMrContext.env("GITHUB_EVENT_NAME") != "pull_request"
        assertTrue(isNoMR)
    }

    @Test
    fun `platform-agnostic release detection using env variables`() {
        // GitLab release label
        val gitlabContext =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("CI_MERGE_REQUEST_LABELS" to "release,important"),
            )
        val isGitLabRelease = gitlabContext.env("CI_MERGE_REQUEST_LABELS")?.contains("release") == true
        assertTrue(isGitLabRelease)

        // Custom convention - branch name
        val branchContext =
            ExecutionContext(
                branch = "release/v1.0.0",
                commitSha = "abc123",
                environment = emptyMap(),
            )
        val isBranchRelease = branchContext.branch.startsWith("release/")
        assertTrue(isBranchRelease)

        // No release indicator
        val noReleaseContext =
            ExecutionContext(
                branch = "feature/test",
                commitSha = "abc123",
                environment = emptyMap(),
            )
        val isNoRelease = noReleaseContext.env("CI_MERGE_REQUEST_LABELS")?.contains("release") != true
        assertTrue(isNoRelease)
    }

    @Test
    fun `isCI detects CI environment from various indicators`() {
        // Local - no CI indicators
        val local =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = emptyMap(),
            )
        assertFalse(local.isCI)

        // Standard CI=true
        val ciStandard =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("CI" to "true"),
            )
        assertTrue(ciStandard.isCI)

        // GitHub Actions
        val github =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("GITHUB_ACTIONS" to "true"),
            )
        assertTrue(github.isCI)

        // GitLab CI
        val gitlab =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("GITLAB_CI" to "true"),
            )
        assertTrue(gitlab.isCI)

        // Jenkins
        val jenkins =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("JENKINS_HOME" to "/var/jenkins"),
            )
        assertTrue(jenkins.isCI)

        // CircleCI
        val circle =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                environment = mapOf("CIRCLECI" to "true"),
            )
        assertTrue(circle.isCI)
    }

    @Test
    fun `toString includes key information`() {
        val context =
            ExecutionContext(
                branch = "feature/test",
                commitSha = "abc123def456",
                environment = mapOf("CI" to "true"),
            )

        val str = context.toString()
        assertTrue(str.contains("feature/test"))
        assertTrue(str.contains("abc123de")) // First 8 chars
        assertTrue(str.contains("isCI"))
    }
}
