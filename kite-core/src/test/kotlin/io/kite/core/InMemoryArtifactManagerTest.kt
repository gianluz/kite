package io.kite.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryArtifactManagerTest {
    @Test
    fun `can put and get artifacts`() {
        val manager = InMemoryArtifactManager()
        val path = Paths.get("/path/to/artifact.apk")

        manager.put("apk", path)

        assertEquals(path, manager.get("apk"))
    }

    @Test
    fun `get returns null for missing artifact`() {
        val manager = InMemoryArtifactManager()

        assertNull(manager.get("missing"))
    }

    @Test
    fun `put requires non-blank name`() {
        val manager = InMemoryArtifactManager()
        val path = Paths.get("/path/to/artifact.apk")

        assertThrows<IllegalArgumentException> {
            manager.put("", path)
        }

        assertThrows<IllegalArgumentException> {
            manager.put("   ", path)
        }
    }

    @Test
    fun `has returns true for existing artifact`() {
        val manager = InMemoryArtifactManager()
        val path = Paths.get("/path/to/artifact.apk")

        manager.put("apk", path)

        assertTrue(manager.has("apk"))
    }

    @Test
    fun `has returns false for missing artifact`() {
        val manager = InMemoryArtifactManager()

        assertFalse(manager.has("missing"))
    }

    @Test
    fun `list returns all artifact names`() {
        val manager = InMemoryArtifactManager()

        manager.put("apk", Paths.get("/apk"))
        manager.put("mapping", Paths.get("/mapping.txt"))
        manager.put("reports", Paths.get("/reports"))

        val artifacts = manager.list()
        assertEquals(3, artifacts.size)
        assertTrue(artifacts.contains("apk"))
        assertTrue(artifacts.contains("mapping"))
        assertTrue(artifacts.contains("reports"))
    }

    @Test
    fun `list returns empty set when no artifacts`() {
        val manager = InMemoryArtifactManager()

        val artifacts = manager.list()
        assertTrue(artifacts.isEmpty())
    }

    @Test
    fun `remove deletes artifact`() {
        val manager = InMemoryArtifactManager()
        val path = Paths.get("/artifact.apk")

        manager.put("apk", path)
        assertTrue(manager.has("apk"))

        manager.remove("apk")
        assertFalse(manager.has("apk"))
    }

    @Test
    fun `remove is safe for missing artifact`() {
        val manager = InMemoryArtifactManager()

        // Should not throw
        manager.remove("missing")
    }

    @Test
    fun `clear removes all artifacts`() {
        val manager = InMemoryArtifactManager()

        manager.put("apk", Paths.get("/apk"))
        manager.put("mapping", Paths.get("/mapping.txt"))

        assertEquals(2, manager.list().size)

        manager.clear()

        assertTrue(manager.list().isEmpty())
        assertFalse(manager.has("apk"))
        assertFalse(manager.has("mapping"))
    }

    @Test
    fun `putting same name overwrites`() {
        val manager = InMemoryArtifactManager()
        val path1 = Paths.get("/artifact1.apk")
        val path2 = Paths.get("/artifact2.apk")

        manager.put("apk", path1)
        assertEquals(path1, manager.get("apk"))

        manager.put("apk", path2)
        assertEquals(path2, manager.get("apk"))
        assertEquals(1, manager.list().size)
    }

    @Test
    fun `toString includes artifact names`() {
        val manager = InMemoryArtifactManager()

        manager.put("apk", Paths.get("/apk"))
        manager.put("mapping", Paths.get("/mapping.txt"))

        val str = manager.toString()
        assertTrue(str.contains("apk"))
        assertTrue(str.contains("mapping"))
    }
}
