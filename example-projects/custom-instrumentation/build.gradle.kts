plugins {
    id("com.ryandens.javaagent.example.instrumentation-conventions")
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
    compileOnly(libs.auto.service.annotations)
    annotationProcessor(libs.auto.service.processor)
    muzzleBootstrap(libs.otel.instrumentation.api)
    muzzleBootstrap(libs.otel.instrumentation.semconv)
    muzzleBootstrap(libs.otel.instrumentation.annotations)
    muzzleBootstrap(libs.otel.instrumentation.appender)
    muzzleTooling(libs.otel.extension.api)
    muzzleTooling(libs.otel.tooling)
    // Explicit versions are supplied by the catalog: the tooling cannot use BOM constraints here.
    compileOnly(libs.otel.extension.api)
    compileOnly(libs.otel.tooling)
    compileOnly(libs.otel.instrumentation.api)
    // Muzzle traverses these classes during reference collection.
    add("codegen", libs.otel.tooling)
}
