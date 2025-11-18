package io.kite.integration

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Integration tests for artifact management between segments.
 */
class ArtifactManagementTest : IntegrationTestBase() {

    @Test
    fun `segment produces artifact consumed by dependent segment`() {
        createSegmentFile(
            "artifacts.kite.kts",
            """
            import java.io.File
            
            segments {
                segment("producer") {
                    outputs {
                        artifact("data-file", "output.txt")
                    }
                    execute {
                        val file = workspace.resolve("output.txt").toFile()
                        file.writeText("Generated data from producer")
                        println("Producer created artifact")
                    }
                }
                
                segment("consumer") {
                    dependsOn("producer")
                    inputs {
                        artifact("data-file")
                    }
                    execute {
                        val artifactPath = artifacts.get("data-file")
                        if (artifactPath != null) {
                            val content = artifactPath.toFile().readText()
                            println("Consumer read: " + content)
                        } else {
                            error("Artifact 'data-file' not found!")
                        }
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "artifacts.kite.kts",
            """
            ride {
                name = "Artifacts Test"
                flow {
                    segment("producer")
                    segment("consumer")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Artifacts Test")

        result.assertSuccess()
        result.assertOutputContains("Producer created artifact")
        result.assertOutputContains("Consumer read: Generated data from producer")
    }

    @Test
    fun `segment produces multiple artifacts`() {
        createSegmentFile(
            "multi-artifacts.kite.kts",
            """
            import java.io.File
            
            segments {
                segment("build") {
                    outputs {
                        artifact("apk", "app.apk")
                        artifact("mapping", "mapping.txt")
                        artifact("logs", "build.log")
                    }
                    execute {
                        workspace.resolve("app.apk").toFile().writeText("fake apk")
                        workspace.resolve("mapping.txt").toFile().writeText("fake mapping")
                        workspace.resolve("build.log").toFile().writeText("fake log")
                        println("Build created 3 artifacts")
                    }
                }
                
                segment("verify") {
                    dependsOn("build")
                    inputs {
                        artifact("apk")
                        artifact("mapping")
                        artifact("logs")
                    }
                    execute {
                        val apk = artifacts.get("apk")
                        val mapping = artifacts.get("mapping")
                        val logs = artifacts.get("logs")
                        
                        println("Has apk: " + (apk != null))
                        println("Has mapping: " + (mapping != null))
                        println("Has logs: " + (logs != null))
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "multi-artifacts.kite.kts",
            """
            ride {
                name = "Multi Artifacts"
                flow {
                    segment("build")
                    segment("verify")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Multi Artifacts")

        result.assertSuccess()
        result.assertOutputContains("Build created 3 artifacts")
        result.assertOutputContains("Has apk: true")
        result.assertOutputContains("Has mapping: true")
        result.assertOutputContains("Has logs: true")
    }

    @Test
    fun `multiple segments share same artifact`() {
        createSegmentFile(
            "shared.kite.kts",
            """
            import java.io.File
            
            segments {
                segment("setup") {
                    outputs {
                        artifact("config", "config.txt")
                    }
                    execute {
                        workspace.resolve("config.txt").toFile().writeText("shared configuration")
                        println("Setup created config")
                    }
                }
                
                segment("task-a") {
                    dependsOn("setup")
                    inputs {
                        artifact("config")
                    }
                    execute {
                        val config = artifacts.get("config")?.toFile()?.readText()
                        println("Task A used: " + config)
                    }
                }
                
                segment("task-b") {
                    dependsOn("setup")
                    inputs {
                        artifact("config")
                    }
                    execute {
                        val config = artifacts.get("config")?.toFile()?.readText()
                        println("Task B used: " + config)
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "shared.kite.kts",
            """
            ride {
                name = "Shared Artifacts"
                maxConcurrency = 2
                flow {
                    segment("setup")
                    parallel {
                        segment("task-a")
                        segment("task-b")
                    }
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Shared Artifacts")

        result.assertSuccess()
        result.assertOutputContains("Setup created config")
        result.assertOutputContains("Task A used: shared configuration")
        result.assertOutputContains("Task B used: shared configuration")
    }

    @Test
    fun `artifact directories work correctly`() {
        createSegmentFile(
            "dir-artifacts.kite.kts",
            """
            import java.io.File
            
            segments {
                segment("create-dir") {
                    outputs {
                        artifact("test-outputs", "test-outputs")
                    }
                    execute {
                        val dir = workspace.resolve("test-outputs").toFile()
                        dir.mkdirs()
                        File(dir, "file1.txt").writeText("content1")
                        File(dir, "file2.txt").writeText("content2")
                        println("Created directory artifact")
                    }
                }
                
                segment("read-dir") {
                    dependsOn("create-dir")
                    inputs {
                        artifact("test-outputs")
                    }
                    execute {
                        val dir = artifacts.get("test-outputs")?.toFile()
                        if (dir != null && dir.isDirectory) {
                            val files = dir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
                            println("Files in artifact: " + files.joinToString(", "))
                        }
                    }
                }
            }
            """.trimIndent()
        )

        createRideFile(
            "dir-artifacts.kite.kts",
            """
            ride {
                name = "Directory Artifacts"
                flow {
                    segment("create-dir")
                    segment("read-dir")
                }
            }
            """.trimIndent()
        )

        val result = executeRide("Directory Artifacts")

        result.assertSuccess()
        result.assertOutputContains("Created directory artifact")
        result.assertOutputContains("Files in artifact:")
        result.assertOutputContains("file1.txt")
        result.assertOutputContains("file2.txt")
    }
}
