// Test segment showing both println and exec output

segments {
    segment("mixed-logging") {
        description = "Test segment with mixed output types"
        execute {
            println("=== Starting mixed logging test ===")
            println("This is a println statement - WILL be captured in log")

            println("Running external command...")
            exec("echo", "This is from exec - may NOT be in log file")

            println("Command completed")
            println("=== End of test ===")
        }
    }
}
