package io.kite.cli

/**
 * Main entry point for the Kite CLI.
 * This is a placeholder that will be fully implemented in Epic 3.1.
 */
private fun shouldShowLogo(args: Array<String>): Boolean =
    System.getenv("CI") == null &&
        !args.contains("--version") &&
        !args.contains("-V") &&
        (args.isEmpty() || (!args.contains("--quiet") && !args.contains("-q")))

fun main(args: Array<String>) {
    // Show logo if not in quiet mode and no arguments
    if (shouldShowLogo(args)) {
        Output.logo()
    }

    // Run CLI
    KiteCli().main(args)
}
