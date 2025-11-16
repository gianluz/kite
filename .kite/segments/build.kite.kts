segments {
    segment("clean") {
        description = "Clean build artifacts"
        execute {
            shell("./gradlew clean")
        }
    }

    segment("compile") {
        description = "Compile all Kotlin modules"
        dependsOn("clean")
        execute {
            exec("./gradlew", "compileKotlin", "compileTestKotlin")
        }
    }

    segment("build") {
        description = "Build all modules (assemble JARs)"
        dependsOn("compile")
        execute {
            // Just assemble - don't run tests or quality checks
            exec("./gradlew", "assemble")
        }
    }
}
