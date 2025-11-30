// Test flexible Gradle plugin
@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")

import io.kite.plugins.gradle.*

segments {

    segment("test-gradle-flexible") {
        description = "Test flexible Gradle plugin"

        execute {
            logger.info("=== Testing Flexible Gradle Plugin ===")

            gradle {
                // Test arbitrary tasks
                logger.info("\n--- Test 1: Simple task ---")
                task("tasks", "--group=help")

                // Test with options
                logger.info("\n--- Test 2: With options ---")
                task("projects") {
                    stacktrace = true
                }

                // Test multiple tasks
                logger.info("\n--- Test 3: Multiple tasks ---")
                task("clean", "projects")

                // Test convenience method
                logger.info("\n--- Test 4: Convenience method ---")
                clean()

                logger.info("\n✅ Flexible Gradle plugin works!")
                logger.info("Can run ANY Gradle task - Java, Kotlin, Android, custom!")
            }
        }
    }
}
