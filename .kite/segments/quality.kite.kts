segments {
    segment("ktlint") {
        description = "Run ktlint code style checks"
        dependsOn("compile")
        execute {
            exec("./gradlew", "ktlintCheck")
        }
    }

    segment("detekt") {
        description = "Run detekt static analysis"
        dependsOn("compile")
        execute {
            exec("./gradlew", "detekt")
        }
    }

    segment("quality-checks") {
        description = "Run all code quality checks"
        dependsOn("ktlint", "detekt")
        execute {
            println("✅ All quality checks passed!")
        }
    }
}
