// Test script for new dependency annotations

// Test Maven Central (existing)
@file:DependsOn("com.google.code.gson:gson:2.10.1")

// Test local JAR (will fail if file doesn't exist - that's expected)
// @file:DependsOnJar("./plugins/test-plugin.jar")

// Test Maven Local (will fail if not published - that's expected)
// @file:DependsOnMavenLocal("io.kite.plugins:test:1.0.0")

import com.google.gson.Gson
import io.kite.dsl.*

segments {
    segment("test-gson") {
        description = "Test that Gson dependency works"

        execute {
            val gson = Gson()
            val json = gson.toJson(mapOf("status" to "works!", "test" to "dependency-resolution"))
            logger.info("✅ Gson test: $json")
        }
    }
}
