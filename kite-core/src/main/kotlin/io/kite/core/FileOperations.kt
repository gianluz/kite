package io.kite.core

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/**
 * File operation extensions for ExecutionContext.
 *
 * These functions provide convenient file manipulation capabilities
 * within segment execution blocks.
 */

/**
 * Reads the entire contents of a file as a string.
 *
 * @param path Path to the file (relative to workspace or absolute)
 * @return File contents as a string
 * @throws NoSuchFileException if file doesn't exist
 */
fun ExecutionContext.readFile(path: String): String {
    val file = resolveFile(path)
    return file.readText()
}

/**
 * Reads the contents of a file as a list of lines.
 *
 * @param path Path to the file (relative to workspace or absolute)
 * @return List of lines in the file
 * @throws NoSuchFileException if file doesn't exist
 */
fun ExecutionContext.readLines(path: String): List<String> {
    val file = resolveFile(path)
    return file.readLines()
}

/**
 * Writes text to a file, creating it if it doesn't exist.
 * Overwrites existing content.
 *
 * @param path Path to the file (relative to workspace or absolute)
 * @param content Content to write
 */
fun ExecutionContext.writeFile(
    path: String,
    content: String,
) {
    val file = resolveFile(path)
    file.parentFile?.mkdirs()
    file.writeText(content)
}

/**
 * Appends text to a file, creating it if it doesn't exist.
 *
 * @param path Path to the file (relative to workspace or absolute)
 * @param content Content to append
 */
fun ExecutionContext.appendFile(
    path: String,
    content: String,
) {
    val file = resolveFile(path)
    file.parentFile?.mkdirs()
    file.appendText(content)
}

/**
 * Copies a file or directory.
 *
 * @param source Source path (relative to workspace or absolute)
 * @param destination Destination path (relative to workspace or absolute)
 * @param overwrite Whether to overwrite destination if it exists
 * @throws FileAlreadyExistsException if destination exists and overwrite is false
 */
fun ExecutionContext.copyFile(
    source: String,
    destination: String,
    overwrite: Boolean = false,
) {
    val srcPath = resolveFile(source).toPath()
    val dstPath = resolveFile(destination).toPath()

    if (Files.isDirectory(srcPath)) {
        copyDirectory(srcPath, dstPath, overwrite)
    } else {
        dstPath.parent?.let { Files.createDirectories(it) }
        if (overwrite) {
            Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING)
        } else {
            Files.copy(srcPath, dstPath)
        }
    }
}

/**
 * Moves a file or directory.
 *
 * @param source Source path (relative to workspace or absolute)
 * @param destination Destination path (relative to workspace or absolute)
 * @param overwrite Whether to overwrite destination if it exists
 * @throws FileAlreadyExistsException if destination exists and overwrite is false
 */
fun ExecutionContext.moveFile(
    source: String,
    destination: String,
    overwrite: Boolean = false,
) {
    val srcPath = resolveFile(source).toPath()
    val dstPath = resolveFile(destination).toPath()

    dstPath.parent?.let { Files.createDirectories(it) }

    if (overwrite) {
        Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING)
    } else {
        Files.move(srcPath, dstPath)
    }
}

/**
 * Deletes a file or directory.
 *
 * @param path Path to delete (relative to workspace or absolute)
 * @param recursive If true, delete directories recursively
 * @throws DirectoryNotEmptyException if path is a non-empty directory and recursive is false
 */
fun ExecutionContext.deleteFile(
    path: String,
    recursive: Boolean = false,
) {
    val file = resolveFile(path)
    val filePath = file.toPath()

    if (!Files.exists(filePath)) {
        return // Already doesn't exist
    }

    if (Files.isDirectory(filePath) && recursive) {
        Files.walkFileTree(
            filePath,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    dir: Path,
                    exc: java.io.IOException?,
                ): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    } else {
        Files.delete(filePath)
    }
}

/**
 * Creates a directory, including any necessary parent directories.
 *
 * @param path Path to create (relative to workspace or absolute)
 */
fun ExecutionContext.createDirectory(path: String) {
    val file = resolveFile(path)
    file.mkdirs()
}

/**
 * Lists files in a directory.
 *
 * @param path Directory path (relative to workspace or absolute)
 * @param recursive If true, list files recursively
 * @return List of file paths relative to workspace
 */
fun ExecutionContext.listFiles(
    path: String,
    recursive: Boolean = false,
): List<String> {
    val dir = resolveFile(path)
    if (!dir.isDirectory) {
        throw NotDirectoryException(path)
    }

    return if (recursive) {
        dir.walkTopDown()
            .filter { it.isFile }
            .map { workspace.relativize(it.toPath()).toString() }
            .toList()
    } else {
        dir.listFiles()
            ?.filter { it.isFile }
            ?.map { workspace.relativize(it.toPath()).toString() }
            ?: emptyList()
    }
}

/**
 * Finds files matching a glob pattern.
 *
 * @param pattern Glob pattern (e.g., "**.kt", "src/**/Test*.java")
 * @param startPath Starting directory (relative to workspace or absolute)
 * @return List of matching file paths relative to workspace
 */
fun ExecutionContext.findFiles(
    pattern: String,
    startPath: String = ".",
): List<String> {
    val start = resolveFile(startPath).toPath()
    val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")

    return Files.walk(start)
        .filter { Files.isRegularFile(it) }
        .filter { matcher.matches(start.relativize(it)) }
        .map { workspace.relativize(it).toString() }
        .toList()
}

/**
 * Checks if a file or directory exists.
 *
 * @param path Path to check (relative to workspace or absolute)
 * @return true if the file exists
 */
fun ExecutionContext.fileExists(path: String): Boolean {
    return resolveFile(path).exists()
}

/**
 * Checks if a path is a directory.
 *
 * @param path Path to check (relative to workspace or absolute)
 * @return true if the path exists and is a directory
 */
fun ExecutionContext.isDirectory(path: String): Boolean {
    return resolveFile(path).isDirectory
}

/**
 * Checks if a path is a regular file.
 *
 * @param path Path to check (relative to workspace or absolute)
 * @return true if the path exists and is a file
 */
fun ExecutionContext.isFile(path: String): Boolean {
    return resolveFile(path).isFile
}

/**
 * Gets the size of a file in bytes.
 *
 * @param path Path to the file (relative to workspace or absolute)
 * @return File size in bytes
 * @throws NoSuchFileException if file doesn't exist
 */
fun ExecutionContext.fileSize(path: String): Long {
    return resolveFile(path).length()
}

/**
 * Creates a temporary directory.
 *
 * @param prefix Optional prefix for the directory name
 * @return Path to the created temporary directory
 */
fun ExecutionContext.createTempDir(prefix: String = "kite-"): String {
    val tempDir = Files.createTempDirectory(prefix).toFile()
    return workspace.relativize(tempDir.toPath()).toString()
}

/**
 * Creates a temporary file.
 *
 * @param prefix Optional prefix for the file name
 * @param suffix Optional suffix for the file name (e.g., ".txt")
 * @return Path to the created temporary file
 */
fun ExecutionContext.createTempFile(
    prefix: String = "kite-",
    suffix: String = ".tmp",
): String {
    val tempFile = Files.createTempFile(prefix, suffix).toFile()
    return workspace.relativize(tempFile.toPath()).toString()
}

/**
 * Gets the absolute path of a file.
 *
 * @param path Path to resolve (relative to workspace or absolute)
 * @return Absolute path as a string
 */
fun ExecutionContext.absolutePath(path: String): String {
    return resolveFile(path).absolutePath
}

/**
 * Resolves a path relative to the workspace.
 * If the path is already absolute, returns it as-is.
 *
 * @param path Path to resolve
 * @return Resolved file
 */
private fun ExecutionContext.resolveFile(path: String): File {
    val file = File(path)
    return if (file.isAbsolute) {
        file
    } else {
        workspace.resolve(path).toFile()
    }
}

/**
 * Copies a directory recursively.
 *
 * @param source Source directory path
 * @param destination Destination directory path
 * @param overwrite Whether to overwrite existing files
 */
private fun copyDirectory(
    source: Path,
    destination: Path,
    overwrite: Boolean,
) {
    Files.walkFileTree(
        source,
        object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(
                dir: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                val targetDir = destination.resolve(source.relativize(dir))
                Files.createDirectories(targetDir)
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(
                file: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                val targetFile = destination.resolve(source.relativize(file))
                if (overwrite) {
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                } else {
                    Files.copy(file, targetFile)
                }
                return FileVisitResult.CONTINUE
            }
        },
    )
}
