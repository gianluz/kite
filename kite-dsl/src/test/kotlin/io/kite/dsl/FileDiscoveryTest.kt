package io.kite.dsl

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileDiscoveryTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `discoverSegmentFiles returns empty list when directory does not exist`() {
        val discovery = FileDiscovery(workspaceRoot = tempDir)

        val files = discovery.discoverSegmentFiles()

        assertTrue(files.isEmpty())
    }

    @Test
    fun `discoverRideFiles returns empty list when directory does not exist`() {
        val discovery = FileDiscovery(workspaceRoot = tempDir)

        val files = discovery.discoverRideFiles()

        assertTrue(files.isEmpty())
    }

    @Test
    fun `discoverSegmentFiles finds kite files`() {
        // Create directory structure
        val segmentsDir = File(tempDir, ".kite/segments").apply { mkdirs() }

        // Create test files
        File(segmentsDir, "build.kite.kts").writeText("// build segments")
        File(segmentsDir, "test.kite.kts").writeText("// test segments")
        File(segmentsDir, "other.txt").writeText("// not a kite file")

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val files = discovery.discoverSegmentFiles()

        assertEquals(2, files.size)
        assertTrue(files.any { it.name == "build.kite.kts" })
        assertTrue(files.any { it.name == "test.kite.kts" })
        assertFalse(files.any { it.name == "other.txt" })
    }

    @Test
    fun `discoverRideFiles finds kite files`() {
        // Create directory structure
        val ridesDir = File(tempDir, ".kite/rides").apply { mkdirs() }

        // Create test files
        File(ridesDir, "mr.kite.kts").writeText("// mr ride")
        File(ridesDir, "release.kite.kts").writeText("// release ride")
        File(ridesDir, "README.md").writeText("// not a kite file")

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val files = discovery.discoverRideFiles()

        assertEquals(2, files.size)
        assertTrue(files.any { it.name == "mr.kite.kts" })
        assertTrue(files.any { it.name == "release.kite.kts" })
        assertFalse(files.any { it.name == "README.md" })
    }

    @Test
    fun `discoverSegmentFiles finds files in subdirectories`() {
        // Create nested directory structure
        val segmentsDir = File(tempDir, ".kite/segments").apply { mkdirs() }
        val androidDir = File(segmentsDir, "android").apply { mkdir() }
        val backendDir = File(segmentsDir, "backend").apply { mkdir() }

        File(segmentsDir, "common.kite.kts").writeText("// common")
        File(androidDir, "build.kite.kts").writeText("// android build")
        File(backendDir, "deploy.kite.kts").writeText("// backend deploy")

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val files = discovery.discoverSegmentFiles()

        assertEquals(3, files.size)
        assertTrue(files.any { it.name == "common.kite.kts" })
        assertTrue(files.any { it.name == "build.kite.kts" })
        assertTrue(files.any { it.name == "deploy.kite.kts" })
    }

    @Test
    fun `loadSegments returns empty when no files`() = runTest {
        val discovery = FileDiscovery(workspaceRoot = tempDir)

        val result = discovery.loadSegments()

        assertTrue(result.success)
        assertTrue(result.segments.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `loadRides returns empty when no files`() = runTest {
        val discovery = FileDiscovery(workspaceRoot = tempDir)

        val result = discovery.loadRides()

        assertTrue(result.success)
        assertTrue(result.rides.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `loadSegments loads valid segment files`() = runTest {
        // Create directory structure
        val segmentsDir = File(tempDir, ".kite/segments").apply { mkdirs() }

        // Create valid segment file
        File(segmentsDir, "build.kite.kts").writeText(
            """
            segments {
                segment("build") {
                    description = "Build the app"
                    execute { }
                }
                segment("test") {
                    execute { }
                }
            }
        """.trimIndent()
        )

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val result = discovery.loadSegments()

        assertTrue(result.success)
        assertEquals(2, result.segments.size)
        assertEquals("build", result.segments[0].name)
        assertEquals("test", result.segments[1].name)
    }

    @Test
    fun `loadRides loads valid ride files`() = runTest {
        // Create directory structure
        val ridesDir = File(tempDir, ".kite/rides").apply { mkdirs() }

        // Create valid ride file
        File(ridesDir, "mr.kite.kts").writeText(
            """
            ride {
                name = "MR Ride"
                flow {
                    segment("build")
                }
            }
        """.trimIndent()
        )

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val result = discovery.loadRides()

        assertTrue(result.success)
        assertEquals(1, result.rides.size)
        assertEquals("MR Ride", result.rides[0].name)
    }

    @Test
    fun `loadSegments handles compilation errors`() = runTest {
        // Create directory structure
        val segmentsDir = File(tempDir, ".kite/segments").apply { mkdirs() }

        // Create invalid segment file
        File(segmentsDir, "invalid.kite.kts").writeText(
            """
            this is not valid kotlin code
        """.trimIndent()
        )

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val result = discovery.loadSegments()

        assertFalse(result.success)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].file.name == "invalid.kite.kts")
    }

    @Test
    fun `loadAll loads both segments and rides`() = runTest {
        // Create directory structure
        val segmentsDir = File(tempDir, ".kite/segments").apply { mkdirs() }
        val ridesDir = File(tempDir, ".kite/rides").apply { mkdirs() }

        // Create segment file
        File(segmentsDir, "build.kite.kts").writeText(
            """
            segments {
                segment("build") {
                    execute { }
                }
            }
        """.trimIndent()
        )

        // Create ride file
        File(ridesDir, "mr.kite.kts").writeText(
            """
            ride {
                name = "MR"
                flow {
                    segment("build")
                }
            }
        """.trimIndent()
        )

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val result = discovery.loadAll()

        assertTrue(result.success)
        assertEquals(1, result.segments.size)
        assertEquals(1, result.rides.size)
    }

    @Test
    fun `KiteLoadResult provides segment map`() = runTest {
        val segmentsDir = File(tempDir, ".kite/segments").apply { mkdirs() }

        File(segmentsDir, "build.kite.kts").writeText(
            """
            segments {
                segment("build") { execute { } }
                segment("test") { execute { } }
            }
        """.trimIndent()
        )

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val result = discovery.loadAll()

        val segmentMap = result.segmentMap()
        assertEquals(2, segmentMap.size)
        assertTrue(segmentMap.containsKey("build"))
        assertTrue(segmentMap.containsKey("test"))
    }

    @Test
    fun `KiteLoadResult provides ride map`() = runTest {
        val ridesDir = File(tempDir, ".kite/rides").apply { mkdirs() }

        File(ridesDir, "mr.kite.kts").writeText(
            """
            ride {
                name = "MR"
                flow { segment("build") }
            }
        """.trimIndent()
        )

        File(ridesDir, "release.kite.kts").writeText(
            """
            ride {
                name = "Release"
                flow { segment("build") }
            }
        """.trimIndent()
        )

        val discovery = FileDiscovery(workspaceRoot = tempDir)
        val result = discovery.loadAll()

        val rideMap = result.rideMap()
        assertEquals(2, rideMap.size)
        assertTrue(rideMap.containsKey("MR"))
        assertTrue(rideMap.containsKey("Release"))
    }
}
