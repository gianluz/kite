ride {
    name = "CI"
    maxConcurrency = 4

    flow {
        // Build Kite first
        segment("clean")
        segment("compile")

        // Publish plugins to Maven Local so they can be used by other segments
        segment("publish-plugins-local")

        // Run quality checks and tests in parallel
        parallel {
            // Code quality checks
            segment("ktlint")
            segment("detekt")

            // Core module tests
            segment("test-core")
            segment("test-dsl")
            segment("test-runtime")
            segment("test-cli")
            segment("test-integration")

            // Plugin tests
            segment("test-plugins-git")
            segment("test-plugins-gradle")
        }

        // Publish combined test results
        segment("publish-test-results")

        // Final build step
        segment("build")
    }
}
