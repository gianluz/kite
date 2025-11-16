// Test segments to demonstrate parallel logging

segments {
    segment("log-test-1") {
        description = "First logging test segment"
        execute {
            println("Starting log-test-1")
            exec("echo", "Output from log-test-1")
            println("Finishing log-test-1")
        }
    }

    segment("log-test-2") {
        description = "Second logging test segment"
        execute {
            println("Starting log-test-2")
            exec("echo", "Output from log-test-2")
            println("Finishing log-test-2")
        }
    }

    segment("log-test-3") {
        description = "Third logging test segment"
        execute {
            println("Starting log-test-3")
            exec("echo", "Output from log-test-3")
            println("Finishing log-test-3")
        }
    }
}
