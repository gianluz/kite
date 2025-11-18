package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.kite.cli.Output
import io.kite.cli.globalOptions
import io.kite.core.ExecutionContext
import io.kite.core.PlatformDetector
import io.kite.core.Segment
import io.kite.dsl.FileDiscovery
import io.kite.runtime.scheduler.ParallelScheduler
import io.kite.runtime.scheduler.SequentialScheduler
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Execute a named ride.
 */
class RideCommand : CliktCommand(
    name = "ride",
    help = "Execute a named ride from .kite/rides/<name>.kite.kts"
) {

    private val rideName by argument(
        name = "name",
        help = "Name of the ride to execute"
    )

    private val dryRun by option(
        "--dry-run",
        help = "Show execution plan without running"
    ).flag()

    private val sequential by option(
        "--sequential",
        help = "Force sequential execution (disable parallelism)"
    ).flag()

    override fun run() {
        val opts = globalOptions
        val startTime = System.currentTimeMillis()

        try {
            // Show header
            if (!opts.quiet) {
                Output.header("🪁 Kite Ride: $rideName")
            }

            // Detect platform and create execution context
            if (opts.verbose) {
                Output.progress("Detecting platform...")
            }
            val platform = PlatformDetector.detect()

            // Create artifact manager with .kite/artifacts/ directory
            val artifactsDir = File(".kite/artifacts").toPath()
            val artifactManager = io.kite.core.FileSystemArtifactManager(artifactsDir)

            val context = platform.createContext(emptyMap(), artifactManager)

            if (opts.verbose) {
                Output.info("Platform: ${context.ciPlatform}")
                Output.info("Branch: ${context.branch}")
                if (context.commitSha != null) Output.info("Commit: ${context.commitSha}")
            }

            // Discover and load files
            if (opts.verbose) {
                Output.progress("Loading segments and rides...")
            }

            val kiteDir = File(".kite")
            if (!kiteDir.exists()) {
                Output.error("No .kite directory found in current directory")
                Output.info("Create .kite/segments/ and .kite/rides/ directories to get started")
                throw Exception("Missing .kite directory")
            }

            val discovery = FileDiscovery()
            val loadResult = kotlinx.coroutines.runBlocking {
                discovery.loadAll()
            }

            if (!loadResult.success) {
                Output.error("Failed to load .kite files:")
                loadResult.errors.forEach { error ->
                    Output.error("  ${error.file}: ${error.message}")
                }
                throw Exception("Failed to load .kite files")
            }

            if (opts.verbose) {
                Output.info("Loaded ${loadResult.segments.size} segments")
                Output.info("Loaded ${loadResult.rides.size} rides")
            }

            // Find the ride
            val ride = loadResult.rideMap()[rideName]
            if (ride == null) {
                Output.error("Ride '$rideName' not found")
                val availableRides = loadResult.rideMap().keys
                if (availableRides.isNotEmpty()) {
                    Output.info("Available rides: ${availableRides.joinToString(", ")}")
                } else {
                    Output.info("No rides found in .kite/rides/")
                }
                throw Exception("Ride not found")
            }

            if (opts.verbose) {
                Output.info("Found ride: ${ride.name}")
            }

            // Build list of segments to execute
            val segmentMap = loadResult.segmentMap()
            val segmentsToExecute = collectSegments(ride.flow, segmentMap)

            if (segmentsToExecute.isEmpty()) {
                Output.warning("No segments to execute")
                return
            }

            if (!opts.quiet) {
                Output.section("Execution Plan")
                Output.info("Segments to execute: ${segmentsToExecute.size}")
                segmentsToExecute.forEach { segment ->
                    val deps = if (segment.dependsOn.isNotEmpty()) {
                        " (depends on: ${segment.dependsOn.joinToString(", ")})"
                    } else ""
                    Output.progress("• ${segment.name}$deps")
                }
            }

            // Dry run mode
            if (dryRun) {
                Output.info("Dry run mode - execution skipped")
                return
            }

            // Execute the ride
            if (!opts.quiet) {
                Output.section("Executing Ride")
            }

            val scheduler = if (sequential) {
                if (opts.verbose) Output.info("Using sequential scheduler")
                SequentialScheduler()
            } else {
                val concurrency = ride.maxConcurrency ?: Runtime.getRuntime().availableProcessors()
                if (opts.verbose) Output.info("Using parallel scheduler (max concurrency: $concurrency)")
                ParallelScheduler(maxConcurrency = concurrency)
            }

            val result = kotlinx.coroutines.runBlocking {
                scheduler.execute(segmentsToExecute, context)
            }

            // Show results
            if (!opts.quiet) {
                Output.section("Results")
                result.segmentResults.values.sortedBy { it.segment.name }.forEach { segResult ->
                    Output.result(
                        segment = segResult.segment.name,
                        status = segResult.status.name,
                        duration = segResult.durationMs
                    )

                    // Show error details if segment failed
                    if (segResult.isFailed && segResult.error != null) {
                        Output.error("    Error: ${segResult.error}")
                    }
                    if (opts.verbose && segResult.exception != null) {
                        Output.info("    Exception: ${segResult.exception!!.message}")
                    }
                }
            }

            // Calculate timing stats
            val totalDuration = System.currentTimeMillis() - startTime
            val sequentialDuration = result.totalDurationMs  // Sum of all segment durations
            val parallelDuration = result.executionTimeMs    // Actual wall-clock execution time

            // Show summary
            if (!opts.quiet) {
                Output.summary(
                    total = result.totalSegments,
                    success = result.successCount,
                    failed = result.failureCount,
                    skipped = result.skippedCount,
                    duration = totalDuration,
                    parallelDuration = parallelDuration,
                    sequentialDuration = sequentialDuration
                )
            }

            // Exit with error code if any failures
            if (result.failureCount > 0) {
                // Show failed segments details
                if (!opts.quiet) {
                    Output.error("Failed Segments:")
                    result.failedSegments().forEach { segResult ->
                        Output.error("  • ${segResult.segment.name}")
                        if (segResult.error != null) {
                            Output.error("    ${segResult.error}")
                        }
                    }
                }
                throw Exception("Ride failed with ${result.failureCount} failed segments")
            }

        } catch (e: Exception) {
            if (opts.debug) {
                Output.error("Exception: ${e.message}")
                e.printStackTrace()
            }
            throw e
        }
    }

    /**
     * Recursively collect all segments from a flow node.
     */
    private fun collectSegments(
        flow: io.kite.core.FlowNode,
        segmentMap: Map<String, Segment>
    ): List<Segment> {
        val segments = mutableListOf<Segment>()

        when (flow) {
            is io.kite.core.FlowNode.Sequential -> {
                flow.nodes.forEach { node ->
                    segments.addAll(collectSegments(node, segmentMap))
                }
            }

            is io.kite.core.FlowNode.Parallel -> {
                flow.nodes.forEach { node ->
                    segments.addAll(collectSegments(node, segmentMap))
                }
            }

            is io.kite.core.FlowNode.SegmentRef -> {
                val segment = segmentMap[flow.segmentName]
                if (segment != null) {
                    // Apply overrides if present
                    val finalSegment = if (flow.overrides != null) {
                        applyOverrides(segment, flow.overrides)
                    } else {
                        segment
                    }
                    segments.add(finalSegment)
                } else {
                    Output.error("Segment '${flow.segmentName}' referenced in ride but not found")
                }
            }
        }

        return segments
    }

    /**
     * Apply ride overrides to a segment.
     */
    private fun applyOverrides(
        segment: Segment,
        overrides: io.kite.core.SegmentOverrides
    ): Segment {
        // For now, return the segment as-is since Segment is immutable
        // In a full implementation, we'd create a new Segment with overrides
        // But the execute lambda makes this tricky - would need a builder pattern
        return segment
    }
}
