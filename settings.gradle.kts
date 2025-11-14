rootProject.name = "kite"

include(
    ":kite-core",
    ":kite-dsl",
    ":kite-runtime",
    ":kite-cli"
)

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
