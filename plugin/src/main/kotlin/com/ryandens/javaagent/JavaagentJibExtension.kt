package com.ryandens.javaagent

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FileEntry
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.gradle.extension.GradleData
import com.google.cloud.tools.jib.gradle.extension.JibGradlePluginExtension
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger
import java.io.File
import java.nio.file.Paths
import java.util.Optional
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy

/**
 * A [JavaagentPlugin] that copies the javaagent into the build directory prior to any [com.google.cloud.tools.jib]
 * task. Then, A [JibGradlePluginExtension] that configures the [ContainerBuildPlan] to have an extra [FileEntriesLayer]
 * with the javaagent in it and a modified [ContainerBuildPlan.entrypoint] that specifies the `javaagent` flag.
 */
class JavaagentJibExtension : JibGradlePluginExtension<Void>, JavaagentPlugin {

    override fun getExtraConfigType(): Optional<Class<Void>> {
        return Optional.empty()
    }

    @ExperimentalStdlibApi
    override fun extendContainerBuildPlan(
        buildPlan: ContainerBuildPlan?,
        properties: MutableMap<String, String>?,
        extraConfig: Optional<Void>?,
        gradleData: GradleData?,
        logger: ExtensionLogger?
    ): ContainerBuildPlan {
        checkNotNull(buildPlan)
        val entrypoint = checkNotNull(buildPlan.entrypoint)
        check(entrypoint.isNotEmpty())

        val localAgentPath = checkNotNull(gradleData?.project?.plugins?.getPlugin(
            JavaagentJibExtension::class.java
        )?.javaagentPathProvider?.invoke())

        val agentFileName = File(localAgentPath).name

        val planBuilder = buildPlan.toBuilder()
        val newEntrypoint = buildList<String> {
            addAll(entrypoint)
            add(1, "-javaagent:/opt/jib-agents/$agentFileName")
        }

        val javaagentFileEntries = buildList {
            add(
                FileEntry(
                    Paths.get(localAgentPath),
                    AbsoluteUnixPath.get("/opt/jib-agents/$agentFileName"),
                    FilePermissions.DEFAULT_FILE_PERMISSIONS,
                    FileEntriesLayer.DEFAULT_MODIFICATION_TIME
                )
            )
        }
        val javaagentLayer = FileEntriesLayer.builder().setName("javaagent").setEntries(javaagentFileEntries).build()
        return planBuilder.setEntrypoint(newEntrypoint).addLayer(javaagentLayer).build()
    }

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>
    ) {
        val destinationDirectory = "${project.buildDir}/jib-agents/"
        val copyAgents = project.tasks.register("copyAgentsToJibDir", Copy::class.java) {
            it.from(javaagentConfiguration)
            it.into(destinationDirectory)
        }

        listOf("jib", "jibDockerBuild", "jibBuildTar").forEach { jibTaskName ->
            project.tasks.named(jibTaskName) { jibTask ->
                jibTask.dependsOn(copyAgents)
            }
        }

        javaagentPathProvider = {
            "$destinationDirectory/${File(javaagentConfiguration.get().asPath).name}"
        }
    }

    private lateinit var javaagentPathProvider: () -> String
}
