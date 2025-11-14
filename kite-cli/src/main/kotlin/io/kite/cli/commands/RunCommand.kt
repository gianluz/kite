package io.kite.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import io.kite.cli.Output

class RunCommand : CliktCommand(
    name = "run",
    help = "Execute specific segments by name",
) {
    private val segmentNames by argument(
        name = "segments",
        help = "Names of segments to execute",
    ).multiple(required = true)

    override fun run() {
        Output.header("🏃 Running Segments")
        Output.info("Segments to run: ${segmentNames.joinToString(", ")}")
        Output.warning("Run command not yet implemented")
    }
}
