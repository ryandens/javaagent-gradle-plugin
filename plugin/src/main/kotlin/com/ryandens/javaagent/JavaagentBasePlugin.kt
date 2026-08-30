package com.ryandens.javaagent

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Creates javaagent configuration for usage in all plugins produced by this project
 */
class JavaagentBasePlugin : Plugin<Project> {
    companion object {
        const val CONFIGURATION_NAME = "javaagent"
        const val EXTENSION_NAME = "javaagent"
    }

    override fun apply(project: Project) {
        // Register configuration
        project.configurations.register(CONFIGURATION_NAME) {
            // we expect javaagents to come as shaded JARs
            it.isTransitive = false
        }
        // Register the extension used to configure agent options. Created once here because the base plugin is
        // applied idempotently by JavaagentPlugin, so the `javaagent { }` block is available to every plugin
        // (application, distribution, test, jib). The extension name does not collide with the `javaagent`
        // configuration, which is reached via the `configurations` container.
        project.extensions.create(EXTENSION_NAME, JavaagentExtension::class.java)
    }
}
