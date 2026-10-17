package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Creates javaagent configuration for usage in all plugins produced by this project
 */
class JavaagentBasePlugin : Plugin<Project> {
    companion object {
        const val CONFIGURATION_NAME = "javaagent"

        /** Creates an isolated configuration for a plugin, inheriting shared agent declarations. */
        fun targetConfiguration(
            project: Project,
            name: String,
            shared: NamedDomainObjectProvider<Configuration>,
        ): NamedDomainObjectProvider<Configuration> =
            project.configurations.register(name) {
                it.isTransitive = false
                it.isCanBeConsumed = false
                it.extendsFrom(shared.get())
            }
    }

    override fun apply(project: Project) {
        // Register configuration
        project.configurations.register(CONFIGURATION_NAME) {
            // we expect javaagents to come as shaded JARs
            it.isTransitive = false
        }
    }
}
