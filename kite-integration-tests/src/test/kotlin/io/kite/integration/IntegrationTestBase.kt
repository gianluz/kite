package io.kite.integration

import io.kite.core.PlatformDetector
import io.kite.dsl.FileDiscovery
import io.kite.runtime.scheduler.ParallelScheduler
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Base class for integration tests that execute full Kite rides.
 *
 * Provides utilities for:
 * - Loading test fixtures from resources
 * - Running rides programmatically
 * - Capturing output
 * - Asserting on execution results
 */
abstract class IntegrationTestBase {

    @TempDir
    lateinit var workspaceRoot: File

    /**
     * Load a test fixture file from resources into the workspace.
     *
     * @param resourcePath Path relative to resources/fixtures (e.g., "segments/build.kite.kts")
     * @param targetPath Path in workspace (e.g., ".kite/segments/build.kite.kts")
     */
    protected fun loadFixture(resourcePath: String, targetPath: String) {
        val resource = javaClass.classLoader.getResourceAsStream("fixtures/$resourcePath")
            ?: error("Fixture not found: fixtures/$resourcePath")

        val targetFile = File(workspaceRoot, targetPath)
        targetFile.parentFile.mkdirs()
        targetFile.outputStream().use { output ->
            resource.copyTo(output)
        }
    }

    /**
     * Create a segment file directly in the workspace.
     */
    protected fun createSegmentFile(name: String, content: String) {
        val file = File(workspaceRoot, ".kite/segments/$name")
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    /**
     * Create a ride file directly in the workspace.
     */
    protected fun createRideFile(name: String, content: String) {
        val file = File(workspaceRoot, ".kite/rides/$name")
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    /**
     * Execute a ride and return the result.
     */
    protected fun executeRide(rideName: String): RideExecutionResult = runBlocking {
        // Discover and load Kite files
        val discovery = FileDiscovery(workspaceRoot)
        val loadResult = discovery.loadAll()

        assertTrue(loadResult.success, "Failed to load Kite files: ${loadResult.errors}")

        // Find the ride
        val ride = loadResult.rides.find { it.name == rideName }
            ?: error("Ride not found: $rideName. Available: ${loadResult.rides.map { it.name }}")

        // Collect segments from the ride's flow
        val segmentMap = loadResult.segmentMap()
        val segments = collectSegmentsFromFlow(ride.flow, segmentMap)

        // Create execution context
        val platform = PlatformDetector.detect()
        val context = platform.createContext(emptyMap())

        // Execute with captured output
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val originalOut = System.out
        val originalErr = System.err

        try {
            System.setOut(PrintStream(outputStream))
            System.setErr(PrintStream(errorStream))

            val scheduler = ParallelScheduler(maxConcurrency = ride.maxConcurrency ?: 4)
            val result = scheduler.execute(segments, context)

            RideExecutionResult(
                success = result.isSuccess,
                totalSegments = result.totalSegments,
                successCount = result.successCount,
                failureCount = result.failureCount,
                duration = result.executionTimeMs.milliseconds,
                output = outputStream.toString(),
                error = errorStream.toString(),
                segmentResults = result.segmentResults.mapValues { it.value.isSuccess }
            )
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    /**
     * Recursively collect segments from a flow node.
     */
    private fun collectSegmentsFromFlow(
        flow: io.kite.core.FlowNode,
        segmentMap: Map<String, io.kite.core.Segment>
    ): List<io.kite.core.Segment> {
        val segments = mutableListOf<io.kite.core.Segment>()

        when (flow) {
            is io.kite.core.FlowNode.Sequential -> {
                flow.nodes.forEach { node ->
                    segments.addAll(collectSegmentsFromFlow(node, segmentMap))
                }
            }

            is io.kite.core.FlowNode.Parallel -> {
                flow.nodes.forEach { node ->
                    segments.addAll(collectSegmentsFromFlow(node, segmentMap))
                }
            }

            is io.kite.core.FlowNode.SegmentRef -> {
                val segment = segmentMap[flow.segmentName]
                if (segment != null) {
                    segments.add(segment)
                }
            }
        }

        return segments
    }

    /**
     * Result of executing a ride in tests.
     */
    data class RideExecutionResult(
        val success: Boolean,
        val totalSegments: Int,
        val successCount: Int,
        val failureCount: Int,
        val duration: kotlin.time.Duration,
        val output: String,
        val error: String,
        val segmentResults: Map<String, Boolean>
    ) {
        fun assertSuccess() {
            assertTrue(success, "Ride execution failed. Output:\n$output\nError:\n$error")
        }

        fun assertSegmentSucceeded(segmentName: String) {
            val result = segmentResults[segmentName]
            assertTrue(result == true, "Segment '$segmentName' did not succeed")
        }

        fun assertSegmentFailed(segmentName: String) {
            val result = segmentResults[segmentName]
            assertTrue(result == false, "Segment '$segmentName' did not fail")
        }

        fun assertOutputContains(text: String) {
            assertTrue(output.contains(text), "Output does not contain '$text'. Actual output:\n$output")
        }

        fun assertErrorContains(text: String) {
            assertTrue(error.contains(text), "Error output does not contain '$text'. Actual error:\n$error")
        }
    }
}
