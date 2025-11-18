package io.kite.runtime.process

import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Executes external processes with timeout and output capture support.
 */
class ProcessExecutor {
    /**
     * Executes a command and returns the result.
     *
     * @param command Command to execute (first element)
     * @param args Command arguments
     * @param workingDir Working directory (default: current directory)
     * @param env Environment variables (default: inherit from parent)
     * @param timeout Maximum execution time (null = no timeout)
     * @throws ProcessExecutionException if command fails or times out
     */
    suspend fun execute(
        command: String,
        vararg args: String,
        workingDir: File = File(System.getProperty("user.dir")),
        env: Map<String, String> = emptyMap(),
        timeout: Duration? = null,
    ): ProcessResult {
        return executeInternal(
            command = command,
            args = args.toList(),
            workingDir = workingDir,
            env = env,
            timeout = timeout,
            throwOnError = true,
        ) as ProcessResult.Success
    }

    /**
     * Executes a command and returns null if it fails.
     * Does not throw on non-zero exit code.
     */
    suspend fun executeOrNull(
        command: String,
        vararg args: String,
        workingDir: File = File(System.getProperty("user.dir")),
        env: Map<String, String> = emptyMap(),
        timeout: Duration? = null,
    ): ProcessResult? {
        return try {
            executeInternal(
                command = command,
                args = args.toList(),
                workingDir = workingDir,
                env = env,
                timeout = timeout,
                throwOnError = false,
            ).takeIf { it is ProcessResult.Success }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Executes a shell command (wraps in sh -c on Unix, cmd /c on Windows).
     */
    suspend fun shell(
        command: String,
        workingDir: File = File(System.getProperty("user.dir")),
        env: Map<String, String> = emptyMap(),
        timeout: Duration? = null,
    ): ProcessResult {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        return if (isWindows) {
            execute("cmd", "/c", command, workingDir = workingDir, env = env, timeout = timeout)
        } else {
            execute("sh", "-c", command, workingDir = workingDir, env = env, timeout = timeout)
        }
    }

    /**
     * Internal execution method.
     */
    private suspend fun executeInternal(
        command: String,
        args: List<String>,
        workingDir: File,
        env: Map<String, String>,
        timeout: Duration?,
        throwOnError: Boolean,
    ): ProcessResult =
        withContext(Dispatchers.IO) {
            val fullCommand = listOf(command) + args
            val commandString = fullCommand.joinToString(" ")

            // Build process
            val processBuilder =
                ProcessBuilder(fullCommand)
                    .directory(workingDir)
                    .redirectErrorStream(true) // Merge stderr into stdout

            // Set environment variables
            if (env.isNotEmpty()) {
                processBuilder.environment().putAll(env)
            }

            val startTime = System.currentTimeMillis()
            val process =
                try {
                    processBuilder.start()
                } catch (e: IOException) {
                    val duration = System.currentTimeMillis() - startTime

                    throw ProcessExecutionException(
                        command = commandString,
                        exitCode = -1,
                        stdout = "",
                        stderr = e.message ?: "Failed to start process",
                        duration = duration,
                        cause = e,
                    )
                }

            // Read output in a separate coroutine
            val outputJob =
                async {
                    process.inputStream.bufferedReader().use { it.readText() }
                }

            // Wait for process with timeout
            val exitCode =
                try {
                    if (timeout != null) {
                        withTimeout(timeout) {
                            waitForProcess(process)
                        }
                    } else {
                        waitForProcess(process)
                    }
                } catch (e: TimeoutCancellationException) {
                    // Kill process on timeout
                    process.destroy()
                    process.waitFor(5, TimeUnit.SECONDS) // Give it time to die
                    if (process.isAlive) {
                        process.destroyForcibly()
                    }

                    val duration = System.currentTimeMillis() - startTime
                    val output = outputJob.getCompleted()

                    throw ProcessExecutionException(
                        command = commandString,
                        exitCode = -1,
                        stdout = output,
                        stderr = "Process timed out after $timeout",
                        duration = duration,
                        cause = e,
                    )
                }

            val duration = System.currentTimeMillis() - startTime
            val output = outputJob.await()

            return@withContext when {
                exitCode == 0 ->
                    ProcessResult.Success(
                        command = commandString,
                        exitCode = exitCode,
                        stdout = output,
                        duration = duration,
                    )

                throwOnError -> throw ProcessExecutionException(
                    command = commandString,
                    exitCode = exitCode,
                    stdout = output,
                    stderr = "",
                    duration = duration,
                )

                else ->
                    ProcessResult.Failure(
                        command = commandString,
                        exitCode = exitCode,
                        stdout = output,
                        duration = duration,
                    )
            }
        }

    /**
     * Suspending wait for process completion.
     */
    private suspend fun waitForProcess(process: Process): Int =
        withContext(Dispatchers.IO) {
            while (process.isAlive) {
                delay(50)
            }
            process.exitValue()
        }
}

/**
 * Result of process execution.
 */
sealed class ProcessResult {
    abstract val command: String
    abstract val exitCode: Int
    abstract val stdout: String
    abstract val duration: Long

    data class Success(
        override val command: String,
        override val exitCode: Int,
        override val stdout: String,
        override val duration: Long,
    ) : ProcessResult() {
        val output: String get() = stdout
    }

    data class Failure(
        override val command: String,
        override val exitCode: Int,
        override val stdout: String,
        override val duration: Long,
    ) : ProcessResult()
}

/**
 * Exception thrown when process execution fails.
 */
class ProcessExecutionException(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val duration: Long,
    cause: Throwable? = null,
) : RuntimeException(
        "Command failed: $command (exit code: $exitCode)\nOutput: $stdout\nError: $stderr",
        cause,
    )
