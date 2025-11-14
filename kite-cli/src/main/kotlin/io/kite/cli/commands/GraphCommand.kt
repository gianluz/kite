package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.kite.cli.Output

class GraphCommand : CliktCommand(
    name = "graph",
    help = "Visualize dependency graph for a ride",
) {
    private val rideName by argument(
        name = "name",
        help = "Name of the ride to visualize",
    )

    override fun run() {
        Output.header("📊 Dependency Graph: $rideName")
        Output.warning("Graph visualization not yet implemented")
    }
}
