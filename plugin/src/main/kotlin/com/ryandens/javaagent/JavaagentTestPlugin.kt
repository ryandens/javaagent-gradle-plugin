package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

class JavaagentTestPlugin : Plugin<Project>, JavaagentPlugin {
    override fun dependentProjectPlugins(): Collection<Class<out Plugin<Project>>> {
        return setOf(JavaPlugin::class.java)
    }

    override fun applyAfterJavaagentSetup(project: Project, javaagentConfiguration: NamedDomainObjectProvider<Configuration>) {
        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.tasks.named(JavaPlugin.TEST_TASK_NAME, Test::class.java).configure {
            JavaForkOptionsConfigurer.configureJavaForkOptions(it, javaagentConfiguration)
        }
    }
}
