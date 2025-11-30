@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")

import io.kite.plugins.gradle.*

segments {
    segment("ktlint") {
        description = "Run ktlint code style checks on main sources"
        dependsOn("compile")
        execute {
            gradle {
                task("ktlintMainSourceSetCheck", "ktlintKotlinScriptCheck")
            }
        }
    }

    segment("detekt") {
        description = "Run detekt static analysis"
        dependsOn("compile")
        execute {
            gradle {
                task("detekt")
            }
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
