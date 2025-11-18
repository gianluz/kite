package io.kite.integration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests simulating real-world CI/CD scenarios.
 */
class RealWorldScenariosTest : IntegrationTestBase() {
    @Test
    fun `typical CI pipeline workflow`() {
        createSegmentFile(
            "ci-pipeline.kite.kts",
            """
            segments {
                segment("checkout") {
                    execute {
                        println("✓ Code checked out")
                    }
                }
                
                segment("install-deps") {
                    dependsOn("checkout")
                    execute {
                        println("✓ Dependencies installed")
                    }
                }
                
                segment("compile") {
                    dependsOn("install-deps")
                    execute {
                        println("✓ Code compiled")
                    }
                }
                
                segment("unit-tests") {
                    dependsOn("compile")
                    execute {
                        println("✓ Unit tests passed")
                    }
                }
                
                segment("integration-tests") {
                    dependsOn("compile")
                    execute {
                        Thread.sleep(50)
                        println("✓ Integration tests passed")
                    }
                }
                
                segment("lint") {
                    dependsOn("compile")
                    execute {
                        println("✓ Linting passed")
                    }
                }
                
                segment("build-artifacts") {
                    dependsOn("unit-tests", "integration-tests", "lint")
                    execute {
                        println("✓ Artifacts built")
                    }
                }
                
                segment("deploy-staging") {
                    dependsOn("build-artifacts")
                    execute {
                        println("✓ Deployed to staging")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "ci.kite.kts",
            """
            ride {
                name = "CI Pipeline"
                maxConcurrency = 4
                flow {
                    segment("checkout")
                    segment("install-deps")
                    segment("compile")
                    parallel {
                        segment("unit-tests")
                        segment("integration-tests")
                        segment("lint")
                    }
                    segment("build-artifacts")
                    segment("deploy-staging")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("CI Pipeline")

        result.assertSuccess()
        result.assertOutputContains("Code checked out")
        result.assertOutputContains("Dependencies installed")
        result.assertOutputContains("Code compiled")
        result.assertOutputContains("Unit tests passed")
        result.assertOutputContains("Integration tests passed")
        result.assertOutputContains("Linting passed")
        result.assertOutputContains("Artifacts built")
    }

    @Test
    fun `multi-module project build`() {
        createSegmentFile(
            "multi-module.kite.kts",
            """
            segments {
                segment("clean") {
                    execute {
                        println("Cleaned build directories")
                    }
                }
                
                segment("build-core") {
                    dependsOn("clean")
                    execute {
                        println("Built core module")
                    }
                }
                
                segment("build-api") {
                    dependsOn("build-core")
                    execute {
                        println("Built API module")
                    }
                }
                
                segment("build-web") {
                    dependsOn("build-core")
                    execute {
                        println("Built web module")
                    }
                }
                
                segment("build-cli") {
                    dependsOn("build-core")
                    execute {
                        println("Built CLI module")
                    }
                }
                
                segment("test-all") {
                    dependsOn("build-api", "build-web", "build-cli")
                    execute {
                        println("All modules tested")
                    }
                }
                
                segment("package") {
                    dependsOn("test-all")
                    execute {
                        println("Application packaged")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "multi-module.kite.kts",
            """
            ride {
                name = "Multi-Module Build"
                maxConcurrency = 4
                flow {
                    segment("clean")
                    segment("build-core")
                    parallel {
                        segment("build-api")
                        segment("build-web")
                        segment("build-cli")
                    }
                    segment("test-all")
                    segment("package")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Multi-Module Build")

        result.assertSuccess()
        assertEquals(7, result.totalSegments)
        result.assertOutputContains("Cleaned build directories")
        result.assertOutputContains("Built core module")
        result.assertOutputContains("Built API module")
        result.assertOutputContains("Built web module")
        result.assertOutputContains("Built CLI module")
        result.assertOutputContains("All modules tested")
        result.assertOutputContains("Application packaged")
    }

    @Test
    fun `release workflow with version bump`() {
        createSegmentFile(
            "release.kite.kts",
            """
            segments {
                segment("validate-branch") {
                    execute {
                        println("Branch validated for release")
                    }
                }
                
                segment("run-tests") {
                    dependsOn("validate-branch")
                    execute {
                        println("All tests passed")
                    }
                }
                
                segment("bump-version") {
                    dependsOn("run-tests")
                    execute {
                        println("Version bumped to 1.2.3")
                    }
                }
                
                segment("build-release") {
                    dependsOn("bump-version")
                    execute {
                        println("Release artifacts built")
                    }
                }
                
                segment("create-tag") {
                    dependsOn("build-release")
                    execute {
                        println("Git tag v1.2.3 created")
                    }
                }
                
                segment("publish") {
                    dependsOn("create-tag")
                    execute {
                        println("Published to repository")
                    }
                }
                
                segment("deploy-production") {
                    dependsOn("publish")
                    execute {
                        println("Deployed to production")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "release.kite.kts",
            """
            ride {
                name = "Release"
                maxConcurrency = 1  // Sequential for release
                flow {
                    segment("validate-branch")
                    segment("run-tests")
                    segment("bump-version")
                    segment("build-release")
                    segment("create-tag")
                    segment("publish")
                    segment("deploy-production")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Release")

        result.assertSuccess()
        assertEquals(7, result.totalSegments)

        // Verify order of operations
        val output = result.output
        val validateIdx = output.indexOf("Branch validated")
        val testsIdx = output.indexOf("All tests passed")
        val bumpIdx = output.indexOf("Version bumped")
        val buildIdx = output.indexOf("Release artifacts built")
        val tagIdx = output.indexOf("Git tag")
        val publishIdx = output.indexOf("Published to repository")
        val deployIdx = output.indexOf("Deployed to production")

        // Ensure proper order
        assertTrue(validateIdx < testsIdx)
        assertTrue(testsIdx < bumpIdx)
        assertTrue(bumpIdx < buildIdx)
        assertTrue(buildIdx < tagIdx)
        assertTrue(tagIdx < publishIdx)
        assertTrue(publishIdx < deployIdx)
    }

    @Test
    fun `matrix build for multiple platforms`() {
        createSegmentFile(
            "matrix.kite.kts",
            """
            segments {
                segment("build-linux") {
                    execute {
                        println("Built for Linux")
                    }
                }
                
                segment("build-macos") {
                    execute {
                        println("Built for macOS")
                    }
                }
                
                segment("build-windows") {
                    execute {
                        println("Built for Windows")
                    }
                }
                
                segment("test-linux") {
                    dependsOn("build-linux")
                    execute {
                        println("Tested on Linux")
                    }
                }
                
                segment("test-macos") {
                    dependsOn("build-macos")
                    execute {
                        println("Tested on macOS")
                    }
                }
                
                segment("test-windows") {
                    dependsOn("build-windows")
                    execute {
                        println("Tested on Windows")
                    }
                }
                
                segment("aggregate-results") {
                    dependsOn("test-linux", "test-macos", "test-windows")
                    execute {
                        println("All platforms tested successfully")
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "matrix.kite.kts",
            """
            ride {
                name = "Matrix Build"
                maxConcurrency = 3
                flow {
                    parallel {
                        segment("build-linux")
                        segment("build-macos")
                        segment("build-windows")
                    }
                    parallel {
                        segment("test-linux")
                        segment("test-macos")
                        segment("test-windows")
                    }
                    segment("aggregate-results")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Matrix Build")

        result.assertSuccess()
        assertEquals(7, result.totalSegments)
        result.assertOutputContains("Built for Linux")
        result.assertOutputContains("Built for macOS")
        result.assertOutputContains("Built for Windows")
        result.assertOutputContains("Tested on Linux")
        result.assertOutputContains("Tested on macOS")
        result.assertOutputContains("Tested on Windows")
        result.assertOutputContains("All platforms tested successfully")
    }
}
