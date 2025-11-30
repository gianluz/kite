// Test Git plugin from Maven Local
@file:DependsOnMavenLocal("com.gianluz.kite:git:0.1.0-alpha")

import io.kite.plugins.git.*

segments {
    segment("test-git-plugin") {
        description = "Test Git plugin from Maven Local"

        execute {
            logger.info("Testing Git plugin...")

            git {
                // Test basic operations
                val branch = currentBranch()
                logger.info("✅ Current branch: $branch")

                val isClean = isClean()
                logger.info("✅ Is clean: $isClean")

                val sha = commitSha(short = true)
                logger.info("✅ Commit SHA: $sha")

                val modified = modifiedFiles()
                logger.info("✅ Modified files: ${modified.size}")
            }

            logger.info("🎉 Git plugin works from Maven Local!")
        }
    }
}
