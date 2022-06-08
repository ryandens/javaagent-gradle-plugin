plugins {
  id("com.ryandens.javaagent.example.java-library-conventions")
  id("io.opentelemetry.instrumentation.muzzle-check") version "1.13.1-alpha"
  id("io.opentelemetry.instrumentation.muzzle-generation") version "1.13.1-alpha"
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

tasks.shadowJar {
  relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
  relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
  relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")
}

tasks.assemble {
  dependsOn(tasks.shadowJar)
}

tasks.jar {
  enabled = false
}

dependencies {
  compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")
  annotationProcessor("com.google.auto.service:auto-service:1.0.1")
  val otelInstrumentationVersion = "1.13.1-alpha"
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:$otelInstrumentationVersion")
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support:$otelInstrumentationVersion")
  muzzleBootstrap( "io.opentelemetry.instrumentation:opentelemetry-instrumentation-appender-api-internal:$otelInstrumentationVersion")
  muzzleTooling("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationVersion")
  muzzleTooling("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersion")
  // for some reason, when pulling this version value from the platform bom, a byte buddy task can't be created
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationVersion")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersion")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")


  /*
    Dependencies added to this configuration will be found by the muzzle gradle plugin during code
    generation phase. These classes become part of the code that plugin inspects and traverses during
    references collection phase.
   */
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersion")
}