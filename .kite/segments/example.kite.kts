segments {
    segment("hello") {
        description = "Say hello"
        execute {
            println("Hello from Kite")
        }
    }
    
    segment("world") {
        description = "Say world"
        dependsOn("hello")
        execute {
            println("World")
        }
    }
}
