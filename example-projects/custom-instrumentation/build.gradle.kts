plugins {
  id("com.ryandens.javaagent.example.java-library-conventions")
  id("io.opentelemetry.instrumentation.muzzle-check")
  id("io.opentelemetry.instrumentation.muzzle-generation")
  id("com.gradleup.shadow")
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
  val autoServiceVersion = "1.1.1"
  compileOnly("com.google.auto.service:auto-service-annotations:$autoServiceVersion")
  annotationProcessor("com.google.auto.service:auto-service:$autoServiceVersion")
  val otelInstrumentationVersion = "2.10.0"
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:$otelInstrumentationVersion")
  muzzleBootstrap("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-annotation-support:$otelInstrumentationVersion")
  muzzleBootstrap( "io.opentelemetry.instrumentation:opentelemetry-instrumentation-appender-api-internal:$otelInstrumentationVersion")
  muzzleTooling("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationVersion-alpha")
  muzzleTooling("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersion-alpha")
  // for some reason, when pulling this version value from the platform bom, a byte buddy task can't be created
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationVersion-alpha")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersion-alpha")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")


  /*
    Dependencies added to this configuration will be found by the muzzle gradle plugin during code
    generation phase. These classes become part of the code that plugin inspects and traverses during
    references collection phase.
   */
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationVersion-alpha")
}
