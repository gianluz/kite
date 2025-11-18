segments {
    segment("test-core") {
        description = "Run kite-core module tests"
        dependsOn("compile")

        outputs {
            // Gradle creates test reports here - just point to them!
            artifact("test-results-core", "kite-core/build/test-results/test")
            artifact("test-reports-core", "kite-core/build/reports/tests/test")
        }

        execute {
            exec("./gradlew", ":kite-core:test")
            // After this completes, Kite automatically copies test results to .kite/artifacts/
        }
    }

    segment("test-dsl") {
        description = "Run kite-dsl module tests"
        dependsOn("compile")

        outputs {
            artifact("test-results-dsl", "kite-dsl/build/test-results/test")
            artifact("test-reports-dsl", "kite-dsl/build/reports/tests/test")
        }

        execute {
            exec("./gradlew", ":kite-dsl:test")
        }
    }

    segment("test-runtime") {
        description = "Run kite-runtime module tests"
        dependsOn("compile")

        outputs {
            artifact("test-results-runtime", "kite-runtime/build/test-results/test")
            artifact("test-reports-runtime", "kite-runtime/build/reports/tests/test")
        }

        execute {
            exec("./gradlew", ":kite-runtime:test")
        }
    }

    segment("test-cli") {
        description = "Run kite-cli module tests"
        dependsOn("compile")

        outputs {
            artifact("test-results-cli", "kite-cli/build/test-results/test")
            artifact("test-reports-cli", "kite-cli/build/reports/tests/test")
        }

        execute {
            exec("./gradlew", ":kite-cli:test")
        }
    }

    segment("test-integration") {
        description = "Run integration tests"
        dependsOn("compile")

        outputs {
            artifact("test-results-integration", "kite-integration-tests/build/test-results/test")
            artifact("test-reports-integration", "kite-integration-tests/build/reports/tests/test")
        }

        execute {
            exec("./gradlew", ":kite-integration-tests:test")
        }
    }

    // New segment: Publish all test results (runs after all tests complete)
    segment("publish-test-results") {
        description = "Publishes combined test results"
        dependsOn("test-core", "test-dsl", "test-runtime", "test-cli", "test-integration")

        inputs {
            artifact("test-reports-core")
            artifact("test-reports-dsl")
            artifact("test-reports-runtime")
            artifact("test-reports-cli")
            artifact("test-reports-integration")
        }

        execute {
            println("\n📊 Test Results Summary")
            println("============================================================")

            // Read each module's test results
            val modules = listOf("core", "dsl", "runtime", "cli", "integration")
            for (module in modules) {
                val reportPath = artifacts.get("test-reports-$module")
                if (reportPath != null) {
                    val indexHtml = reportPath.resolve("index.html").toFile()
                    if (indexHtml.exists()) {
                        println("✅ $module: ${indexHtml.absolutePath}")
                    }
                }
            }

            println("============================================================")
            println("📁 All test artifacts saved to: .kite/artifacts/")
            println("   You can upload this directory to CI for archiving!")
            println()
        }
    }
}
