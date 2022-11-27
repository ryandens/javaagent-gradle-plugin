plugins {
    id("com.ryandens.plugin-conventions")
}

pluginBundle {
    tags = listOf("otel", "instrumentation", "observability")
}

dependencies {
    implementation(project(":plugin"))
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

gradlePlugin {
    plugins {
        create("JavaagentOTelModificationPlugin") {
            id = "com.ryandens.javaagent-otel-modification"
            displayName = "OpenTelemetry Javaagent Plugin"
            description =
                "Enables easy modification of OpenTelemetry javaagent distributions with 3rd party extensions or auto-instrumentation modules"
            implementationClass = "com.ryandens.javaagent.otel.JavaagentOTelModificationPlugin"
        }
    }
}

tasks.named<Test>("functionalTest") {
    useJUnitPlatform()
}
