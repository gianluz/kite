dependencies {
    // Depend on core and dsl
    api(project(":kite-core"))
    implementation(project(":kite-dsl"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Testing coroutines
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

description = "Task execution runtime for Kite"
