ride {
    name = "Logging Test"

    flow {
        parallel {
            segment("log-test-1")
            segment("log-test-2")
            segment("log-test-3")
        }
    }
}
