// Example showing file operation helpers in Kite segments

segments {
    segment("file-read-write-example") {
        description = "Demonstrates reading and writing files"
        execute {
            // Write a file
            writeFile("output/hello.txt", "Hello from Kite!")

            // Read it back
            val content = readFile("output/hello.txt")
            println("Read: $content")

            // Append to it
            appendFile("output/hello.txt", "\nAppended line")

            // Read lines
            val lines = readLines("output/hello.txt")
            println("Lines: ${lines.size}")
            lines.forEach { println("  - $it") }
        }
    }

    segment("file-copy-move-example") {
        description = "Demonstrates copying and moving files"
        execute {
            // Create source file
            writeFile("source.txt", "Content to copy")

            // Copy file
            copyFile("source.txt", "copy1.txt")
            println("✓ Copied to copy1.txt")

            // Copy with overwrite
            copyFile("source.txt", "copy2.txt", overwrite = true)
            println("✓ Copied to copy2.txt")

            // Move file
            moveFile("copy2.txt", "moved.txt")
            println("✓ Moved copy2.txt to moved.txt")
        }
    }

    segment("directory-operations-example") {
        description = "Demonstrates directory operations"
        execute {
            // Create directories
            createDirectory("test-dir/nested/deep")
            println("✓ Created nested directories")

            // Create some files
            writeFile("test-dir/file1.txt", "File 1")
            writeFile("test-dir/file2.txt", "File 2")
            writeFile("test-dir/nested/file3.txt", "File 3")

            // List files (non-recursive)
            val files = listFiles("test-dir")
            println("Files in test-dir: ${files.size}")

            // List files (recursive)
            val allFiles = listFiles("test-dir", recursive = true)
            println("All files in test-dir (recursive): ${allFiles.size}")
            allFiles.forEach { println("  - $it") }

            // Copy entire directory
            copyFile("test-dir", "test-dir-copy")
            println("✓ Copied entire directory")

            // Delete directory
            deleteFile("test-dir-copy", recursive = true)
            println("✓ Deleted directory")
        }
    }

    segment("file-search-example") {
        description = "Demonstrates finding files with patterns"
        execute {
            // Create test files
            writeFile("src/Main.kt", "fun main() {}")
            writeFile("src/Utils.kt", "fun utils() {}")
            writeFile("src/test/MainTest.kt", "fun test() {}")
            writeFile("src/README.md", "# Readme")

            // Find all Kotlin files
            val ktFiles = findFiles("**.kt", "src")
            println("Kotlin files found: ${ktFiles.size}")
            ktFiles.forEach { println("  - $it") }

            // Find test files
            val testFiles = findFiles("**Test.kt", "src")
            println("Test files found: ${testFiles.size}")
            testFiles.forEach { println("  - $it") }
        }
    }

    segment("file-checks-example") {
        description = "Demonstrates file existence and type checks"
        execute {
            writeFile("test-file.txt", "content")
            createDirectory("test-directory")

            // Check existence
            println("test-file.txt exists: ${fileExists("test-file.txt")}")
            println("nonexistent.txt exists: ${fileExists("nonexistent.txt")}")

            // Check type
            println("test-file.txt is file: ${isFile("test-file.txt")}")
            println("test-file.txt is directory: ${isDirectory("test-file.txt")}")
            println("test-directory is directory: ${isDirectory("test-directory")}")

            // Get file size
            val size = fileSize("test-file.txt")
            println("test-file.txt size: $size bytes")

            // Get absolute path
            val absPath = absolutePath("test-file.txt")
            println("Absolute path: $absPath")
        }
    }

    segment("temp-files-example") {
        description = "Demonstrates temporary file/directory creation"
        execute {
            // Create temp directory
            val tempDir = createTempDir("kite-test-")
            println("Created temp directory: $tempDir")

            // Use it
            writeFile("$tempDir/temp-data.txt", "Temporary data")
            println("Created file in temp dir")

            // Create temp file
            val tempFile = createTempFile("data-", ".json")
            println("Created temp file: $tempFile")
            writeFile(tempFile, """{"key": "value"}""")

            // Read it back
            val json = readFile(tempFile)
            println("Temp file contents: $json")

            // Clean up (optional - temp files are in system temp dir)
            deleteFile(tempFile)
            deleteFile(tempDir, recursive = true)
            println("✓ Cleaned up temp files")
        }
    }

    segment("build-artifact-example") {
        description = "Real-world example: copying build artifacts"
        dependsOn("compile")  // Assume compile segment exists
        execute {
            // Create artifacts directory
            createDirectory("artifacts")

            // Find all JAR files in build directories
            val jars = findFiles("**.jar", "build")
            println("Found ${jars.size} JAR files")

            // Copy them to artifacts directory
            jars.forEach { jar ->
                val fileName = jar.substringAfterLast("/")
                copyFile(jar, "artifacts/$fileName")
                println("✓ Copied $fileName to artifacts/")
            }

            // Create a build info file
            val buildInfo = """
                Build: ${env("CI_BUILD_ID") ?: "local"}
                Branch: $branch
                Commit: $commitSha
                Artifacts: ${jars.size}
            """.trimIndent()
            writeFile("artifacts/BUILD_INFO.txt", buildInfo)

            println("✓ Build artifacts prepared in artifacts/")
        }
    }
}
