package com.ryandens.javaagent

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.application.CreateStartScripts

/**
 * Configures a project that leverages the [ApplicationPlugin] to easily integrate a javaagent into its tasks and outputs.
 */
class JavaagentApplicationPlugin : Plugin<Project>, JavaagentPlugin {
    override fun apply(project: Project) {
        failIfPluginNotApplied(project, ApplicationPlugin.APPLICATION_PLUGIN_NAME)
        val javaagentConfiguration = setupAndGetJavaagentConfiguration(project)
        project.pluginManager.apply(JavaagentDistributionPlugin::class.java)
        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.tasks.named(ApplicationPlugin.TASK_RUN_NAME, JavaExec::class.java).configure {
            it.jvmArgumentProviders.add {
                mutableListOf("-javaagent:${javaagentConfiguration.get().asPath}").plus(it.jvmArgs ?: mutableListOf())
            }
        }
        // get the relative path of the javaagent artifact in the distribution
        // configure the startScripts task to have the `javaagent` flag templated in to the defaultJvmOpts
        project.tasks.named(ApplicationPlugin.TASK_START_SCRIPTS_NAME, CreateStartScripts::class.java) {
            // the defaultJvmOpts String is escaped, so we can not directly specify the APP_HOME environment variable
            // here. Instead, when we put in a placeholder String so that when we generate the start script we can
            // unescape the defaultJvmOpts String and replace directly replace our placeholder with the environment
            // variable that corresponds to the distribution installation's home directory
            val agentRelativeDistributionPath = project.plugins.getPlugin(
                JavaagentDistributionPlugin::class.java
            ).getAgentRelativeDistributionPath()
            it.defaultJvmOpts =
                listOf(
                    "-javaagent:COM_RYANDENS_APP_HOME_ENV_VAR_PLACEHOLDER/$agentRelativeDistributionPath"
                ).plus(
                    it.defaultJvmOpts ?: listOf()
                )
            // custom start script generator
            it.unixStartScriptGenerator = JavaagentAwareStartScriptGenerator()
            // TODO build support for windows
            it.windowsStartScriptGenerator = WindowsStartScriptGenerator()
        }
    }
}
