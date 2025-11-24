package io.kite.runtime.scheduler

import io.kite.core.ExecutionContext
import io.kite.core.ProcessExecutionContext
import io.kite.core.Segment
import io.kite.core.SegmentStatus
import io.kite.runtime.process.ProcessExecutionProviderImpl
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Shared logic for executing segments with timeout and retry support.
 */
object SegmentExecutor {
    /**
     * Executes a single segment with timeout and retry support.
     */
    suspend fun executeSegment(
        segment: Segment,
        context: ExecutionContext,
    ): SegmentResult {
        val startTime = System.currentTimeMillis()

        // Attempt execution with retries
        var attemptNumber = 0
        val maxAttempts = segment.maxRetries + 1 // +1 for initial attempt

        while (attemptNumber < maxAttempts) {
            val isRetry = attemptNumber > 0
            val attemptStartTime = System.currentTimeMillis()

            if (isRetry) {
                // Wait before retry
                val retryDelay = segment.retryDelay
                if (retryDelay.isPositive()) {
                    delay(retryDelay.inWholeMilliseconds)
                }
            }

            val result = executeSegmentAttempt(segment, context, attemptNumber, isRetry)

            // If success or timeout, return immediately (don't retry)
            if (result.status == SegmentStatus.SUCCESS || result.status == SegmentStatus.TIMEOUT) {
                val totalDuration = System.currentTimeMillis() - startTime
                return result.copy(durationMs = totalDuration)
            }

            // Check if we should retry based on exception type
            if (result.exception != null && shouldRetry(segment, result.exception)) {
                attemptNumber++
                if (attemptNumber < maxAttempts) {
                    // Log retry attempt
                    val logger = io.kite.runtime.logging.LogManager.getLogger(segment.name)
                    logger?.info("Retrying segment '${segment.name}' (attempt ${attemptNumber + 1}/$maxAttempts)")
                    continue
                }
            }

            // No more retries or shouldn't retry, return the result
            val totalDuration = System.currentTimeMillis() - startTime
            return result.copy(durationMs = totalDuration)
        }

        // Should never reach here, but return failure just in case
        val totalDuration = System.currentTimeMillis() - startTime
        return SegmentResult(
            segment = segment,
            status = SegmentStatus.FAILURE,
            error = "Max retries exceeded",
            durationMs = totalDuration,
        )
    }

    /**
     * Checks if segment should be retried based on exception type.
     */
    private fun shouldRetry(
        segment: Segment,
        exception: Throwable,
    ): Boolean {
        // If no retry filter specified, retry on any exception
        if (segment.retryOn.isEmpty()) {
            return true
        }

        // Check if exception matches any of the retry filters
        val exceptionClassName = exception::class.qualifiedName ?: exception::class.simpleName ?: ""
        return segment.retryOn.any { retryClassName ->
            exceptionClassName.contains(retryClassName) ||
                exception::class.simpleName?.contains(retryClassName) == true
        }
    }

    /**
     * Executes a single attempt of a segment with timeout support.
     */
    private suspend fun executeSegmentAttempt(
        segment: Segment,
        context: ExecutionContext,
        attemptNumber: Int,
        isRetry: Boolean,
    ): SegmentResult {
        val attemptStartTime = System.currentTimeMillis()
        var finalStatus = SegmentStatus.FAILURE
        var finalError: String? = null
        var finalException: Throwable? = null

        try {
            // Set up process execution provider for this segment
            val provider = ProcessExecutionProviderImpl()
            ProcessExecutionContext.setProvider(provider)

            // Set up logging for this segment
            val logger =
                if (attemptNumber == 0) {
                    io.kite.runtime.logging.LogManager.startSegmentLogging(segment.name, showInConsole = false)
                } else {
                    io.kite.runtime.logging.LogManager.getLogger(segment.name)
                        ?: io.kite.runtime.logging.LogManager.startSegmentLogging(segment.name, showInConsole = false)
                }

            if (isRetry) {
                logger.info("Retry attempt ${attemptNumber + 1}")
            }

            try {
                val contextWithLogger = context.copy(logger = logger)

                // Execute with timeout if specified
                val timeout = segment.timeout
                if (timeout != null) {
                    withTimeout(timeout.inWholeMilliseconds) {
                        segment.execute.invoke(contextWithLogger)
                    }
                } else {
                    // No timeout, execute normally
                    segment.execute.invoke(contextWithLogger)
                }

                // Store output artifacts if segment succeeded
                segment.outputs.forEach { (artifactName, artifactPath) ->
                    try {
                        val fullPath = contextWithLogger.workspace.resolve(artifactPath)
                        contextWithLogger.artifacts.put(artifactName, fullPath)
                        logger.info("Stored artifact '$artifactName' from '$artifactPath'")
                    } catch (e: Exception) {
                        logger.warn("Failed to store artifact '$artifactName': ${e.message}")
                    }
                }

                finalStatus = SegmentStatus.SUCCESS

                // Call onSuccess hook on success (we don't retry after success anyway)
                segment.onSuccess?.invoke(contextWithLogger)
            } catch (e: TimeoutCancellationException) {
                // Handle timeout exceptions
                finalStatus = SegmentStatus.TIMEOUT
                finalError = "Segment '${segment.name}' exceeded timeout of ${segment.timeout}"
                finalException = e
                logger.error(finalError!!)
            } catch (e: Exception) {
                // Handle non-timeout exceptions
                finalStatus = SegmentStatus.FAILURE
                finalError = e.message
                finalException = e

                // Call onFailure hook only on final attempt
                if (attemptNumber >= segment.maxRetries) {
                    try {
                        val contextWithLogger = context.copy(logger = logger)
                        segment.onFailure?.invoke(contextWithLogger, e)
                    } catch (hookError: Exception) {
                        logger.error("onFailure hook failed: ${hookError.message}")
                    }
                }
            } finally {
                // Call onComplete hook only on final attempt (last retry or success)
                if (finalStatus == SegmentStatus.SUCCESS || finalStatus == SegmentStatus.TIMEOUT || attemptNumber >= segment.maxRetries) {
                    try {
                        val contextWithLogger = context.copy(logger = logger)
                        segment.onComplete?.invoke(contextWithLogger, finalStatus)
                    } catch (hookError: Exception) {
                        logger.error("onComplete hook failed: ${hookError.message}")
                    }
                }

                // Clean up logging only on final attempt
                if (finalStatus == SegmentStatus.SUCCESS || finalStatus == SegmentStatus.TIMEOUT || attemptNumber >= segment.maxRetries) {
                    io.kite.runtime.logging.LogManager.stopSegmentLogging(segment.name)
                }

                // Clean up provider
                ProcessExecutionContext.clear()
            }

            val endTime = System.currentTimeMillis()
            val duration = endTime - attemptStartTime
            val finalLogger = io.kite.runtime.logging.LogManager.getLogger(segment.name)

            return SegmentResult(
                segment = segment,
                status = finalStatus,
                error = finalError,
                exception = finalException,
                durationMs = duration,
                logOutput = finalLogger?.getOutput(),
            )
        } catch (e: Exception) {
            // This catches setup/teardown errors
            val endTime = System.currentTimeMillis()
            val duration = endTime - attemptStartTime

            // Stop logging and capture output (only on final attempt)
            val logger = io.kite.runtime.logging.LogManager.getLogger(segment.name)
            if (attemptNumber >= segment.maxRetries || finalStatus == SegmentStatus.TIMEOUT) {
                io.kite.runtime.logging.LogManager.stopSegmentLogging(segment.name)
            }

            // Determine status based on exception type
            val status =
                when (e) {
                    is TimeoutCancellationException -> SegmentStatus.TIMEOUT
                    else -> SegmentStatus.FAILURE
                }

            return SegmentResult(
                segment = segment,
                status = status,
                error = e.message,
                exception = e,
                durationMs = duration,
                logOutput = logger?.getOutput(),
            )
        }
    }
}
