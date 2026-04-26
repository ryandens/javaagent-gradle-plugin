package com.ryandens.javaagent.jib

import com.google.cloud.tools.jib.gradle.JibExtension
import com.ryandens.javaagent.JavaagentBasePlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File

/**
 * Plugin for using the [JavaagentJibExtension] with Google's jib plugin
 *
 * We [Suppress] `unused` here, but the class is used by virtue of being published to Gradle Plugin portal
 */
@Suppress("unused")
class JavaagentJibPlugin : Plugin<Project> {
    @OptIn(ExperimentalStdlibApi::class)
    override fun apply(project: Project) {
        project.pluginManager.apply(JavaagentJibExtension::class.java)

        val javaagentConfiguration = project.configurations.named(JavaagentBasePlugin.CONFIGURATION_NAME)

        val destinationDirectory =
            project.tasks
                .named(
                    JavaagentJibExtension.COPY_AGENTS_TASK_NAME,
                    Copy::class.java,
                ).map { it.destinationDir }

        val jibExtension: JibExtension? = project.extensions.findByType(JibExtension::class.java)

        jibExtension?.pluginExtensions { extensionParametersSpec ->
            extensionParametersSpec.pluginExtension { extension ->
                extension.implementation = "com.ryandens.javaagent.jib.JavaagentJibExtension"
                extension.configuration(
                    Action<JibExtensionConfiguration> { extensionConfiguration ->
                        extensionConfiguration.javaagentFiles.set(
                            project.provider {
                                javaagentConfiguration
                                    .get()
                                    .files
                                    .map { File(destinationDirectory.get(), it.name) }
                                    .toList()
                            },
                        )
                    },
                )
            }
        }
    }
}
