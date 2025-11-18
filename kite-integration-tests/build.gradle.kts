plugins {
    kotlin("jvm")
}

dependencies {
    // Test against the full Kite stack
    testImplementation(project(":kite-cli"))
    testImplementation(project(":kite-dsl"))
    testImplementation(project(":kite-runtime"))
    testImplementation(project(":kite-core"))

    // Test frameworks
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(kotlin("test"))

    // For assertions
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()

    // Give tests more time for ride execution
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

description = "Integration tests for Kite workflows"
