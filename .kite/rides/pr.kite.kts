ride {
    name = "PR"
    maxConcurrency = 4

    flow {
        // First: Clean and compile
        segment("clean")
        segment("compile")

        // Note: Skipping publish-plugins-local due to Gradle 9.2 bug with project dependencies

        // Then: Run quality checks and tests in parallel
        parallel {
            // Code quality checks (will fail PR if violations found)
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

        // Finally: Full build to ensure everything compiles
        segment("build")
    }
}
