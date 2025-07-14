package com.ryandens.javaagent.utils

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

/**
 * Utility class for determining the version of a dependency in the javaagent configuration from other configurations
 */
object JavaagentVersionUtil {
    /**
     * Determines the version of a dependency in the javaagent configuration from another configuration based on the
     * requested [ModuleVersionSelector] and the provided [NamedDomainObjectProvider] of [Configuration]s to search.
     *
     * @param requested the requested dependency
     * @param configuration the configuration to search
     * @return the version of the dependency to use in the javaagent configuration
     */
    fun versionFromDependencyAndConfiguration(
        requested: ModuleVersionSelector,
        configuration: NamedDomainObjectProvider<Configuration>,
    ): String {
        val artifacts =
            configuration
                .get()
                .incoming
                .artifactView { viewConfiguration ->
                    viewConfiguration.componentFilter {
                        it.displayName.startsWith("${requested.group}:${requested.name}:")
                    }
                }.artifacts

        if (artifacts.count() == 0) {
            throw IllegalArgumentException(
                "No artifacts found for ${requested.group}:${requested.name} in configuration ${configuration.name}",
            )
        } else if (artifacts.count() > 1) {
            throw IllegalArgumentException(
                "Multiple artifacts found for ${requested.group}:${requested.name} in configuration ${configuration.name}",
            )
        }
        return (artifacts.single().id.componentIdentifier as ModuleComponentIdentifier).version
    }
}
