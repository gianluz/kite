description = "Gradle operations plugin for Kite"

repositories {
    mavenLocal() // Check Maven Local first for kite-core
    mavenCentral()
}

dependencies {
    // Plugin depends on Kite core
    // For compilation, prefer local project
    compileOnly(project(":kite-core"))

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(project(":kite-core"))
}

// Add kite-core as a compile-time dependency in POM (will be resolved from Maven)
configurations.create("kiteApi")
dependencies {
    add("kiteApi", "com.gianluz.kite:kite-core:${project.version}")
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

                // Manually add kite-core
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")
                    configurations.getByName("kiteApi").allDependencies.forEach { dep ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dep.group)
                        dependencyNode.appendNode("artifactId", dep.name)
                        dependencyNode.appendNode("version", dep.version)
                        dependencyNode.appendNode("scope", "compile")
                    }
                }
            }
        }
    }
}
