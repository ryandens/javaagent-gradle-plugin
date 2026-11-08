plugins {
    id("com.ryandens.javaagent.example.java-application-conventions")
    id("com.ryandens.javaagent-otel-modification")
    id("com.ryandens.javaagent-application")
}

dependencies {
    otel(libs.otel.agent)
    otelExtension(libs.otel.google.exporter) {
        artifact {
            classifier = "shaded"
        }
    }
    otelInstrumentation(project(":custom-instrumentation")) {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
    }
}

application {
    // Define the main class for the application.
    mainClass.set("com.ryandens.javaagent.example.App")
    applicationDefaultJvmArgs = listOf("-Dotel.javaagent.debug=true", "-Dotel.metrics.exporter=none")
}

// Track the generated agent as an input so distributions retain its producer dependency.
setOf(tasks.distTar, tasks.distZip, tasks.startScripts).forEach {
    it.configure {
        inputs.file(tasks.extendedAgent.flatMap { it.archiveFile })
            .withPropertyName("extendedAgent")
            .withPathSensitivity(PathSensitivity.NONE)
    }
}
