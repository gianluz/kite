package io.kite.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
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
        // Environment overrides for CI automation
        val envDebug = System.getenv("KITE_DEBUG")?.equals("true", ignoreCase = true) == true
        val envVerbose = System.getenv("KITE_VERBOSE")?.equals("true", ignoreCase = true) == true
        val envQuiet = System.getenv("KITE_QUIET")?.equals("true", ignoreCase = true) == true

        // Store global options in context for subcommands
        // CLI flags take precedence, then env vars
        currentContext.obj =
            GlobalOptions(
                debug = debug || envDebug,
                verbose = verbose || envVerbose,
                quiet = quiet || envQuiet,
            )
    }
}
