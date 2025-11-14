package io.kite.runtime.scheduler

import io.kite.core.ExecutionContext
import io.kite.core.Segment
import io.kite.core.SegmentStatus
import io.kite.runtime.graph.SegmentGraph
import io.kite.runtime.graph.TopologicalSort
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

        return SchedulerResult(results)
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
            // Execute the segment
            segment.execute.invoke(context)

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            SegmentResult(
                segment = segment,
                status = SegmentStatus.SUCCESS,
                durationMs = duration
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            SegmentResult(
                segment = segment,
                status = SegmentStatus.FAILURE,
                error = e.message,
                exception = e,
                durationMs = duration
            )
        }
    }
}
