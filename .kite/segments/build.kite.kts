segments {
    segment("clean") {
        description = "Clean build artifacts"
        execute {
            exec("./gradlew", "clean", "--no-configuration-cache")
        }
    }

    segment("compile") {
        description = "Compile all Kotlin modules"
        dependsOn("clean")
        execute {
            exec("./gradlew", "compileKotlin", "compileTestKotlin", "--no-configuration-cache")
        }
    }

    segment("build") {
        description = "Build all modules (assemble JARs)"
        dependsOn("compile")
        execute {
            exec("./gradlew", "assemble", "--no-configuration-cache")
        }
    }

    segment("publish-plugins-local") {
        description = "Publish plugins to Maven Local for use in other segments"
        dependsOn("compile")
        execute {
            logger.info("📦 Publishing core modules to Maven Local first...")
            // Publish core dependencies first
            exec(
                "./gradlew",
                ":kite-core:publishToMavenLocal",
                "--no-configuration-cache",
            )

            logger.info("📦 Publishing plugins to Maven Local...")
            // Now publish plugins
            exec(
                "./gradlew",
                ":kite-plugins:git:publishToMavenLocal",
                ":kite-plugins:gradle:publishToMavenLocal",
                "--no-configuration-cache",
            )
            logger.info("✅ Plugins published to Maven Local")
        }
    }
}
