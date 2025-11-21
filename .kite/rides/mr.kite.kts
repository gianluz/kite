ride {
    name = "MR"
    maxConcurrency = 4
    
    flow {
        // First: Clean and compile
        segment("clean")
        segment("compile")
        
        // Then: Run quality checks and tests in parallel
        parallel {
            // Code quality checks (will fail PR if violations found)
            segment("ktlint")
            segment("detekt")

            // Tests
            segment("test-core")
            segment("test-dsl")
            segment("test-runtime")
            segment("test-cli")
            segment("test-integration")
        }

        // Finally: Full build to ensure everything compiles
        segment("build")
    }
}
