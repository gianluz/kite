description = "Gradle operations plugin for Kite"

repositories {
    mavenLocal() // Check Maven Local first for kite-core
    mavenCentral()
}

dependencies {
    // Plugin depends on Kite core (published to Maven Local during development)
    compileOnly("com.gianluz.kite:kite-core:${project.version}")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.gianluz.kite:kite-core:${project.version}")
}
