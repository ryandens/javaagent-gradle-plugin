package com.ryandens.javaagent

import java.io.File
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.application.CreateStartScripts

/**
 * Gradle plugin for configuration of the [DistributionPlugin.MAIN_DISTRIBUTION_NAME] after it has been configured by
 * the [ApplicationPlugin] to include javaagents and configure the [ApplicationPlugin.TASK_START_SCRIPTS_NAME] to
 * automatically include the `-javaagent` flag when running the distribution.
 */
class JavaagentApplicationDistributionPlugin : Plugin<Project>, JavaagentPlugin {

    /**
     * Destination directory for dependencies as specified by the [ApplicationPlugin]
     */
    private val destinationDirectory = "lib"

    override fun dependentProjectPlugins(): Collection<Class<out Plugin<Project>>> {
        return setOf(ApplicationPlugin::class.java)
    }

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>
    ) {
        project.extensions.getByType(DistributionContainer::class.java).named(DistributionPlugin.MAIN_DISTRIBUTION_NAME)
            .configure { distribution ->
                distribution.contents { copy ->
                    copy.from(javaagentConfiguration) {
                        it.into(destinationDirectory)
                    }
                }
            }

        // get the relative path of the javaagent artifact in the distribution
        // configure the startScripts task to have the `javaagent` flag templated in to the defaultJvmOpts
        project.tasks.named(ApplicationPlugin.TASK_START_SCRIPTS_NAME, CreateStartScripts::class.java) {
            // the defaultJvmOpts String is escaped, so we can not directly specify the APP_HOME environment variable
            // here. Instead, when we put in a placeholder String so that when we generate the start script we can
            // unescape the defaultJvmOpts String and replace directly replace our placeholder with the environment
            // variable that corresponds to the distribution installation's home directory
            val agentRelativeDistributionPath = "$destinationDirectory/${File(javaagentConfiguration.get().asPath).name}"
            it.defaultJvmOpts =
                listOf(
                    "-javaagent:COM_RYANDENS_APP_HOME_ENV_VAR_PLACEHOLDER/$agentRelativeDistributionPath"
                ).plus(
                    it.defaultJvmOpts ?: listOf()
                )
            // custom start script generator that replaces the placeholder
            it.unixStartScriptGenerator = JavaagentAwareStartScriptGenerator()
            // TODO build support for windows
            it.windowsStartScriptGenerator = WindowsStartScriptGenerator()
        }
    }
}
