package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.kite.cli.Output
import io.kite.cli.globalOptions

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

    override fun run() {
        val opts = globalOptions

        if (opts.verbose) {
            Output.info("Executing ride: $rideName")
            if (dryRun) Output.info("Dry run mode enabled")
        }

        // TODO: Implement ride execution in next task
        Output.header("🪁 Kite Ride: $rideName")
        Output.warning("Ride execution not yet implemented")
        Output.info("This will be implemented in Task 3.1.2")
    }
}
