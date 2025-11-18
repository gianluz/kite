package io.kite.runtime.process

import io.kite.core.ProcessExecutionProvider
import io.kite.core.ProcessExecutionResult
import java.io.File
import kotlin.time.Duration

/**
 * Implementation of ProcessExecutionProvider that uses ProcessExecutor.
 */
class ProcessExecutionProviderImpl : ProcessExecutionProvider {
    private val executor = ProcessExecutor()

    override suspend fun execute(
        command: String,
        vararg args: String,
        workingDir: File,
        env: Map<String, String>,
        timeout: Duration?,
    ): ProcessExecutionResult {
        val result =
            executor.execute(
                command = command,
                args = *args,
                workingDir = workingDir,
                env = env,
                timeout = timeout,
            )

        return ProcessExecutionResult(
            command = result.command,
            exitCode = result.exitCode,
            output = result.stdout,
            duration = result.duration,
        )
    }

    override suspend fun executeOrNull(
        command: String,
        vararg args: String,
        workingDir: File,
        env: Map<String, String>,
        timeout: Duration?,
    ): ProcessExecutionResult? {
        val result =
            executor.executeOrNull(
                command = command,
                args = *args,
                workingDir = workingDir,
                env = env,
                timeout = timeout,
            ) ?: return null

        return ProcessExecutionResult(
            command = result.command,
            exitCode = result.exitCode,
            output = result.stdout,
            duration = result.duration,
        )
    }

    override suspend fun shell(
        command: String,
        workingDir: File,
        env: Map<String, String>,
        timeout: Duration?,
    ): ProcessExecutionResult {
        val result =
            executor.shell(
                command = command,
                workingDir = workingDir,
                env = env,
                timeout = timeout,
            )

        return ProcessExecutionResult(
            command = result.command,
            exitCode = result.exitCode,
            output = result.stdout,
            duration = result.duration,
        )
    }
}
