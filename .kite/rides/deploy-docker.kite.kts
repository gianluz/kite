ride {
    name = "Deploy Docker"
    maxConcurrency = 1

    flow {
        // Ensure build is ready
        segment("build")

        // Build and deploy Docker image
        segment("build-docker-image")
        segment("deploy-docker")
    }
}
