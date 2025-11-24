/**
 * Automatic Git Hooks Installation
 *
 * This script automatically installs git hooks from scripts/git-hooks/
 * before any compilation or clean task runs.
 *
 * Usage: Apply this script in your root build.gradle.kts:
 *   apply(from = "gradle/git-hooks.gradle.kts")
 */

val installGitHooks by tasks.registering {
    description = "Install git hooks automatically"
    group = "git"

    val rootDir = layout.projectDirectory.asFile
    val hooksDir = File(rootDir, ".git/hooks")
    val sourceDir = File(rootDir, "scripts/git-hooks")

    // Always run this task (don't cache)
    outputs.upToDateWhen { false }

    doLast {
        if (!hooksDir.exists()) {
            return@doLast // Not a git repository
        }

        if (!sourceDir.exists()) {
            return@doLast // No hooks to install
        }

        var installedCount = 0
        sourceDir.listFiles()?.filter { it.isFile && it.name != "README.md" }?.forEach { hookFile ->
            val targetFile = File(hooksDir, hookFile.name)

            // Remove existing hook if it exists
            if (targetFile.exists() || targetFile.toPath().toFile().exists()) {
                targetFile.delete()
            }

            // Create symlink
            try {
                val relativePath = "../../scripts/git-hooks/${hookFile.name}"
                java.nio.file.Files.createSymbolicLink(
                    targetFile.toPath(),
                    java.nio.file.Paths.get(relativePath)
                )

                // Make sure source is executable
                hookFile.setExecutable(true)

                installedCount++
            } catch (e: Exception) {
                logger.warn("Failed to install git hook ${hookFile.name}: ${e.message}")
            }
        }

        if (installedCount > 0) {
            logger.quiet("✅ Installed $installedCount git hook(s)")
        }
    }
}

// Run installGitHooks before clean or any compilation in subprojects
subprojects {
    tasks.matching { it.name == "clean" || it.name == "compileKotlin" }.configureEach {
        dependsOn(rootProject.tasks.named("installGitHooks"))
    }
}
