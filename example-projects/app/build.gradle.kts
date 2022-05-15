plugins {
    id("com.ryandens.javaaagent.example.java-application-conventions")
    id("com.ryandens.javaagent-otel")
    id("com.ryandens.javaagent-application")
}

dependencies {
  otel("io.opentelemetry.javaagent:opentelemetry-javaagent:1.12.0")
  javaagent(files("$buildDir/agents/extended-opentelemetry-javaagent.jar"))
  otelExtension("io.opentelemetry.contrib:opentelemetry-samplers:1.12.0-alpha")
  otelInstrumentation(project(":custom-instrumentation"))
}

application {
    // Define the main class for the application.
    mainClass.set("com.ryandens.javaaagent.example.App")
}
