package io.kite.dsl

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScriptCompilerTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `compile requires existing file`() =
        runTest {
            val compiler = ScriptCompiler()
            val nonExistentFile = File(tempDir, "nonexistent.kite.kts")

            assertThrows<IllegalArgumentException> {
                compiler.compile(nonExistentFile)
            }
        }

    @Test
    fun `compile requires kts extension`() =
        runTest {
            val compiler = ScriptCompiler()
            val wrongExtension =
                File(tempDir, "script.txt").apply {
                    writeText("println(\"test\")")
                }

            assertThrows<IllegalArgumentException> {
                compiler.compile(wrongExtension)
            }
        }

    @Test
    fun `compile simple script successfully`() =
        runTest {
            val compiler = ScriptCompiler()
            val scriptFile =
                File(tempDir, "simple.kite.kts").apply {
                    writeText(
                        """
                        println("Hello from Kite")
                        """.trimIndent(),
                    )
                }

            val result = compiler.compile(scriptFile)

            assertTrue(result is ResultWithDiagnostics.Success)
        }

    @Test
    fun `compile caches compiled scripts`() =
        runTest {
            val compiler = ScriptCompiler(enableCache = true)
            val scriptFile =
                File(tempDir, "cached.kite.kts").apply {
                    writeText(
                        """
                        println("Cached script")
                        """.trimIndent(),
                    )
                }

            assertEquals(0, compiler.cacheSize())

            // First compilation
            val result1 = compiler.compile(scriptFile)
            assertTrue(result1 is ResultWithDiagnostics.Success)
            assertEquals(1, compiler.cacheSize())

            // Second compilation (should use cache)
            val result2 = compiler.compile(scriptFile)
            assertTrue(result2 is ResultWithDiagnostics.Success)
            assertEquals(1, compiler.cacheSize())
        }

    @Test
    fun `clearCache removes cached scripts`() =
        runTest {
            val compiler = ScriptCompiler(enableCache = true)
            val scriptFile =
                File(tempDir, "cached.kite.kts").apply {
                    writeText(
                        """
                        println("Cached script")
                        """.trimIndent(),
                    )
                }

            compiler.compile(scriptFile)
            assertEquals(1, compiler.cacheSize())

            compiler.clearCache()
            assertEquals(0, compiler.cacheSize())
        }

    @Test
    fun `cache can be disabled`() =
        runTest {
            val compiler = ScriptCompiler(enableCache = false)
            val scriptFile =
                File(tempDir, "uncached.kite.kts").apply {
                    writeText(
                        """
                        println("Uncached script")
                        """.trimIndent(),
                    )
                }

            compiler.compile(scriptFile)
            assertEquals(0, compiler.cacheSize())
        }

    @Test
    fun `compileAndEvaluate executes simple script`() =
        runTest {
            val compiler = ScriptCompiler()
            val scriptFile =
                File(tempDir, "execute.kite.kts").apply {
                    writeText(
                        """
                        val x = 42
                        x
                        """.trimIndent(),
                    )
                }

            val result = compiler.compileAndEvaluate(scriptFile)

            assertTrue(result is ResultWithDiagnostics.Success)
        }

    @Test
    fun `compile reports errors for invalid syntax`() =
        runTest {
            val compiler = ScriptCompiler()
            val scriptFile =
                File(tempDir, "invalid.kite.kts").apply {
                    writeText(
                        """
                        this is not valid kotlin
                        """.trimIndent(),
                    )
                }

            val result = compiler.compile(scriptFile)

            assertTrue(result is ResultWithDiagnostics.Failure)
            assertTrue(result.reports.isNotEmpty())
        }

    @Test
    fun `compile allows implicit imports`() =
        runTest {
            val compiler = ScriptCompiler()
            val scriptFile =
                File(tempDir, "imports.kite.kts").apply {
                    writeText(
                        """
                        // Should be able to use imports from compilation configuration
                        val duration = 5.seconds
                        duration
                        """.trimIndent(),
                    )
                }

            val result = compiler.compile(scriptFile)

            assertTrue(result is ResultWithDiagnostics.Success)
        }
}

class ScriptResultTest {
    @Test
    fun `Success contains value`() {
        val result: ScriptResult<Int> = ScriptResult.Success(42)

        assertTrue(result is ScriptResult.Success)
        assertEquals(42, result.value)
    }

    @Test
    fun `Failure contains diagnostics`() {
        val diagnostic =
            ScriptDiagnostic(
                message = "Error message",
                severity = ScriptDiagnostic.Severity.ERROR,
                location = null,
            )
        val result: ScriptResult<Nothing> = ScriptResult.Failure(listOf(diagnostic))

        assertTrue(result is ScriptResult.Failure)
        assertEquals(1, result.errors.size)
        assertEquals("Error message", result.errors[0].message)
    }
}

class ScriptDiagnosticTest {
    @Test
    fun `toString includes severity and message`() {
        val diagnostic =
            ScriptDiagnostic(
                message = "Test error",
                severity = ScriptDiagnostic.Severity.ERROR,
                location = null,
            )

        val str = diagnostic.toString()
        assertTrue(str.contains("ERROR"))
        assertTrue(str.contains("Test error"))
    }

    @Test
    fun `toString without location`() {
        val diagnostic =
            ScriptDiagnostic(
                message = "Test warning",
                severity = ScriptDiagnostic.Severity.WARNING,
                location = null,
            )

        val str = diagnostic.toString()
        assertTrue(str.contains("WARNING"))
        assertTrue(str.contains("Test warning"))
    }

    @Test
    fun `severity enum has all values`() {
        val severities = ScriptDiagnostic.Severity.values()

        assertEquals(3, severities.size)
        assertTrue(severities.contains(ScriptDiagnostic.Severity.ERROR))
        assertTrue(severities.contains(ScriptDiagnostic.Severity.WARNING))
        assertTrue(severities.contains(ScriptDiagnostic.Severity.INFO))
    }
}
