description = "Gradle operations plugin for Kite"

repositories {
    mavenLocal() // Check Maven Local first for kite-core
    mavenCentral()
}

dependencies {
    // Plugin depends on Kite core (using project dependency for build)
    compileOnly(project(":kite-core"))

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(project(":kite-core"))
}
