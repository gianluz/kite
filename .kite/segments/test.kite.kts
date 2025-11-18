segments {
    segment("test-core") {
        description = "Run kite-core module tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-core:test")
        }
    }

    segment("test-dsl") {
        description = "Run kite-dsl module tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-dsl:test")
        }
    }

    segment("test-runtime") {
        description = "Run kite-runtime module tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-runtime:test")
        }
    }

    segment("test-cli") {
        description = "Run kite-cli module tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-cli:test")
        }
    }

    segment("test-integration") {
        description = "Run integration tests"
        dependsOn("compile")
        execute {
            exec("./gradlew", ":kite-integration-tests:test")
        }
    }
}
