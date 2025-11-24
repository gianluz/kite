plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

allprojects {
    group = "io.kite"
    version = "0.1.0-alpha"

    repositories {
        mavenCentral()
    }
}

// Auto-install git hooks before any build
val installGitHooks by tasks.registering {
    description = "Install git hooks automatically"
    group = "git"

    val rootDir = layout.projectDirectory.asFile
    val hooksDir = File(rootDir, ".git/hooks")
    val sourceDir = File(rootDir, "scripts/git-hooks")

    // Don't use inputs/outputs to always run
    outputs.upToDateWhen { false }

    doLast {
        if (!hooksDir.exists()) {
            return@doLast // Not a git repository
        }

        if (!sourceDir.exists()) {
            return@doLast // No hooks to install
        }

        var installedCount = 0
        sourceDir.listFiles()?.filter { it.isFile && it.name != "README.md" }?.forEach { hookFile ->
            val targetFile = File(hooksDir, hookFile.name)

            // Remove existing hook if it exists
            if (targetFile.exists() || targetFile.toPath().toFile().exists()) {
                targetFile.delete()
            }

            // Create symlink
            try {
                val relativePath = "../../scripts/git-hooks/${hookFile.name}"
                java.nio.file.Files.createSymbolicLink(
                    targetFile.toPath(),
                    java.nio.file.Paths.get(relativePath)
                )

                // Make sure source is executable
                hookFile.setExecutable(true)

                installedCount++
            } catch (e: Exception) {
                logger.warn("Failed to install git hook ${hookFile.name}: ${e.message}")
            }
        }

        if (installedCount > 0) {
            logger.quiet("✅ Installed $installedCount git hook(s)")
        }
    }
}

// Run installGitHooks before clean or any compilation in subprojects
subprojects {
    tasks.matching { it.name == "clean" || it.name == "compileKotlin" }.configureEach {
        dependsOn(rootProject.tasks.named("installGitHooks"))
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "maven-publish")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        val implementation by configurations
        val testImplementation by configurations
        val testRuntimeOnly by configurations

        // Kotlin stdlib
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))

        // Testing
        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("io.mockk:mockk:1.13.8")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.0.1")
        android.set(false)
        outputToConsole.set(true)
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("detekt.yml"))
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set(project.description)
                    url.set("https://github.com/yourusername/kite")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}
