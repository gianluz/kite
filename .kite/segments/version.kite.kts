import java.io.File

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Reads the current version from the root build.gradle.kts.
 * This is the SINGLE SOURCE OF TRUTH for the Kite version.
 */
fun readGradleVersion(workspace: java.nio.file.Path): String {
    val buildGradle = workspace.resolve("build.gradle.kts").toFile().readText()
    val versionRegex = Regex("""version\s*=\s*"([^"]+)"""")
    return versionRegex.find(buildGradle)?.groupValues?.get(1)
        ?: throw IllegalStateException("Could not find `version = \"...\"` in build.gradle.kts")
}

/**
 * Applies all Kite-specific version substitutions to a block of text.
 * Only replaces strings in Kite-specific contexts so Kotlin/Gradle/Java versions
 * are never accidentally touched.
 *
 * @param text    The file content to process.
 * @param version The new Kite version string (e.g. "0.1.0-alpha9").
 */
fun applyVersionReplacements(text: String, version: String): String {
    var result = text

    // 1. Maven Central coordinates: com.gianluz.kite:ARTIFACT:OLD_VERSION
    result = result.replace(
        Regex("""(com\.gianluz\.kite:[a-zA-Z0-9_-]+:)\d+\.\d+\.\d+[a-zA-Z0-9.-]*""")
    ) { m -> m.groupValues[1] + version }

    // 2. GHCR image tags (not :latest): ghcr.io/gianluz/kite:VERSION
    result = result.replace(
        Regex("""(ghcr\.io/gianluz/kite:)(?!latest)(\d+\.\d+\.\d+[a-zA-Z0-9.-]*)""")
    ) { m -> m.groupValues[1] + version }

    // 3. Docker Hub image tags (not :latest): gianluz/kite:VERSION
    result = result.replace(
        Regex("""(gianluz/kite:)(?!latest)(\d+\.\d+\.\d+[a-zA-Z0-9.-]*)""")
    ) { m -> m.groupValues[1] + version }

    // 4. Shields.io version badge — hyphens are doubled in the URL segment
    //    e.g. version-0.1.0--alpha9-blue
    val badgeVersion = version.replace("-", "--")
    result = result.replace(
        Regex("""(img\.shields\.io/badge/version-)[\d.]+(?:--[a-zA-Z0-9]+)*(-blue)""")
    ) { m -> m.groupValues[1] + badgeVersion + m.groupValues[2] }

    // 5. Distribution archive names: kite-cli-VERSION.tar / kite-cli-VERSION.zip
    result = result.replace(
        Regex("""(kite-cli-)\d+\.\d+\.\d+[a-zA-Z0-9.-]*(\.(?:tar|zip))""")
    ) { m -> m.groupValues[1] + version + m.groupValues[2] }

    // 6. Plugin JAR paths: git-VERSION.jar, gradle-VERSION.jar
    result = result.replace(
        Regex("""((?:git|gradle)-)\d+\.\d+\.\d+[a-zA-Z0-9.-]*(\.jar)""")
    ) { m -> m.groupValues[1] + version + m.groupValues[2] }

    // 7. install.sh KITE_VERSION example: KITE_VERSION=vVERSION
    result = result.replace(
        Regex("""(KITE_VERSION=v)\d+\.\d+\.\d+[a-zA-Z0-9.-]*""")
    ) { m -> m.groupValues[1] + version }

    // 8. Status line in plugin docs: **Status:** ✅ Complete (v0.1.0-alpha9)
    result = result.replace(
        Regex("""(\*\*Status:\*\*[^\n]*\(v)\d+\.\d+\.\d+[a-zA-Z0-9.-]*(\))""")
    ) { m -> m.groupValues[1] + version + m.groupValues[2] }

    // 9. Latest release note in docs: **Latest release:** `v0.1.0-alpha9`
    result = result.replace(
        Regex("""(\*\*Latest release:\*\*\s*`v)\d+\.\d+\.\d+[a-zA-Z0-9.-]*(`\s*—)""")
    ) { m -> m.groupValues[1] + version + m.groupValues[2] }

    return result
}

/**
 * Files/directories that contain Kite version strings and should be
 * automatically updated. CHANGELOG.md is intentionally excluded because
 * it contains historical version entries.
 */
val VERSION_FILES = listOf(
    "README.md",
    "install.sh",
    "docs/00-index.md",
    "docs/01-getting-started.md",
    "docs/02-installation.md",
    "docs/10-external-dependencies.md",
    "docs/11-ci-integration.md",
    "docs/12-cli-reference.md",
    "docs/plugins/00-index.md",
    "docs/plugins/01-plugin-git.md",
    "docs/plugins/02-plugin-gradle.md",
    "docs/dev/05-plugin-development.md",
    "kite-plugins/git/README.md",
    "kite-plugins/gradle/README.md",
)

// ── Segments ──────────────────────────────────────────────────────────────────

segments {

    /**
     * Update ALL Kite version references across the repository to match
     * the version declared in build.gradle.kts.
     *
     * Run this locally after bumping the version, before committing:
     *   kite-cli run update-version-refs
     *   git add -A
     *   git commit -m "chore: Bump to vX.Y.Z"
     */
    segment("update-version-refs") {
        description = "Update ALL version references in docs/READMEs to match build.gradle.kts"

        execute {
            val version = readGradleVersion(workspace)
            logger.info("📌 Kite version (source of truth): $version")

            var updatedCount = 0
            var skippedCount = 0

            for (relativePath in VERSION_FILES) {
                val file = workspace.resolve(relativePath).toFile()
                if (!file.exists()) {
                    logger.warn("⚠️  Skipping (not found): $relativePath")
                    skippedCount++
                    continue
                }

                val original = file.readText()
                val updated = applyVersionReplacements(original, version)

                if (original != updated) {
                    file.writeText(updated)
                    logger.info("✅ Updated: $relativePath")
                    updatedCount++
                } else {
                    logger.info("ℹ️  Already up to date: $relativePath")
                }
            }

            logger.info("")
            logger.info("─────────────────────────────────────────────")
            logger.info("Updated $updatedCount file(s), $skippedCount not found.")
            if (updatedCount > 0) {
                logger.info("")
                logger.info("📝 Stage the changes before committing:")
                logger.info("   git add -A")
                logger.info("   git commit -m \"chore: Bump version references to $version\"")
            }
        }
    }

    /**
     * Verify that every tracked file's version references match build.gradle.kts.
     * Used by the pre-commit hook to block commits with stale version strings.
     */
    segment("check-version-sync") {
        description = "Verify all version references match build.gradle.kts (used by pre-commit hook)"

        execute {
            val version = readGradleVersion(workspace)
            logger.info("🔍 Checking all version references against: $version")
            logger.info("")

            val outOfSync = mutableListOf<String>()

            for (relativePath in VERSION_FILES) {
                val file = workspace.resolve(relativePath).toFile()
                if (!file.exists()) continue

                val original = file.readText()
                val updated = applyVersionReplacements(original, version)

                if (original != updated) {
                    outOfSync.add(relativePath)
                    logger.error("❌ Out of sync: $relativePath")
                } else {
                    logger.info("✅ OK: $relativePath")
                }
            }

            if (outOfSync.isNotEmpty()) {
                logger.error("")
                logger.error("${outOfSync.size} file(s) have stale version references.")
                logger.error("")
                logger.error("💡 Fix with:")
                logger.error("   kite-cli run update-version-refs")
                logger.error("   git add -A")
                throw IllegalStateException("Version references out of sync in: ${outOfSync.joinToString(", ")}")
            }

            logger.info("")
            logger.info("✅ All ${VERSION_FILES.size} files are in sync with version $version")
        }
    }

    // Kept for backward compatibility with any existing callers.
    segment("update-version-badge") {
        description = "Deprecated — use 'update-version-refs' instead (updates all files, not just badge)"

        execute {
            logger.warn("⚠️  'update-version-badge' is deprecated. Running 'update-version-refs' instead.")
            val version = readGradleVersion(workspace)
            val readmeFile = workspace.resolve("README.md").toFile()
            val original = readmeFile.readText()
            val updated = applyVersionReplacements(original, version)
            if (original != updated) {
                readmeFile.writeText(updated)
                logger.info("✅ README.md badge updated to $version")
            } else {
                logger.info("ℹ️  README.md already up to date")
            }
        }
    }
}
