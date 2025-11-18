ride {
    name = "ARTIFACT-EXAMPLE"
    maxConcurrency = 2

    flow {
        // Example 1: Simple report generation and publishing (sequential)
        segment("create-report")
        segment("publish-report")

        // Example 2: Build, then test and deploy in parallel
        segment("build-app")
        parallel {
            segment("test-app")
            segment("deploy-app")
        }

        // Example 3: Directory artifacts
        segment("run-tests")
        segment("archive-results")
    }
}
