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
        val extension = project.extensions.getByType(JavaagentExtension::class.java)
        val optionsByFileName =
            AgentOptionsResolver.optionsByFileName(javaagentConfiguration.get(), extension.agentOptions)
        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.tasks.named(ApplicationPlugin.TASK_RUN_NAME, JavaExec::class.java).configure {
            // The agent jars are passed to the JVM via a CommandLineArgumentProvider (see
            // JavaForkOptionsConfigurer), which resolves the configuration's files at execution time but is
            // not a tracked task input. Without registering the configuration as an input, Gradle
            // establishes no ordering between the run task and the tasks that build the agent jars, so under
            // parallel execution the run task can launch with -javaagent:<jar> before that jar has been
            // produced, failing with "Error opening zip file or JAR manifest missing".
            //
            // A Configuration is Buildable, so registering it as an input makes the run task depend on the
            // tasks that build the agent jars, the same way JavaagentApplicationDistributionPlugin does for
            // the start-script task. This requires every artifact added to the javaagent configuration to
            // carry its producing-task dependency (builtBy); consumers that synthesize a plain File must
            // preserve it (see the otel example's extendedAgent wiring).
            it.inputs.files(javaagentConfiguration)
            it.inputs.property("agentOptions", extension.agentOptions)
            JavaForkOptionsConfigurer.configureJavaForkOptions(
                it,
                javaagentConfiguration.map { configuration -> configuration.files },
                optionsByFileName,
            )
        }
    }
}
