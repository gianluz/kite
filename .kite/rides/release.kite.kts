ride {
    name = "Release"
    maxConcurrency = 2

    flow {
        // Ensure build is ready
        segment("build")

        // Create GitHub release with binaries
        segment("create-github-release")
    }
}
