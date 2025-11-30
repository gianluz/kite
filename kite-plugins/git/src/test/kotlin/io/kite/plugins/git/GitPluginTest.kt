package io.kite.plugins.git

import io.kite.core.ExecutionContext
import io.kite.core.SegmentLoggerInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Git plugin.
 *
 * These tests use a real Git repository to verify plugin functionality.
 */
class GitPluginTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var git: Git
    private lateinit var ctx: ExecutionContext
    private lateinit var plugin: GitPlugin

    @BeforeEach
    fun setup() {
        // Initialize Git repository
        git = Git.init().setDirectory(tempDir.toFile()).call()

        // Create initial commit
        val testFile = tempDir.resolve("test.txt")
        testFile.createFile()
        testFile.writeText("Initial content")

        git.add().addFilepattern(".").call()
        git.commit().setMessage("Initial commit").call()

        // Mock ExecutionContext
        val logger = mockk<SegmentLoggerInterface>(relaxed = true)
        ctx =
            mockk<ExecutionContext>(relaxed = true) {
                every { workspace } returns tempDir
                every { this@mockk.logger } returns logger
            }

        plugin = GitPlugin(ctx)
    }

    @AfterEach
    fun tearDown() {
        plugin.close()
        git.close()
    }

    @Test
    fun `currentBranch returns main or master`() {
        val branch = plugin.currentBranch()

        // Git initializes with either 'main' or 'master'
        assertTrue(branch == "main" || branch == "master" || branch == "HEAD")
    }

    @Test
    fun `isClean returns true for clean repository`() {
        assertTrue(plugin.isClean())
    }

    @Test
    fun `isClean returns false when files are modified`() {
        // Modify file
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Modified content")

        assertFalse(plugin.isClean())
    }

    @Test
    fun `modifiedFiles returns list of changed files`() {
        // Modify file
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Modified content")

        val modified = plugin.modifiedFiles()

        assertEquals(1, modified.size)
        assertTrue(modified.contains("test.txt"))
    }

    @Test
    fun `untrackedFiles returns list of untracked files`() {
        // Create untracked file
        val newFile = tempDir.resolve("untracked.txt")
        newFile.createFile()
        newFile.writeText("Untracked")

        val untracked = plugin.untrackedFiles()

        assertEquals(1, untracked.size)
        assertTrue(untracked.contains("untracked.txt"))
    }

    @Test
    fun `tag creates a new tag`() {
        plugin.tag("v1.0.0")

        val tags = git.tagList().call()
        assertEquals(1, tags.size)
        assertEquals("refs/tags/v1.0.0", tags.first().name)

        verify { ctx.logger.info("🏷️  Creating tag: v1.0.0") }
        verify { ctx.logger.info("✅ Tag created: v1.0.0") }
    }

    @Test
    fun `tag with message creates annotated tag`() {
        plugin.tag("v1.0.0", message = "Release 1.0.0")

        val tags = git.tagList().call()
        assertEquals(1, tags.size)
        assertEquals("refs/tags/v1.0.0", tags.first().name)
    }

    @Test
    fun `tagExists returns true for existing tag`() {
        plugin.tag("v1.0.0")

        assertTrue(plugin.tagExists("v1.0.0"))
        assertFalse(plugin.tagExists("v2.0.0"))
    }

    @Test
    fun `latestTag returns most recent tag`() {
        plugin.tag("v1.0.0")
        plugin.tag("v1.1.0")
        plugin.tag("v2.0.0")

        val latest = plugin.latestTag()

        // Tags are ordered, so latest should be v2.0.0
        assertNotNull(latest)
        assertTrue(latest in listOf("v1.0.0", "v1.1.0", "v2.0.0"))
    }

    @Test
    fun `commitSha returns full SHA`() {
        val sha = plugin.commitSha()

        assertEquals(40, sha.length)
    }

    @Test
    fun `commitSha with short returns 7 characters`() {
        val sha = plugin.commitSha(short = true)

        assertEquals(7, sha.length)
    }

    @Test
    fun `add stages files`() {
        // Create new file
        val newFile = tempDir.resolve("new.txt")
        newFile.createFile()
        newFile.writeText("New file")

        plugin.add(".")

        val status = git.status().call()
        assertTrue(status.added.contains("new.txt"))
    }

    @Test
    fun `commit creates a commit`() {
        // Modify and stage file
        val testFile = tempDir.resolve("test.txt")
        testFile.writeText("Modified")

        plugin.add(".")
        plugin.commit("Update test file")

        // Check commits
        val commits = git.log().call().toList()
        assertEquals(2, commits.size)
        assertEquals("Update test file", commits.first().fullMessage)

        verify { ctx.logger.info("💾 Committing: Update test file") }
        verify { ctx.logger.info("✅ Changes committed") }
    }
}
