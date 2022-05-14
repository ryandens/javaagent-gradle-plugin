package com.ryandens.javaagent.otel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import java.io.File

/**
 * Enables easy consumption of external extensions and instrumentation libraries by creating a new jar with extra
 * classes and jars included.
 */
class OTelJavaagentPlugin : Plugin<Project> {
    companion object {
        const val AGENT_CONFIGURATION_NAME = "otel"
        const val EXTENSION_CONFIGURATION_NAME = "otelExtension"
        const val INSTRUMENTATION_CONFIGURATION_NAME = "otelInstrumentation"
    }

    override fun apply(project: Project) {
        val otel = project.configurations.register(AGENT_CONFIGURATION_NAME) {
            it.isTransitive = false
        }
        val otelExtension = project.configurations.register(EXTENSION_CONFIGURATION_NAME) {
            it.isTransitive = false
        }

        val otelInstrumentation = project.configurations.register(INSTRUMENTATION_CONFIGURATION_NAME) {
            it.isTransitive = false
        }

        project.tasks.register("extendedAgent", Jar::class.java) { jar ->
            jar.archiveFileName.set("extended-opentelemetry-javaagent.jar")
            jar.destinationDirectory.set(File(project.buildDir, "agents"))
            jar.from(project.zipTree(otel.get().singleFile))
            jar.from(otelExtension) {
                it.into("extensions")
            }
            jar.from(otelInstrumentation) {
                it.into("inst")
            }
        }
    }
}
