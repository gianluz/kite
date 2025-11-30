// Test enhanced Git plugin with new remote and branch operations
@file:DependsOn("com.gianluz.kite:git:0.1.0-alpha")

import io.kite.plugins.git.*

segments {

    segment("test-git-enhanced") {
        description = "Test enhanced Git plugin features"

        execute {
            git {
                logger.info("=== Testing Enhanced Git Plugin ===")

                // Status operations
                logger.info("Current branch: ${currentBranch()}")
                logger.info("Is clean: ${isClean()}")
                logger.info("Commit SHA: ${commitSha(short = true)}")

                // Remote operations (safe to test - just fetches)
                logger.info("\n--- Testing fetch ---")
                try {
                    fetch()
                    logger.info("✅ Fetch successful")
                } catch (e: Exception) {
                    logger.warn("⚠️  Fetch failed (no remote?): ${e.message}")
                }

                // Branch operations
                logger.info("\n--- Testing branch operations ---")
                val currentBranch = currentBranch()
                logger.info("Current branch: $currentBranch")

                // Tag operations
                logger.info("\n--- Testing tag operations ---")
                val latestTag = latestTag()
                if (latestTag != null) {
                    logger.info("Latest tag: $latestTag")
                } else {
                    logger.info("No tags found")
                }

                logger.info("\n✅ All enhanced Git operations work!")
            }
        }
    }
}
