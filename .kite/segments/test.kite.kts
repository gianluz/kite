segments {
    segment("test-core") {
        description = "Run kite-core unit tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-core:test")
        }
    }

    segment("test-dsl") {
        description = "Run kite-dsl unit tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-dsl:test")
        }
    }

    segment("test-runtime") {
        description = "Run kite-runtime unit tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-runtime:test")
        }
    }

    segment("test-cli") {
        description = "Run kite-cli unit tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-cli:test")
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
