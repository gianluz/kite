segments {
    segment("deploy-maven-central") {
        description = "Publish artifacts to Maven Central"
        dependsOn("build")

        condition { ctx ->
            // Only deploy when running on a release tag in CI
            val hasReleaseTag = ctx.env("CI_COMMIT_TAG")?.startsWith("v") == true
            val isCI = ctx.isCI

            // Print debug info to console (will be captured in deployment.log)
            println("🔍 Maven Central deployment condition check:")
            println("   CI_COMMIT_TAG: ${ctx.env("CI_COMMIT_TAG")}")
            println("   hasReleaseTag: $hasReleaseTag")
            println("   isCI: $isCI")
            println("   Result: ${hasReleaseTag && isCI}")

            hasReleaseTag && isCI
        }

        execute {
            logger.info("Publishing to Maven Central...")

            // Verify required secrets are present
            requireSecret("OSSRH_USERNAME")
            requireSecret("OSSRH_PASSWORD")
            requireSecret("SIGNING_KEY")
            requireSecret("SIGNING_PASSWORD")

            // Publish to Maven Central via the Central Portal API
            // --no-build-cache ensures all 5 modules are freshly staged (avoids stale cache skipping kite-core)
            exec(
                "./gradlew",
                "publishAggregationToCentralPortal",
                "--no-daemon",
                "--no-configuration-cache",
                "--no-build-cache"
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
            val hasReleaseTag = ctx.env("CI_COMMIT_TAG")?.startsWith("v") == true
            val isCI = ctx.isCI
            hasReleaseTag && isCI
        }

        execute {
            logger.info("Publishing to GitHub Packages...")

            requireSecret("GITHUB_TOKEN")

            exec(
                "./gradlew",
                "publish",
                "-Pgithub=true",
                "--no-daemon",
                "--no-configuration-cache"
            )

            logger.info("✅ Published to GitHub Packages!")
        }
    }

    segment("build-docker-image") {
        description = "Build Docker image for Kite CLI (tagged for both GHCR and Docker Hub)"
        dependsOn("build")

        condition { ctx ->
            // Build Docker image if we have Docker credentials OR if running in CI (for GHCR)
            val hasDockerCredentials = ctx.env("DOCKER_USERNAME") != null && ctx.env("DOCKER_PASSWORD") != null
            val isCI = ctx.isCI
            hasDockerCredentials || isCI
        }

        execute {
            logger.info("Building Docker image...")

            // Ensure kite-cli installDist output is present
            exec("./gradlew", ":kite-cli:installDist")

            val tag = env("CI_COMMIT_TAG")?.removePrefix("v") ?: env("VERSION")?.removePrefix("v") ?: "latest"

            // Build image tagged for Docker Hub
            exec(
                "docker", "build",
                "-t", "gianluz/kite:$tag",
                "-t", "gianluz/kite:latest",
                // Also tag for GHCR
                "-t", "ghcr.io/gianluz/kite:$tag",
                "-t", "ghcr.io/gianluz/kite:latest",
                "-f", "Dockerfile",
                "."
            )

            logger.info("✅ Docker image built:")
            logger.info("   gianluz/kite:$tag")
            logger.info("   ghcr.io/gianluz/kite:$tag")
        }
    }

    segment("deploy-docker") {
        description = "Log in and push Docker image to GHCR (always in CI) and Docker Hub (if credentials set)"
        dependsOn("build-docker-image")

        condition { ctx ->
            val hasReleaseTag = ctx.env("CI_COMMIT_TAG")?.startsWith("v") == true
            val isCI = ctx.isCI
            hasReleaseTag && isCI
        }

        execute {
            val tag = env("CI_COMMIT_TAG")?.removePrefix("v") ?: "latest"

            // ── GHCR (GitHub Container Registry) ───────────────────────────
            // GITHUB_TOKEN is always available in CI; GITHUB_ACTOR is set automatically
            // by GitHub Actions and inherited via System.getenv()
            val githubToken = requireSecret("GITHUB_TOKEN")
            val githubActor = env("GITHUB_ACTOR") ?: "gianluz"

            logger.info("Logging in to ghcr.io as $githubActor...")
            exec("docker", "login", "--username", githubActor, "--password", githubToken, "ghcr.io")

            exec("docker", "push", "ghcr.io/gianluz/kite:$tag")
            exec("docker", "push", "ghcr.io/gianluz/kite:latest")
            logger.info("✅ Pushed to GHCR:")
            logger.info("   docker pull ghcr.io/gianluz/kite:$tag")

            // ── Docker Hub (optional) ───────────────────────────────────────
            // Runs only when DOCKER_USERNAME and DOCKER_PASSWORD secrets are set
            val dockerUsername = env("DOCKER_USERNAME")
            val dockerPassword = env("DOCKER_PASSWORD")

            if (dockerUsername != null && dockerPassword != null) {
                logger.info("Logging in to Docker Hub as $dockerUsername...")
                exec("docker", "login", "--username", dockerUsername, "--password", dockerPassword)

                exec("docker", "push", "gianluz/kite:$tag")
                exec("docker", "push", "gianluz/kite:latest")
                logger.info("✅ Pushed to Docker Hub:")
                logger.info("   docker pull gianluz/kite:$tag")
            } else {
                logger.info("⚠️  DOCKER_USERNAME/DOCKER_PASSWORD not set — skipping Docker Hub push")
            }
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
                # GitHub Container Registry (no login needed)
                docker run --rm -v $(pwd):/workspace ghcr.io/gianluz/kite:${tag.removePrefix("v")} ride CI
                
                # Docker Hub
                docker run --rm -v $(pwd):/workspace gianluz/kite:${tag.removePrefix("v")} ride CI
                ```
            """.trimIndent()

            // Create release using GitHub CLI — attach binaries and the install script
            exec(
                "gh", "release", "create", tag,
                "kite-cli/build/distributions/kite-cli-${tag.removePrefix("v")}.tar",
                "kite-cli/build/distributions/kite-cli-${tag.removePrefix("v")}.zip",
                "install.sh",
                "--title", "Kite $tag",
                "--notes", releaseNotes
            )

            logger.info("✅ GitHub release created!")
            logger.info("  https://github.com/gianluz/kite/releases/tag/$tag")
        }
    }

    segment("publish-release-summary") {
        description = "Publish release summary to GitHub Actions"
        dependsOn("create-github-release", "deploy-maven-central")

        condition { ctx ->
            // Only in CI environment
            ctx.isCI
        }

        execute {
            val tag = env("CI_COMMIT_TAG") ?: env("VERSION") ?: "unknown"
            val version = tag.removePrefix("v")

            logger.info("Publishing release summary to GitHub Actions...")

            // Get GitHub step summary file (GitHub Actions specific)
            val summaryFile = env("GITHUB_STEP_SUMMARY")

            if (summaryFile != null) {
                val summary = """
                    # 🚀 Release Summary
                    
                    **Version:** $tag
                    
                    ## 📦 Distribution Channels
                    
                    - ✅ **Maven Central:** `com.gianluz.kite:kite-core:$version`
                    - ✅ **GitHub Packages:** Available in this repository
                    - ✅ **Docker Hub:** `gianluz/kite:$version`
                    - ✅ **GitHub Release:** [View Release](https://github.com/gianluz/kite/releases/tag/$tag)
                    
                    ## 📥 Installation
                    
                    ```bash
                    # Via Docker
                    docker pull gianluz/kite:$version
                    
                    # Via direct download
                    curl -LO https://github.com/gianluz/kite/releases/download/$tag/kite-cli-$version.tar
                    tar -xf kite-cli-$version.tar
                    export PATH="${'$'}PWD/kite-cli-$version/bin:${'$'}PATH"
                    ```
                    
                    ## 📚 Documentation
                    
                    - [GitHub Repository](https://github.com/gianluz/kite)
                    - [Getting Started](https://github.com/gianluz/kite/blob/main/docs/01-getting-started.md)
                    - [CLI Reference](https://github.com/gianluz/kite/blob/main/docs/12-cli-reference.md)
                    
                    ## 🎉 Thank You!
                    
                    Special thanks to [Luno](https://www.luno.com) for supporting this project!
                """.trimIndent()

                // Write to GitHub step summary
                shell("echo '$summary' >> $summaryFile")

                logger.info("✅ Release summary published!")
            } else {
                logger.info("⚠️  Not in GitHub Actions, skipping summary")
            }
        }
    }
}
