// Test Gradle plugin
@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")

import io.kite.plugins.gradle.*

segments {

    segment("test-gradle-plugin") {
        description = "Test Gradle plugin operations"

        execute {
            logger.info("=== Testing Gradle Plugin ===")

            gradle {
                // Test simple operations
                logger.info("\n--- Testing tasks method ---")
                tasks("tasks", "--group=help")

                logger.info("\n✅ Gradle plugin works!")
            }
        }
    }
}
