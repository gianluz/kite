package io.kite.runtime.scheduler

import io.kite.core.ExecutionContext
import io.kite.core.ProcessExecutionContext
import io.kite.core.Segment
import io.kite.core.SegmentStatus
import io.kite.runtime.graph.SegmentGraph
import io.kite.runtime.graph.TopologicalSort
import io.kite.runtime.process.ProcessExecutionProviderImpl

/**
 * Interface for segment schedulers.
 *
 * Schedulers are responsible for executing segments in the correct order,
 * respecting dependencies and handling execution status.
 */
interface SegmentScheduler {
    /**
     * Executes segments according to the scheduler's strategy.
     *
     * @param segments List of segments to execute
     * @param context Execution context
     * @return Execution result with status for each segment
     */
    suspend fun execute(
        segments: List<Segment>,
        context: ExecutionContext
    ): SchedulerResult
}

/**
 * Sequential scheduler that executes segments one at a time in topological order.
 *
 * This is the simplest scheduler - segments are executed sequentially,
 * respecting their dependencies.
 */
class SequentialScheduler : SegmentScheduler {

    override suspend fun execute(
        segments: List<Segment>,
        context: ExecutionContext
    ): SchedulerResult {
        if (segments.isEmpty()) {
            return SchedulerResult(emptyMap())
        }

        // Build graph and sort
        val graph = SegmentGraph(segments)
        val validation = graph.validate()

        if (!validation.isValid) {
            return SchedulerResult(
                segmentResults = segments.associate {
                    it.name to SegmentResult(
                        segment = it,
                        status = SegmentStatus.SKIPPED,
                        error = "Graph validation failed: ${validation.errors.first()}"
                    )
                }
            )
        }

        val sorter = TopologicalSort(graph)
        val sorted = try {
            sorter.sort()
        } catch (e: Exception) {
            return SchedulerResult(
                segmentResults = segments.associate {
                    it.name to SegmentResult(
                        segment = it,
                        status = SegmentStatus.FAILURE,
                        error = e.message
                    )
                }
            )
        }

        // Track execution time
        val executionStartTime = System.currentTimeMillis()

        // Execute segments sequentially
        val results = mutableMapOf<String, SegmentResult>()

        for (segment in sorted) {
            // Check if segment should execute based on condition
            if (!segment.shouldExecute(context)) {
                results[segment.name] = SegmentResult(
                    segment = segment,
                    status = SegmentStatus.SKIPPED,
                    message = "Skipped due to condition"
                )
                continue
            }

            // Check if dependencies succeeded
            // Segments skip if dependencies failed OR were skipped (cascading skips)
            val dependenciesFailed = segment.dependsOn.any { depName ->
                val depResult = results[depName]
                depResult?.status?.isFailed == true || depResult?.status == SegmentStatus.SKIPPED
            }

            if (dependenciesFailed) {
                results[segment.name] = SegmentResult(
                    segment = segment,
                    status = SegmentStatus.SKIPPED,
                    message = "Skipped due to failed or skipped dependencies"
                )
                continue
            }

            // Execute the segment
            val result = executeSegment(segment, context)
            results[segment.name] = result
        }

        val executionEndTime = System.currentTimeMillis()
        val executionTimeMs = executionEndTime - executionStartTime

        return SchedulerResult(results, executionTimeMs)
    }

    /**
     * Executes a single segment.
     */
    private suspend fun executeSegment(
        segment: Segment,
        context: ExecutionContext
    ): SegmentResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Set up process execution provider for this segment
            val provider = ProcessExecutionProviderImpl()
            ProcessExecutionContext.setProvider(provider)

            // Set up logging for this segment
            // TODO: Get showInConsole flag from global options
            val logger = io.kite.runtime.logging.LogManager.startSegmentLogging(segment.name, showInConsole = false)

            try {
                // Execute the segment with logger in context
                val contextWithLogger = context.copy(logger = logger)
                segment.execute.invoke(contextWithLogger)

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

                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                SegmentResult(
                    segment = segment,
                    status = SegmentStatus.SUCCESS,
                    durationMs = duration,
                    logOutput = logger.getOutput()
                )
            } finally {
                // Clean up logging
                io.kite.runtime.logging.LogManager.stopSegmentLogging(segment.name)

                // Clean up provider
                ProcessExecutionContext.clear()
            }
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            // Stop logging and capture output
            val logger = io.kite.runtime.logging.LogManager.getLogger(segment.name)
            io.kite.runtime.logging.LogManager.stopSegmentLogging(segment.name)

            SegmentResult(
                segment = segment,
                status = SegmentStatus.FAILURE,
                error = e.message,
                exception = e,
                durationMs = duration,
                logOutput = logger?.getOutput()
            )
        }
    }
}

/**
 * Result of executing a single segment.
 */
data class SegmentResult(
    val segment: Segment,
    val status: SegmentStatus,
    val message: String? = null,
    val error: String? = null,
    val exception: Throwable? = null,
    val durationMs: Long = 0,
    val logOutput: String? = null
) {
    val isSuccess: Boolean get() = status.isSuccessful
    val isFailed: Boolean get() = status.isFailed

    override fun toString(): String {
        val durationStr = if (durationMs > 0) " (${durationMs}ms)" else ""
        return when (status) {
            SegmentStatus.SUCCESS -> "✓ ${segment.name}$durationStr"
            SegmentStatus.SKIPPED -> "○ ${segment.name} - ${message ?: "skipped"}"
            SegmentStatus.FAILURE -> "✗ ${segment.name}$durationStr - ${error ?: "failed"}"
            SegmentStatus.TIMEOUT -> "⏱ ${segment.name}$durationStr - timeout"
            else -> "${segment.name}: $status"
        }
    }
}

/**
 * Result of executing multiple segments.
 */
data class SchedulerResult(
    val segmentResults: Map<String, SegmentResult>,
    val executionTimeMs: Long = 0 // Actual wall-clock time for execution
) {
    val totalSegments: Int get() = segmentResults.size
    val successCount: Int get() = segmentResults.values.count { it.isSuccess }
    val failureCount: Int get() = segmentResults.values.count { it.isFailed }
    val skippedCount: Int get() = segmentResults.values.count { it.status == SegmentStatus.SKIPPED }

    val isSuccess: Boolean get() = failureCount == 0 && successCount > 0

    /** Sum of all segment durations (sequential time if run sequentially) */
    val totalDurationMs: Long get() = segmentResults.values.sumOf { it.durationMs }

    /**
     * Gets the result for a specific segment.
     */
    fun getResult(segmentName: String): SegmentResult? = segmentResults[segmentName]

    /**
     * Gets all failed segments.
     */
    fun failedSegments(): List<SegmentResult> =
        segmentResults.values.filter { it.isFailed }

    /**
     * Gets all successful segments.
     */
    fun successfulSegments(): List<SegmentResult> =
        segmentResults.values.filter { it.isSuccess }

    override fun toString(): String {
        return "SchedulerResult(total=$totalSegments, success=$successCount, " +
                "failed=$failureCount, skipped=$skippedCount, duration=${totalDurationMs}ms, executionTime=${executionTimeMs}ms)"
    }
}
