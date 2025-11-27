ride {
    name = "Deploy Maven"
    maxConcurrency = 2

    flow {
        // Ensure build is ready
        segment("build")

        // Deploy to Maven Central
        segment("deploy-maven-central")
    }
}
