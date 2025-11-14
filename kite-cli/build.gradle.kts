plugins {
    application
}

dependencies {
    // Depend on all other modules
    implementation(project(":kite-core"))
    implementation(project(":kite-dsl"))
    implementation(project(":kite-runtime"))

    // CLI framework
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // Terminal colors and styling (includes emoji support)
    implementation("com.github.ajalt.mordant:mordant:2.2.0")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

application {
    mainClass.set("io.kite.cli.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.kite.cli.MainKt"
    }

    // Create fat JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

description = "Command-line interface for Kite"
