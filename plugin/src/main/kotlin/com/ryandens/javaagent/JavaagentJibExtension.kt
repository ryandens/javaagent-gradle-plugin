package com.ryandens.javaagent

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FileEntry
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.api.buildplan.LayerObject
import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.extension.GradleData
import com.google.cloud.tools.jib.gradle.extension.JibGradlePluginExtension
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import java.io.File
import java.util.Optional

/**
 * A [JavaagentPlugin] that copies the javaagent into the build directory prior to any [com.google.cloud.tools.jib]
 * task. Then, A [JibGradlePluginExtension] that configures the [ContainerBuildPlan] to have an extra [FileEntriesLayer]
 * with the javaagent in it and a modified [ContainerBuildPlan.entrypoint] that specifies the `javaagent` flag.
 */
@ExperimentalStdlibApi
class JavaagentJibExtension :
    JibGradlePluginExtension<JibExtensionConfiguration>,
    JavaagentPlugin {
    override fun getExtraConfigType(): Optional<Class<JibExtensionConfiguration>> = Optional.of(JibExtensionConfiguration::class.java)

    override fun extendContainerBuildPlan(
        buildPlan: ContainerBuildPlan,
        properties: MutableMap<String, String>,
        extraConfig: Optional<JibExtensionConfiguration>,
        gradleData: GradleData?,
        logger: ExtensionLogger?,
    ): ContainerBuildPlan {
        val entrypoint = checkNotNull(buildPlan.entrypoint)
        check(entrypoint.isNotEmpty())
        if (extraConfig.isEmpty) {
            throw GradleException("Javaagent Jib plugin must be provided an extraConfig containing javaagent files to configure")
        }

        val localAgentPaths = extraConfig.get().javaagentFiles.get()

        val planBuilder = buildPlan.toBuilder()
        val newEntrypoint =
            buildList<String> {
                addAll(entrypoint)
                addAll(1, localAgentPaths.map { localAgentPath -> "-javaagent:/opt/jib-agents/${localAgentPath.name}" })
            }
        val javaagentFileEntries =
            localAgentPaths.map { localAgentPath ->
                FileEntry(
                    localAgentPath.toPath(),
                    AbsoluteUnixPath.get("/opt/jib-agents/${localAgentPath.name}"),
                    FilePermissions.DEFAULT_FILE_PERMISSIONS,
                    FileEntriesLayer.DEFAULT_MODIFICATION_TIME,
                )
            }
        val javaagentLayer =
            FileEntriesLayer
                .builder()
                .setName("javaagent")
                .setEntries(javaagentFileEntries)
                .build()
        val layers =
            buildList<LayerObject> {
                add(javaagentLayer)
                addAll(buildPlan.layers)
            }
        return planBuilder.setEntrypoint(newEntrypoint).setLayers(layers).build()
    }

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        val destinationDirectory = project.layout.buildDirectory.dir("jib-agents")
        val copyAgents =
            project.tasks.register("copyAgentsToJibDir", Copy::class.java) {
                it.from(javaagentConfiguration)
                it.into(destinationDirectory)
            }

        listOf("jib", "jibDockerBuild", "jibBuildTar").forEach { jibTaskName ->
            project.tasks.named(jibTaskName) { jibTask ->
                jibTask.dependsOn(copyAgents)
            }
        }

        val jibExtension: JibExtension? = project.extensions.findByType(JibExtension::class.java)

        jibExtension?.pluginExtensions { extensionParametersSpec ->
            extensionParametersSpec.pluginExtension { extension ->
                extension.implementation = "com.ryandens.javaagent.JavaagentJibExtension"
                extension.configuration(
                    Action<JibExtensionConfiguration> { extensionConfiguration ->
                        extensionConfiguration.javaagentFiles.set(
                            project.provider {
                                javaagentConfiguration.get().files.map { File(destinationDirectory.get().asFile, it.name) }.toList()
                            },
                        )
                    },
                )
            }
        }
    }
}
