// Simple plugin for testing dependency resolution

plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

group = "io.kite.test"
version = "1.0.0-TEST"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
