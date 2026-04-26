package com.ryandens.javaagent.jib

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FileEntry
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.api.buildplan.LayerObject
import com.google.cloud.tools.jib.gradle.extension.GradleData
import com.google.cloud.tools.jib.gradle.extension.JibGradlePluginExtension
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger
import com.ryandens.javaagent.JavaagentPlugin
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
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
    companion object {
        const val COPY_AGENTS_TASK_NAME = "copyAgentsToJibDir"
    }

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
            project.tasks.register(COPY_AGENTS_TASK_NAME, Copy::class.java) {
                it.from(javaagentConfiguration)
                it.into(destinationDirectory)
            }

        if (project.pluginManager.hasPlugin("com.google.cloud.tools.jib")) {
            listOf("jib", "jibDockerBuild", "jibBuildTar").forEach { jibTaskName ->
                project.tasks.named(jibTaskName) { jibTask ->
                    jibTask.dependsOn(copyAgents)
                }
            }
        } else if (project.pluginManager.hasPlugin("tel.schich.tinyjib")) {
            listOf("tinyJibPublish", "tinyJibDocker", "tinyJibTar").forEach { jibTaskName ->
                project.tasks.named(jibTaskName) { jibTask ->
                    jibTask.dependsOn(copyAgents)
                }
            }
        } else {
            throw IllegalStateException("Should not be possible")
        }
    }
}
