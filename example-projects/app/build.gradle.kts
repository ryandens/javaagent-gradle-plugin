plugins {
    id("com.ryandens.javaagent.example.java-application-conventions")
    id("com.ryandens.javaagent-otel-modification")
    id("com.ryandens.javaagent-application")
}

dependencies {
  otel("io.opentelemetry.javaagent:opentelemetry-javaagent:2.8.0")
  otelExtension("io.opentelemetry.contrib:opentelemetry-samplers:1.32.0-alpha")
  otelInstrumentation(project(":custom-instrumentation", "shadow"))
}

application {
    // Define the main class for the application.
    mainClass.set("com.ryandens.javaagent.example.App")
    applicationDefaultJvmArgs = listOf("-Dotel.javaagent.debug=true", "-Dotel.metrics.exporter=none")
}

/*
  see https://github.com/johnrengelman/shadow/issues/713

  Currently, tasks that consume the output of the extendedAgent shadowJar task need to be made aware of
  the implicit dependency (https://docs.gradle.org/7.4.2/userguide/validation_problems.html#implicit_dependency)
  due to an issue with the shadowJar plugin
*/
setOf(tasks.distTar, tasks.distZip, tasks.startScripts).forEach {
  it.configure {
    dependsOn(tasks.extendedAgent)
  }
}
