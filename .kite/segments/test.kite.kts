segments {
    segment("test-core") {
        description = "Run kite-core unit tests"
        dependsOn("compile")
        execute {
            println("Starting test-core tests...")
            exec("./gradlew", ":kite-core:test")
            println("test-core completed ✓")
        }
    }

    segment("test-dsl") {
        description = "Run kite-dsl unit tests"
        dependsOn("compile")
        execute {
            println("Starting test-dsl tests...")
            exec("./gradlew", ":kite-dsl:test")
            println("test-dsl completed ✓")
        }
    }

    segment("test-runtime") {
        description = "Run kite-runtime unit tests"
        dependsOn("compile")
        execute {
            println("Starting test-runtime tests...")
            exec("./gradlew", ":kite-runtime:test")
            println("test-runtime completed ✓")
        }
    }

    segment("test-cli") {
        description = "Run kite-cli unit tests"
        dependsOn("compile")
        execute {
            println("Starting test-cli tests...")
            exec("./gradlew", ":kite-cli:test")
            println("test-cli completed ✓")
        }
    }

    segment("test-all") {
        description = "Run all unit tests"
        dependsOn("test-core", "test-dsl", "test-runtime", "test-cli")
        execute {
            println("✅ All unit tests passed!")
        }
    }
}
