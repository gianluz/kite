segments {
    segment("logging-detail") {
        description = "Test detailed logging with command output"
        execute {
            println("Before command")
            exec("echo", "Hello from echo command")
            println("After command")
            exec("ls", "-la", ".kite")
            println("Done")
        }
    }
}
