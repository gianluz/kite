plugins {
    id("com.gradleup.nmcp")
}

description = "Gradle operations plugin for Kite"

repositories {
    mavenLocal() // Check Maven Local first for kite-core
    mavenCentral()
}

dependencies {
    // Plugin depends on Kite core (api so it appears as a compile dependency in POM)
    api(project(":kite-core"))

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

// Customize publication
publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            artifactId = "gradle"

            pom {
                name.set("Kite Gradle Plugin")
                description.set("Flexible Gradle task execution for Kite workflows")

                developers {
                    developer {
                        name.set("Kite Team")
                        url.set("https://github.com/gianluz/kite")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/gianluz/kite.git")
                    developerConnection.set("scm:git:ssh://github.com/gianluz/kite.git")
                    url.set("https://github.com/gianluz/kite")
                }
            }
        }
    }
}
