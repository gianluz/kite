// Test using external dependency (Gson) that's available on the classpath
// Note: @file:DependsOn annotation is not yet implemented
// Currently, dependencies must be added to kite-dsl/build.gradle.kts

import com.google.gson.Gson

segments {
    segment("test-json") {
        description = "Test external dependency with Gson"
        execute {
            val gson = Gson()

            val data = mapOf(
                "project" to "Kite",
                "version" to "0.1.0",
                "language" to "Kotlin",
                "features" to listOf(
                    "Type-safe DSL",
                    "Parallel execution",
                    "Dependency resolution"
                )
            )

            val json = gson.toJson(data)
            println("✅ JSON serialization successful!")
            println(json)
        }
    }

    segment("test-json-parse") {
        description = "Test parsing JSON with Gson"
        dependsOn("test-json")
        execute {
            val gson = Gson()

            val jsonString = """
                {
                    "name": "Test Segment",
                    "status": "running",
                    "timestamp": 1234567890
                }
            """.trimIndent()

            @Suppress("UNCHECKED_CAST")
            val parsed = gson.fromJson(jsonString, Map::class.java) as Map<String, Any>
            println("✅ JSON parsing successful!")
            println("Parsed data: $parsed")
        }
    }
}