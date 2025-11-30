package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.kite.cli.Output
import io.kite.cli.globalOptions
import io.kite.core.FileSystemArtifactManager
import io.kite.core.FlowNode
import io.kite.core.PlatformDetector
import io.kite.core.Segment
import io.kite.core.SegmentOverrides
import io.kite.core.restoreFromManifest
import io.kite.core.saveManifest
import io.kite.dsl.FileDiscovery
import io.kite.runtime.graph.SegmentGraph
import io.kite.runtime.scheduler.ParallelScheduler
import io.kite.runtime.scheduler.SequentialScheduler
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Execute a named ride.
 */
class RideCommand : CliktCommand(
    name = "ride",
    help = "Execute a named ride from .kite/rides/<name>.kite.kts",
) {
    private val rideName by argument(
        name = "name",
        help = "Name of the ride to execute",
    )

    private val dryRun by option(
        "--dry-run",
        help = "Show execution plan without running",
    ).flag()

    private val sequential by option(
        "--sequential",
        help = "Force sequential execution (disable parallelism)",
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

            // Restore artifacts from manifest (for cross-ride/CI artifact sharing)
            val restoredCount = artifactManager.restoreFromManifest(artifactsDir.toFile())
            if (restoredCount > 0 && opts.verbose) {
                Output.info("Restored $restoredCount artifacts from previous ride")
            }

            val context = platform.createContext(emptyMap(), artifactManager)

            if (opts.verbose) {
                Output.info("Branch: ${context.branch}, CI: ${context.isCI}")
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
            val loadResult =
                kotlinx.coroutines.runBlocking {
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
            val (segmentsToExecute, missingSegments) = collectSegmentsWithValidation(ride.flow, segmentMap)

            if (segmentsToExecute.isEmpty()) {
                Output.warning("No segments to execute")
                return
            }

            // Validate and show execution plan
            val graph = validateAndShowPlan(segmentsToExecute, missingSegments, loadResult.segments)

            // Dry run mode
            if (dryRun) {
                Output.info("")
                Output.info("Dry run mode - execution skipped")
                return
            }

            // Execute the ride
            if (!opts.quiet) {
                Output.section("Executing Ride")
            }

            val scheduler =
                if (sequential) {
                    if (opts.verbose) Output.info("Using sequential scheduler")
                    SequentialScheduler()
                } else {
                    val concurrency = ride.maxConcurrency ?: Runtime.getRuntime().availableProcessors()
                    if (opts.verbose) Output.info("Using parallel scheduler (max concurrency: $concurrency)")
                    ParallelScheduler(maxConcurrency = concurrency)
                }

            val result =
                kotlinx.coroutines.runBlocking {
                    scheduler.execute(segmentsToExecute, context)
                }

            // Call ride lifecycle hooks
            kotlinx.coroutines.runBlocking {
                if (result.isSuccess) {
                    // Call onSuccess hook
                    ride.onSuccess?.invoke()
                } else {
                    // Call onFailure hook with first error
                    val firstError = result.failedSegments().firstOrNull()?.exception
                    if (firstError != null) {
                        ride.onFailure?.invoke(firstError)
                    }
                }

                // Always call onComplete hook
                ride.onComplete?.invoke(result.isSuccess)
            }

            // Save artifact manifest for cross-ride/CI sharing
            artifactManager.saveManifest(artifactsDir.toFile())
            if (opts.verbose && artifactManager.list().isNotEmpty()) {
                Output.info("Saved artifact manifest with ${artifactManager.list().size} artifacts")
            }

            // Show results
            if (!opts.quiet) {
                Output.section("Results")
                result.segmentResults.values.sortedBy { it.segment.name }.forEach { segResult ->
                    Output.result(
                        segment = segResult.segment.name,
                        status = segResult.status.name,
                        duration = segResult.durationMs,
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
            val sequentialDuration = result.totalDurationMs // Sum of all segment durations
            val parallelDuration = result.executionTimeMs // Actual wall-clock execution time

            // Show summary
            if (!opts.quiet) {
                Output.summary(
                    total = result.totalSegments,
                    success = result.successCount,
                    failed = result.failureCount,
                    skipped = result.skippedCount,
                    duration = totalDuration,
                    parallelDuration = parallelDuration,
                    sequentialDuration = sequentialDuration,
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
     * Validates the segment graph and shows execution plan.
     *
     * @return The validated SegmentGraph
     * @throws Exception if validation fails
     */
    private fun validateAndShowPlan(
        segmentsToExecute: List<Segment>,
        missingSegments: Set<String>,
        allSegments: List<Segment>,
    ): SegmentGraph {
        val opts = globalOptions

        // Validate the segment graph before execution
        if (!opts.quiet) {
            Output.section("Validating Execution Plan")
        }

        val validationErrors = mutableListOf<String>()

        // Check for missing segment references
        if (missingSegments.isNotEmpty()) {
            validationErrors.add("Missing segment references:")
            for (segmentName in missingSegments) {
                validationErrors.add("  • Segment '$segmentName' is referenced but not found")
            }
        }

        // Build dependency graph and validate
        val graph = SegmentGraph(segmentsToExecute)
        val graphValidation = graph.validate()

        if (!graphValidation.isValid) {
            validationErrors.addAll(graphValidation.errors.map { "  • $it" })
        }

        // If there are validation errors, fail immediately
        if (validationErrors.isNotEmpty()) {
            Output.error("Validation failed:")
            for (error in validationErrors) {
                Output.error(error)
            }

            // Show helpful context
            if (missingSegments.isNotEmpty()) {
                val availableSegments = allSegments.map { it.name }.sorted()
                Output.info("")
                Output.info("Available segments: ${availableSegments.joinToString(", ")}")
            }

            throw Exception("Ride validation failed with ${validationErrors.size} error(s)")
        }

        if (opts.verbose) {
            Output.info("✓ All segment references valid")
            Output.info("✓ No circular dependencies detected")
            Output.info("✓ Dependency graph is valid")
        }

        // Show execution plan
        if (!opts.quiet) {
            Output.section("Execution Plan")
            Output.info("Segments to execute: ${segmentsToExecute.size}")
            for (segment in segmentsToExecute) {
                val deps =
                    if (segment.dependsOn.isNotEmpty()) {
                        " (depends on: ${segment.dependsOn.joinToString(", ")})"
                    } else {
                        ""
                    }
                Output.progress("• ${segment.name}$deps")
            }

            // Show graph statistics
            if (opts.verbose) {
                val stats = graph.stats()
                Output.info("")
                Output.info("Graph statistics:")
                Output.info("  • Total dependencies: ${stats.totalEdges}")
                Output.info("  • Max dependencies per segment: ${stats.maxDependencies}")
                Output.info("  • Isolated segments: ${stats.isolatedSegments}")
            }
        }

        return graph
    }

    /**
     * Recursively collect all segments from a flow node with validation tracking.
     *
     * Returns a pair of (found segments, missing segment names).
     */
    private fun collectSegmentsWithValidation(
        flow: io.kite.core.FlowNode,
        segmentMap: Map<String, Segment>,
    ): Pair<List<Segment>, Set<String>> {
        val segments = mutableListOf<Segment>()
        val missingSegments = mutableSetOf<String>()

        fun collectRecursive(node: io.kite.core.FlowNode) {
            when (node) {
                is io.kite.core.FlowNode.Sequential -> {
                    node.nodes.forEach { childNode ->
                        collectRecursive(childNode)
                    }
                }

                is io.kite.core.FlowNode.Parallel -> {
                    node.nodes.forEach { childNode ->
                        collectRecursive(childNode)
                    }
                }

                is io.kite.core.FlowNode.SegmentRef -> {
                    val segment = segmentMap[node.segmentName]
                    if (segment != null) {
                        // Apply overrides if present
                        val finalSegment =
                            if (node.overrides != null) {
                                applyOverrides(segment, node.overrides)
                            } else {
                                segment
                            }
                        segments.add(finalSegment)
                    } else {
                        missingSegments.add(node.segmentName)
                    }
                }
            }
        }

        collectRecursive(flow)
        return Pair(segments, missingSegments)
    }



    /**
     * Apply ride overrides to a segment.
     *
     * Creates a new segment with overridden properties. Only non-null overrides
     * are applied, allowing fine-grained control over which properties to override.
     */
    private fun applyOverrides(
        segment: Segment,
        overrides: io.kite.core.SegmentOverrides,
    ): Segment {
        // Start with the original segment
        var result = segment

        // Apply dependsOn override (additive - adds to existing dependencies)
        overrides.dependsOn?.let { overrideDeps ->
            val combinedDeps = (segment.dependsOn + overrideDeps).distinct()
            result = result.copy(dependsOn = combinedDeps)
        }

        // Apply condition override (replaces original condition)
        overrides.condition?.let { overrideCondition ->
            result = result.copy(condition = overrideCondition)
        }

        // Apply timeout override (replaces original timeout)
        overrides.timeout?.let { overrideTimeout ->
            result = result.copy(timeout = overrideTimeout)
        }

        // Apply enabled flag - if false, wrap condition to always return false
        if (!overrides.enabled) {
            result = result.copy(condition = { false })
        }

        return result
    }
}
