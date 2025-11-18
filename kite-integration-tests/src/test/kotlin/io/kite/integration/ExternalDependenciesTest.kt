package io.kite.integration

import org.junit.jupiter.api.Test

/**
 * Integration tests for @DependsOn external dependency resolution.
 */
class ExternalDependenciesTest : IntegrationTestBase() {
    @Test
    fun `resolve and use Gson via DependsOn annotation`() {
        createSegmentFile(
            "gson-test.kite.kts",
            """
            @file:DependsOn("com.google.code.gson:gson:2.10.1")
            
            import com.google.gson.Gson
            
            segments {
                segment("test-gson") {
                    execute {
                        val gson = Gson()
                        val json = gson.toJson(mapOf("status" to "success", "test" to "gson"))
                        println("JSON: " + json)
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "gson.kite.kts",
            """
            ride {
                name = "Gson Test"
                flow {
                    segment("test-gson")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Gson Test")

        result.assertSuccess()
        result.assertOutputContains("JSON:")
        result.assertOutputContains("status")
        result.assertOutputContains("success")
    }

    @Test
    fun `resolve and use Apache Commons Lang3 via DependsOn`() {
        createSegmentFile(
            "commons-test.kite.kts",
            """
            @file:DependsOn("org.apache.commons:commons-lang3:3.14.0")
            
            import org.apache.commons.lang3.StringUtils
            
            segments {
                segment("test-commons") {
                    execute {
                        val capitalized = StringUtils.capitalize("hello kite")
                        val reversed = StringUtils.reverse("!yvI htiw skrow")
                        println("Capitalized: " + capitalized)
                        println("Reversed: " + reversed)
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "commons.kite.kts",
            """
            ride {
                name = "Commons Test"
                flow {
                    segment("test-commons")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Commons Test")

        result.assertSuccess()
        result.assertOutputContains("Capitalized: Hello kite")
        result.assertOutputContains("Reversed: works with Ivy!")
    }

    @Test
    fun `resolve multiple dependencies in single segment`() {
        createSegmentFile(
            "multi-deps.kite.kts",
            """
            @file:DependsOn("com.google.code.gson:gson:2.10.1")
            @file:DependsOn("org.apache.commons:commons-lang3:3.14.0")
            
            import com.google.gson.Gson
            import org.apache.commons.lang3.StringUtils
            
            segments {
                segment("test-multi") {
                    execute {
                        val gson = Gson()
                        val message = StringUtils.capitalize("integration test")
                        val json = gson.toJson(mapOf("message" to message))
                        println("Result: " + json)
                    }
                }
            }
            """.trimIndent(),
        )

        createRideFile(
            "multi-deps.kite.kts",
            """
            ride {
                name = "Multi Deps"
                flow {
                    segment("test-multi")
                }
            }
            """.trimIndent(),
        )

        val result = executeRide("Multi Deps")

        result.assertSuccess()
        result.assertOutputContains("Result:")
        result.assertOutputContains("Integration test")
    }
}
