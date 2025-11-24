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
        @Suppress("DEPRECATION")
        assertNull(context.mrNumber)
        @Suppress("DEPRECATION")
        assertFalse(context.isRelease)
        assertFalse(context.isCI) // No CI env var, so not in CI
        assertEquals(emptyMap(), context.environment)
    }

    @Test
    fun `context with all properties`() {
        val env = mapOf("CI" to "true", "KEY" to "value")
        val artifacts = InMemoryArtifactManager()
        val workspace = Paths.get("/workspace")

        @Suppress("DEPRECATION")
        val context =
            ExecutionContext(
                branch = "feature/test",
                commitSha = "def456",
                mrNumber = "123",
                isRelease = true,
                isLocal = false,
                environment = env,
                workspace = workspace,
                artifacts = artifacts,
            )

        assertEquals("feature/test", context.branch)
        assertEquals("def456", context.commitSha)
        @Suppress("DEPRECATION")
        assertEquals("123", context.mrNumber)
        @Suppress("DEPRECATION")
        assertTrue(context.isRelease)
        @Suppress("DEPRECATION")
        assertFalse(context.isLocal)
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
    fun `isMergeRequest is true when mrNumber is set`() {
        val withMR =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                mrNumber = "42",
            )
        assertTrue(withMR.isMergeRequest)

        val withoutMR =
            ExecutionContext(
                branch = "main",
                commitSha = "abc123",
                mrNumber = null,
            )
        assertFalse(withoutMR.isMergeRequest)
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
