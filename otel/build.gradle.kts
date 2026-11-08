import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.ryandens.plugin-conventions")
}

dependencies {
    implementation(project(":plugin"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.launcher)
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
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

tasks.functionalTest {
    useJUnitPlatform()
    inputs.files(
        layout.projectDirectory.file("../example-projects/settings.gradle.kts"),
        layout.projectDirectory.file("../example-projects/gradle.properties"),
        layout.projectDirectory.file("../example-projects/build-logic/settings.gradle.kts"),
    )
    inputs.file(layout.projectDirectory.file("../example-projects/build-logic/build.gradle.kts"))
    inputs.files(layout.projectDirectory.dir("../example-projects/build-logic/src/"))

    inputs.file(layout.projectDirectory.file("../example-projects/app/build.gradle.kts"))
    inputs.files(layout.projectDirectory.dir("../example-projects/app/src/"))

    inputs.file(layout.projectDirectory.file("../example-projects/custom-instrumentation/build.gradle.kts"))
    inputs.files(layout.projectDirectory.dir("../example-projects/custom-instrumentation/src/"))

    inputs.files(layout.projectDirectory.dir("../example-projects/gradle/"))
}
