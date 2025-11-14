package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import io.kite.cli.Output

class RidesCommand : CliktCommand(
    name = "rides",
    help = "List all available rides",
) {
    override fun run() {
        Output.header("🎢 Available Rides")
        Output.warning("Ride listing not yet implemented")
    }
}
