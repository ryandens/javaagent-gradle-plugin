package com.ryandens.javaagent.otel

import com.ryandens.javaagent.JavaagentBasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import java.io.ByteArrayOutputStream

/**
 * Enables easy consumption of external extensions and instrumentation libraries by creating a new jar with extra
 * classes and jars included.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/#creating-agent-extensions">creating-agent-extensions<a/>
 * @see <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/writing-instrumentation.md">writing-instrumentation</a>
 *
 */
@Suppress("unused")
class JavaagentOTelModificationPlugin : Plugin<Project> {
    companion object {
        const val AGENT_CONFIGURATION_NAME = "otel"
        const val EXTENSION_CONFIGURATION_NAME = "otelExtension"
        const val INSTRUMENTATION_CONFIGURATION_NAME = "otelInstrumentation"
    }

    override fun apply(project: Project) {
        val otel =
            project.configurations.register(AGENT_CONFIGURATION_NAME) {
                it.isTransitive = false
            }
        val otelExtension =
            project.configurations.register(EXTENSION_CONFIGURATION_NAME) {
                it.isTransitive = false
            }

        val otelInstrumentation =
            project.configurations.register(INSTRUMENTATION_CONFIGURATION_NAME) {
                it.isTransitive = false
            }

        project.plugins.apply(JavaagentBasePlugin::class.java)
        val extendedAgent =
            project.tasks.register("extendedAgent", Jar::class.java) { jar ->
                jar.inputs.files(otelInstrumentation)
                jar.archiveFileName.set("extended-opentelemetry-javaagent.jar")
                jar.destinationDirectory.set(project.layout.buildDirectory.dir("agents"))
                jar.from(otel.map { project.zipTree(it.singleFile) })
                jar.from(otelExtension.map { it.singleFile }) {
                    it.into("extensions")
                }
                jar.manifest {
                    it.attributes["Main-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
                    it.attributes["Agent-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
                    it.attributes["Premain-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
                    it.attributes["Can-Redefine-Classes"] = "true"
                    it.attributes["Can-Retransform-Classes"] = "true"
                    it.attributes["Implementation-Vendor"] = "Custom"
                    it.attributes["Implementation-Version"] = "custom-${project.version}-otel"
                }
                jar.from(
                    otelInstrumentation.map { resolvedOtelInstrumentation ->
                        resolvedOtelInstrumentation.files.map { instrumentationJar ->
                            project.zipTree(instrumentationJar)
                        }
                    },
                ) {
                    it.into("inst")
                    it.exclude("META-INF/MANIFEST.MF")
                    it.rename("(^.*)\\.class\$", "\$1.classdata")
                    it.duplicatesStrategy = DuplicatesStrategy.INCLUDE
                    it.eachFile { fileCopyDetails ->
                        if (fileCopyDetails.name.startsWith("META-INF/services")) {
                            val existingFile = fileCopyDetails.file
                            if (existingFile.exists()) {
                                // Append content from the existing file to the file being copied
                                val existingContent = existingFile.readText()
                                val newContent = ByteArrayOutputStream()
                                fileCopyDetails.copyTo(newContent)
                                newContent.write(System.lineSeparator().toByteArray())
                                newContent.write(existingContent.toByteArray())
                                fileCopyDetails.file.writeBytes(newContent.toByteArray())
                            }
                        }
                    }
                }
            }
        project.dependencies.add("javaagent", extendedAgent.map { project.files(it.outputs.files.singleFile) })
    }
}
