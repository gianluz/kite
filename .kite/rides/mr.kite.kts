ride {
    name = "MR Validation"
    maxConcurrency = 4

    flow {
        // First: Clean and compile
        segment("clean")
        segment("compile")

        // Then: Run tests and quality checks in parallel
        parallel {
            segment("test-core")
            segment("test-dsl")
            segment("test-runtime")
            segment("test-cli")
            segment("ktlint")
            segment("detekt")
        }

        // Finally: Full build
        segment("build")
    }
}
