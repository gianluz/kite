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
            exec("./gradlew", "compileKotlin")
        }
    }

    segment("build") {
        description = "Build all modules (compile + resources)"
        dependsOn("compile")
        execute {
            exec("./gradlew", "build", "-x", "test", "-x", "ktlint", "-x", "detekt")
        }
    }
}
