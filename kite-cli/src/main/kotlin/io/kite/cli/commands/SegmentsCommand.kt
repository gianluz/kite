package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import io.kite.cli.Output
import io.kite.cli.globalOptions
import io.kite.cli.terminal
import io.kite.dsl.FileDiscovery
import kotlinx.coroutines.runBlocking
import java.io.File

class SegmentsCommand : CliktCommand(
    name = "segments",
    help = "List all available segments",
) {
    private val json by option(
        "--json",
        help = "Output in JSON format"
    ).flag()

    override fun run() {
        val opts = globalOptions

        try {
            // Check for .kite directory
            val kiteDir = File(".kite")
            if (!kiteDir.exists()) {
                if (!json) {
                    Output.error("No .kite directory found in current directory")
                    Output.info("Create .kite/segments/ directory to define segments")
                }
                throw Exception("Missing .kite directory")
            }

            // Discover and load segments
            val discovery = FileDiscovery()
            val loadResult = runBlocking {
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

            val segments = loadResult.segments.sortedBy { it.name }

            if (json) {
                // JSON output
                outputJson(segments)
            } else {
                // Pretty terminal output
                if (!opts.quiet) {
                    Output.header("📦 Available Segments")
                }

                if (segments.isEmpty()) {
                    Output.warning("No segments found in .kite/segments/")
                    Output.info("Create .kite.kts files in .kite/segments/ to define segments")
                    return
                }

                terminal.println((bold + cyan)("Found ${segments.size} segments:\n"))

                segments.forEach { segment ->
                    // Segment name
                    terminal.println((bold)("  ${segment.name}"))

                    // Description
                    if (segment.description != null) {
                        terminal.println((dim)("    ${segment.description}"))
                    }

                    // Dependencies
                    if (segment.dependsOn.isNotEmpty()) {
                        terminal.println((dim)("    Depends on: ${segment.dependsOn.joinToString(", ")}"))
                    }

                    // Timeout
                    if (segment.timeout != null) {
                        terminal.println((dim)("    Timeout: ${segment.timeout}"))
                    }

                    // Retries
                    if (segment.maxRetries > 0) {
                        terminal.println((dim)("    Max retries: ${segment.maxRetries}"))
                    }

                    // Conditional
                    if (segment.condition != null) {
                        terminal.println(yellow("    ⚠ Conditional execution"))
                    }

                    terminal.println()
                }

                terminal.println((dim)("Total: ${segments.size} segments"))
            }

        } catch (e: Exception) {
            if (opts.debug) {
                Output.error("Exception: ${e.message}")
                e.printStackTrace()
            }
            throw e
        }
    }

    private fun outputJson(segments: List<io.kite.core.Segment>) {
        val json = buildString {
            appendLine("{")
            appendLine("  \"segments\": [")
            segments.forEachIndexed { index, segment ->
                appendLine("    {")
                appendLine("      \"name\": \"${segment.name}\",")
                if (segment.description != null) {
                    appendLine("      \"description\": \"${segment.description}\",")
                }
                appendLine("      \"dependsOn\": [${segment.dependsOn.joinToString(", ") { "\"$it\"" }}],")
                appendLine("      \"timeout\": ${segment.timeout?.toString() ?: "null"},")
                appendLine("      \"maxRetries\": ${segment.maxRetries},")
                appendLine("      \"hasCondition\": ${segment.condition != null}")
                append("    }")
                if (index < segments.size - 1) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }
            appendLine("  ],")
            appendLine("  \"total\": ${segments.size}")
            appendLine("}")
        }
        println(json)
    }
}
