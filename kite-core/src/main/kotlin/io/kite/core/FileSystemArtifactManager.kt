package io.kite.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * File system-based implementation of ArtifactManager.
 *
 * This implementation stores artifacts in a dedicated directory (typically `.kite/artifacts/`).
 * Artifacts are copied to this directory when stored and can be retrieved by name.
 *
 * @param artifactsDir The directory where artifacts will be stored
 */
class FileSystemArtifactManager(private val artifactsDir: Path) : ArtifactManager {

    init {
        // Ensure artifacts directory exists
        Files.createDirectories(artifactsDir)
    }

    override fun put(name: String, path: Path) {
        require(name.isNotBlank()) { "Artifact name cannot be blank" }
        require(path.exists()) { "Artifact path does not exist: $path" }

        val destination = artifactsDir.resolve(name)

        // If source is a directory, copy recursively
        if (path.isDirectory()) {
            copyDirectory(path, destination)
        } else {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun get(name: String): Path? {
        val artifactPath = artifactsDir.resolve(name)
        return if (artifactPath.exists()) artifactPath else null
    }

    override fun has(name: String): Boolean {
        return artifactsDir.resolve(name).exists()
    }

    override fun list(): Set<String> {
        val dir = artifactsDir.toFile()
        if (!dir.exists() || !dir.isDirectory) {
            return emptySet()
        }
        return dir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
    }

    override fun remove(name: String) {
        val artifactPath = artifactsDir.resolve(name)
        if (artifactPath.exists()) {
            if (artifactPath.isDirectory()) {
                deleteDirectory(artifactPath.toFile())
            } else {
                Files.delete(artifactPath)
            }
        }
    }

    override fun clear() {
        val dir = artifactsDir.toFile()
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }
    }

    /**
     * Recursively copies a directory.
     */
    private fun copyDirectory(source: Path, destination: Path) {
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
