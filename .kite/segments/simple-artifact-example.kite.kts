import java.io.File

segments {
    //
    // REAL-WORLD PATTERN: Just specify where Gradle puts the APK
    // Kite will copy it from there to .kite/artifacts/ automatically!
    //
    segment("build-apk") {
        description = "Builds Android APK with Gradle"

        outputs {
            // Just point to where Gradle creates the APK - that's it!
            artifact("apk", "app/build/outputs/apk/release/app-release.apk")
        }

        execute {
            // Build the APK
            exec("./gradlew", "assembleRelease")

            // That's it! Kite will automatically copy the APK to .kite/artifacts/apk
            // No workspace.resolve() needed!
        }
    }

    segment("test-apk") {
        dependsOn("build-apk")

        inputs {
            artifact("apk")
        }

        execute {
            // Get the artifact from .kite/artifacts/
            val apk = artifacts.get("apk")?.toFile()
            if (apk == null) error("APK not found!")

            println("Installing APK: ${apk.absolutePath}")
            exec("adb", "install", "-r", apk.absolutePath)
            exec("adb", "shell", "am", "instrument", "-w", "com.example.test")
        }
    }

    //
    // Another example: Multiple Gradle outputs
    //
    segment("build-all") {
        description = "Builds all variants"

        outputs {
            // Just list all the files Gradle creates
            artifact("debug-apk", "app/build/outputs/apk/debug/app-debug.apk")
            artifact("release-apk", "app/build/outputs/apk/release/app-release.apk")
            artifact("mapping", "app/build/outputs/mapping/release/mapping.txt")
        }

        execute {
            exec("./gradlew", "assemble")
            // Kite copies all 3 files after this segment succeeds
        }
    }

    segment("upload-to-playstore") {
        dependsOn("build-all")

        inputs {
            artifact("release-apk")
        }

        execute {
            val apk = artifacts.get("release-apk")?.toFile()
            if (apk == null) error("Release APK not found!")

            println("Uploading ${apk.name} (${apk.length()} bytes)")
            exec("bundle", "exec", "fastlane", "upload_to_play_store", "apk:${apk.absolutePath}")
        }
    }

    //
    // For demos/tests: You only need workspace.resolve() if you're CREATING files
    //
    segment("create-test-file") {
        description = "Only needed for demos - creates a file for testing"

        outputs {
            artifact("test-data", "test.txt")
        }

        execute {
            // This is ONLY needed because we're creating a file from scratch
            // In real builds, Gradle/tools create the files for you!
            val file = workspace.resolve("test.txt").toFile()
            file.writeText("test data")
        }
    }
}
