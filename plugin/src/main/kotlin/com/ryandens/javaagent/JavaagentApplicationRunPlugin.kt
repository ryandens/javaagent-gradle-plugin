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
class JavaagentApplicationRunPlugin : Plugin<Project>, JavaagentPlugin {

    override fun dependentProjectPlugins(): Collection<Class<out Plugin<Project>>> {
        return setOf(ApplicationPlugin::class.java)
    }

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>
    ) {
        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.tasks.named(ApplicationPlugin.TASK_RUN_NAME, JavaExec::class.java).configure {
            it.jvmArgumentProviders.add {
                mutableListOf(
                    *javaagentConfiguration.get().asPath.split(":")
                        .map { javaagentJarPath -> "-javaagent:$javaagentJarPath" }.toTypedArray()
                ).plus(it.jvmArgs ?: mutableListOf())
            }
        }
    }
}
