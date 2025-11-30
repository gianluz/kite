description = "Git operations plugin for Kite"

repositories {
    mavenLocal() // Check Maven Local first for kite-core
    mavenCentral()
}

dependencies {
    // Plugin depends on Kite core (using project dependency for build)
    compileOnly(project(":kite-core"))

    // JGit for Git operations
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")

    // Testing
    testImplementation(project(":kite-core"))
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
            }
        }
    }
}
