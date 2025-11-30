package io.kite.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileOperationsTest {
    @Test
    fun `readFile reads file contents`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("test.txt").toFile()
        file.writeText("Hello, World!")

        val content = context.readFile("test.txt")

        assertEquals("Hello, World!", content)
    }

    @Test
    fun `readFile throws when file doesn't exist`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        // File.readText() throws FileNotFoundException, not NoSuchFileException
        assertFailsWith<FileNotFoundException> {
            context.readFile("nonexistent.txt")
        }
    }

    @Test
    fun `readLines reads file as lines`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("lines.txt").toFile()
        file.writeText("Line 1\nLine 2\nLine 3")

        val lines = context.readLines("lines.txt")

        assertEquals(listOf("Line 1", "Line 2", "Line 3"), lines)
    }

    @Test
    fun `writeFile creates new file with content`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        context.writeFile("new.txt", "New content")

        val file = tempDir.resolve("new.txt").toFile()
        assertTrue(file.exists())
        assertEquals("New content", file.readText())
    }

    @Test
    fun `writeFile overwrites existing file`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("existing.txt").toFile()
        file.writeText("Old content")

        context.writeFile("existing.txt", "New content")

        assertEquals("New content", file.readText())
    }

    @Test
    fun `writeFile creates parent directories`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        context.writeFile("subdir/nested/file.txt", "Content")

        val file = tempDir.resolve("subdir/nested/file.txt").toFile()
        assertTrue(file.exists())
        assertEquals("Content", file.readText())
    }

    @Test
    fun `appendFile appends to existing file`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("append.txt").toFile()
        file.writeText("First")

        context.appendFile("append.txt", " Second")

        assertEquals("First Second", file.readText())
    }

    @Test
    fun `appendFile creates new file if doesn't exist`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        context.appendFile("new.txt", "Content")

        val file = tempDir.resolve("new.txt").toFile()
        assertTrue(file.exists())
        assertEquals("Content", file.readText())
    }

    @Test
    fun `copyFile copies single file`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val source = tempDir.resolve("source.txt").toFile()
        source.writeText("Content to copy")

        context.copyFile("source.txt", "destination.txt")

        val destination = tempDir.resolve("destination.txt").toFile()
        assertTrue(destination.exists())
        assertEquals("Content to copy", destination.readText())
        assertTrue(source.exists()) // Source still exists
    }

    @Test
    fun `copyFile copies directory recursively`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val sourceDir = tempDir.resolve("source").toFile()
        sourceDir.mkdirs()
        sourceDir.resolve("file1.txt").writeText("File 1")
        sourceDir.resolve("subdir").mkdirs()
        sourceDir.resolve("subdir/file2.txt").writeText("File 2")

        context.copyFile("source", "destination")

        val destDir = tempDir.resolve("destination").toFile()
        assertTrue(destDir.exists())
        assertTrue(destDir.resolve("file1.txt").exists())
        assertEquals("File 1", destDir.resolve("file1.txt").readText())
        assertTrue(destDir.resolve("subdir/file2.txt").exists())
        assertEquals("File 2", destDir.resolve("subdir/file2.txt").readText())
    }

    @Test
    fun `copyFile with overwrite replaces existing file`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val source = tempDir.resolve("source.txt").toFile()
        source.writeText("New content")
        val destination = tempDir.resolve("destination.txt").toFile()
        destination.writeText("Old content")

        context.copyFile("source.txt", "destination.txt", overwrite = true)

        assertEquals("New content", destination.readText())
    }

    @Test
    fun `copyFile without overwrite throws when destination exists`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val source = tempDir.resolve("source.txt").toFile()
        source.writeText("Content")
        val destination = tempDir.resolve("destination.txt").toFile()
        destination.writeText("Existing")

        assertFailsWith<FileAlreadyExistsException> {
            context.copyFile("source.txt", "destination.txt", overwrite = false)
        }
    }

    @Test
    fun `moveFile moves file to new location`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val source = tempDir.resolve("source.txt").toFile()
        source.writeText("Content to move")

        context.moveFile("source.txt", "destination.txt")

        val destination = tempDir.resolve("destination.txt").toFile()
        assertTrue(destination.exists())
        assertEquals("Content to move", destination.readText())
        assertFalse(source.exists()) // Source should not exist
    }

    @Test
    fun `moveFile with overwrite replaces existing file`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val source = tempDir.resolve("source.txt").toFile()
        source.writeText("New content")
        val destination = tempDir.resolve("destination.txt").toFile()
        destination.writeText("Old content")

        context.moveFile("source.txt", "destination.txt", overwrite = true)

        assertEquals("New content", destination.readText())
        assertFalse(source.exists())
    }

    @Test
    fun `deleteFile deletes single file`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("delete-me.txt").toFile()
        file.writeText("Content")

        context.deleteFile("delete-me.txt")

        assertFalse(file.exists())
    }

    @Test
    fun `deleteFile with recursive deletes directory`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val dir = tempDir.resolve("delete-dir").toFile()
        dir.mkdirs()
        dir.resolve("file1.txt").writeText("Content 1")
        dir.resolve("subdir").mkdirs()
        dir.resolve("subdir/file2.txt").writeText("Content 2")

        context.deleteFile("delete-dir", recursive = true)

        assertFalse(dir.exists())
    }

    @Test
    fun `deleteFile does nothing if path doesn't exist`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        // Should not throw
        context.deleteFile("nonexistent.txt")
    }

    @Test
    fun `createDirectory creates single directory`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        context.createDirectory("new-dir")

        val dir = tempDir.resolve("new-dir").toFile()
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun `createDirectory creates nested directories`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        context.createDirectory("level1/level2/level3")

        val dir = tempDir.resolve("level1/level2/level3").toFile()
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
    }

    @Test
    fun `listFiles lists files in directory`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val dir = tempDir.resolve("list-test").toFile()
        dir.mkdirs()
        dir.resolve("file1.txt").writeText("Content 1")
        dir.resolve("file2.txt").writeText("Content 2")
        dir.resolve("subdir").mkdirs() // Should not be included (it's a directory)

        val files = context.listFiles("list-test")

        assertEquals(2, files.size)
        assertTrue(files.contains("list-test${File.separator}file1.txt"))
        assertTrue(files.contains("list-test${File.separator}file2.txt"))
    }

    @Test
    fun `listFiles with recursive lists all nested files`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val dir = tempDir.resolve("recursive-test").toFile()
        dir.mkdirs()
        dir.resolve("file1.txt").writeText("Content 1")
        dir.resolve("subdir").mkdirs()
        dir.resolve("subdir/file2.txt").writeText("Content 2")
        dir.resolve("subdir/nested").mkdirs()
        dir.resolve("subdir/nested/file3.txt").writeText("Content 3")

        val files = context.listFiles("recursive-test", recursive = true)

        assertEquals(3, files.size)
        assertTrue(files.any { it.endsWith("file1.txt") })
        assertTrue(files.any { it.endsWith("file2.txt") })
        assertTrue(files.any { it.endsWith("file3.txt") })
    }

    @Test
    fun `listFiles throws when path is not a directory`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("file.txt").toFile()
        file.writeText("Content")

        assertFailsWith<NotDirectoryException> {
            context.listFiles("file.txt")
        }
    }

    @Test
    fun `findFiles finds files matching glob pattern`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val dir = tempDir.toFile()
        dir.resolve("test1.kt").writeText("Kotlin 1")
        dir.resolve("test2.kt").writeText("Kotlin 2")
        dir.resolve("test.java").writeText("Java")
        dir.resolve("subdir").mkdirs()
        dir.resolve("subdir/test3.kt").writeText("Kotlin 3")

        val files = context.findFiles("**.kt")

        assertEquals(3, files.size)
        assertTrue(files.all { it.endsWith(".kt") })
    }

    @Test
    fun `fileExists returns true when file exists`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("exists.txt").toFile()
        file.writeText("Content")

        assertTrue(context.fileExists("exists.txt"))
    }

    @Test
    fun `fileExists returns false when file doesn't exist`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        assertFalse(context.fileExists("nonexistent.txt"))
    }

    @Test
    fun `isDirectory returns true for directories`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val dir = tempDir.resolve("test-dir").toFile()
        dir.mkdirs()

        assertTrue(context.isDirectory("test-dir"))
    }

    @Test
    fun `isDirectory returns false for files`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("test.txt").toFile()
        file.writeText("Content")

        assertFalse(context.isDirectory("test.txt"))
    }

    @Test
    fun `isFile returns true for files`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("test.txt").toFile()
        file.writeText("Content")

        assertTrue(context.isFile("test.txt"))
    }

    @Test
    fun `isFile returns false for directories`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val dir = tempDir.resolve("test-dir").toFile()
        dir.mkdirs()

        assertFalse(context.isFile("test-dir"))
    }

    @Test
    fun `fileSize returns size in bytes`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("sized.txt").toFile()
        val content = "12345" // 5 bytes
        file.writeText(content)

        val size = context.fileSize("sized.txt")

        assertEquals(5L, size)
    }

    @Test
    fun `createTempDir creates temporary directory`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        val tempDirPath = context.createTempDir("test-")

        val tempDirFile = tempDir.resolve(tempDirPath).toFile()
        assertTrue(tempDirFile.exists())
        assertTrue(tempDirFile.isDirectory)
        assertTrue(tempDirPath.contains("test-"))
    }

    @Test
    fun `createTempFile creates temporary file`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        val tempFilePath = context.createTempFile("test-", ".tmp")

        val tempFile = tempDir.resolve(tempFilePath).toFile()
        assertTrue(tempFile.exists())
        assertTrue(tempFile.isFile)
        assertTrue(tempFilePath.contains("test-"))
        assertTrue(tempFilePath.endsWith(".tmp"))
    }

    @Test
    fun `absolutePath returns absolute path`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val file = tempDir.resolve("test.txt").toFile()
        file.writeText("Content")

        val absPath = context.absolutePath("test.txt")

        assertTrue(File(absPath).isAbsolute)
        assertTrue(absPath.endsWith("test.txt"))
    }

    @Test
    fun `file operations work with absolute paths`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)
        val absolutePath = tempDir.resolve("absolute.txt").toFile().absolutePath

        context.writeFile(absolutePath, "Absolute content")

        val content = context.readFile(absolutePath)
        assertEquals("Absolute content", content)
    }

    @Test
    fun `file operations work with nested relative paths`(
        @TempDir tempDir: Path,
    ) {
        val context = createContext(tempDir)

        context.writeFile("level1/level2/nested.txt", "Nested content")

        val content = context.readFile("level1/level2/nested.txt")
        assertEquals("Nested content", content)
    }

    private fun createContext(tempDir: Path): ExecutionContext {
        return ExecutionContext(
            branch = "test",
            commitSha = "abc123",
            workspace = tempDir,
        )
    }
}
