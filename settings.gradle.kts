rootProject.name = "kite"

include(
    ":kite-core",
    ":kite-dsl",
    ":kite-runtime",
    ":kite-cli",
    ":kite-integration-tests",
    // Plugins
    ":kite-plugins:git",
    ":kite-plugins:gradle"
)

// Plugin projects
project(":kite-plugins:git").projectDir = file("kite-plugins/git")
project(":kite-plugins:gradle").projectDir = file("kite-plugins/gradle")
