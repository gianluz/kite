package io.kite.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.terminal.Terminal

// ... existing KiteCli class code ...

/**
 * Global options shared across all commands.
 */
data class GlobalOptions(
    val debug: Boolean = false,
    val verbose: Boolean = false,
    val quiet: Boolean = false,
)

/**
 * True when output should avoid Unicode glyphs that some CI log renderers replace with '?'.
 */
val useAsciiOutput: Boolean =
    System.getenv("KITE_ASCII_OUTPUT")?.equals("true", ignoreCase = true) == true ||
        System.getenv("CI") != null ||
        System.getenv("GITLAB_CI") != null

object OutputSymbols {
    val success = if (useAsciiOutput) "[OK]" else "✓"
    val failure = if (useAsciiOutput) "[FAIL]" else "✗"
    val warning = if (useAsciiOutput) "[WARN]" else "⚠"
    val info = if (useAsciiOutput) "i" else "ℹ"
    val section = if (useAsciiOutput) ">" else "▶"
    val progress = if (useAsciiOutput) "..." else "⋯"
    val bullet = if (useAsciiOutput) "-" else "•"
    val skipped = if (useAsciiOutput) "[SKIP]" else "○"
    val timeout = if (useAsciiOutput) "[TIMEOUT]" else "⏱"
    val separator = if (useAsciiOutput) "=" else "═"
    val successSummary = if (useAsciiOutput) "All segments completed successfully!" else "🎉 All segments completed successfully!"
    val failureSummary = if (useAsciiOutput) "Some segments failed" else "❌ Some segments failed"
    val parallelStats = if (useAsciiOutput) "Parallel Execution Stats:" else "⚡ Parallel Execution Stats:"
}

fun String.toCiSafeText(): String =
    if (!useAsciiOutput) {
        this
    } else {
        replace("🪁", "")
            .replace("🏃", "")
            .replace("✓", "[OK]")
            .replace("✗", "[FAIL]")
            .replace("⚠", "[WARN]")
            .replace("ℹ", "i")
            .replace("▶", ">")
            .replace("⋯", "...")
            .replace("•", "-")
            .replace("○", "[SKIP]")
            .replace("⏱", "[TIMEOUT]")
            .replace("⚡", "")
            .replace("🎉", "")
            .replace("❌", "")
            .trim()
    }

/**
 * Extension to easily access global options from subcommands.
 */
val CliktCommand.globalOptions: GlobalOptions
    get() = currentContext.findObject() ?: GlobalOptions()

/**
 * Terminal instance for colorful output.
 */
val terminal =
    Terminal(
        ansiLevel = if (System.getenv("CI") != null) AnsiLevel.ANSI16 else null,
    )

/**
 * Format duration in human-readable format.
 * - < 1s: show in ms
 * - < 1m: show in seconds with 1 decimal
 * - < 1h: show in m:ss
 * - >= 1h: show in h:mm:ss
 */
fun formatDuration(durationMs: Long): String {
    return when {
        durationMs < 1000 -> "${durationMs}ms"
        durationMs < 60_000 -> String.format("%.1fs", durationMs / 1000.0)
        durationMs < 3_600_000 -> {
            val minutes = durationMs / 60_000
            val seconds = (durationMs % 60_000) / 1000
            "${minutes}m ${seconds}s"
        }

        else -> {
            val hours = durationMs / 3_600_000
            val minutes = (durationMs % 3_600_000) / 60_000
            val seconds = (durationMs % 60_000) / 1000
            "${hours}h ${minutes}m ${seconds}s"
        }
    }
}

/**
 * Print functions with emoji and colors.
 */
object Output {
    fun success(message: String) {
        terminal.println(green("${OutputSymbols.success} ${message.toCiSafeText()}"))
    }

    fun error(message: String) {
        terminal.println(red("${OutputSymbols.failure} ${message.toCiSafeText()}"))
    }

    fun warning(message: String) {
        terminal.println(yellow("${OutputSymbols.warning} ${message.toCiSafeText()}"))
    }

    fun info(message: String) {
        terminal.println(cyan("${OutputSymbols.info} ${message.toCiSafeText()}"))
    }

    fun header(message: String) {
        terminal.println()
        terminal.println((bold + cyan)(OutputSymbols.separator.repeat(60)))
        terminal.println((bold + cyan)("  ${message.toCiSafeText()}"))
        terminal.println((bold + cyan)(OutputSymbols.separator.repeat(60)))
        terminal.println()
    }

    fun section(message: String) {
        terminal.println()
        terminal.println((bold + white)("${OutputSymbols.section} ${message.toCiSafeText()}"))
    }

    fun progress(message: String) {
        terminal.println((dim)("  ${OutputSymbols.progress} ${message.toCiSafeText()}"))
    }

    fun result(
        segment: String,
        status: String,
        duration: Long,
    ) {
        val icon =
            when (status) {
                "SUCCESS" -> green(OutputSymbols.success)
                "FAILURE" -> red(OutputSymbols.failure)
                "SKIPPED" -> yellow(OutputSymbols.skipped)
                "TIMEOUT" -> yellow(OutputSymbols.timeout)
                else -> white(OutputSymbols.bullet)
            }
        val durationStr = dim("(${formatDuration(duration)})")
        terminal.println("  $icon ${segment.toCiSafeText()} $durationStr")
    }

    fun summary(
        total: Int,
        success: Int,
        failed: Int,
        skipped: Int,
        duration: Long,
        parallelDuration: Long? = null,
        sequentialDuration: Long? = null,
    ) {
        terminal.println()
        terminal.println((bold + white)("Summary:"))
        terminal.println("  Total: $total segments")
        if (success > 0) terminal.println(green("  ${OutputSymbols.success} Success: $success"))
        if (failed > 0) terminal.println(red("  ${OutputSymbols.failure} Failed: $failed"))
        if (skipped > 0) terminal.println(yellow("  ${OutputSymbols.skipped} Skipped: $skipped"))
        terminal.println((dim)("  Duration: ${formatDuration(duration)}"))

        // Show parallel efficiency if we have the data
        if (parallelDuration != null && sequentialDuration != null && sequentialDuration > parallelDuration) {
            val saved = sequentialDuration - parallelDuration
            val efficiency = ((sequentialDuration - parallelDuration) * 100.0 / sequentialDuration)
            terminal.println()
            terminal.println((bold + cyan)(OutputSymbols.parallelStats))
            terminal.println((dim)("  Sequential time: ${formatDuration(sequentialDuration)}"))
            terminal.println((dim)("  Parallel time: ${formatDuration(parallelDuration)}"))
            terminal.println(
                (green)(
                    "  Time saved: ${formatDuration(saved)} (${
                        String.format(
                            "%.1f",
                            efficiency,
                        )
                    }% faster)",
                ),
            )
        }

        terminal.println()

        if (failed == 0) {
            terminal.println((green + bold)(OutputSymbols.successSummary))
        } else {
            terminal.println((red + bold)(OutputSymbols.failureSummary))
        }
    }

    fun bullet(message: String): String = "${OutputSymbols.bullet} ${message.toCiSafeText()}"

    fun logo() {
        terminal.println(
            cyan(
                """
                
                ██╗  ██╗██╗████████╗███████╗
                ██║ ██╔╝██║╚══██╔══╝██╔════╝
                █████╔╝ ██║   ██║   █████╗  
                ██╔═██╗ ██║   ██║   ██╔══╝  
                ██║  ██╗██║   ██║   ███████╗
                ╚═╝  ╚═╝╚═╝   ╚═╝   ╚══════╝
                
                Modern CI/CD Workflow Runner
                
                """.trimIndent(),
            ),
        )
    }
}
