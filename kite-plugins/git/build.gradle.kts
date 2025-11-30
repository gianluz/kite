description = "Git operations plugin for Kite"

repositories {
    mavenLocal() // Check Maven Local first for kite-core
    mavenCentral()
}

dependencies {
    // Plugin depends on Kite core
    // For compilation, prefer local project
    compileOnly(project(":kite-core"))

    // JGit for Git operations
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")

    // Testing
    testImplementation(project(":kite-core"))
}

// Add kite-core as a compile-time dependency in POM (will be resolved from Maven)
configurations.create("kiteApi")
dependencies {
    add("kiteApi", "com.gianluz.kite:kite-core:${project.version}")
}

// Customize publication (configured by root build script)
publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            artifactId = "git"

            pom {
                name.set("Kite Git Plugin")
                description.set("Type-safe Git operations for Kite workflows")

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
