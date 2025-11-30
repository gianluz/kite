@file:DependsOn("com.gianluz.kite:gradle:0.1.0-alpha")

import io.kite.plugins.gradle.*

segments {
    segment("test-core") {
        description = "Run kite-core module tests"
        dependsOn("compile")

        outputs {
            artifact("test-results-core", "kite-core/build/test-results/test")
            artifact("test-reports-core", "kite-core/build/reports/tests/test")
        }

        execute {
            gradle {
                task(":kite-core:test")
            }
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
            gradle {
                task(":kite-dsl:test")
            }
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
            gradle {
                task(":kite-runtime:test")
            }
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
            gradle {
                task(":kite-cli:test")
            }
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
            gradle {
                task(":kite-integration-tests:test")
            }
        }
    }

    segment("test-plugins-git") {
        description = "Run Git plugin tests"
        dependsOn("compile")

        outputs {
            artifact("test-results-plugin-git", "kite-plugins/git/build/test-results/test")
            artifact("test-reports-plugin-git", "kite-plugins/git/build/reports/tests/test")
        }

        execute {
            gradle {
                task(":kite-plugins:git:test")
            }
        }
    }

    segment("test-plugins-gradle") {
        description = "Run Gradle plugin tests"
        dependsOn("compile")

        outputs {
            artifact("test-results-plugin-gradle", "kite-plugins/gradle/build/test-results/test")
            artifact("test-reports-plugin-gradle", "kite-plugins/gradle/build/reports/tests/test")
        }

        execute {
            gradle {
                task(":kite-plugins:gradle:test")
            }
        }
    }

    segment("publish-test-results") {
        description = "Publishes combined test results"
        dependsOn(
            "test-core",
            "test-dsl",
            "test-runtime",
            "test-cli",
            "test-integration",
            "test-plugins-git",
            "test-plugins-gradle",
        )

        inputs {
            artifact("test-reports-core")
            artifact("test-reports-dsl")
            artifact("test-reports-runtime")
            artifact("test-reports-cli")
            artifact("test-reports-integration")
            artifact("test-reports-plugin-git")
            artifact("test-reports-plugin-gradle")
        }

        execute {
            logger.info("\n📊 Test Results Summary")
            logger.info("============================================================")

            // Read each module's test results
            val modules =
                listOf(
                    "core",
                    "dsl",
                    "runtime",
                    "cli",
                    "integration",
                    "plugin-git",
                    "plugin-gradle",
                )

            var totalPassed = 0
            var totalFailed = 0

            for (module in modules) {
                val reportPath = artifacts.get("test-reports-$module")
                if (reportPath != null) {
                    val indexHtml = reportPath.resolve("index.html").toFile()
                    if (indexHtml.exists()) {
                        logger.info("✅ $module: ${indexHtml.absolutePath}")
                        totalPassed++
                    } else {
                        logger.warn("⚠️  $module: No test report found")
                        totalFailed++
                    }
                }
            }

            logger.info("============================================================")
            logger.info("📁 All test artifacts saved to: .kite/artifacts/")
            logger.info("📊 Test suites: $totalPassed passed, $totalFailed failed")
            logger.info("")
        }
    }
}
