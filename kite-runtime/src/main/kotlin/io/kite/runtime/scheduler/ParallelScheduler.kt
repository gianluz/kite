package io.kite.runtime.scheduler

import io.kite.core.ExecutionContext
import io.kite.core.ProcessExecutionContext
import io.kite.core.Segment
import io.kite.core.SegmentStatus
import io.kite.runtime.graph.SegmentGraph
import io.kite.runtime.graph.TopologicalSort
import io.kite.runtime.process.ProcessExecutionProviderImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentHashMap

/**
 * Parallel scheduler that executes independent segments concurrently.
 *
 * Uses Kotlin coroutines to execute segments that don't depend on each other
 * in parallel, while respecting maxConcurrency limits.
 */
class ParallelScheduler(
    private val maxConcurrency: Int = Runtime.getRuntime().availableProcessors()
) : SegmentScheduler {

    init {
        require(maxConcurrency > 0) { "maxConcurrency must be > 0" }
    }

    override suspend fun execute(
        segments: List<Segment>,
        context: ExecutionContext
    ): SchedulerResult {
        if (segments.isEmpty()) {
            return SchedulerResult(emptyMap())
        }

        // Build graph and validate
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

        // Get topological sort with levels
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

        // Get execution levels for parallel execution
        val levels = sorter.sortByLevels()

        // Track execution time
        val executionStartTime = System.currentTimeMillis()

        // Execute segments level by level
        val results = ConcurrentHashMap<String, SegmentResult>()
        val semaphore = Semaphore(maxConcurrency)

        for (level in levels) {
            // Execute all segments in this level in parallel
            coroutineScope {
                level.map { segment ->
                    async {
                        semaphore.acquire()
                        try {
                            executeSegmentWithChecks(segment, results, context)
                        } finally {
                            semaphore.release()
                        }
                    }
                }.awaitAll()
            }
        }

        val executionEndTime = System.currentTimeMillis()
        val executionTimeMs = executionEndTime - executionStartTime

        return SchedulerResult(results, executionTimeMs)
    }

    /**
     * Executes a segment after checking conditions and dependencies.
     */
    private suspend fun executeSegmentWithChecks(
        segment: Segment,
        results: ConcurrentHashMap<String, SegmentResult>,
        context: ExecutionContext
    ): SegmentResult {
        // Check if segment should execute based on condition
        if (!segment.shouldExecute(context)) {
            val result = SegmentResult(
                segment = segment,
                status = SegmentStatus.SKIPPED,
                message = "Skipped due to condition"
            )
            results[segment.name] = result
            return result
        }

        // Check if dependencies succeeded
        val dependenciesFailed = segment.dependsOn.any { depName ->
            val depResult = results[depName]
            depResult?.status?.isFailed == true || depResult?.status == SegmentStatus.SKIPPED
        }

        if (dependenciesFailed) {
            val result = SegmentResult(
                segment = segment,
                status = SegmentStatus.SKIPPED,
                message = "Skipped due to failed or skipped dependencies"
            )
            results[segment.name] = result
            return result
        }

        // Execute the segment
        val result = executeSegment(segment, context)
        results[segment.name] = result
        return result
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
