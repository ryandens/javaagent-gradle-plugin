package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.JavaExec

/**
 * Configures a project that leverages the [ApplicationPlugin] to easily integrate a javaagent into its tasks and outputs.
 */
class JavaagentApplicationRunPlugin :
    Plugin<Project>,
    JavaagentPlugin {
    override fun dependentProjectPlugins(): Collection<String> = setOf("application")

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.tasks.named(ApplicationPlugin.TASK_RUN_NAME, JavaExec::class.java).configure {
            // Register the configuration as an input so the run task depends on the tasks that build the
            // javaagent artifacts (a Configuration is Buildable). Without this the agent jar can be launched
            // via -javaagent before it has been produced, failing with "Error opening zip file".
            it.inputs.files(javaagentConfiguration)
            JavaForkOptionsConfigurer.configureJavaForkOptions(it, javaagentConfiguration.map { configuration -> configuration.files })
        }
    }
}
