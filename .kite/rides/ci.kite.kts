ride {
    name = "CI"
    maxConcurrency = 4

    flow {
        // Check version sync before building
        segment("check-version-sync")

        // Build Kite first
        segment("clean")
        segment("compile")

        // Run quality checks and tests in parallel
        parallel {
            // Code quality checks
            segment("ktlint")
            segment("detekt")

            // Tests
            segment("test-core")
            segment("test-dsl")
            segment("test-runtime")
            segment("test-cli")
            segment("test-integration")
        }

        // Publish combined test results
        segment("publish-test-results")

        // Final build step
        segment("build")
    }
}
