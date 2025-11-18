import java.io.File

segments {
    // EXAMPLE 1: Simple text file artifact
    segment("create-report") {
        description = "Generates a build report"

        outputs {
            artifact("build-report", "report.txt")
        }

        execute {
            val report = workspace.resolve("report.txt").toFile()
            val timestamp = System.currentTimeMillis()
            val content = """
                Build Report
                ============
                Timestamp: $timestamp
                Status: Success
                Platform: ${System.getProperty("os.name")}
                Java: ${System.getProperty("java.version")}
            """.trimIndent()

            report.writeText(content)
            println("Created build report (" + report.length() + " bytes)")
        }
    }

    segment("publish-report") {
        description = "Publishes the build report"
        dependsOn("create-report")

        inputs {
            artifact("build-report")
        }

        execute {
            val reportPath = artifacts.get("build-report")

            if (reportPath != null) {
                val report = reportPath.toFile()
                println("Publishing report from: " + reportPath.toString())
                println("\n--- Report Contents ---")
                println(report.readText())
                println("--- End Report ---\n")
                println("Report published successfully!")
            } else {
                error("Report artifact not found!")
            }
        }
    }

    // EXAMPLE 2: Build artifact pipeline (simulated APK)
    segment("build-app") {
        description = "Builds the application"

        outputs {
            artifact("app-binary", "app.apk")
            artifact("build-log", "build.log")
        }

        execute {
            println("Building application...")

            // Simulate build
            Thread.sleep(500)

            // Create fake APK
            val apk = workspace.resolve("app.apk").toFile()
            apk.writeBytes(
                byteArrayOf(
                    0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte(), // ZIP signature
                    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
                )
            )

            // Create build log
            val log = workspace.resolve("build.log").toFile()
            log.writeText(
                """
                Build started at ${System.currentTimeMillis()}
                Compiling sources...
                Packaging application...
                Build successful!
                Output: app.apk (${apk.length()} bytes)
            """.trimIndent()
            )

            println("Build complete: app.apk (" + apk.length() + " bytes)")
        }
    }

    segment("test-app") {
        description = "Tests the application"
        dependsOn("build-app")

        inputs {
            artifact("app-binary")
        }

        execute {
            val apkPath = artifacts.get("app-binary")

            if (apkPath != null) {
                val apk = apkPath.toFile()
                println("Testing application")
                println("  Location: " + apkPath.toString())
                println("  Size: " + apk.length() + " bytes")

                // Simulate testing
                Thread.sleep(300)

                println("All tests passed!")
            } else {
                error("APK artifact not found!")
            }
        }
    }

    segment("deploy-app") {
        description = "Deploys the application"
        dependsOn("build-app")  // Note: runs in parallel with test-app!

        inputs {
            artifact("app-binary")
            artifact("build-log")
        }

        execute {
            val apkPath = artifacts.get("app-binary")
            val logPath = artifacts.get("build-log")

            if (apkPath != null && logPath != null) {
                val apk = apkPath.toFile()
                val log = logPath.toFile()
                println("Deploying application...")
                println("  APK size: " + apk.length() + " bytes")
                println("  Log size: " + log.length() + " bytes")

                // Simulate deployment
                Thread.sleep(400)

                println("Deployed to production!")
            } else {
                error("Required artifacts not found!")
            }
        }
    }

    // EXAMPLE 3: Directory artifact
    segment("run-tests") {
        description = "Runs test suite and generates reports"

        outputs {
            artifact("test-results", "test-results")
        }

        execute {
            println("Running test suite...")

            // Create test results directory
            val resultsDir = workspace.resolve("test-results").toFile()
            resultsDir.mkdirs()

            // Create fake test reports
            File(resultsDir, "summary.txt").writeText(
                """
                Test Summary
                ============
                Total tests: 42
                Passed: 42
                Failed: 0
                Skipped: 0
                Duration: 5.2s
            """.trimIndent()
            )

            File(resultsDir, "unit-tests.xml").writeText(
                """
                <?xml version="1.0"?>
                <testsuite tests="30" failures="0" time="3.1"/>
            """.trimIndent()
            )

            File(resultsDir, "integration-tests.xml").writeText(
                """
                <?xml version="1.0"?>
                <testsuite tests="12" failures="0" time="2.1"/>
            """.trimIndent()
            )

            println("Tests complete (3 reports generated)")
        }
    }

    segment("archive-results") {
        description = "Archives test results"
        dependsOn("run-tests")

        inputs {
            artifact("test-results")
        }

        execute {
            val resultsDirPath = artifacts.get("test-results")

            if (resultsDirPath != null) {
                val resultsDir = resultsDirPath.toFile()
                println("Archiving test results from: " + resultsDirPath.toString())

                val files = resultsDir.listFiles()
                if (files != null) {
                    println("\nFound " + files.size + " files:")
                    for (file in files) {
                        println("  - " + file.name + " (" + file.length() + " bytes)")
                    }
                }

                println("\nResults archived successfully!")
            } else {
                error("Test results directory not found!")
            }
        }
    }
}
