package com.ryandens.javaagent

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FileEntry
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.api.buildplan.LayerObject
import com.google.cloud.tools.jib.gradle.BuildDockerTask
import com.google.cloud.tools.jib.gradle.BuildImageTask
import com.google.cloud.tools.jib.gradle.BuildTarTask
import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.extension.GradleData
import com.google.cloud.tools.jib.gradle.extension.JibGradlePluginExtension
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger
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
class JavaagentJibExtension : JibGradlePluginExtension<Void>, JavaagentPlugin {

    override fun getExtraConfigType(): Optional<Class<Void>> {
        return Optional.empty()
    }

    override fun extendContainerBuildPlan(
        buildPlan: ContainerBuildPlan?,
        properties: MutableMap<String, String>?,
        extraConfig: Optional<Void>?,
        gradleData: GradleData?,
        logger: ExtensionLogger?,
    ): ContainerBuildPlan {
        checkNotNull(buildPlan)
        val entrypoint = checkNotNull(buildPlan.entrypoint)
        check(entrypoint.isNotEmpty())

        val localAgentPaths = checkNotNull(
            gradleData?.project?.plugins?.getPlugin(
                JavaagentJibExtension::class.java,
            )?.javaagentPathProvider?.invoke(),
        )

        val planBuilder = buildPlan.toBuilder()
        val newEntrypoint = buildList<String> {
            addAll(entrypoint)
            addAll(1, localAgentPaths.map { localAgentPath -> "-javaagent:/opt/jib-agents/${localAgentPath.name}" })
        }
        val javaagentFileEntries = localAgentPaths.map { localAgentPath ->
            FileEntry(
                localAgentPath.toPath(),
                AbsoluteUnixPath.get("/opt/jib-agents/${localAgentPath.name}"),
                FilePermissions.DEFAULT_FILE_PERMISSIONS,
                FileEntriesLayer.DEFAULT_MODIFICATION_TIME,
            )
        }
        val javaagentLayer = FileEntriesLayer.builder().setName("javaagent").setEntries(javaagentFileEntries).build()
        val layers = buildList<LayerObject> {
            add(javaagentLayer)
            addAll(buildPlan.layers)
        }
        return planBuilder.setEntrypoint(newEntrypoint).setLayers(layers).build()
    }

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        val destinationDirectory = File("${project.buildDir}/jib-agents/")
        val copyAgents = project.tasks.register("copyAgentsToJibDir", Copy::class.java) {
            it.from(javaagentConfiguration)
            it.into(destinationDirectory)
        }

        listOf(
            BuildDockerTask::class.java,
            BuildImageTask::class.java,
            BuildTarTask::class.java
        ).forEach { jibTaskType ->
            project.tasks.withType(jibTaskType).configureEach { jibTask ->
                run {
                    println(jibTask)
                    jibTask.dependsOn(copyAgents)
                }
            }
        }

        val jibExtension: JibExtension? = project.extensions.findByType(JibExtension::class.java)

        jibExtension?.pluginExtensions { extensionParametersSpec ->
            extensionParametersSpec.pluginExtension {
                it.implementation = "com.ryandens.javaagent.JavaagentJibExtension"
            }
        }

        javaagentPathProvider = {
            javaagentConfiguration.get().files.map { File(destinationDirectory, it.name) }
        }
    }

    private lateinit var javaagentPathProvider: () -> List<File>
}
