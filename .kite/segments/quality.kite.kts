segments {
    segment("ktlint") {
        description = "Run ktlint code style checks on main sources"
        dependsOn("compile")
        execute {
            exec("./gradlew", "ktlintMainSourceSetCheck", "ktlintKotlinScriptCheck", "--no-configuration-cache")
        }
    }

    segment("detekt") {
        description = "Run detekt static analysis"
        dependsOn("compile")
        execute {
            exec("./gradlew", "detekt", "--no-configuration-cache")
        }
    }

    segment("quality-checks") {
        description = "Run all code quality checks"
        dependsOn("ktlint", "detekt")
        execute {
            logger.info("✅ All quality checks passed!")
        }
    }
}
