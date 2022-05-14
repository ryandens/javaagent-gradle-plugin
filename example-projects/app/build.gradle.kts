plugins {
    id("com.ryandens.javaaagent.example.java-application-conventions")
    id("com.ryandens.javaagent-otel")
}

dependencies {
  otel("io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")
}

application {
    // Define the main class for the application.
    mainClass.set("com.ryandens.javaaagent.example.App")
}
