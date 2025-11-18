ride {
    name = "MR"
    maxConcurrency = 4
    
    flow {
        // First: Clean and compile
        segment("clean")
        segment("compile")
        
        // Then: Run all tests in parallel (this is the real validation)
        parallel {
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
