package io.kite.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.terminal.Terminal
import io.kite.cli.commands.GraphCommand
import io.kite.cli.commands.RideCommand
import io.kite.cli.commands.RidesCommand
import io.kite.cli.commands.RunCommand
import io.kite.cli.commands.SegmentsCommand

/**
 * Main Kite CLI application.
 */
class KiteCli : CliktCommand(
    name = "kite",
    help =
        """
        🪁 Kite - A modern CI/CD workflow runner
        
        Kite helps you define and execute CI/CD workflows using type-safe Kotlin DSL.
        Define segments (units of work) and rides (workflows) in .kite.kts files.
        """.trimIndent(),
    printHelpOnEmptyArgs = true,
) {
    private val debug by option("--debug", "-d", help = "Enable debug output").flag()
    private val verbose by option("--verbose", "-v", help = "Enable verbose output").flag()
    private val quiet by option("--quiet", "-q", help = "Suppress non-essential output").flag()

    init {
        versionOption("0.1.0-SNAPSHOT", names = setOf("--version", "-V"))

        // Use Mordant for beautiful help formatting
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
        }

        // Register subcommands
        subcommands(
            RideCommand(),
            RunCommand(),
            SegmentsCommand(),
            RidesCommand(),
            GraphCommand(),
        )
    }

    override fun run() {
        // Store global options in context for subcommands
        currentContext.obj =
            GlobalOptions(
                debug = debug,
                verbose = verbose,
                quiet = quiet,
            )
    }
}

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
val terminal = Terminal()

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
        terminal.println(dim("  ⋯ $message"))
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
        val durationStr = dim("(${duration}ms)")
        terminal.println("  $icon $segment $durationStr")
    }

    fun summary(
        total: Int,
        success: Int,
        failed: Int,
        skipped: Int,
        duration: Long,
    ) {
        terminal.println()
        terminal.println((bold + white)("Summary:"))
        terminal.println("  Total: $total segments")
        if (success > 0) terminal.println(green("  ✓ Success: $success"))
        if (failed > 0) terminal.println(red("  ✗ Failed: $failed"))
        if (skipped > 0) terminal.println(yellow("  ○ Skipped: $skipped"))
        terminal.println(dim("  Duration: ${duration}ms"))
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
