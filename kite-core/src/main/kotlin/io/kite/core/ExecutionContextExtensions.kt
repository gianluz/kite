package io.kite.core

import java.io.File
import kotlin.time.Duration

/**
 * Execution context extension functions for process execution.
 *
 * These functions will be implemented by the runtime module.
 * This file defines the API that segments can use.
 */

/**
 * Placeholder for process execution.
 * The actual implementation will be provided by kite-runtime.
 */
interface ProcessExecutionProvider {
    suspend fun execute(
        command: String,
        vararg args: String,
        workingDir: File = File(System.getProperty("user.dir")),
        env: Map<String, String> = emptyMap(),
        timeout: Duration? = null
    ): ProcessExecutionResult

    suspend fun executeOrNull(
        command: String,
        vararg args: String,
        workingDir: File = File(System.getProperty("user.dir")),
        env: Map<String, String> = emptyMap(),
        timeout: Duration? = null
    ): ProcessExecutionResult?

    suspend fun shell(
        command: String,
        workingDir: File = File(System.getProperty("user.dir")),
        env: Map<String, String> = emptyMap(),
        timeout: Duration? = null
    ): ProcessExecutionResult
}

/**
 * Result of process execution (simplified for core module).
 */
data class ProcessExecutionResult(
    val command: String,
    val exitCode: Int,
    val output: String,
    val duration: Long
)

/**
 * Thread-local storage for the process execution provider.
 * This will be set by the runtime when executing segments.
 */
object ProcessExecutionContext {
    private val provider = ThreadLocal<ProcessExecutionProvider>()

    fun setProvider(p: ProcessExecutionProvider) {
        provider.set(p)
    }

    fun getProvider(): ProcessExecutionProvider? {
        return provider.get()
    }

    fun clear() {
        provider.remove()
    }
}

/**
 * Executes a command from within a segment.
 * Throws exception if command fails.
 */
suspend fun ExecutionContext.exec(
    command: String,
    vararg args: String,
    workingDir: File = File(System.getProperty("user.dir")),
    timeout: Duration? = null
): ProcessExecutionResult {
    val provider = ProcessExecutionContext.getProvider()
        ?: throw IllegalStateException("Process execution not available. Are you running outside a segment?")

    // Log command execution
    logger.logCommandStart("$command ${args.joinToString(" ")}")

    val startTime = System.currentTimeMillis()
    val result = try {
        provider.execute(command, *args, workingDir = workingDir, timeout = timeout)
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        logger.logCommandComplete("$command ${args.joinToString(" ")}", -1, duration)
        logger.error("Command failed: ${e.message}")
        throw e
    }

    // Log command output and completion
    logger.logCommandOutput(result.output, isError = result.exitCode != 0)
    logger.logCommandComplete("$command ${args.joinToString(" ")}", result.exitCode, result.duration)

    return result
}

/**
 * Executes a command and returns null if it fails.
 */
suspend fun ExecutionContext.execOrNull(
    command: String,
    vararg args: String,
    workingDir: File = File(System.getProperty("user.dir")),
    timeout: Duration? = null
): ProcessExecutionResult? {
    val provider = ProcessExecutionContext.getProvider()
        ?: throw IllegalStateException("Process execution not available. Are you running outside a segment?")

    // Log command execution
    logger.logCommandStart("$command ${args.joinToString(" ")}")

    val startTime = System.currentTimeMillis()
    val result = provider.executeOrNull(command, *args, workingDir = workingDir, timeout = timeout)

    if (result != null) {
        logger.logCommandOutput(result.output, isError = result.exitCode != 0)
        logger.logCommandComplete("$command ${args.joinToString(" ")}", result.exitCode, result.duration)
    } else {
        val duration = System.currentTimeMillis() - startTime
        logger.logCommandComplete("$command ${args.joinToString(" ")}", -1, duration)
    }

    return result
}

/**
 * Executes a shell command.
 */
suspend fun ExecutionContext.shell(
    command: String,
    workingDir: File = File(System.getProperty("user.dir")),
    timeout: Duration? = null
): ProcessExecutionResult {
    val provider = ProcessExecutionContext.getProvider()
        ?: throw IllegalStateException("Process execution not available. Are you running outside a segment?")

    // Log command execution
    logger.logCommandStart(command)

    val startTime = System.currentTimeMillis()
    val result = try {
        provider.shell(command, workingDir = workingDir, timeout = timeout)
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        logger.logCommandComplete(command, -1, duration)
        logger.error("Shell command failed: ${e.message}")
        throw e
    }

    // Log command output and completion
    logger.logCommandOutput(result.output, isError = result.exitCode != 0)
    logger.logCommandComplete(command, result.exitCode, result.duration)

    return result
}
