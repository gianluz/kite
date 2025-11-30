package io.kite.plugins.gradle

import io.kite.core.ExecutionContext
import io.kite.core.ProcessExecutionContext
import io.kite.core.ProcessExecutionProvider
import io.kite.core.ProcessExecutionResult
import io.kite.core.SegmentLoggerInterface
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Gradle plugin.
 *
 * These tests mock ExecutionContext and verify proper behavior.
 */
class GradlePluginTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var ctx: ExecutionContext
    private lateinit var logger: SegmentLoggerInterface
    private lateinit var processProvider: ProcessExecutionProvider
    private lateinit var plugin: GradlePlugin
    private val executedCommands = mutableListOf<Pair<String, List<String>>>()

    @BeforeEach
    fun setup() {
        executedCommands.clear()
        logger = mockk(relaxed = true)
        ctx =
            mockk<ExecutionContext>(relaxed = true) {
                every { workspace } returns tempDir
                every { logger } returns this@GradlePluginTest.logger
            }

        // Mock process execution provider that records commands
        processProvider =
            mockk<ProcessExecutionProvider>(relaxed = true) {
                coEvery {
                    execute(
                        any<String>(),
                        *anyVararg(),
                        workingDir = any(),
                        env = any(),
                        timeout = any(),
                    )
                } answers {
                    val command = firstArg<String>()
                    // Varargs come as an array at index 1
                    val argsArray = secondArg<Array<String>>()
                    val args = argsArray.toList()
                    executedCommands.add(command to args)
                    ProcessExecutionResult(
                        command = command,
                        exitCode = 0,
                        output = "SUCCESS",
                        duration = 100L,
                    )
                }
            }

        ProcessExecutionContext.setProvider(processProvider)

        plugin = GradlePlugin(ctx)
    }

    @AfterEach
    fun tearDown() {
        ProcessExecutionContext.clear()
    }

    @Test
    fun `task executes single task`() =
        runTest {
            plugin.task("build")

            assertEquals(1, executedCommands.size)
            assertEquals("./gradlew", executedCommands[0].first)
            assertTrue(executedCommands[0].second.contains("build"))
        }

    @Test
    fun `task executes multiple tasks`() =
        runTest {
            plugin.task("clean", "build", "test")

            assertEquals(1, executedCommands.size)
            assertEquals("./gradlew", executedCommands[0].first)
            val args = executedCommands[0].second
            assertTrue(args.contains("clean"))
            assertTrue(args.contains("build"))
            assertTrue(args.contains("test"))
        }

    @Test
    fun `task with parallel option`() =
        runTest {
            plugin.task("build") {
                parallel = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("build"))
            assertTrue(args.contains("--parallel"))
        }

    @Test
    fun `task with properties`() =
        runTest {
            plugin.task("build") {
                property("version", "1.0.0")
                property("env", "prod")
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("build"))
            assertTrue(args.contains("-Pversion=1.0.0"))
            assertTrue(args.contains("-Penv=prod"))
        }

    @Test
    fun `task with stacktrace`() =
        runTest {
            plugin.task("build") {
                stacktrace = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--stacktrace"))
        }

    @Test
    fun `task with no-daemon`() =
        runTest {
            plugin.task("build") {
                daemon = false
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--no-daemon"))
        }

    @Test
    fun `task with info mode`() =
        runTest {
            plugin.task("build") {
                info = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--info"))
        }

    @Test
    fun `task with debug mode`() =
        runTest {
            plugin.task("build") {
                debug = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--debug"))
        }

    @Test
    fun `task with continueOnFailure`() =
        runTest {
            plugin.task("build") {
                continueOnFailure = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--continue"))
        }

    @Test
    fun `task with offline mode`() =
        runTest {
            plugin.task("build") {
                offline = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--offline"))
        }

    @Test
    fun `task with refreshDependencies`() =
        runTest {
            plugin.task("build") {
                refreshDependencies = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--refresh-dependencies"))
        }

    @Test
    fun `task with system properties`() =
        runTest {
            plugin.task("build") {
                systemProperty("file.encoding", "UTF-8")
                systemProperty("user.timezone", "UTC")
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("-Dfile.encoding=UTF-8"))
            assertTrue(args.contains("-Duser.timezone=UTC"))
        }

    @Test
    fun `task with custom arguments`() =
        runTest {
            plugin.task("build") {
                arg("--warning-mode=all")
                arg("--scan")
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("--warning-mode=all"))
            assertTrue(args.contains("--scan"))
        }

    @Test
    fun `task with all options`() =
        runTest {
            plugin.task("build", "test") {
                parallel = true
                daemon = false
                stacktrace = true
                info = true
                debug = false
                continueOnFailure = true
                offline = true
                refreshDependencies = true
                property("version", "1.0.0")
                systemProperty("file.encoding", "UTF-8")
                arg("--scan")
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("build"))
            assertTrue(args.contains("test"))
            assertTrue(args.contains("--parallel"))
            assertTrue(args.contains("--no-daemon"))
            assertTrue(args.contains("--stacktrace"))
            assertTrue(args.contains("--info"))
            assertTrue(args.contains("--continue"))
            assertTrue(args.contains("--offline"))
            assertTrue(args.contains("--refresh-dependencies"))
            assertTrue(args.contains("-Pversion=1.0.0"))
            assertTrue(args.contains("-Dfile.encoding=UTF-8"))
            assertTrue(args.contains("--scan"))
        }

    @Test
    fun `build convenience method`() =
        runTest {
            plugin.build()

            val args = executedCommands[0].second
            assertTrue(args.contains("build"))
        }

    @Test
    fun `build with options`() =
        runTest {
            plugin.build {
                parallel = true
                stacktrace = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("build"))
            assertTrue(args.contains("--parallel"))
            assertTrue(args.contains("--stacktrace"))
        }

    @Test
    fun `clean convenience method`() =
        runTest {
            plugin.clean()

            val args = executedCommands[0].second
            assertTrue(args.contains("clean"))
        }

    @Test
    fun `test convenience method`() =
        runTest {
            plugin.test()

            val args = executedCommands[0].second
            assertTrue(args.contains("test"))
        }

    @Test
    fun `Android assembleDebug task`() =
        runTest {
            plugin.task(":app:assembleDebug")

            val args = executedCommands[0].second
            assertTrue(args.contains(":app:assembleDebug"))
        }

    @Test
    fun `Android bundleRelease with signing properties`() =
        runTest {
            plugin.task(":app:bundleRelease") {
                property("android.injected.signing.store.file", "/path/to/keystore")
                property("android.injected.signing.store.password", "password")
                property("android.injected.signing.key.alias", "key0")
            }

            val args = executedCommands[0].second
            assertTrue(args.contains(":app:bundleRelease"))
            assertTrue(args.contains("-Pandroid.injected.signing.store.file=/path/to/keystore"))
            assertTrue(args.contains("-Pandroid.injected.signing.store.password=password"))
            assertTrue(args.contains("-Pandroid.injected.signing.key.alias=key0"))
        }

    @Test
    fun `Multi-module build`() =
        runTest {
            plugin.task(":core:build", ":api:build", ":app:build") {
                parallel = true
            }

            val args = executedCommands[0].second
            assertTrue(args.contains(":core:build"))
            assertTrue(args.contains(":api:build"))
            assertTrue(args.contains(":app:build"))
            assertTrue(args.contains("--parallel"))
        }

    @Test
    fun `Extension function creates plugin instance`() =
        runTest {
            ctx.gradle {
                task("build")
            }

            assertEquals(1, executedCommands.size)
            assertTrue(executedCommands[0].second.contains("build"))
        }

    @Test
    fun `Custom Gradle task with custom properties`() =
        runTest {
            plugin.task("dependencyUpdates") {
                property("revision", "release")
                property("outputFormatter", "json")
            }

            val args = executedCommands[0].second
            assertTrue(args.contains("dependencyUpdates"))
            assertTrue(args.contains("-Prevision=release"))
            assertTrue(args.contains("-PoutputFormatter=json"))
        }
}
