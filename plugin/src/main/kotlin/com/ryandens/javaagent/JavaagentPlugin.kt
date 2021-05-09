package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ApplicationPlugin

/**
 * Mixin for [org.gradle.api.Plugin]s that rely on the [JavaagentBasePlugin]
 */
interface JavaagentPlugin {

    /**
     * Applies the [JavaagentBasePlugin] and returns the javaagent [Configuration] in [NamedDomainObjectProvider] for
     * use by implementing [org.gradle.api.Plugin]s
     */
    fun setupAndGetJavaagentConfiguration(project: Project): NamedDomainObjectProvider<Configuration> {
        // apply base plugin
        project.pluginManager.apply(JavaagentBasePlugin::class.java)
        // get configuration
        return project.configurations.named(JavaagentBasePlugin.CONFIGURATION_NAME)
    }

    fun failIfPluginNotApplied(project : Project, pluginId: String) {
        if (!project.pluginManager.hasPlugin(pluginId)) {
            throw IllegalStateException("In order to use this plugin, the $pluginId plugin must be applied")
        }
    }
}