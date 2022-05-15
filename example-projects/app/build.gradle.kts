plugins {
    id("com.ryandens.javaaagent.example.java-application-conventions")
    id("com.ryandens.javaagent-otel-modification")
    id("com.ryandens.javaagent-application")
}

dependencies {
  otel("io.opentelemetry.javaagent:opentelemetry-javaagent:1.12.0")
  otelExtension("io.opentelemetry.contrib:opentelemetry-samplers:1.12.0-alpha")
  otelInstrumentation(project(":custom-instrumentation", "shadow"))
}

application {
    // Define the main class for the application.
    mainClass.set("com.ryandens.javaaagent.example.App")
}

/*
  see https://github.com/johnrengelman/shadow/issues/713

  Currently, tasks that consume the output of the extendedAgent shadowJar task need to be made aware of
  the implicit dependency (https://docs.gradle.org/7.4.2/userguide/validation_problems.html#implicit_dependency)
  due to an issue with the shadowJar plugin
*/
setOf(tasks.distTar, tasks.distZip).forEach {
  it.configure {
    dependsOn(tasks.extendedAgent)
  }
}
