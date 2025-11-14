package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import io.kite.cli.Output

class SegmentsCommand : CliktCommand(
    name = "segments",
    help = "List all available segments",
) {
    override fun run() {
        Output.header("📦 Available Segments")
        Output.warning("Segment listing not yet implemented")
    }
}
