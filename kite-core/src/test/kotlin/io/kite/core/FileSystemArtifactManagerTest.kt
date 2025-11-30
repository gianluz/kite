package io.kite.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileSystemArtifactManagerTest {
    @TempDir
    lateinit var tempDir: File

    private fun createArtifactManager(): FileSystemArtifactManager {
        val artifactsDir = tempDir.toPath().resolve("artifacts")
        return FileSystemArtifactManager(artifactsDir)
    }

    @Test
    fun `put and get file artifact`() {
        val manager = createArtifactManager()

        // Create a test file
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")

        // Store it as an artifact
        manager.put("test-file", testFile.toPath())

        // Retrieve it
        val retrieved = manager.get("test-file")
        assertNotNull(retrieved)
        assertTrue(retrieved.toFile().exists())
        assertEquals("test content", retrieved.toFile().readText())
    }

    @Test
    fun `put and get directory artifact`() {
        val manager = createArtifactManager()

        // Create a test directory with files
        val testDir = File(tempDir, "test-dir")
        testDir.mkdir()
        File(testDir, "file1.txt").writeText("content1")
        File(testDir, "file2.txt").writeText("content2")
        val subDir = File(testDir, "subdir")
        subDir.mkdir()
        File(subDir, "file3.txt").writeText("content3")

        // Store it as an artifact
        manager.put("test-dir", testDir.toPath())

        // Retrieve it
        val retrieved = manager.get("test-dir")
        assertNotNull(retrieved)
        assertTrue(retrieved.toFile().exists())
        assertTrue(retrieved.toFile().isDirectory)
        assertTrue(File(retrieved.toFile(), "file1.txt").exists())
        assertTrue(File(retrieved.toFile(), "file2.txt").exists())
        assertTrue(File(retrieved.toFile(), "subdir/file3.txt").exists())
        assertEquals("content1", File(retrieved.toFile(), "file1.txt").readText())
    }

    @Test
    fun `has returns true for existing artifact`() {
        val manager = createArtifactManager()

        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")

        manager.put("test", testFile.toPath())

        assertTrue(manager.has("test"))
        assertFalse(manager.has("nonexistent"))
    }

    @Test
    fun `list returns all artifacts`() {
        val manager = createArtifactManager()

        val file1 = File(tempDir, "file1.txt")
        file1.writeText("content1")
        val file2 = File(tempDir, "file2.txt")
        file2.writeText("content2")

        manager.put("artifact1", file1.toPath())
        manager.put("artifact2", file2.toPath())

        val artifacts = manager.list()
        assertEquals(2, artifacts.size)
        assertTrue(artifacts.contains("artifact1"))
        assertTrue(artifacts.contains("artifact2"))
    }

    @Test
    fun `remove deletes artifact`() {
        val manager = createArtifactManager()

        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")

        manager.put("test", testFile.toPath())
        assertTrue(manager.has("test"))

        manager.remove("test")
        assertFalse(manager.has("test"))
        assertNull(manager.get("test"))
    }

    @Test
    fun `remove directory artifact`() {
        val manager = createArtifactManager()

        val testDir = File(tempDir, "test-dir")
        testDir.mkdir()
        File(testDir, "file.txt").writeText("content")

        manager.put("test-dir", testDir.toPath())
        assertTrue(manager.has("test-dir"))

        manager.remove("test-dir")
        assertFalse(manager.has("test-dir"))
    }

    @Test
    fun `clear removes all artifacts`() {
        val manager = createArtifactManager()

        val file1 = File(tempDir, "file1.txt")
        file1.writeText("content1")
        val file2 = File(tempDir, "file2.txt")
        file2.writeText("content2")

        manager.put("artifact1", file1.toPath())
        manager.put("artifact2", file2.toPath())

        assertEquals(2, manager.list().size)

        manager.clear()

        assertEquals(0, manager.list().size)
        assertFalse(manager.has("artifact1"))
        assertFalse(manager.has("artifact2"))
    }

    @Test
    fun `put replaces existing artifact`() {
        val manager = createArtifactManager()

        val file1 = File(tempDir, "file1.txt")
        file1.writeText("content1")
        val file2 = File(tempDir, "file2.txt")
        file2.writeText("content2")

        manager.put("test", file1.toPath())
        assertEquals("content1", manager.get("test")?.toFile()?.readText())

        manager.put("test", file2.toPath())
        assertEquals("content2", manager.get("test")?.toFile()?.readText())
    }

    @Test
    fun `put throws for blank name`() {
        val manager = createArtifactManager()

        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")

        assertFailsWith<IllegalArgumentException> {
            manager.put("", testFile.toPath())
        }
    }

    @Test
    fun `put throws for nonexistent path`() {
        val manager = createArtifactManager()

        val nonexistent = tempDir.toPath().resolve("nonexistent.txt")

        assertFailsWith<IllegalArgumentException> {
            manager.put("test", nonexistent)
        }
    }

    @Test
    fun `toString shows artifacts`() {
        val manager = createArtifactManager()

        val file = File(tempDir, "test.txt")
        file.writeText("test")

        manager.put("artifact1", file.toPath())

        val string = manager.toString()
        assertTrue(string.contains("FileSystemArtifactManager"))
        assertTrue(string.contains("artifact1"))
    }
}
