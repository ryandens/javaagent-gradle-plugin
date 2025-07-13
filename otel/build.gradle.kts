plugins {
    id("com.ryandens.plugin-conventions")
}

dependencies {
    implementation(project(":plugin"))
    testImplementation(platform("org.junit:junit-bom:5.13.3"))
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

tasks.functionalTest {
    useJUnitPlatform()
    inputs.file(layout.projectDirectory.file("../example-projects/buildSrc/build.gradle.kts"))
    inputs.files(layout.projectDirectory.dir("../example-projects/buildSrc/src/"))

    inputs.file(layout.projectDirectory.file("../example-projects/app/build.gradle.kts"))
    inputs.files(layout.projectDirectory.dir("../example-projects/app/src/"))

    inputs.file(layout.projectDirectory.file("../example-projects/custom-instrumentation/build.gradle.kts"))
    inputs.files(layout.projectDirectory.dir("../example-projects/custom-instrumentation/src/"))

    inputs.files(layout.projectDirectory.dir("../example-projects/gradle/"))
}
