package io.kite.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * File system-based implementation of ArtifactManager.
 *
 * This implementation stores artifacts in a dedicated directory (typically `.kite/artifacts/`).
 * Artifacts are copied to this directory when stored and can be retrieved by name.
 *
 * Thread-safe: Uses ConcurrentHashMap for artifact tracking.
 *
 * @param artifactsDir The directory where artifacts will be stored
 */
class FileSystemArtifactManager(private val artifactsDir: Path) : ArtifactManager {
    // Track artifacts in memory for fast lookups
    private val artifacts = ConcurrentHashMap<String, Path>()

    init {
        // Ensure artifacts directory exists
        Files.createDirectories(artifactsDir)
    }

    override fun put(
        name: String,
        path: Path,
    ) {
        require(name.isNotBlank()) { "Artifact name cannot be blank" }
        require(path.exists()) { "Artifact path does not exist: $path" }

        val destination = artifactsDir.resolve(name)

        // If source is a directory, copy recursively
        if (path.isDirectory()) {
            copyDirectory(path, destination)
        } else {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
        }

        // Track the artifact
        artifacts[name] = destination
    }

    override fun get(name: String): Path? {
        return artifacts[name]
    }

    override fun has(name: String): Boolean {
        return artifacts.containsKey(name)
    }

    override fun list(): Set<String> {
        return artifacts.keys.toSet()
    }

    override fun remove(name: String) {
        val artifactPath = artifacts.remove(name) ?: return

        if (artifactPath.exists()) {
            if (artifactPath.isDirectory()) {
                deleteDirectory(artifactPath.toFile())
            } else {
                Files.delete(artifactPath)
            }
        }
    }

    override fun clear() {
        // Delete all tracked artifacts
        for ((_, path) in artifacts) {
            if (path.exists()) {
                if (path.isDirectory()) {
                    deleteDirectory(path.toFile())
                } else {
                    Files.delete(path)
                }
            }
        }
        artifacts.clear()
    }

    /**
     * Recursively copies a directory.
     */
    private fun copyDirectory(
        source: Path,
        destination: Path,
    ) {
        Files.walk(source).forEach { sourcePath ->
            val targetPath = destination.resolve(source.relativize(sourcePath))
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath)
            } else {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    /**
     * Recursively deletes a directory.
     */
    private fun deleteDirectory(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        }
        dir.delete()
    }

    override fun toString(): String {
        return "FileSystemArtifactManager(artifactsDir=$artifactsDir, artifacts=${list()})"
    }
}
