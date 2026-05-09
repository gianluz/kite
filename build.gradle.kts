plugins {
    kotlin("jvm") version "2.1.20" apply false
    kotlin("plugin.serialization") version "2.1.20" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("com.gradleup.nmcp.aggregation") version "1.4.4"
}

allprojects {
    group = "com.gianluz.kite"  // Maven Central namespace (project-specific)
    version = "0.1.0-alpha2"

    repositories {
        mavenCentral()
    }
}

// Auto-install git hooks before any build
apply(from = "gradle/git-hooks.gradle.kts")

// Configure Maven Central publishing via the new Central Portal API
nmcpAggregation {
    centralPortal {
        username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrhUsername") as String? ?: ""
        password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrhPassword") as String? ?: ""
        publishingType = "AUTOMATIC"
    }
}

dependencies {
    nmcpAggregation(project(":kite-core"))
    nmcpAggregation(project(":kite-dsl"))
    nmcpAggregation(project(":kite-runtime"))
    nmcpAggregation(project(":kite-cli"))
    nmcpAggregation(project(":kite-plugins:git"))
    nmcpAggregation(project(":kite-plugins:gradle"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
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

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
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
                    description.set(project.description ?: "Kite - Modern CI/CD Workflow Runner")
                    url.set("https://github.com/gianluz/kite")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("gianluz")
                            name.set("Gianluca Zuddas")
                            email.set("gianluca.zuddas@gmail.com")
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

    configure<SigningExtension> {
        // Sign publications only if signing credentials are available
        val signingKey = System.getenv("SIGNING_KEY") ?: project.findProperty("signingKey") as String?
        val signingPassword = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signingPassword") as String?

        if (signingKey != null && signingPassword != null) {
            // Use in-memory key (for CI)
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(the<PublishingExtension>().publications["mavenJava"])
        } else {
            // Skip signing if no credentials (for local builds)
            isRequired = false
        }
    }
}
