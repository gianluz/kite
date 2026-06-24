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
        terminal.println(green("✓ $message"))
    }

    fun error(message: String) {
        terminal.println(red("✗ $message"))
    }

    fun warning(message: String) {
        terminal.println(yellow("⚠ $message"))
    }

    fun info(message: String) {
        terminal.println(cyan("ℹ $message"))
    }

    fun header(message: String) {
        terminal.println()
        terminal.println((bold + cyan)("═".repeat(60)))
        terminal.println((bold + cyan)("  $message"))
        terminal.println((bold + cyan)("═".repeat(60)))
        terminal.println()
    }

    fun section(message: String) {
        terminal.println()
        terminal.println((bold + white)("▶ $message"))
    }

    fun progress(message: String) {
        terminal.println((dim)("  ⋯ $message"))
    }

    fun result(
        segment: String,
        status: String,
        duration: Long,
    ) {
        val icon =
            when (status) {
                "SUCCESS" -> green("✓")
                "FAILURE" -> red("✗")
                "SKIPPED" -> yellow("○")
                "TIMEOUT" -> yellow("⏱")
                else -> white("•")
            }
        val durationStr = dim("(${formatDuration(duration)})")
        terminal.println("  $icon $segment $durationStr")
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
        if (success > 0) terminal.println(green("  ✓ Success: $success"))
        if (failed > 0) terminal.println(red("  ✗ Failed: $failed"))
        if (skipped > 0) terminal.println(yellow("  ○ Skipped: $skipped"))
        terminal.println((dim)("  Duration: ${formatDuration(duration)}"))

        // Show parallel efficiency if we have the data
        if (parallelDuration != null && sequentialDuration != null && sequentialDuration > parallelDuration) {
            val saved = sequentialDuration - parallelDuration
            val efficiency = ((sequentialDuration - parallelDuration) * 100.0 / sequentialDuration)
            terminal.println()
            terminal.println((bold + cyan)("⚡ Parallel Execution Stats:"))
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
            terminal.println((green + bold)("🎉 All segments completed successfully!"))
        } else {
            terminal.println((red + bold)("❌ Some segments failed"))
        }
    }

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
