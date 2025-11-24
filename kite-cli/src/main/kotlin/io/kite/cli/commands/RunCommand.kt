package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.kite.cli.Output
import io.kite.cli.globalOptions
import io.kite.core.PlatformDetector
import io.kite.core.Segment
import io.kite.dsl.FileDiscovery
import io.kite.runtime.scheduler.ParallelScheduler
import io.kite.runtime.scheduler.SequentialScheduler
import kotlinx.coroutines.runBlocking
import java.io.File

class RunCommand : CliktCommand(
    name = "run",
    help = "Execute specific segments by name",
) {
    private val segmentNames by argument(
        name = "segments",
        help = "Names of segments to execute",
    ).multiple(required = true)

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
                Output.header("🏃 Running Segments")
            }

            // Detect platform and create execution context
            if (opts.verbose) {
                Output.progress("Detecting platform...")
            }
            val platform = PlatformDetector.detect()
            val context = platform.createContext(emptyMap())

            if (opts.verbose) {
                Output.info("Branch: ${context.branch}, CI: ${context.isCI}")
            }

            // Discover and load segments
            if (opts.verbose) {
                Output.progress("Loading segments...")
            }

            val kiteDir = File(".kite")
            if (!kiteDir.exists()) {
                Output.error("No .kite directory found in current directory")
                Output.info("Create .kite/segments/ directory to get started")
                throw Exception("Missing .kite directory")
            }

            val discovery = FileDiscovery()
            val loadResult =
                runBlocking {
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
            }

            // Build segment map
            val segmentMap = loadResult.segmentMap()

            // Find requested segments
            val requestedSegments = mutableListOf<Segment>()
            val notFound = mutableListOf<String>()

            segmentNames.forEach { name ->
                val segment = segmentMap[name]
                if (segment != null) {
                    requestedSegments.add(segment)
                } else {
                    notFound.add(name)
                }
            }

            // Report not found segments
            if (notFound.isNotEmpty()) {
                Output.error("Segments not found: ${notFound.joinToString(", ")}")
                val available = segmentMap.keys.sorted()
                if (available.isNotEmpty()) {
                    Output.info("Available segments: ${available.joinToString(", ")}")
                }
                throw Exception("Segments not found")
            }

            // Collect dependencies transitively
            val segmentsToExecute = collectDependencies(requestedSegments, segmentMap)

            if (segmentsToExecute.isEmpty()) {
                Output.warning("No segments to execute")
                return
            }

            if (!opts.quiet) {
                Output.section("Execution Plan")
                Output.info("Segments to execute: ${segmentsToExecute.size}")
                segmentsToExecute.forEach { segment ->
                    val deps =
                        if (segment.dependsOn.isNotEmpty()) {
                            " (depends on: ${segment.dependsOn.joinToString(", ")})"
                        } else {
                            ""
                        }
                    val requested = if (segment.name in segmentNames) " [requested]" else ""
                    Output.progress("• ${segment.name}$deps$requested")
                }
            }

            // Dry run mode
            if (dryRun) {
                Output.info("Dry run mode - execution skipped")
                return
            }

            // Execute the segments
            if (!opts.quiet) {
                Output.section("Executing Segments")
            }

            val scheduler =
                if (sequential) {
                    if (opts.verbose) Output.info("Using sequential scheduler")
                    SequentialScheduler()
                } else {
                    val concurrency = Runtime.getRuntime().availableProcessors()
                    if (opts.verbose) Output.info("Using parallel scheduler (max concurrency: $concurrency)")
                    ParallelScheduler(maxConcurrency = concurrency)
                }

            val result =
                runBlocking {
                    scheduler.execute(segmentsToExecute, context)
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
                }
            }

            // Show summary
            val totalDuration = System.currentTimeMillis() - startTime
            if (!opts.quiet) {
                Output.summary(
                    total = result.totalSegments,
                    success = result.successCount,
                    failed = result.failureCount,
                    skipped = result.skippedCount,
                    duration = totalDuration,
                )
            }

            // Exit with error code if any failures
            if (result.failureCount > 0) {
                throw Exception("Execution failed with ${result.failureCount} failed segments")
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
     * Collect all segments including transitive dependencies.
     */
    private fun collectDependencies(
        requestedSegments: List<Segment>,
        segmentMap: Map<String, Segment>,
    ): List<Segment> {
        val collected = mutableSetOf<Segment>()
        val toProcess = ArrayDeque(requestedSegments)

        while (toProcess.isNotEmpty()) {
            val segment = toProcess.removeFirst()

            if (segment in collected) {
                continue
            }

            collected.add(segment)

            // Add dependencies to process
            segment.dependsOn.forEach { depName ->
                val dep = segmentMap[depName]
                if (dep != null && dep !in collected) {
                    toProcess.add(dep)
                } else if (dep == null) {
                    Output.warning("Segment '${segment.name}' depends on '$depName' which was not found")
                }
            }
        }

        return collected.toList()
    }
}
