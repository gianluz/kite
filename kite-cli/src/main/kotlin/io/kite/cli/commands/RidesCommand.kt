package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import io.kite.cli.Output
import io.kite.cli.globalOptions
import io.kite.cli.terminal
import io.kite.dsl.FileDiscovery
import kotlinx.coroutines.runBlocking
import java.io.File

class RidesCommand : CliktCommand(
    name = "rides",
    help = "List all available rides",
) {
    private val json by option(
        "--json",
        help = "Output in JSON format",
    ).flag()

    override fun run() {
        val opts = globalOptions

        try {
            // Check for .kite directory
            val kiteDir = File(".kite")
            if (!kiteDir.exists()) {
                if (!json) {
                    Output.error("No .kite directory found in current directory")
                    Output.info("Create .kite/rides/ directory to define rides")
                }
                throw Exception("Missing .kite directory")
            }

            // Discover and load rides
            val discovery = FileDiscovery()
            val loadResult =
                runBlocking {
                    discovery.loadAll()
                }

            if (!loadResult.success) {
                if (!json) {
                    Output.error("Failed to load .kite files:")
                    loadResult.errors.forEach { error ->
                        Output.error("  ${error.file}: ${error.message}")
                    }
                }
                throw Exception("Failed to load .kite files")
            }

            val rides = loadResult.rides.sortedBy { it.name }

            if (json) {
                // JSON output
                outputJson(rides)
            } else {
                // Pretty terminal output
                if (!opts.quiet) {
                    Output.header("🎢 Available Rides")
                }

                if (rides.isEmpty()) {
                    Output.warning("No rides found in .kite/rides/")
                    Output.info("Create .kite.kts files in .kite/rides/ to define rides")
                    return
                }

                terminal.println((bold + cyan)("Found ${rides.size} rides:\n"))

                rides.forEach { ride ->
                    // Ride name
                    terminal.println((bold)("  ${ride.name}"))

                    // Segment count
                    val segmentCount = countSegments(ride.flow)
                    terminal.println((dim)("    Segments: $segmentCount"))

                    // Max concurrency
                    if (ride.maxConcurrency != null) {
                        terminal.println((dim)("    Max concurrency: ${ride.maxConcurrency}"))
                    }

                    terminal.println()
                }

                terminal.println((dim)("Total: ${rides.size} rides"))
            }
        } catch (e: Exception) {
            if (opts.debug) {
                Output.error("Exception: ${e.message}")
                e.printStackTrace()
            }
            throw e
        }
    }

    private fun countSegments(flow: io.kite.core.FlowNode): Int {
        return when (flow) {
            is io.kite.core.FlowNode.Sequential -> {
                flow.nodes.sumOf { countSegments(it) }
            }

            is io.kite.core.FlowNode.Parallel -> {
                flow.nodes.sumOf { countSegments(it) }
            }

            is io.kite.core.FlowNode.SegmentRef -> 1
        }
    }

    private fun outputJson(rides: List<io.kite.core.Ride>) {
        val json =
            buildString {
                appendLine("{")
                appendLine("  \"rides\": [")
                rides.forEachIndexed { index, ride ->
                    appendLine("    {")
                    appendLine("      \"name\": \"${ride.name}\",")
                    appendLine("      \"segmentCount\": ${countSegments(ride.flow)},")
                    appendLine("      \"maxConcurrency\": ${ride.maxConcurrency ?: "null"}")
                    append("    }")
                    if (index < rides.size - 1) {
                        appendLine(",")
                    } else {
                        appendLine()
                    }
                }
                appendLine("  ],")
                appendLine("  \"total\": ${rides.size}")
                appendLine("}")
            }
        println(json)
    }
}
