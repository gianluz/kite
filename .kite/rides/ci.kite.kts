ride {
    name = "CI"
    maxConcurrency = 4

    flow {
        // Build Kite first
        segment("clean")
        segment("compile")

        // Run all tests in parallel
        parallel {
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
