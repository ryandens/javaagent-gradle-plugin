package com.ryandens.javaagent.otel

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.ryandens.javaagent.JavaagentBasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
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

        project.plugins.apply("com.github.johnrengelman.shadow")
        project.plugins.apply(JavaagentBasePlugin::class.java)
        val extendedAgent = project.tasks.register("extendedAgent", ShadowJar::class.java) { jar ->
            jar.inputs.files(otelInstrumentation)
            jar.archiveFileName.set("extended-opentelemetry-javaagent.jar")
            jar.destinationDirectory.set(File(project.buildDir, "agents"))
            val resolvedOtelConfiguration = otel.get()
            jar.from(project.zipTree(resolvedOtelConfiguration.singleFile))
            jar.from(otelExtension) {
                it.into("extensions")
            }
            jar.mergeServiceFiles()
            jar.manifest {
                it.attributes["Main-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
                it.attributes["Agent-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
                it.attributes["Premain-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
                it.attributes["Can-Redefine-Classes"] = "true"
                it.attributes["Can-Retransform-Classes"] = "true"
                it.attributes["Implementation-Vendor"] = "Custom"
                it.attributes["Implementation-Version"] = "custom-${project.version}-otel-${resolvedOtelConfiguration.dependencies.first().version}"
            }
            otelInstrumentation.get().files.forEach { instrumentationJar ->
                jar.from(project.zipTree(instrumentationJar)) {
                    it.into("inst")
                    it.exclude("META-INF/MANIFEST.MF")
                    it.rename("(^.*)\\.class\$", "\$1.classdata")
                }
            }
        }
        project.dependencies.add("javaagent", extendedAgent.map { project.files(it.outputs.files.singleFile) })
    }
}
