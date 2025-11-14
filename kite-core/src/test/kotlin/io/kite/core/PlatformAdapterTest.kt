package io.kite.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitLabCIPlatformAdapterTest {

    @Test
    fun `detects GitLab CI environment`() {
        val adapter = GitLabCIPlatformAdapter()

        val gitlabEnv = mapOf("GITLAB_CI" to "true")
        assertTrue(adapter.detect(gitlabEnv))

        val nonGitlabEnv = mapOf("CI" to "true")
        assertFalse(adapter.detect(nonGitlabEnv))
    }

    @Test
    fun `creates context from GitLab CI environment`() {
        val adapter = GitLabCIPlatformAdapter()
        val env = mapOf(
            "GITLAB_CI" to "true",
            "CI_COMMIT_REF_NAME" to "feature/test",
            "CI_COMMIT_SHA" to "abc123def456",
            "CI_MERGE_REQUEST_IID" to "42",
            "CI_MERGE_REQUEST_LABELS" to "bug,release,hotfix",
            "CI_PROJECT_DIR" to "/builds/project"
        )

        val context = adapter.createContext(env)

        assertEquals("feature/test", context.branch)
        assertEquals("abc123def456", context.commitSha)
        assertEquals("42", context.mrNumber)
        assertTrue(context.isRelease) // "release" label present
        assertFalse(context.isLocal)
        assertEquals(CIPlatform.GITLAB, context.ciPlatform)
    }

    @Test
    fun `handles missing GitLab CI environment variables`() {
        val adapter = GitLabCIPlatformAdapter()
        val env = mapOf("GITLAB_CI" to "true")

        val context = adapter.createContext(env)

        assertEquals("unknown", context.branch)
        assertEquals("unknown", context.commitSha)
        assertNull(context.mrNumber)
        assertFalse(context.isRelease)
    }

    @Test
    fun `detects release label case-insensitively`() {
        val adapter = GitLabCIPlatformAdapter()

        val env1 = mapOf("CI_MERGE_REQUEST_LABELS" to "Release")
        val context1 = adapter.createContext(env1)
        assertTrue(context1.isRelease)

        val env2 = mapOf("CI_MERGE_REQUEST_LABELS" to "RELEASE")
        val context2 = adapter.createContext(env2)
        assertTrue(context2.isRelease)

        val env3 = mapOf("CI_MERGE_REQUEST_LABELS" to "bug,enhancement")
        val context3 = adapter.createContext(env3)
        assertFalse(context3.isRelease)
    }
}

class GitHubActionsPlatformAdapterTest {

    @Test
    fun `detects GitHub Actions environment`() {
        val adapter = GitHubActionsPlatformAdapter()

        val githubEnv = mapOf("GITHUB_ACTIONS" to "true")
        assertTrue(adapter.detect(githubEnv))

        val nonGithubEnv = mapOf("CI" to "true")
        assertFalse(adapter.detect(nonGithubEnv))
    }

    @Test
    fun `creates context from GitHub Actions environment`() {
        val adapter = GitHubActionsPlatformAdapter()
        val env = mapOf(
            "GITHUB_ACTIONS" to "true",
            "GITHUB_REF" to "refs/heads/feature/test",
            "GITHUB_SHA" to "abc123def456",
            "GITHUB_WORKSPACE" to "/home/runner/work/project"
        )

        val context = adapter.createContext(env)

        assertEquals("feature/test", context.branch)
        assertEquals("abc123def456", context.commitSha)
        assertFalse(context.isLocal)
        assertEquals(CIPlatform.GITHUB, context.ciPlatform)
    }

    @Test
    fun `extracts branch from pull request ref`() {
        val adapter = GitHubActionsPlatformAdapter()
        val env = mapOf(
            "GITHUB_ACTIONS" to "true",
            "GITHUB_REF" to "refs/pull/42/merge",
            "GITHUB_SHA" to "abc123"
        )

        val context = adapter.createContext(env)

        assertEquals("pr-42", context.branch)
    }

    @Test
    fun `extracts PR number from pull request event`() {
        val adapter = GitHubActionsPlatformAdapter()
        val env = mapOf(
            "GITHUB_ACTIONS" to "true",
            "GITHUB_REF" to "refs/pull/42/merge",
            "GITHUB_EVENT_NAME" to "pull_request",
            "GITHUB_SHA" to "abc123"
        )

        val context = adapter.createContext(env)

        assertEquals("42", context.mrNumber)
    }

    @Test
    fun `handles missing GitHub Actions environment variables`() {
        val adapter = GitHubActionsPlatformAdapter()
        val env = mapOf("GITHUB_ACTIONS" to "true")

        val context = adapter.createContext(env)

        assertEquals("unknown", context.branch)
        assertEquals("unknown", context.commitSha)
        assertNull(context.mrNumber)
    }
}

class LocalPlatformAdapterTest {

    @Test
    fun `detects local environment`() {
        val adapter = LocalPlatformAdapter()

        val localEnv = emptyMap<String, String>()
        assertTrue(adapter.detect(localEnv))

        val ciEnv = mapOf("CI" to "true")
        assertFalse(adapter.detect(ciEnv))

        val gitlabEnv = mapOf("GITLAB_CI" to "true")
        assertFalse(adapter.detect(gitlabEnv))

        val githubEnv = mapOf("GITHUB_ACTIONS" to "true")
        assertFalse(adapter.detect(githubEnv))
    }

    @Test
    fun `creates context for local environment`() {
        val adapter = LocalPlatformAdapter()
        val env = emptyMap<String, String>()

        val context = adapter.createContext(env)

        // Branch and SHA detection not yet implemented, so expect placeholders
        assertEquals("main", context.branch)
        assertTrue(context.isLocal)
        assertEquals(CIPlatform.LOCAL, context.ciPlatform)
    }

    @Test
    fun `platform is LOCAL`() {
        val adapter = LocalPlatformAdapter()
        assertEquals(CIPlatform.LOCAL, adapter.platform)
    }
}

class GenericPlatformAdapterTest {

    @Test
    fun `detects generic CI environment`() {
        val adapter = GenericPlatformAdapter()

        val ciEnv = mapOf("CI" to "true")
        assertTrue(adapter.detect(ciEnv))

        val nonCiEnv = emptyMap<String, String>()
        assertFalse(adapter.detect(nonCiEnv))
    }

    @Test
    fun `creates context from common CI variables`() {
        val adapter = GenericPlatformAdapter()
        val env = mapOf(
            "CI" to "true",
            "CI_BRANCH" to "main",
            "CI_COMMIT_SHA" to "abc123",
            "CI_WORKSPACE" to "/workspace"
        )

        val context = adapter.createContext(env)

        assertEquals("main", context.branch)
        assertEquals("abc123", context.commitSha)
        assertFalse(context.isLocal)
        assertEquals(CIPlatform.GENERIC, context.ciPlatform)
    }

    @Test
    fun `tries multiple branch variable names`() {
        val adapter = GenericPlatformAdapter()

        val env1 = mapOf("CI" to "true", "CI_BRANCH" to "main")
        assertEquals("main", adapter.createContext(env1).branch)

        val env2 = mapOf("CI" to "true", "BRANCH_NAME" to "develop")
        assertEquals("develop", adapter.createContext(env2).branch)

        val env3 = mapOf("CI" to "true", "GIT_BRANCH" to "feature")
        assertEquals("feature", adapter.createContext(env3).branch)
    }

    @Test
    fun `tries multiple commit SHA variable names`() {
        val adapter = GenericPlatformAdapter()

        val env1 = mapOf("CI" to "true", "CI_COMMIT_SHA" to "abc123")
        assertEquals("abc123", adapter.createContext(env1).commitSha)

        val env2 = mapOf("CI" to "true", "GIT_COMMIT" to "def456")
        assertEquals("def456", adapter.createContext(env2).commitSha)

        val env3 = mapOf("CI" to "true", "COMMIT_SHA" to "ghi789")
        assertEquals("ghi789", adapter.createContext(env3).commitSha)
    }

    @Test
    fun `handles missing environment variables`() {
        val adapter = GenericPlatformAdapter()
        val env = mapOf("CI" to "true")

        val context = adapter.createContext(env)

        assertEquals("unknown", context.branch)
        assertEquals("unknown", context.commitSha)
        assertNull(context.mrNumber)
        assertFalse(context.isRelease)
    }
}

class PlatformDetectorTest {

    @Test
    fun `detects GitLab CI platform`() {
        val env = mapOf("GITLAB_CI" to "true")
        val adapter = PlatformDetector.detect(env)

        assertEquals(CIPlatform.GITLAB, adapter.platform)
        assertTrue(adapter is GitLabCIPlatformAdapter)
    }

    @Test
    fun `detects GitHub Actions platform`() {
        val env = mapOf("GITHUB_ACTIONS" to "true")
        val adapter = PlatformDetector.detect(env)

        assertEquals(CIPlatform.GITHUB, adapter.platform)
        assertTrue(adapter is GitHubActionsPlatformAdapter)
    }

    @Test
    fun `detects generic CI platform`() {
        val env = mapOf("CI" to "true")
        val adapter = PlatformDetector.detect(env)

        assertEquals(CIPlatform.GENERIC, adapter.platform)
        assertTrue(adapter is GenericPlatformAdapter)
    }

    @Test
    fun `detects local platform as fallback`() {
        val env = emptyMap<String, String>()
        val adapter = PlatformDetector.detect(env)

        assertEquals(CIPlatform.LOCAL, adapter.platform)
        assertTrue(adapter is LocalPlatformAdapter)
    }

    @Test
    fun `GitLab CI takes precedence over generic CI`() {
        val env = mapOf("CI" to "true", "GITLAB_CI" to "true")
        val adapter = PlatformDetector.detect(env)

        assertEquals(CIPlatform.GITLAB, adapter.platform)
    }

    @Test
    fun `GitHub Actions takes precedence over generic CI`() {
        val env = mapOf("CI" to "true", "GITHUB_ACTIONS" to "true")
        val adapter = PlatformDetector.detect(env)

        assertEquals(CIPlatform.GITHUB, adapter.platform)
    }
}
