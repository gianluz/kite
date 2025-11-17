dependencies {
    // Depend on core
    api(project(":kite-core"))

    // Kotlin scripting
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime")

    // IDE Support: These dependencies are REQUIRED for IntelliJ to load script definitions
    // with @DependsOn/@Repository support. They must be 'implementation' not 'compileOnly'
    // because MavenDependenciesResolver needs them on the classpath when IntelliJ loads
    // the script definition. Without them, you get: NoClassDefFoundError: com/google/inject/Provider
    //
    // The full dependency chain is:
    // kotlin-scripting-dependencies-maven -> maven-core -> plexus-container -> sisu -> guice
    // But Guice is marked as 'provided' in Sisu, so we must explicitly include it
    implementation("com.google.inject:guice:4.2.2")
    implementation("org.eclipse.sisu:org.eclipse.sisu.inject:0.3.5")
    implementation("org.codehaus.plexus:plexus-component-annotations:2.1.1")
    implementation("javax.inject:javax.inject:1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Test dependencies (also available to scripts via classpath)
    implementation("com.google.code.gson:gson:2.10.1") // For testing script dependencies

    // Test dependencies
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

description = "DSL and scripting engine for Kite"
