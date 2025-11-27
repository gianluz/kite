segments {
    segment("update-version-badge") {
        description = "Update version badge in README to match build.gradle.kts (run locally before commit)"

        execute {
            logger.info("Updating version badge in README...")

            // Read version from build.gradle.kts
            val buildGradle = workspace.resolve("build.gradle.kts").toFile().readText()
            val versionRegex = Regex("""version\s*=\s*"([^"]+)"""")
            val version = versionRegex.find(buildGradle)?.groupValues?.get(1)

            if (version == null) {
                logger.error("❌ Could not find version in build.gradle.kts")
                throw IllegalStateException("Version not found in build.gradle.kts")
            }

            logger.info("Found version: $version")

            // Read README
            val readmePath = workspace.resolve("README.md").toFile()
            val readme = readmePath.readText()

            // Update badge
            val badgeRegex = Regex("""!\[Version]\(https://img\.shields\.io/badge/version-[^-]+-blue\.svg\)""")
            val newBadge = "![Version](https://img.shields.io/badge/version-$version-blue.svg)"

            if (!badgeRegex.containsMatchIn(readme)) {
                logger.error("❌ Version badge not found in README.md")
                throw IllegalStateException("Version badge not found in README.md")
            }

            val updatedReadme = badgeRegex.replace(readme, newBadge)

            // Check if there are actual changes
            val hasChanges = readme != updatedReadme

            if (hasChanges) {
                // Write back
                readmePath.writeText(updatedReadme)
                logger.info("✅ Version badge updated to: $version")
                logger.info("📝 README.md has been updated")
                logger.info("💡 Don't forget to commit: git add README.md && git commit -m \"docs: Update version badge to $version\"")
            } else {
                logger.info("ℹ️  README.md already up to date (version: $version)")
            }
        }
    }

    segment("check-version-sync") {
        description = "Check if version badge matches build.gradle.kts version (used by pre-commit hook)"

        execute {
            logger.info("Checking version synchronization...")

            // Read version from build.gradle.kts
            val buildGradle = workspace.resolve("build.gradle.kts").toFile().readText()
            val versionRegex = Regex("""version\s*=\s*"([^"]+)"""")
            val gradleVersion = versionRegex.find(buildGradle)?.groupValues?.get(1)

            if (gradleVersion == null) {
                logger.error("❌ Could not find version in build.gradle.kts")
                throw IllegalStateException("Version not found in build.gradle.kts")
            }

            // Read version from README badge
            val readme = workspace.resolve("README.md").toFile().readText()
            val badgeRegex = Regex("""version-([^-]+)-blue""")
            val readmeVersion = badgeRegex.find(readme)?.groupValues?.get(1)

            if (readmeVersion == null) {
                logger.error("❌ Could not find version badge in README.md")
                throw IllegalStateException("Version badge not found in README.md")
            }

            // Compare
            if (gradleVersion == readmeVersion) {
                logger.info("✅ Versions are in sync: $gradleVersion")
            } else {
                logger.error("❌ Version mismatch!")
                logger.error("   build.gradle.kts: $gradleVersion")
                logger.error("   README.md badge:  $readmeVersion")
                logger.error("")
                logger.error("💡 Run: kite-cli run update-version-badge")
                throw IllegalStateException("Version mismatch: build.gradle.kts ($gradleVersion) != README.md ($readmeVersion)")
            }
        }
    }
}
