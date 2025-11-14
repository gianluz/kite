package io.kite.cli

/**
 * Main entry point for the Kite CLI.
 * This is a placeholder that will be fully implemented in Epic 3.1.
 */
fun main(args: Array<String>) {
    // Show logo if not in quiet mode and no arguments
    if (args.isEmpty() || (args.isNotEmpty() && !args.contains("--quiet") && !args.contains("-q"))) {
        Output.logo()
    }

    // Run CLI
    KiteCli().main(args)
}
