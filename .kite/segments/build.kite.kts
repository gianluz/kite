segments {
    segment("clean") {
        description = "Clean build artifacts"
        execute {
            exec("./gradlew", "clean")
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
            exec("./gradlew", "assemble")
        }
    }

    segment("publish-plugins-local") {
        description = "Publish plugins to Maven Local for use in other segments"
        dependsOn("compile")
        execute {
            logger.info("📦 Publishing plugins to Maven Local...")
            exec(
                "./gradlew",
                ":kite-plugins:git:publishToMavenLocal",
                ":kite-plugins:gradle:publishToMavenLocal",
            )
            logger.info("✅ Plugins published to Maven Local")
        }
    }
}
