package io.kite.runtime.process

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ProcessExecutorTest {

    private val executor = ProcessExecutor()

    @Test
    fun `execute simple command successfully`() = runTest {
        val result = executor.execute("echo", "Hello, World!")

        assertTrue(result is ProcessResult.Success)
        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("Hello, World!"))
    }

    @Test
    fun `execute command with multiple arguments`() = runTest {
        val result = executor.execute("echo", "arg1", "arg2", "arg3")

        assertTrue(result is ProcessResult.Success)
        assertTrue(result.stdout.contains("arg1"))
        assertTrue(result.stdout.contains("arg2"))
        assertTrue(result.stdout.contains("arg3"))
    }

    @Test
    fun `capture command output`() = runTest {
        val result = executor.execute("echo", "test output")

        assertEquals("test output", result.stdout.trim())
    }

    @Test
    fun `execute command in working directory`(@TempDir tempDir: File) = runTest {
        // Create a test file in temp directory
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")

        // List files in that directory
        val result = if (System.getProperty("os.name").lowercase().contains("windows")) {
            executor.execute("cmd", "/c", "dir", "/b", workingDir = tempDir)
        } else {
            executor.execute("ls", workingDir = tempDir)
        }

        assertTrue(result.stdout.contains("test.txt"))
    }

    @Test
    fun `execute command with environment variables`() = runTest {
        val result = if (System.getProperty("os.name").lowercase().contains("windows")) {
            executor.execute("cmd", "/c", "echo", "%TEST_VAR%", env = mapOf("TEST_VAR" to "test_value"))
        } else {
            executor.shell("echo \$TEST_VAR", env = mapOf("TEST_VAR" to "test_value"))
        }

        assertTrue(result.stdout.contains("test_value"))
    }

    @Test
    fun `execute command with timeout success`() = runTest {
        val result = executor.execute("echo", "fast", timeout = 1.seconds)

        assertTrue(result is ProcessResult.Success)
    }

    @Test
    fun `execute command with timeout failure`() = runTest {
        try {
            // Sleep command should timeout
            // Use 2 second timeout to be very reliable in slower CI environments
            // The sleep is 5 seconds, so it will definitely timeout
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                executor.execute("timeout", "5", timeout = 2.seconds)
            } else {
                executor.execute("sleep", "5", timeout = 2.seconds)
            }
            throw AssertionError("Should have thrown ProcessExecutionException")
        } catch (e: ProcessExecutionException) {
            assertTrue(e.message?.contains("timed out") == true)
        }
    }

    @Test
    fun `execute failing command throws exception`() = runTest {
        try {
            executor.execute("false") // 'false' command always exits with 1
            throw AssertionError("Should have thrown ProcessExecutionException")
        } catch (e: ProcessExecutionException) {
            assertEquals(1, e.exitCode)
            assertNotNull(e.command)
        }
    }

    @Test
    fun `executeOrNull returns null on failure`() = runTest {
        val result = executor.executeOrNull("false")

        assertNull(result)
    }

    @Test
    fun `executeOrNull returns result on success`() = runTest {
        val result = executor.executeOrNull("echo", "success")

        assertNotNull(result)
        assertTrue(result is ProcessResult.Success)
        assertTrue(result.stdout.contains("success"))
    }

    @Test
    fun `shell command execution`() = runTest {
        val result = executor.shell("echo Hello && echo World")

        assertTrue(result.stdout.contains("Hello"))
        assertTrue(result.stdout.contains("World"))
    }

    @Test
    fun `shell command with pipes`() = runTest {
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            val result = executor.shell("echo 'test' | grep test")

            assertTrue(result.stdout.contains("test"))
        }
    }

    @Test
    fun `execute nonexistent command throws exception`() = runTest {
        try {
            executor.execute("nonexistent_command_xyz123")
            throw AssertionError("Should have thrown ProcessExecutionException")
        } catch (e: ProcessExecutionException) {
            assertEquals(-1, e.exitCode)
            assertTrue(e.message?.contains("Failed to start") == true || e.exitCode == -1)
        }
    }

    @Test
    fun `result includes duration`() = runTest {
        val result = executor.execute("echo", "test")

        assertTrue(result.duration >= 0)
    }

    @Test
    fun `result includes command string`() = runTest {
        val result = executor.execute("echo", "arg1", "arg2")

        assertTrue(result.command.contains("echo"))
        assertTrue(result.command.contains("arg1"))
        assertTrue(result.command.contains("arg2"))
    }

    @Test
    fun `ProcessResult Success has output alias`() = runTest {
        val result = executor.execute("echo", "test") as ProcessResult.Success

        assertEquals(result.stdout, result.output)
    }

    @Test
    fun `execute command that produces large output`() = runTest {
        // Generate a larger output to test buffering
        val result = if (System.getProperty("os.name").lowercase().contains("windows")) {
            executor.shell("for /L %i in (1,1,100) do @echo Line %i")
        } else {
            executor.shell("seq 1 100")
        }

        assertTrue(result.stdout.lines().size >= 100)
    }

    @Test
    fun `concurrent execution works correctly`() = runTest {
        // Execute multiple commands concurrently
        val results = coroutineScope {
            (1..5).map { i ->
                async {
                    executor.execute("echo", "test$i")
                }
            }.map { it.await() }
        }

        assertEquals(5, results.size)
        results.forEachIndexed { index, result ->
            assertTrue((result as ProcessResult.Success).stdout.contains("test${index + 1}"))
        }
    }
}
