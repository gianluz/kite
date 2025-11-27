ride {
    name = "Deploy"
    maxConcurrency = 3

    flow {
        // First: Ensure everything is built and tested
        segment("clean")
        segment("compile")

        parallel {
            segment("test-core")
            segment("test-dsl")
            segment("test-runtime")
            segment("test-cli")
            segment("test-integration")
        }

        segment("build")

        // Then: Deploy in parallel to all channels
        parallel {
            segment("deploy-maven-central")
            segment("deploy-github-packages")
            segment("build-docker-image")
        }

        // Docker deploy depends on build-docker-image
        segment("deploy-docker")

        // Finally: Create GitHub release with binaries
        segment("create-github-release")

        // Publish summary to GitHub Actions
        segment("publish-release-summary")
    }
}
