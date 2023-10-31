plugins {
    id("com.ryandens.plugin-conventions")
}

dependencies {
    implementation(project(":plugin"))
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("JavaagentOTelModificationPlugin") {
            id = "com.ryandens.javaagent-otel-modification"
            displayName = "OpenTelemetry Javaagent Plugin"
            description =
                """
                Enables easy modification of OpenTelemetry javaagent distributions with 3rd party extensions or auto-instrumentation modules
                """.trimIndent()
            implementationClass = "com.ryandens.javaagent.otel.JavaagentOTelModificationPlugin"
            tags.set(listOf("otel", "instrumentation", "observability"))
        }
    }
}

tasks.named<Test>("functionalTest") {
    useJUnitPlatform()
}
