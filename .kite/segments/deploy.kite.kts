segments {
    segment("deploy-maven-central") {
        description = "Publish artifacts to Maven Central"
        dependsOn("build")

        condition { ctx ->
            // Only deploy on main branch with release tag
            val isMainBranch = ctx.branch == "main"
            val hasReleaseTag = ctx.env("CI_COMMIT_TAG")?.startsWith("v") == true
            isMainBranch && hasReleaseTag
        }

        execute {
            logger.info("Publishing to Maven Central...")

            // Verify required secrets are present
            requireSecret("OSSRH_USERNAME")
            requireSecret("OSSRH_PASSWORD")
            requireSecret("SIGNING_KEY")
            requireSecret("SIGNING_PASSWORD")

            // Publish to Sonatype OSSRH
            exec(
                "./gradlew",
                "publishToSonatype",
                "closeAndReleaseSonatypeStagingRepository",
                "--no-daemon"
            )

            logger.info("✅ Published to Maven Central!")
            logger.info("Artifacts will be available at:")
            logger.info("  https://repo1.maven.org/maven2/com/gianluz/kite/")
        }
    }

    segment("deploy-github-packages") {
        description = "Publish artifacts to GitHub Packages"
        dependsOn("build")

        condition { ctx ->
            val isMainBranch = ctx.branch == "main"
            val hasReleaseTag = ctx.env("CI_COMMIT_TAG")?.startsWith("v") == true
            isMainBranch && hasReleaseTag
        }

        execute {
            logger.info("Publishing to GitHub Packages...")

            requireSecret("GITHUB_TOKEN")

            exec(
                "./gradlew",
                "publish",
                "-Pgithub=true",
                "--no-daemon"
            )

            logger.info("✅ Published to GitHub Packages!")
        }
    }

    segment("build-docker-image") {
        description = "Build Docker image for Kite CLI"
        dependsOn("build")

        execute {
            logger.info("Building Docker image...")

            // Ensure kite-cli is built
            exec("./gradlew", ":kite-cli:installDist")

            // Build Docker image
            val version = env("VERSION") ?: "latest"
            exec(
                "docker", "build",
                "-t", "gianluz/kite:$version",
                "-f", "Dockerfile",
                "."
            )

            logger.info("✅ Docker image built: gianluz/kite:$version")
        }
    }

    segment("deploy-docker") {
        description = "Push Docker image to Docker Hub"
        dependsOn("build-docker-image")

        condition { ctx ->
            val isMainBranch = ctx.branch == "main"
            val hasReleaseTag = ctx.env("CI_COMMIT_TAG")?.startsWith("v") == true
            isMainBranch && hasReleaseTag
        }

        execute {
            logger.info("Pushing Docker image to Docker Hub...")

            requireSecret("DOCKER_USERNAME")
            val dockerPassword = requireSecret("DOCKER_PASSWORD")
            val dockerUsername = requireSecret("DOCKER_USERNAME")

            // Login to Docker Hub (password via stdin)
            shell("echo '$dockerPassword' | docker login -u $dockerUsername --password-stdin")

            // Get version from tag (v1.0.0 -> 1.0.0)
            val tag = env("CI_COMMIT_TAG")?.removePrefix("v") ?: "latest"

            // Tag with version
            exec("docker", "tag", "gianluz/kite:latest", "gianluz/kite:$tag")

            // Push both tags
            exec("docker", "push", "gianluz/kite:$tag")
            exec("docker", "push", "gianluz/kite:latest")

            logger.info("✅ Docker image pushed!")
            logger.info("  docker pull gianluz/kite:$tag")
            logger.info("  docker pull gianluz/kite:latest")
        }
    }

    segment("create-github-release") {
        description = "Create GitHub release with binaries"
        dependsOn("build")

        condition { ctx ->
            val hasReleaseTag = ctx.env("CI_COMMIT_TAG")?.startsWith("v") == true
            hasReleaseTag
        }

        execute {
            logger.info("Creating GitHub release...")

            requireSecret("GITHUB_TOKEN")

            // Build distribution archives
            exec("./gradlew", ":kite-cli:distTar", ":kite-cli:distZip")

            val tag = requireEnv("CI_COMMIT_TAG")
            val releaseNotes = """
                # Kite $tag
                
                ## Installation
                
                ### Linux/macOS
                ```bash
                curl -LO https://github.com/gianluz/kite/releases/download/$tag/kite-cli-${tag.removePrefix("v")}.tar
                tar -xf kite-cli-${tag.removePrefix("v")}.tar
                export PATH="${'$'}PWD/kite-cli-${tag.removePrefix("v")}/bin:${'$'}PATH"
                kite-cli --version
                ```
                
                ### Windows
                Download `kite-cli-${tag.removePrefix("v")}.zip`, extract, and add `bin/` to PATH.
                
                ### Maven Central
                ```kotlin
                dependencies {
                    implementation("com.gianluz.kite:kite-core:${tag.removePrefix("v")}")
                    implementation("com.gianluz.kite:kite-dsl:${tag.removePrefix("v")}")
                    implementation("com.gianluz.kite:kite-runtime:${tag.removePrefix("v")}")
                }
                ```
                
                ### Docker
                ```bash
                docker pull gianluz/kite:${tag.removePrefix("v")}
                ```
            """.trimIndent()

            // Create release using GitHub CLI
            exec(
                "gh", "release", "create", tag,
                "kite-cli/build/distributions/kite-cli-${tag.removePrefix("v")}.tar",
                "kite-cli/build/distributions/kite-cli-${tag.removePrefix("v")}.zip",
                "--title", "Kite $tag",
                "--notes", releaseNotes
            )

            logger.info("✅ GitHub release created!")
            logger.info("  https://github.com/gianluz/kite/releases/tag/$tag")
        }
    }
}
