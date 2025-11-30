// Test Git plugin from Maven Local using regular @DependsOn
// Maven Local is automatically checked by Ivy! No special annotation needed.

@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:DependsOn("com.gianluz.kite:git:0.1.0-alpha")  // From Maven Local ✅

import com.google.gson.Gson
import io.kite.plugins.git.*

segments {

    segment("test-git-plugin") {
        description = "Test Git plugin resolution from Maven Local"

        execute {
            logger.info("Testing Maven Local resolution...")

            // Test Git plugin (from Maven Local)
            git {
                logger.info("Current branch: ${currentBranch()}")
                logger.info("Is clean: ${isClean()}")
                logger.info("Commit SHA: ${commitSha(short = true)}")
            }

            // Test regular Maven Central dependency
            val gson = Gson()
            val json = gson.toJson(mapOf("maven_local" to "works", "transitives" to "included"))
            logger.info("JSON test: $json")

            logger.info("✅ All dependencies resolved correctly!")
        }
    }
}
