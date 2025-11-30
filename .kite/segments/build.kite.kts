@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")

import io.kite.plugins.gradle.*

segments {
    segment("clean") {
        description = "Clean build artifacts"
        execute {
            gradle {
                clean()
            }
        }
    }

    segment("compile") {
        description = "Compile all Kotlin modules"
        dependsOn("clean")
        execute {
            gradle {
                task("compileKotlin", "compileTestKotlin")
            }
        }
    }

    segment("build") {
        description = "Build all modules (assemble JARs)"
        dependsOn("compile")
        execute {
            gradle {
                task("assemble")
            }
        }
    }
}
